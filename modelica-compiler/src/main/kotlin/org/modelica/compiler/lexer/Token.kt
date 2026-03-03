package org.modelica.compiler.lexer

/**
 * Token数据类，表示词法分析产生的词法单元
 *
 * @property type Token类型
 * @property lexeme Token的文本内容
 * @property location Token在源代码中的位置信息
 */
data class Token(
    val type: TokenType,
    val lexeme: String,
    val location: SourceLocation
) {
    /**
     * 获取Token的数值（用于字面量）
     */
    val value: Any? = when (type) {
        TokenType.INTEGER_LITERAL -> lexeme.toLongOrNull()
        TokenType.REAL_LITERAL -> lexeme.toDoubleOrNull()
        TokenType.STRING_LITERAL -> lexeme.removeSurrounding("\"")
        TokenType.TRUE -> true
        TokenType.FALSE -> false
        else -> null
    }

    override fun toString(): String {
        return when (type) {
            TokenType.EOF -> "EOF at ${location.line}:${location.column}"
            TokenType.ERROR -> "ERROR('$lexeme') at ${location.line}:${location.column}"
            TokenType.WHITESPACE, TokenType.LINE_COMMENT, TokenType.BLOCK_COMMENT -> type.name
            else -> "${type.name}('$lexeme') at ${location.line}:${location.column}"
        }
    }

    /**
     * 判断是否为字面量Token
     */
    fun isLiteral(): Boolean = type in listOf(
        TokenType.INTEGER_LITERAL,
        TokenType.REAL_LITERAL,
        TokenType.STRING_LITERAL,
        TokenType.TRUE,
        TokenType.FALSE
    )

    /**
     * 判断是否为关键字Token
     */
    fun isKeyword(): Boolean = type.description.contains("关键字")

    /**
     * 判断是否为操作符Token
     */
    fun isOperator(): Boolean = type.description.contains("运算符")

    /**
     * 判断是否为分隔符Token
     */
    fun isDelimiter(): Boolean = type in listOf(
        TokenType.LPAREN, TokenType.RPAREN,
        TokenType.LBRACKET, TokenType.RBRACKET,
        TokenType.LBRACE, TokenType.RBRACE,
        TokenType.COMMA, TokenType.SEMICOLON,
        TokenType.COLON, TokenType.DOT
    )
}

/**
 * 源代码位置信息
 *
 * @property line 行号（从1开始）
 * @property column 列号（从1开始）
 * @property offset 字符偏移量（从0开始）
 * @property sourceFile 源文件名（可选）
 */
data class SourceLocation(
    val line: Int,
    val column: Int,
    val offset: Int,
    val sourceFile: String? = null
) {
    override fun toString(): String = "$line:$column"

    /**
     * 创建一个新的位置（前进n个字符）
     */
    fun advance(n: Int = 1): SourceLocation {
        return SourceLocation(line, column + n, offset + n, sourceFile)
    }

    /**
     * 创建一个新行的位置
     */
    fun newLine(): SourceLocation {
        return SourceLocation(line + 1, 1, offset + 1, sourceFile)
    }

    companion object {
        /** 初始位置 */
        val START = SourceLocation(1, 1, 0, null)

        /** 创建指定文件的初始位置 */
        fun startOfFile(fileName: String) = SourceLocation(1, 1, 0, fileName)
    }
}

/**
 * Token范围，表示从起始位置到结束位置
 */
data class TokenRange(
    val start: SourceLocation,
    val end: SourceLocation
) {
    val length: Int get() = end.offset - start.offset

    fun contains(location: SourceLocation): Boolean {
        return location.offset >= start.offset && location.offset <= end.offset
    }

    companion object {
        /** 从单个Token创建范围 */
        fun of(token: Token): TokenRange {
            val endLocation = token.location.advance(token.lexeme.length)
            return TokenRange(token.location, endLocation)
        }

        /** 从两个Token创建范围 */
        fun between(start: Token, end: Token): TokenRange {
            val endLocation = end.location.advance(end.lexeme.length)
            return TokenRange(start.location, endLocation)
        }
    }
}