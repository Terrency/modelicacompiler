package org.modelica.compiler.ast.visitor

import org.modelica.compiler.ast.nodes.*

/**
 * 默认AST访问者
 *
 * 提供所有访问方法的默认实现，子类可以覆盖需要的方法
 */
open class DefaultVisitor<T>(protected val defaultValue: T) : ASTVisitor<T> {

    // 程序
    override fun visitProgram(node: Program): T {
        node.classes.forEach { it.accept(this) }
        node.imports.forEach { it.accept(this) }
        return defaultValue
    }

    // 类定义
    override fun visitClassDefinition(node: ClassDefinition): T {
        node.composition.elements.forEach { it.accept(this) }
        node.composition.equations.forEach { it.accept(this) }
        node.composition.algorithms.forEach { it.accept(this) }
        return defaultValue
    }

    // 元素
    override fun visitImportClause(node: ImportClause): T = defaultValue

    override fun visitExtendsClause(node: ExtendsClause): T = defaultValue

    override fun visitComponentDeclaration(node: ComponentDeclaration): T {
        node.components.forEach { component ->
            component.modification?.let { mod ->
                processModification(mod)
            }
        }
        return defaultValue
    }

    override fun visitNestedClassElement(node: NestedClassElement): T {
        node.classDefinition.accept(this)
        return defaultValue
    }

    private fun processModification(mod: Modification) {
        when (mod) {
            is Modification.Value -> mod.expression.accept(this)
            is Modification.Arguments -> mod.args.forEach { arg ->
                when (arg) {
                    is Argument.Named -> arg.value?.accept(this)
                    is Argument.Positional -> arg.value.accept(this)
                    is Argument.ComponentModification -> {
                        // 嵌套组件修改，递归处理
                        processModification(arg.modification)
                    }
                }
            }
        }
    }

    // 方程
    override fun visitSimpleEquation(node: Equation.Simple): T {
        node.left.accept(this)
        node.right.accept(this)
        return defaultValue
    }

    override fun visitConnectEquation(node: Equation.Connect): T {
        node.left.accept(this)
        node.right.accept(this)
        return defaultValue
    }

    override fun visitForEquation(node: Equation.For): T {
        node.range.accept(this)
        node.body.forEach { it.accept(this) }
        return defaultValue
    }

    override fun visitIfEquation(node: Equation.If): T {
        node.condition.accept(this)
        node.thenBranch.forEach { it.accept(this) }
        node.elseIfBranches.forEach { (condition, body) ->
            condition.accept(this)
            body.forEach { it.accept(this) }
        }
        node.elseBranch?.forEach { it.accept(this) }
        return defaultValue
    }

    override fun visitWhenEquation(node: Equation.When): T {
        node.condition.accept(this)
        node.body.forEach { it.accept(this) }
        node.elseWhen?.accept(this)
        return defaultValue
    }

    // 语句
    override fun visitAssignment(node: Statement.Assignment): T {
        node.target.accept(this)
        node.value.accept(this)
        return defaultValue
    }

    override fun visitFunctionCallStatement(node: Statement.FunctionCall): T {
        node.call.accept(this)
        return defaultValue
    }

    override fun visitIfStatement(node: Statement.If): T {
        node.condition.accept(this)
        node.thenBranch.forEach { it.accept(this) }
        node.elseIfBranches.forEach { (condition, body) ->
            condition.accept(this)
            body.forEach { it.accept(this) }
        }
        node.elseBranch?.forEach { it.accept(this) }
        return defaultValue
    }

    override fun visitForStatement(node: Statement.For): T {
        node.range.accept(this)
        node.body.forEach { it.accept(this) }
        return defaultValue
    }

    override fun visitWhileStatement(node: Statement.While): T {
        node.condition.accept(this)
        node.body.forEach { it.accept(this) }
        return defaultValue
    }

    override fun visitWhenStatement(node: Statement.When): T {
        node.condition.accept(this)
        node.body.forEach { it.accept(this) }
        node.elseWhen?.accept(this)
        return defaultValue
    }

    override fun visitBreak(node: Statement.Break): T = defaultValue

    override fun visitReturn(node: Statement.Return): T {
        node.value?.accept(this)
        return defaultValue
    }

    override fun visitAlgorithmSection(node: AlgorithmSection): T {
        node.statements.forEach { it.accept(this) }
        return defaultValue
    }

    // 表达式
    override fun visitIntegerLiteral(node: Expression.IntegerLiteral): T = defaultValue

    override fun visitRealLiteral(node: Expression.RealLiteral): T = defaultValue

    override fun visitStringLiteral(node: Expression.StringLiteral): T = defaultValue

    override fun visitBooleanLiteral(node: Expression.BooleanLiteral): T = defaultValue

    override fun visitIdentifier(node: Expression.Identifier): T = defaultValue

    override fun visitBinaryExpression(node: Expression.Binary): T {
        node.left.accept(this)
        node.right.accept(this)
        return defaultValue
    }

    override fun visitUnaryExpression(node: Expression.Unary): T {
        node.operand.accept(this)
        return defaultValue
    }

    override fun visitFunctionCall(node: Expression.FunctionCall): T {
        node.function.accept(this)
        node.arguments.forEach { arg ->
            when (arg) {
                is Expression.CallArgument.Positional -> arg.value.accept(this)
                is Expression.CallArgument.Named -> arg.value.accept(this)
            }
        }
        return defaultValue
    }

    override fun visitArrayAccess(node: Expression.ArrayAccess): T {
        node.array.accept(this)
        node.indices.forEach { it.accept(this) }
        return defaultValue
    }

    override fun visitMemberAccess(node: Expression.MemberAccess): T {
        node.target.accept(this)
        return defaultValue
    }

    override fun visitArrayLiteral(node: Expression.ArrayLiteral): T {
        node.elements.forEach { it.accept(this) }
        return defaultValue
    }

    override fun visitRangeExpression(node: Expression.Range): T {
        node.start.accept(this)
        node.step?.accept(this)
        node.end.accept(this)
        return defaultValue
    }

    override fun visitDerExpression(node: Expression.Der): T {
        node.expression.accept(this)
        return defaultValue
    }

    override fun visitConditionalExpression(node: Expression.Conditional): T {
        node.condition.accept(this)
        node.thenExpression.accept(this)
        node.elseExpression.accept(this)
        return defaultValue
    }
}