package org.modelica.compiler.semantic.types

/**
 * Modelica类型系统
 */
sealed class ModelicaType {
    abstract val name: String
    abstract val dimensions: List<Int>

    /** 是否为基本类型 */
    open val isBasic: Boolean get() = false

    /** 是否为数组类型 */
    val isArray: Boolean get() = dimensions.isNotEmpty()

    /** 获取元素类型 */
    open val elementType: ModelicaType get() = this

    /** 类型兼容性检查 */
    open fun isCompatibleWith(other: ModelicaType): Boolean {
        return when {
            this == other -> true
            this is AnyType || other is AnyType -> true
            this is ErrorType || other is ErrorType -> true
            else -> false
        }
    }

    /** 获取数组维度数量 */
    val ndims: Int get() = dimensions.size
}

/**
 * 任意类型（用于类型推断前）
 */
object AnyType : ModelicaType() {
    override val name: String = "anytype"
    override val dimensions: List<Int> = emptyList()
    override val isBasic: Boolean = false
}

/**
 * 错误类型（用于错误恢复）
 */
object ErrorType : ModelicaType() {
    override val name: String = "<error>"
    override val dimensions: List<Int> = emptyList()
    override val isBasic: Boolean = false
}

/**
 * 内置基本类型
 */
sealed class BuiltInType : ModelicaType() {
    override val isBasic: Boolean = true
}

/**
 * 实数类型
 */
object RealType : BuiltInType() {
    override val name: String = "Real"
    override val dimensions: List<Int> = emptyList()

    override fun isCompatibleWith(other: ModelicaType): Boolean {
        return other is RealType || other is IntegerType || other is AnyType || other is ErrorType
    }
}

/**
 * 整数类型
 */
object IntegerType : BuiltInType() {
    override val name: String = "Integer"
    override val dimensions: List<Int> = emptyList()

    override fun isCompatibleWith(other: ModelicaType): Boolean {
        return other is IntegerType || other is RealType || other is AnyType || other is ErrorType
    }
}

/**
 * 布尔类型
 */
object BooleanType : BuiltInType() {
    override val name: String = "Boolean"
    override val dimensions: List<Int> = emptyList()

    override fun isCompatibleWith(other: ModelicaType): Boolean {
        return other is BooleanType || other is AnyType || other is ErrorType
    }
}

/**
 * 字符串类型
 */
object StringType : BuiltInType() {
    override val name: String = "String"
    override val dimensions: List<Int> = emptyList()

    override fun isCompatibleWith(other: ModelicaType): Boolean {
        return other is StringType || other is AnyType || other is ErrorType
    }
}

/**
 * 数组类型
 */
data class ArrayType(
    override val elementType: ModelicaType,
    override val dimensions: List<Int>
) : ModelicaType() {
    override val name: String = "${elementType.name}[${dimensions.joinToString(",")}]"

    override fun isCompatibleWith(other: ModelicaType): Boolean {
        return when (other) {
            is ArrayType -> elementType.isCompatibleWith(other.elementType) &&
                    dimensions == other.dimensions
            is AnyType, is ErrorType -> true
            else -> false
        }
    }

    companion object {
        /** 创建数组类型 */
        fun of(elementType: ModelicaType, vararg dims: Int): ArrayType {
            return ArrayType(elementType, dims.toList())
        }
    }
}

/**
 * 用户定义类型（类、模型等）
 */
data class UserType(
    override val name: String,
    val qualifiedName: String,
    override val dimensions: List<Int> = emptyList()
) : ModelicaType() {
    override fun isCompatibleWith(other: ModelicaType): Boolean {
        return when (other) {
            is UserType -> qualifiedName == other.qualifiedName
            is AnyType, is ErrorType -> true
            else -> false
        }
    }
}

/**
 * 函数类型
 */
data class FunctionType(
    val parameters: List<ModelicaType>,
    val returnType: ModelicaType
) : ModelicaType() {
    override val name: String = "(${parameters.joinToString(",") { it.name }}) -> ${returnType.name}"
    override val dimensions: List<Int> = emptyList()
}

/**
 * 未定义类型（用于前向引用）
 */
data class UndefinedType(
    override val name: String
) : ModelicaType() {
    override val dimensions: List<Int> = emptyList()
}