package org.modelica.compiler.semantic

import org.modelica.compiler.ast.nodes.*
import org.modelica.compiler.ast.visitor.DefaultVisitor
import org.modelica.compiler.lexer.SourceLocation
import org.modelica.compiler.semantic.types.*

/**
 * 语义分析错误
 */
data class SemanticError(
    val message: String,
    val location: SourceLocation?,
    val errorType: SemanticErrorType = SemanticErrorType.UNKNOWN
) {
    override fun toString(): String {
        val loc = location?.let { " at $it" } ?: ""
        return "SemanticError$loc: $message"
    }
}

/**
 * 语义错误类型
 */
enum class SemanticErrorType {
    UNKNOWN,
    UNDEFINED_VARIABLE,
    UNDEFINED_TYPE,
    UNDEFINED_FUNCTION,
    REDEFINED_SYMBOL,
    TYPE_MISMATCH,
    ARGUMENT_COUNT_MISMATCH,
    NOT_A_FUNCTION,
    NOT_A_TYPE,
    INVALID_OPERATION,
    INVALID_ASSIGNMENT,
    MISSING_RETURN,
    CIRCULAR_DEPENDENCY
}

/**
 * 语义分析结果
 */
data class SemanticResult(
    val success: Boolean,
    val errors: List<SemanticError>,
    val symbolTable: SymbolTable
)

/**
 * 语义分析器
 *
 * 执行类型检查、作用域分析和符号解析
 */
class SemanticAnalyzer : DefaultVisitor<Unit>(Unit) {

    private val symbolTable = SymbolTable()
    private val errors = mutableListOf<SemanticError>()
    private val typeChecker = TypeChecker()

    /** 当前表达式类型（用于类型推断） */
    private var currentExpressionType: ModelicaType = AnyType

    /**
     * 分析程序
     */
    fun analyze(program: Program): SemanticResult {
        // 第一遍：收集所有类定义
        program.classes.forEach { classDef ->
            symbolTable.registerClass(classDef.name, classDef)
            symbolTable.define(Symbol.Class(
                name = classDef.name,
                type = UserType(classDef.name, classDef.name),
                location = classDef.location,
                definition = classDef
            ))
        }

        // 第二遍：分析每个类
        program.classes.forEach { classDef ->
            analyzeClass(classDef)
        }

        return SemanticResult(
            success = errors.isEmpty(),
            errors = errors.toList(),
            symbolTable = symbolTable
        )
    }

    /**
     * 分析类定义
     */
    private fun analyzeClass(classDef: ClassDefinition) {
        symbolTable.enterScope(ScopeType.CLASS, classDef.name)

        // 分析继承
        classDef.composition.elements
            .filterIsInstance<ExtendsClause>()
            .forEach { analyzeExtends(it) }

        // 分析元素
        classDef.composition.elements.forEach { element ->
            when (element) {
                is ImportClause -> analyzeImport(element)
                is ComponentDeclaration -> analyzeComponentDeclaration(element)
                else -> {}
            }
        }

        // 分析方程
        classDef.composition.equations.forEach { analyzeEquation(it) }

        // 分析算法
        classDef.composition.algorithms.forEach { analyzeAlgorithmSection(it) }

        symbolTable.exitScope()
    }

    /**
     * 分析继承子句
     */
    private fun analyzeExtends(extends: ExtendsClause) {
        val baseClass = symbolTable.getClass(extends.baseClass)
        if (baseClass == null) {
            errors.add(SemanticError(
                "Undefined base class: ${extends.baseClass}",
                extends.location,
                SemanticErrorType.UNDEFINED_TYPE
            ))
        }
    }

    /**
     * 分析导入子句
     */
    private fun analyzeImport(import: ImportClause) {
        // 简化实现：记录导入
    }

    /**
     * 分析组件声明
     */
    private fun analyzeComponentDeclaration(decl: ComponentDeclaration) {
        val type = resolveType(decl.type)

        decl.components.forEach { component ->
            val componentType = if (component.dimensions.isNotEmpty()) {
                // 数组类型
                val dims = component.dimensions.map {
                    when (it) {
                        is ArrayDimension.Known -> {
                            analyzeExpression(it.size)
                            0 // 简化：需要计算维度大小
                        }
                        ArrayDimension.Unknown -> 0
                    }
                }
                ArrayType(type, dims)
            } else {
                type
            }

            val symbol = when {
                decl.prefixes.parameter -> Symbol.Parameter(
                    name = component.name,
                    type = componentType,
                    location = decl.location
                )
                decl.prefixes.constant -> Symbol.Constant(
                    name = component.name,
                    type = componentType,
                    location = decl.location
                )
                else -> Symbol.Variable(
                    name = component.name,
                    type = componentType,
                    location = decl.location,
                    isInput = decl.prefixes.input,
                    isOutput = decl.prefixes.output,
                    isFlow = decl.prefixes.flow,
                    isStream = decl.prefixes.stream,
                    isDiscrete = decl.prefixes.discrete
                )
            }

            // 检查重复定义
            if (symbolTable.resolveLocal(component.name) != null) {
                errors.add(SemanticError(
                    "Redefinition of '${component.name}'",
                    decl.location,
                    SemanticErrorType.REDEFINED_SYMBOL
                ))
            } else {
                symbolTable.define(symbol)
            }

            // 分析修改
            component.modification?.let { analyzeModification(it) }
        }
    }

    /**
     * 分析修改
     */
    private fun analyzeModification(mod: Modification) {
        when (mod) {
            is Modification.Value -> analyzeExpression(mod.expression)
            is Modification.Arguments -> mod.args.forEach { arg ->
                when (arg) {
                    is Argument.Named -> arg.value?.let { analyzeExpression(it) }
                    is Argument.Positional -> analyzeExpression(arg.value)
                    is Argument.ComponentModification -> {
                        // 嵌套组件修改，递归分析
                        analyzeModification(arg.modification)
                    }
                }
            }
        }
    }

    /**
     * 分析方程
     */
    private fun analyzeEquation(equation: Equation) {
        when (equation) {
            is Equation.Simple -> {
                val leftType = analyzeExpression(equation.left)
                val rightType = analyzeExpression(equation.right)
                if (!leftType.isCompatibleWith(rightType)) {
                    errors.add(SemanticError(
                        "Type mismatch in equation: ${leftType.name} vs ${rightType.name}",
                        equation.location,
                        SemanticErrorType.TYPE_MISMATCH
                    ))
                }
            }
            is Equation.Connect -> {
                analyzeExpression(equation.left)
                analyzeExpression(equation.right)
            }
            is Equation.For -> {
                symbolTable.enterScope(ScopeType.FOR)
                symbolTable.define(Symbol.Variable(
                    name = equation.iterator,
                    type = IntegerType,
                    location = equation.location
                ))
                analyzeExpression(equation.range)
                equation.body.forEach { analyzeEquation(it) }
                symbolTable.exitScope()
            }
            is Equation.If -> {
                analyzeExpression(equation.condition)
                equation.thenBranch.forEach { analyzeEquation(it) }
                equation.elseIfBranches.forEach { (cond, body) ->
                    analyzeExpression(cond)
                    body.forEach { analyzeEquation(it) }
                }
                equation.elseBranch?.forEach { analyzeEquation(it) }
            }
            is Equation.When -> {
                analyzeExpression(equation.condition)
                equation.body.forEach { analyzeEquation(it) }
                equation.elseWhen?.let { analyzeEquation(it) }
            }
        }
    }

    /**
     * 分析算法段
     */
    private fun analyzeAlgorithmSection(section: AlgorithmSection) {
        section.statements.forEach { analyzeStatement(it) }
    }

    /**
     * 分析语句
     */
    private fun analyzeStatement(stmt: Statement) {
        when (stmt) {
            is Statement.Assignment -> {
                val targetType = analyzeExpression(stmt.target)
                val valueType = analyzeExpression(stmt.value)
                if (!valueType.isCompatibleWith(targetType)) {
                    errors.add(SemanticError(
                        "Type mismatch in assignment: cannot assign ${valueType.name} to ${targetType.name}",
                        stmt.location,
                        SemanticErrorType.TYPE_MISMATCH
                    ))
                }
            }
            is Statement.FunctionCall -> analyzeExpression(stmt.call)
            is Statement.If -> {
                analyzeExpression(stmt.condition)
                stmt.thenBranch.forEach { analyzeStatement(it) }
                stmt.elseIfBranches.forEach { (cond, body) ->
                    analyzeExpression(cond)
                    body.forEach { analyzeStatement(it) }
                }
                stmt.elseBranch?.forEach { analyzeStatement(it) }
            }
            is Statement.For -> {
                symbolTable.enterScope(ScopeType.FOR)
                symbolTable.define(Symbol.Variable(
                    name = stmt.iterator,
                    type = IntegerType,
                    location = stmt.location
                ))
                analyzeExpression(stmt.range)
                stmt.body.forEach { analyzeStatement(it) }
                symbolTable.exitScope()
            }
            is Statement.While -> {
                analyzeExpression(stmt.condition)
                stmt.body.forEach { analyzeStatement(it) }
            }
            is Statement.When -> {
                analyzeExpression(stmt.condition)
                stmt.body.forEach { analyzeStatement(it) }
                stmt.elseWhen?.let { analyzeStatement(it) }
            }
            is Statement.Return -> stmt.value?.let { analyzeExpression(it) }
            is Statement.Break -> {}
        }
    }

    /**
     * 分析表达式并返回类型
     */
    private fun analyzeExpression(expr: Expression): ModelicaType {
        return when (expr) {
            is Expression.IntegerLiteral -> IntegerType
            is Expression.RealLiteral -> RealType
            is Expression.StringLiteral -> StringType
            is Expression.BooleanLiteral -> BooleanType
            is Expression.Identifier -> {
                val symbol = symbolTable.resolve(expr.name)
                if (symbol == null) {
                    errors.add(SemanticError(
                        "Undefined variable: ${expr.name}",
                        expr.location,
                        SemanticErrorType.UNDEFINED_VARIABLE
                    ))
                    ErrorType
                } else {
                    symbol.type
                }
            }
            is Expression.Binary -> {
                val leftType = analyzeExpression(expr.left)
                val rightType = analyzeExpression(expr.right)
                typeChecker.checkBinaryOperator(expr.operator, leftType, rightType, expr.location)
            }
            is Expression.Unary -> {
                val operandType = analyzeExpression(expr.operand)
                typeChecker.checkUnaryOperator(expr.operator, operandType, expr.location)
            }
            is Expression.FunctionCall -> {
                val funcType = analyzeExpression(expr.function)
                val argTypes = expr.arguments.map { analyzeExpression(it) }
                typeChecker.checkFunctionCall(funcType, argTypes, expr.location)
            }
            is Expression.ArrayAccess -> {
                val arrayType = analyzeExpression(expr.array)
                expr.indices.forEach { analyzeExpression(it) }
                when (arrayType) {
                    is ArrayType -> arrayType.elementType
                    else -> {
                        errors.add(SemanticError(
                            "Cannot index non-array type: ${arrayType.name}",
                            expr.location,
                            SemanticErrorType.INVALID_OPERATION
                        ))
                        ErrorType
                    }
                }
            }
            is Expression.MemberAccess -> {
                val targetType = analyzeExpression(expr.target)
                // 简化：查找成员类型
                when (targetType) {
                    is UserType -> {
                        val classDef = symbolTable.getClass(targetType.name)
                        classDef?.let { findMemberType(it, expr.member) } ?: ErrorType
                    }
                    else -> {
                        errors.add(SemanticError(
                            "Cannot access member '${expr.member}' on type ${targetType.name}",
                            expr.location,
                            SemanticErrorType.INVALID_OPERATION
                        ))
                        ErrorType
                    }
                }
            }
            is Expression.ArrayLiteral -> {
                val elementTypes = expr.elements.map { analyzeExpression(it) }
                if (elementTypes.isEmpty()) {
                    AnyType
                } else {
                    val firstType = elementTypes.first()
                    val allSame = elementTypes.all { it.isCompatibleWith(firstType) }
                    if (!allSame) {
                        errors.add(SemanticError(
                            "Array literal has inconsistent element types",
                            expr.location,
                            SemanticErrorType.TYPE_MISMATCH
                        ))
                    }
                    ArrayType(firstType, listOf(expr.elements.size))
                }
            }
            is Expression.Range -> {
                val startType = analyzeExpression(expr.start)
                expr.step?.let { analyzeExpression(it) }
                val endType = analyzeExpression(expr.end)
                if (!startType.isCompatibleWith(endType)) {
                    errors.add(SemanticError(
                        "Range expression has inconsistent types",
                        expr.location,
                        SemanticErrorType.TYPE_MISMATCH
                    ))
                }
                ArrayType(startType, listOf(0)) // 维度未知
            }
            is Expression.Der -> {
                val innerType = analyzeExpression(expr.expression)
                when (innerType) {
                    RealType, is ArrayType -> innerType
                    else -> {
                        errors.add(SemanticError(
                            "der() requires Real or array type",
                            expr.location,
                            SemanticErrorType.TYPE_MISMATCH
                        ))
                        ErrorType
                    }
                }
            }
            is Expression.Conditional -> {
                val condType = analyzeExpression(expr.condition)
                val thenType = analyzeExpression(expr.thenExpression)
                val elseType = analyzeExpression(expr.elseExpression)

                if (condType != BooleanType) {
                    errors.add(SemanticError(
                        "Condition must be Boolean",
                        expr.location,
                        SemanticErrorType.TYPE_MISMATCH
                    ))
                }

                if (!thenType.isCompatibleWith(elseType)) {
                    errors.add(SemanticError(
                        "Conditional branches have incompatible types",
                        expr.location,
                        SemanticErrorType.TYPE_MISMATCH
                    ))
                }
                thenType
            }
        }
    }

    /**
     * 解析类型
     */
    private fun resolveType(typeSpec: TypeSpec): ModelicaType {
        return when (typeSpec.name) {
            "Real" -> RealType
            "Integer" -> IntegerType
            "Boolean" -> BooleanType
            "String" -> StringType
            else -> {
                val classDef = symbolTable.getClass(typeSpec.name)
                if (classDef != null) {
                    UserType(typeSpec.name, typeSpec.name)
                } else {
                    errors.add(SemanticError(
                        "Undefined type: ${typeSpec.name}",
                        null,
                        SemanticErrorType.UNDEFINED_TYPE
                    ))
                    ErrorType
                }
            }
        }.let { baseType ->
            if (typeSpec.dimensions.isNotEmpty()) {
                val dims = typeSpec.dimensions.map { 0 } // 简化
                ArrayType(baseType, dims)
            } else {
                baseType
            }
        }
    }

    /**
     * 查找类成员类型
     */
    private fun findMemberType(classDef: ClassDefinition, memberName: String): ModelicaType {
        // 在组件声明中查找
        classDef.composition.elements
            .filterIsInstance<ComponentDeclaration>()
            .forEach { decl ->
                decl.components.find { it.name == memberName }?.let { component ->
                    return resolveType(decl.type)
                }
            }
        return ErrorType
    }

    // 覆盖访问者方法
    override fun visitClassDefinition(node: ClassDefinition) {
        analyzeClass(node)
    }
}

/**
 * 类型检查器
 */
class TypeChecker {
    /**
     * 检查二元运算符类型
     */
    fun checkBinaryOperator(
        op: BinaryOperator,
        leftType: ModelicaType,
        rightType: ModelicaType,
        location: SourceLocation?
    ): ModelicaType {
        return when (op) {
            // 算术运算符
            BinaryOperator.ADD, BinaryOperator.SUB,
            BinaryOperator.MUL, BinaryOperator.DIV -> {
                if (leftType is RealType || rightType is RealType) RealType
                else if (leftType is IntegerType && rightType is IntegerType) IntegerType
                else {
                    // 报告错误但返回一个合理类型
                    if (leftType !is RealType && leftType !is IntegerType &&
                        leftType !is ErrorType && leftType !is AnyType) {
                        ErrorType
                    } else rightType
                }
            }
            BinaryOperator.POWER -> {
                if (leftType is RealType || leftType is IntegerType) RealType
                else ErrorType
            }
            // 比较运算符
            BinaryOperator.EQ, BinaryOperator.NE,
            BinaryOperator.LT, BinaryOperator.LE,
            BinaryOperator.GT, BinaryOperator.GE -> BooleanType
            // 逻辑运算符
            BinaryOperator.AND, BinaryOperator.OR -> {
                if (leftType == BooleanType && rightType == BooleanType) BooleanType
                else ErrorType
            }
        }
    }

    /**
     * 检查一元运算符类型
     */
    fun checkUnaryOperator(
        op: UnaryOperator,
        operandType: ModelicaType,
        location: SourceLocation?
    ): ModelicaType {
        return when (op) {
            UnaryOperator.NEG -> {
                if (operandType is RealType || operandType is IntegerType) operandType
                else ErrorType
            }
            UnaryOperator.NOT -> {
                if (operandType == BooleanType) BooleanType
                else ErrorType
            }
        }
    }

    /**
     * 检查函数调用类型
     */
    fun checkFunctionCall(
        funcType: ModelicaType,
        argTypes: List<ModelicaType>,
        location: SourceLocation?
    ): ModelicaType {
        return when (funcType) {
            is FunctionType -> {
                if (funcType.parameters.size != argTypes.size) {
                    ErrorType
                } else {
                    funcType.returnType
                }
            }
            else -> funcType // 简化：假设返回相同类型
        }
    }
}