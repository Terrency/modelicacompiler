package org.modelica.compiler.ast.nodes

import org.modelica.compiler.lexer.SourceLocation

/**
 * 表达式基类
 */
sealed class Expression : ASTNode {

    // 字面量
    data class IntegerLiteral(
        val value: Long,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitIntegerLiteral(this)
    }

    data class RealLiteral(
        val value: Double,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitRealLiteral(this)
    }

    data class StringLiteral(
        val value: String,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitStringLiteral(this)
    }

    data class BooleanLiteral(
        val value: Boolean,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitBooleanLiteral(this)
    }

    // 标识符
    data class Identifier(
        val name: String,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitIdentifier(this)
    }

    // 二元运算
    data class Binary(
        val left: Expression,
        val operator: BinaryOperator,
        val right: Expression,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitBinaryExpression(this)
    }

    // 一元运算
    data class Unary(
        val operator: UnaryOperator,
        val operand: Expression,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitUnaryExpression(this)
    }

    // 函数调用
    data class FunctionCall(
        val function: Expression,
        val arguments: List<Expression>,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitFunctionCall(this)
    }

    // 数组访问
    data class ArrayAccess(
        val array: Expression,
        val indices: List<Expression>,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitArrayAccess(this)
    }

    // 成员访问
    data class MemberAccess(
        val target: Expression,
        val member: String,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitMemberAccess(this)
    }

    // 数组字面量
    data class ArrayLiteral(
        val elements: List<Expression>,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitArrayLiteral(this)
    }

    // 范围表达式
    data class Range(
        val start: Expression,
        val step: Expression? = null,
        val end: Expression,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitRangeExpression(this)
    }

    // 导数
    data class Der(
        val expression: Expression,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitDerExpression(this)
    }

    // 条件表达式
    data class Conditional(
        val condition: Expression,
        val thenExpression: Expression,
        val elseExpression: Expression,
        override val location: SourceLocation? = null
    ) : Expression() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitConditionalExpression(this)
    }
}

/**
 * 二元运算符
 */
enum class BinaryOperator(val symbol: String) {
    // 算术
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    POWER("^"),

    // 比较
    EQ("=="),
    NE("<>"),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),

    // 逻辑
    AND("and"),
    OR("or");

    override fun toString(): String = symbol
}

/**
 * 一元运算符
 */
enum class UnaryOperator(val symbol: String) {
    NEG("-"),
    NOT("not");

    override fun toString(): String = symbol
}