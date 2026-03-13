package org.modelica.compiler.ast.visitor

import org.modelica.compiler.ast.nodes.*

/**
 * AST访问者接口
 *
 * 使用访问者模式遍历AST节点
 */
interface ASTVisitor<T> {
    // 程序
    fun visitProgram(node: Program): T

    // 类定义
    fun visitClassDefinition(node: ClassDefinition): T

    // 元素
    fun visitImportClause(node: ImportClause): T
    fun visitExtendsClause(node: ExtendsClause): T
    fun visitComponentDeclaration(node: ComponentDeclaration): T
    fun visitNestedClassElement(node: NestedClassElement): T

    // 方程
    fun visitSimpleEquation(node: Equation.Simple): T
    fun visitConnectEquation(node: Equation.Connect): T
    fun visitForEquation(node: Equation.For): T
    fun visitIfEquation(node: Equation.If): T
    fun visitWhenEquation(node: Equation.When): T

    // 语句
    fun visitAssignment(node: Statement.Assignment): T
    fun visitFunctionCallStatement(node: Statement.FunctionCall): T
    fun visitIfStatement(node: Statement.If): T
    fun visitForStatement(node: Statement.For): T
    fun visitWhileStatement(node: Statement.While): T
    fun visitWhenStatement(node: Statement.When): T
    fun visitBreak(node: Statement.Break): T
    fun visitReturn(node: Statement.Return): T
    fun visitAlgorithmSection(node: AlgorithmSection): T

    // 表达式
    fun visitIntegerLiteral(node: Expression.IntegerLiteral): T
    fun visitRealLiteral(node: Expression.RealLiteral): T
    fun visitStringLiteral(node: Expression.StringLiteral): T
    fun visitBooleanLiteral(node: Expression.BooleanLiteral): T
    fun visitIdentifier(node: Expression.Identifier): T
    fun visitBinaryExpression(node: Expression.Binary): T
    fun visitUnaryExpression(node: Expression.Unary): T
    fun visitFunctionCall(node: Expression.FunctionCall): T
    fun visitArrayAccess(node: Expression.ArrayAccess): T
    fun visitMemberAccess(node: Expression.MemberAccess): T
    fun visitArrayLiteral(node: Expression.ArrayLiteral): T
    fun visitRangeExpression(node: Expression.Range): T
    fun visitDerExpression(node: Expression.Der): T
    fun visitConditionalExpression(node: Expression.Conditional): T
}