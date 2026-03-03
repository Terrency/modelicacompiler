package org.modelica.compiler.lexer

/**
 * 词法分析错误
 *
 * @property message 错误消息
 * @property location 错误位置
 * @property errorType 错误类型
 */
data class LexerError(
    override val message: String,
    val location: SourceLocation,
    val errorType: LexerErrorType = LexerErrorType.UNKNOWN
) : Exception(message) {

    override fun toString(): String {
        return "LexerError at $location: $message (${errorType.name})"
    }
}

/**
 * 词法错误类型枚举
 */
enum class LexerErrorType {
    /** 未知错误 */
    UNKNOWN,

    /** 非法字符 */
    ILLEGAL_CHARACTER,

    /** 未终止的字符串 */
    UNTERMINATED_STRING,

    /** 未终止的注释 */
    UNTERMINATED_COMMENT,

    /** 无效的数字格式 */
    INVALID_NUMBER_FORMAT,

    /** 无效的转义序列 */
    INVALID_ESCAPE_SEQUENCE
}