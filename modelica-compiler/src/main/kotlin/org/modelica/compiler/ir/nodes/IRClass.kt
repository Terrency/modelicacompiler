package org.modelica.compiler.ir.nodes

/**
 * IR类定义
 */
data class IRClass(
    val name: String,
    val superClass: String = "java/lang/Object",
    val interfaces: List<String> = emptyList(),
    val fields: List<IRField> = emptyList(),
    val methods: List<IRMethod> = emptyList()
)

/**
 * IR字段定义
 */
data class IRField(
    val name: String,
    val type: String,
    val isStatic: Boolean = false,
    val isFinal: Boolean = false
)

/**
 * IR方法定义
 */
data class IRMethod(
    val name: String,
    val returnType: String,
    val parameters: List<IRParameter>,
    val body: List<IRStatement>,
    val isStatic: Boolean = false,
    val isConstructor: Boolean = false
)

/**
 * IR参数定义
 */
data class IRParameter(
    val name: String,
    val type: String
)