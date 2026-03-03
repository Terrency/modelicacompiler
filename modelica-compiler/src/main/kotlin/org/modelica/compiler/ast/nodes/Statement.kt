package org.modelica.compiler.ast.nodes

import org.modelica.compiler.lexer.SourceLocation

/**
 * 语句基类
 */
sealed class Statement : ASTNode {
    data class Assignment(
        val target: Expression,
        val value: Expression,
        override val location: SourceLocation? = null
    ) : Statement() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitAssignment(this)
    }

    data class FunctionCall(
        val call: Expression,
        override val location: SourceLocation? = null
    ) : Statement() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitFunctionCallStatement(this)
    }

    data class If(
        val condition: Expression,
        val thenBranch: List<Statement>,
        val elseIfBranches: List<Pair<Expression, List<Statement>>> = emptyList(),
        val elseBranch: List<Statement>? = null,
        override val location: SourceLocation? = null
    ) : Statement() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitIfStatement(this)
    }

    data class For(
        val iterator: String,
        val range: Expression,
        val body: List<Statement>,
        override val location: SourceLocation? = null
    ) : Statement() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitForStatement(this)
    }

    data class While(
        val condition: Expression,
        val body: List<Statement>,
        override val location: SourceLocation? = null
    ) : Statement() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitWhileStatement(this)
    }

    data class When(
        val condition: Expression,
        val body: List<Statement>,
        val elseWhen: When? = null,
        override val location: SourceLocation? = null
    ) : Statement() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitWhenStatement(this)
    }

    data object Break : Statement() {
        override val location: SourceLocation? = null
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitBreak(this)
    }

    data class Return(
        val value: Expression? = null,
        override val location: SourceLocation? = null
    ) : Statement() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitReturn(this)
    }
}

/**
 * 算法段
 */
data class AlgorithmSection(
    val statements: List<Statement>,
    val isInitial: Boolean = false,
    override val location: SourceLocation? = null
) : ASTNode {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitAlgorithmSection(this)
}