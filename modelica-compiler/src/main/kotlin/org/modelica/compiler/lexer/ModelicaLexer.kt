package org.modelica.compiler.lexer

/**
 * Modelica词法分析器
 *
 * 将源代码字符串转换为Token序列
 * 支持完整的Modelica语言词法规则
 */
class ModelicaLexer(
    private val source: String,
    private val sourceFile: String? = null
) {
    /** 当前位置 */
    private var position: Int = 0

    /** 当前行号 */
    private var line: Int = 1

    /** 当前列号 */
    private var column: Int = 1

    /** 收集的错误列表 */
    private val errors: MutableList<LexerError> = mutableListOf()

    /** 是否到达文件末尾 */
    private val isAtEnd: Boolean get() = position >= source.length

    /** 当前字符 */
    private val currentChar: Char? get() = if (isAtEnd) null else source[position]

    /** 下一个字符 */
    private val nextChar: Char? get() = if (position + 1 >= source.length) null else source[position + 1]

    /**
     * 执行词法分析，返回Token序列
     */
    fun tokenize(): LexerResult {
        val tokens = mutableListOf<Token>()

        while (!isAtEnd) {
            val token = scanToken()
            if (token != null) {
                // 跳过空白和注释，不加入Token列表
                if (token.type != TokenType.WHITESPACE &&
                    token.type != TokenType.LINE_COMMENT &&
                    token.type != TokenType.BLOCK_COMMENT) {
                    tokens.add(token)
                }
            }
        }

        // 添加EOF Token
        tokens.add(Token(TokenType.EOF, "", currentLocation()))

        return LexerResult(tokens, errors.toList())
    }

    /**
     * 扫描单个Token
     */
    private fun scanToken(): Token? {
        val startLocation = currentLocation()
        val startChar = currentChar ?: return null

        return when {
            // 空白字符
            startChar.isWhitespace() -> scanWhitespace()

            // 注释
            startChar == '/' && nextChar == '/' -> scanLineComment()
            startChar == '/' && nextChar == '*' -> scanBlockComment()

            // 标识符和关键字
            startChar.isLetter() || startChar == '_' -> scanIdentifier()

            // 数字
            startChar.isDigit() -> scanNumber()

            // 字符串
            startChar == '"' -> scanString()

            // 操作符和分隔符
            else -> scanOperatorOrDelimiter()
        }?.also { token ->
            // 更新位置
            repeat(token.lexeme.length) { advance() }
        }
    }

    /**
     * 扫描空白字符
     */
    private fun scanWhitespace(): Token {
        val startLocation = currentLocation()
        val start = position

        while (currentChar?.isWhitespace() == true) {
            if (currentChar == '\n') {
                line++
                column = 1
            }
            advance()
        }

        val lexeme = source.substring(start, position)
        return Token(TokenType.WHITESPACE, lexeme, startLocation)
    }

    /**
     * 扫描单行注释
     */
    private fun scanLineComment(): Token {
        val startLocation = currentLocation()
        val start = position

        // 跳过 //
        advance()
        advance()

        // 读取直到行尾
        while (currentChar != null && currentChar != '\n') {
            advance()
        }

        val lexeme = source.substring(start, position)
        return Token(TokenType.LINE_COMMENT, lexeme, startLocation)
    }

    /**
     * 扫描多行注释
     */
    private fun scanBlockComment(): Token {
        val startLocation = currentLocation()
        val start = position

        // 跳过 /*
        advance()
        advance()

        var depth = 1
        while (!isAtEnd && depth > 0) {
            when {
                currentChar == '/' && nextChar == '*' -> {
                    advance()
                    advance()
                    depth++
                }
                currentChar == '*' && nextChar == '/' -> {
                    advance()
                    advance()
                    depth--
                }
                currentChar == '\n' -> {
                    line++
                    column = 1
                    advance()
                }
                else -> advance()
            }
        }

        if (depth > 0) {
            errors.add(LexerError(
                "Unterminated block comment",
                startLocation,
                LexerErrorType.UNTERMINATED_COMMENT
            ))
        }

        val lexeme = source.substring(start, position)
        return Token(TokenType.BLOCK_COMMENT, lexeme, startLocation)
    }

    /**
     * 扫描标识符或关键字
     */
    private fun scanIdentifier(): Token {
        val startLocation = currentLocation()
        val start = position

        // 读取标识符字符
        while (currentChar?.isLetterOrDigit() == true || currentChar == '_') {
            advance()
        }

        val lexeme = source.substring(start, position)

        // 检查是否为关键字
        val tokenType = TokenType.keywords[lexeme] ?: TokenType.IDENTIFIER

        return Token(tokenType, lexeme, startLocation)
    }

    /**
     * 扫描数字（整数或浮点数）
     */
    private fun scanNumber(): Token {
        val startLocation = currentLocation()
        val start = position

        // 读取整数部分
        while (currentChar?.isDigit() == true) {
            advance()
        }

        // 检查小数部分
        if (currentChar == '.' && nextChar?.isDigit() == true) {
            advance() // 消耗小数点
            while (currentChar?.isDigit() == true) {
                advance()
            }
        }

        // 检查指数部分
        if (currentChar == 'e' || currentChar == 'E') {
            advance()
            if (currentChar == '+' || currentChar == '-') {
                advance()
            }
            while (currentChar?.isDigit() == true) {
                advance()
            }
        }

        val lexeme = source.substring(start, position)
        return Token(TokenType.REAL_LITERAL, lexeme, startLocation)
    }

    /**
     * 扫描字符串
     */
    private fun scanString(): Token {
        val startLocation = currentLocation()
        val start = position

        advance() // 消耗开始的引号

        while (currentChar != null && currentChar != '"') {
            if (currentChar == '\\') {
                advance() // 跳过转义字符
                if (currentChar != null) {
                    advance()
                }
            } else if (currentChar == '\n') {
                // Modelica不支持多行字符串
                errors.add(LexerError(
                    "Unterminated string literal",
                    startLocation,
                    LexerErrorType.UNTERMINATED_STRING
                ))
                break
            } else {
                advance()
            }
        }

        if (isAtEnd) {
            errors.add(LexerError(
                "Unterminated string literal",
                startLocation,
                LexerErrorType.UNTERMINATED_STRING
            ))
        } else {
            advance() // 消耗结束的引号
        }

        val lexeme = source.substring(start, position)
        return Token(TokenType.STRING_LITERAL, lexeme, startLocation)
    }

    /**
     * 扫描操作符或分隔符
     */
    private fun scanOperatorOrDelimiter(): Token {
        val startLocation = currentLocation()
        val char = currentChar ?: return errorToken("Unexpected end of file")

        // 双字符操作符
        val twoChar = if (!isAtEnd && position + 1 < source.length) {
            source.substring(position, position + 2)
        } else ""

        val tokenType = when (twoChar) {
            ":=" -> TokenType.ASSIGN
            "==" -> TokenType.EQ
            "<>" -> TokenType.NE
            "<=" -> TokenType.LE
            ">=" -> TokenType.GE
            "->" -> TokenType.ARROW
            else -> null
        }

        if (tokenType != null) {
            advance()
            advance()
            return Token(tokenType, twoChar, startLocation)
        }

        // 单字符操作符和分隔符
        val singleTokenType = when (char) {
            '+' -> TokenType.PLUS
            '-' -> TokenType.MINUS
            '*' -> TokenType.STAR
            '/' -> TokenType.SLASH
            '^' -> TokenType.POWER
            '<' -> TokenType.LT
            '>' -> TokenType.GT
            '=' -> TokenType.EQUALS
            '(' -> TokenType.LPAREN
            ')' -> TokenType.RPAREN
            '[' -> TokenType.LBRACKET
            ']' -> TokenType.RBRACKET
            '{' -> TokenType.LBRACE
            '}' -> TokenType.RBRACE
            ',' -> TokenType.COMMA
            ';' -> TokenType.SEMICOLON
            ':' -> TokenType.COLON
            '.' -> TokenType.DOT
            else -> null
        }

        return if (singleTokenType != null) {
            advance()
            Token(singleTokenType, char.toString(), startLocation)
        } else {
            advance()
            errors.add(LexerError(
                "Illegal character: '$char'",
                startLocation,
                LexerErrorType.ILLEGAL_CHARACTER
            ))
            Token(TokenType.ERROR, char.toString(), startLocation)
        }
    }

    /**
     * 创建错误Token
     */
    private fun errorToken(message: String): Token {
        val location = currentLocation()
        errors.add(LexerError(message, location, LexerErrorType.UNKNOWN))
        return Token(TokenType.ERROR, "", location)
    }

    /**
     * 前进一个字符
     */
    private fun advance() {
        if (!isAtEnd) {
            position++
            column++
        }
    }

    /**
     * 获取当前位置
     */
    private fun currentLocation(): SourceLocation {
        return SourceLocation(line, column, position, sourceFile)
    }

    companion object {
        /**
         * 便捷方法：分析源代码并返回Token列表
         */
        fun tokenize(source: String, sourceFile: String? = null): LexerResult {
            return ModelicaLexer(source, sourceFile).tokenize()
        }
    }
}

/**
 * 词法分析结果
 *
 * @property tokens Token列表
 * @property errors 错误列表
 */
data class LexerResult(
    val tokens: List<Token>,
    val errors: List<LexerError>
) {
    /** 是否有错误 */
    val hasErrors: Boolean get() = errors.isNotEmpty()

    /** 获取所有错误消息 */
    val errorMessages: List<String> get() = errors.map { it.toString() }
}