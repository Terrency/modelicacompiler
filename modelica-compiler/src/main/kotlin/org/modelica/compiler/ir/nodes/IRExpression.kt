package org.modelica.compiler.ir.nodes

/**
 * IR表达式
 */
sealed class IRExpression {
    /**
     * 整数字面量
     */
    data class IntegerLiteral(val value: Long) : IRExpression()

    /**
     * 实数字面量
     */
    data class RealLiteral(val value: Double) : IRExpression()

    /**
     * 字符串字面量
     */
    data class StringLiteral(val value: String) : IRExpression()

    /**
     * 布尔字面量
     */
    data class BooleanLiteral(val value: Boolean) : IRExpression()

    /**
     * 空值
     */
    object Null : IRExpression()

    /**
     * this引用
     */
    object This : IRExpression()

    /**
     * 变量引用
     */
    data class Variable(val name: String) : IRExpression()

    /**
     * 二元运算
     */
    data class Binary(
        val left: IRExpression,
        val operator: String,
        val right: IRExpression
    ) : IRExpression()

    /**
     * 一元运算
     */
    data class Unary(
        val operator: String,
        val operand: IRExpression
    ) : IRExpression()

    /**
     * 函数调用
     */
    data class FunctionCall(
        val function: String,
        val arguments: List<IRExpression>
    ) : IRExpression()

    /**
     * 方法调用
     */
    data class MethodCall(
        val target: IRExpression,
        val methodName: String,
        val arguments: List<IRExpression>
    ) : IRExpression()

    /**
     * 字段访问
     */
    data class FieldAccess(
        val target: IRExpression,
        val fieldName: String
    ) : IRExpression()

    /**
     * 数组访问
     */
    data class ArrayAccess(
        val array: IRExpression,
        val indices: List<IRExpression>
    ) : IRExpression()

    /**
     * 数组字面量
     */
    data class ArrayLiteral(
        val elements: List<IRExpression>
    ) : IRExpression()

    /**
     * 范围表达式
     */
    data class Range(
        val start: IRExpression,
        val end: IRExpression,
        val step: IRExpression? = null
    ) : IRExpression()

    /**
     * 条件表达式
     */
    data class Conditional(
        val condition: IRExpression,
        val thenExpression: IRExpression,
        val elseExpression: IRExpression
    ) : IRExpression()

    /**
     * 新对象创建
     */
    data class NewObject(
        val className: String,
        val arguments: List<IRExpression>
    ) : IRExpression()

    /**
     * 新数组创建
     */
    data class NewArray(
        val elementType: String,
        val dimensions: List<IRExpression>
    ) : IRExpression()

    /**
     * 类型转换
     */
    data class Cast(
        val target: IRExpression,
        val targetType: String
    ) : IRExpression()
}