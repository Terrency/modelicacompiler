package org.modelica.compiler.ast.nodes

import org.modelica.compiler.lexer.SourceLocation
import org.modelica.compiler.ast.visitor.ASTVisitor

/**
 * 方程基类
 */
sealed class Equation : ASTNode {
    abstract val isInitial: Boolean

    data class Simple(
        val left: Expression,
        val right: Expression,
        override val isInitial: Boolean = false,
        override val location: SourceLocation? = null
    ) : Equation() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitSimpleEquation(this)
    }

    data class Connect(
        val left: Expression,
        val right: Expression,
        override val isInitial: Boolean = false,
        override val location: SourceLocation? = null
    ) : Equation() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitConnectEquation(this)
    }

    data class For(
        val iterator: String,
        val range: Expression,
        val body: List<Equation>,
        override val isInitial: Boolean = false,
        override val location: SourceLocation? = null
    ) : Equation() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitForEquation(this)
    }

    data class If(
        val condition: Expression,
        val thenBranch: List<Equation>,
        val elseIfBranches: List<Pair<Expression, List<Equation>>> = emptyList(),
        val elseBranch: List<Equation>? = null,
        override val isInitial: Boolean = false,
        override val location: SourceLocation? = null
    ) : Equation() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitIfEquation(this)
    }

    data class When(
        val condition: Expression,
        val body: List<Equation>,
        val elseWhen: When? = null,
        override val isInitial: Boolean = false,
        override val location: SourceLocation? = null
    ) : Equation() {
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitWhenEquation(this)
    }
}