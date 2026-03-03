package org.modelica.compiler.ir.nodes

/**
 * IR语句
 */
sealed class IRStatement {
    /**
     * 赋值语句
     */
    data class Assignment(
        val target: IRExpression,
        val value: IRExpression
    ) : IRStatement()

    /**
     * 表达式语句
     */
    data class ExpressionStatement(
        val expression: IRExpression
    ) : IRStatement()

    /**
     * 返回语句
     */
    data class Return(
        val value: IRExpression? = null
    ) : IRStatement()

    /**
     * If语句
     */
    data class If(
        val condition: IRExpression,
        val thenBranch: List<IRStatement>,
        val elseBranch: List<IRStatement> = emptyList()
    ) : IRStatement()

    /**
     * For循环
     */
    data class ForLoop(
        val iterator: String,
        val start: IRExpression,
        val end: IRExpression,
        val body: List<IRStatement>
    ) : IRStatement()

    /**
     * While循环
     */
    data class WhileLoop(
        val condition: IRExpression,
        val body: List<IRStatement>
    ) : IRStatement()

    /**
     * Break语句
     */
    object Break : IRStatement()

    /**
     * Continue语句
     */
    object Continue : IRStatement()

    /**
     * 变量声明
     */
    data class VariableDeclaration(
        val name: String,
        val type: String,
        val initialValue: IRExpression? = null
    ) : IRStatement()
}