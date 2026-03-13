package org.modelica.compiler.ast.nodes

import org.modelica.compiler.lexer.SourceLocation
import org.modelica.compiler.ast.visitor.ASTVisitor

/**
 * AST节点基类
 */
interface ASTNode {
    /** 节点位置信息 */
    val location: SourceLocation?

    /** 接受访问者 */
    fun <T> accept(visitor: ASTVisitor<T>): T
}

/**
 * 程序根节点
 */
data class Program(
    val classes: List<ClassDefinition>,
    val imports: List<ImportClause>
) : ASTNode {
    override val location: SourceLocation? = null

    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitProgram(this)
}

/**
 * 类定义前缀
 */
data class ClassPrefixes(
    val encapsulated: Boolean = false,
    val partial: Boolean = false,
    val final: Boolean = false,
    val abstract: Boolean = false
)

/**
 * 类类型枚举
 */
enum class ClassType {
    CLASS, MODEL, RECORD, BLOCK, CONNECTOR, TYPE, PACKAGE, FUNCTION
}

/**
 * 类定义
 */
data class ClassDefinition(
    val prefixes: ClassPrefixes,
    val classType: ClassType,
    val name: String,
    val specialization: ClassSpecialization? = null,
    val description: String? = null,
    val composition: ClassComposition,
    override val location: SourceLocation?
) : ASTNode {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitClassDefinition(this)
}

/**
 * 类特化
 */
data class ClassSpecialization(
    val baseType: TypeSpec
)

/**
 * 类组合内容
 */
data class ClassComposition(
    val elements: List<Element> = emptyList(),
    val equations: List<Equation> = emptyList(),
    val algorithms: List<AlgorithmSection> = emptyList(),
    val short: Expression? = null
)

/**
 * 元素接口
 */
interface Element : ASTNode

/**
 * 导入子句
 */
data class ImportClause(
    val path: String,
    val alias: String? = null,
    override val location: SourceLocation? = null
) : Element {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitImportClause(this)
}

/**
 * 继承子句
 */
data class ExtendsClause(
    val baseClass: String,
    val modifications: List<Argument> = emptyList(),
    override val location: SourceLocation? = null
) : Element {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitExtendsClause(this)
}

/**
 * 嵌套类元素
 * 用于支持package和其他类定义内的嵌套类定义
 */
data class NestedClassElement(
    val classDefinition: ClassDefinition,
    override val location: SourceLocation? = classDefinition.location
) : Element {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitNestedClassElement(this)
}

/**
 * 类型前缀
 */
data class TypePrefixes(
    val flow: Boolean = false,
    val stream: Boolean = false,
    val discrete: Boolean = false,
    val parameter: Boolean = false,
    val constant: Boolean = false,
    val input: Boolean = false,
    val output: Boolean = false
)

/**
 * 类型规格
 */
data class TypeSpec(
    val name: String,
    val dimensions: List<ArrayDimension> = emptyList()
)

/**
 * 数组维度
 */
sealed class ArrayDimension {
    data class Known(val size: Expression) : ArrayDimension()
    object Unknown : ArrayDimension()
}

/**
 * 组件声明
 */
data class ComponentDeclaration(
    val prefixes: TypePrefixes,
    val type: TypeSpec,
    val components: List<ComponentItem>,
    override val location: SourceLocation? = null
) : Element {
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitComponentDeclaration(this)
}

/**
 * 组件项
 */
data class ComponentItem(
    val name: String,
    val dimensions: List<ArrayDimension> = emptyList(),
    val modification: Modification? = null,
    val description: String? = null
)

/**
 * 修改
 */
sealed class Modification {
    data class Value(val expression: Expression) : Modification()
    data class Arguments(val args: List<Argument>) : Modification()
}

/**
 * 参数
 */
sealed class Argument {
    data class Named(val name: String, val value: Expression?) : Argument()
    data class Positional(val value: Expression) : Argument()
    data class ComponentModification(val name: String, val modification: Modification) : Argument()
}