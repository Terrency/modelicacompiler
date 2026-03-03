package org.modelica.compiler.parser

/**
 * 语法分析结果
 *
 * @property root AST根节点
 * @property errors 错误列表
 */
data class ParseResult<T>(
    val root: T?,
    val errors: List<ParseError>
) {
    /** 是否有错误 */
    val hasErrors: Boolean get() = errors.isNotEmpty()

    /** 是否成功 */
    val isSuccess: Boolean get() = root != null && !hasErrors

    /** 获取所有错误消息 */
    val errorMessages: List<String> get() = errors.map { it.toString() }

    companion object {
        /** 创建成功结果 */
        fun <T> success(root: T): ParseResult<T> = ParseResult(root, emptyList())

        /** 创建失败结果 */
        fun <T> failure(errors: List<ParseError>): ParseResult<T> = ParseResult(null, errors)

        /** 创建单个错误的失败结果 */
        fun <T> failure(error: ParseError): ParseResult<T> = ParseResult(null, listOf(error))
    }
}