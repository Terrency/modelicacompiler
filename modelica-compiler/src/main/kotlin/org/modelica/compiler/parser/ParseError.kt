package org.modelica.compiler.parser

import org.modelica.compiler.lexer.SourceLocation
import org.modelica.compiler.lexer.Token
import org.modelica.compiler.lexer.TokenType

/**
 * 语法分析错误
 *
 * @property message 错误消息
 * @property location 错误位置
 * @property expected 期望的Token类型
 * @property actual 实际的Token
 */
data class ParseError(
    override val message: String,
    val location: SourceLocation,
    val expected: List<TokenType> = emptyList(),
    val actual: Token? = null
) : Exception(message) {

    override fun toString(): String {
        val expectedStr = if (expected.isNotEmpty()) {
            ", expected: ${expected.joinToString(", ") { it.name }}"
        } else ""
        val actualStr = actual?.let { ", got: ${it.type.name}('${it.lexeme}')" } ?: ""
        return "ParseError at $location: $message$expectedStr$actualStr"
    }

    companion object {
        /**
         * 创建"期望某个Token"的错误
         */
        fun expected(expectedType: TokenType, actual: Token): ParseError {
            return ParseError(
                "Expected ${expectedType.name}",
                actual.location,
                listOf(expectedType),
                actual
            )
        }

        /**
         * 创建"期望多个Token之一"的错误
         */
        fun expectedOneOf(expectedTypes: List<TokenType>, actual: Token): ParseError {
            return ParseError(
                "Expected one of: ${expectedTypes.joinToString(", ") { it.name }}",
                actual.location,
                expectedTypes,
                actual
            )
        }

        /**
         * 创建"意外的Token"错误
         */
        fun unexpected(token: Token): ParseError {
            return ParseError(
                "Unexpected token: ${token.type.name}('${token.lexeme}')",
                token.location,
                emptyList(),
                token
            )
        }
    }
}

/**
 * 语法错误类型枚举
 */
enum class ParseErrorType {
    /** 语法错误 */
    SYNTAX_ERROR,

    /** 缺少必要的Token */
    MISSING_TOKEN,

    /** 意外的Token */
    UNEXPECTED_TOKEN,

    /** 无效的表达式 */
    INVALID_EXPRESSION,

    /** 无效的语句 */
    INVALID_STATEMENT,

    /** 无效的声明 */
    INVALID_DECLARATION,

    /** 缺少结束标记 */
    MISSING_END
}