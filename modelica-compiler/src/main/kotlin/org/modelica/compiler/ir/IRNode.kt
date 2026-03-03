package org.modelica.compiler.ir

import org.modelica.compiler.ir.nodes.*

/**
 * IR节点基类
 */
interface IRNode

/**
 * IR模块（编译单元）
 */
data class IRModule(
    val name: String,
    val classes: List<IRClass>
) : IRNode

/**
 * IR构建器
 *
 * 将AST转换为中间表示
 */
class IRBuilder {

    /**
     * 从程序AST构建IR模块
     */
    fun build(program: org.modelica.compiler.ast.nodes.Program): IRModule {
        val classes = program.classes.map { buildClass(it) }
        return IRModule("main", classes)
    }

    /**
     * 构建类IR
     */
    private fun buildClass(classDef: org.modelica.compiler.ast.nodes.ClassDefinition): IRClass {
        val fields = mutableListOf<IRField>()
        val methods = mutableListOf<IRMethod>()

        // 构建字段
        classDef.composition.elements
            .filterIsInstance<org.modelica.compiler.ast.nodes.ComponentDeclaration>()
            .forEach { decl ->
                decl.components.forEach { component ->
                    fields.add(IRField(
                        name = component.name,
                        type = mapType(decl.type),
                        isStatic = false,
                        isFinal = decl.prefixes.constant || decl.prefixes.parameter
                    ))
                }
            }

        // 构建初始化方法
        val initStatements = mutableListOf<IRStatement>()
        classDef.composition.elements
            .filterIsInstance<org.modelica.compiler.ast.nodes.ComponentDeclaration>()
            .forEach { decl ->
                decl.components.forEach { component ->
                    component.modification?.let { mod ->
                        when (mod) {
                            is org.modelica.compiler.ast.nodes.Modification.Value -> {
                                initStatements.add(IRStatement.Assignment(
                                    target = IRExpression.FieldAccess(
                                        target = IRExpression.This,
                                        fieldName = component.name
                                    ),
                                    value = buildExpression(mod.expression)
                                ))
                            }
                            else -> {}
                        }
                    }
                }
            }

        if (initStatements.isNotEmpty()) {
            methods.add(IRMethod(
                name = "<init>",
                returnType = "void",
                parameters = emptyList(),
                body = initStatements,
                isConstructor = true
            ))
        }

        // 构建方程求解方法
        if (classDef.composition.equations.isNotEmpty()) {
            val equationStatements = classDef.composition.equations.map { buildEquation(it) }.flatten()
            methods.add(IRMethod(
                name = "solveEquations",
                returnType = "void",
                parameters = listOf(IRParameter("time", "double")),
                body = equationStatements
            ))
        }

        // 构建算法方法
        classDef.composition.algorithms.forEachIndexed { index, algo ->
            val algoStatements = algo.statements.map { buildStatement(it) }.flatten()
            methods.add(IRMethod(
                name = "algorithm${if (index > 0) index else ""}",
                returnType = "void",
                parameters = emptyList(),
                body = algoStatements
            ))
        }

        return IRClass(
            name = classDef.name,
            superClass = "java/lang/Object",
            fields = fields,
            methods = methods
        )
    }

    /**
     * 构建方程IR
     */
    private fun buildEquation(equation: org.modelica.compiler.ast.nodes.Equation): List<IRStatement> {
        return when (equation) {
            is org.modelica.compiler.ast.nodes.Equation.Simple -> {
                // 方程转换为赋值（简化处理）
                listOf(IRStatement.Assignment(
                    target = buildExpression(equation.left),
                    value = buildExpression(equation.right)
                ))
            }
            is org.modelica.compiler.ast.nodes.Equation.Connect -> {
                // 连接方程：生成连接调用
                listOf(IRStatement.ExpressionStatement(
                    IRExpression.FunctionCall(
                        function = "connect",
                        arguments = listOf(
                            buildExpression(equation.left),
                            buildExpression(equation.right)
                        )
                    )
                ))
            }
            is org.modelica.compiler.ast.nodes.Equation.For -> {
                listOf(IRStatement.ForLoop(
                    iterator = equation.iterator,
                    start = IRExpression.IntegerLiteral(0),
                    end = buildExpression(equation.range),
                    body = equation.body.map { buildEquation(it) }.flatten()
                ))
            }
            is org.modelica.compiler.ast.nodes.Equation.If -> {
                val elseBranch = equation.elseBranch?.map { buildEquation(it) }.flatten() ?: emptyList()
                listOf(IRStatement.If(
                    condition = buildExpression(equation.condition),
                    thenBranch = equation.thenBranch.map { buildEquation(it) }.flatten(),
                    elseBranch = elseBranch
                ))
            }
            is org.modelica.compiler.ast.nodes.Equation.When -> {
                listOf(IRStatement.If(
                    condition = buildExpression(equation.condition),
                    thenBranch = equation.body.map { buildEquation(it) }.flatten(),
                    elseBranch = emptyList()
                ))
            }
        }
    }

    /**
     * 构建语句IR
     */
    private fun buildStatement(stmt: org.modelica.compiler.ast.nodes.Statement): List<IRStatement> {
        return when (stmt) {
            is org.modelica.compiler.ast.nodes.Statement.Assignment -> {
                listOf(IRStatement.Assignment(
                    target = buildExpression(stmt.target),
                    value = buildExpression(stmt.value)
                ))
            }
            is org.modelica.compiler.ast.nodes.Statement.FunctionCall -> {
                listOf(IRStatement.ExpressionStatement(buildExpression(stmt.call)))
            }
            is org.modelica.compiler.ast.nodes.Statement.If -> {
                val elseBranch = stmt.elseBranch?.map { buildStatement(it) }.flatten() ?: emptyList()
                listOf(IRStatement.If(
                    condition = buildExpression(stmt.condition),
                    thenBranch = stmt.thenBranch.map { buildStatement(it) }.flatten(),
                    elseBranch = elseBranch
                ))
            }
            is org.modelica.compiler.ast.nodes.Statement.For -> {
                listOf(IRStatement.ForLoop(
                    iterator = stmt.iterator,
                    start = IRExpression.IntegerLiteral(0),
                    end = buildExpression(stmt.range),
                    body = stmt.body.map { buildStatement(it) }.flatten()
                ))
            }
            is org.modelica.compiler.ast.nodes.Statement.While -> {
                listOf(IRStatement.WhileLoop(
                    condition = buildExpression(stmt.condition),
                    body = stmt.body.map { buildStatement(it) }.flatten()
                ))
            }
            is org.modelica.compiler.ast.nodes.Statement.Return -> {
                listOf(IRStatement.Return(stmt.value?.let { buildExpression(it) }))
            }
            is org.modelica.compiler.ast.nodes.Statement.Break -> {
                listOf(IRStatement.Break)
            }
            is org.modelica.compiler.ast.nodes.Statement.When -> {
                listOf(IRStatement.If(
                    condition = buildExpression(stmt.condition),
                    thenBranch = stmt.body.map { buildStatement(it) }.flatten(),
                    elseBranch = emptyList()
                ))
            }
        }
    }

    /**
     * 构建表达式IR
     */
    private fun buildExpression(expr: org.modelica.compiler.ast.nodes.Expression): IRExpression {
        return when (expr) {
            is org.modelica.compiler.ast.nodes.Expression.IntegerLiteral ->
                IRExpression.IntegerLiteral(expr.value)
            is org.modelica.compiler.ast.nodes.Expression.RealLiteral ->
                IRExpression.RealLiteral(expr.value)
            is org.modelica.compiler.ast.nodes.Expression.StringLiteral ->
                IRExpression.StringLiteral(expr.value)
            is org.modelica.compiler.ast.nodes.Expression.BooleanLiteral ->
                IRExpression.BooleanLiteral(expr.value)
            is org.modelica.compiler.ast.nodes.Expression.Identifier ->
                IRExpression.Variable(expr.name)
            is org.modelica.compiler.ast.nodes.Expression.Binary ->
                IRExpression.Binary(
                    left = buildExpression(expr.left),
                    operator = expr.operator.name,
                    right = buildExpression(expr.right)
                )
            is org.modelica.compiler.ast.nodes.Expression.Unary ->
                IRExpression.Unary(
                    operator = expr.operator.name,
                    operand = buildExpression(expr.operand)
                )
            is org.modelica.compiler.ast.nodes.Expression.FunctionCall ->
                IRExpression.FunctionCall(
                    function = (expr.function as? org.modelica.compiler.ast.nodes.Expression.Identifier)?.name ?: "unknown",
                    arguments = expr.arguments.map { buildExpression(it) }
                )
            is org.modelica.compiler.ast.nodes.Expression.ArrayAccess ->
                IRExpression.ArrayAccess(
                    array = buildExpression(expr.array),
                    indices = expr.indices.map { buildExpression(it) }
                )
            is org.modelica.compiler.ast.nodes.Expression.MemberAccess ->
                IRExpression.FieldAccess(
                    target = buildExpression(expr.target),
                    fieldName = expr.member
                )
            is org.modelica.compiler.ast.nodes.Expression.ArrayLiteral ->
                IRExpression.ArrayLiteral(
                    elements = expr.elements.map { buildExpression(it) }
                )
            is org.modelica.compiler.ast.nodes.Expression.Range ->
                IRExpression.Range(
                    start = buildExpression(expr.start),
                    end = buildExpression(expr.end),
                    step = expr.step?.let { buildExpression(it) }
                )
            is org.modelica.compiler.ast.nodes.Expression.Der ->
                IRExpression.FunctionCall(
                    function = "der",
                    arguments = listOf(buildExpression(expr.expression))
                )
            is org.modelica.compiler.ast.nodes.Expression.Conditional ->
                IRExpression.Conditional(
                    condition = buildExpression(expr.condition),
                    thenExpression = buildExpression(expr.thenExpression),
                    elseExpression = buildExpression(expr.elseExpression)
                )
        }
    }

    /**
     * 映射类型
     */
    private fun mapType(typeSpec: org.modelica.compiler.ast.nodes.TypeSpec): String {
        return when (typeSpec.name) {
            "Real" -> "double"
            "Integer" -> "int"
            "Boolean" -> "boolean"
            "String" -> "java/lang/String"
            else -> typeSpec.name.replace(".", "/")
        }
    }
}