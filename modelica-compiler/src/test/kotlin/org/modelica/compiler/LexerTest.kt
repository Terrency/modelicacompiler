package org.modelica.compiler

import org.modelica.compiler.lexer.ModelicaLexer
import org.modelica.compiler.lexer.TokenType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LexerTest {

    @Test
    fun testTokenizeSimpleModel() {
        val source = """
            model SimpleModel
              Real x;
              Real y;
            equation
              x = y + 1.0;
            end SimpleModel;
        """.trimIndent()

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors, "Lexer should not have errors")

        // 检查关键Token
        val tokens = result.tokens
        assertTrue(tokens.any { it.type == TokenType.MODEL })
        assertTrue(tokens.any { it.type == TokenType.IDENTIFIER && it.lexeme == "SimpleModel" })
        assertTrue(tokens.any { it.type == TokenType.REAL })
        assertTrue(tokens.any { it.type == TokenType.EQUATION })
        assertTrue(tokens.any { it.type == TokenType.END })
    }

    @Test
    fun testTokenizeNumbers() {
        val source = "Real x = 3.14; Integer n = 42;"

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors)
        assertTrue(result.tokens.any { it.type == TokenType.REAL_LITERAL && it.lexeme == "3.14" })
        assertTrue(result.tokens.any { it.type == TokenType.INTEGER_LITERAL && it.lexeme == "42" })
    }

    @Test
    fun testTokenizeKeywords() {
        val source = """
            model Test
              parameter Real p = 1.0;
              input Real u;
              output Real y;
            equation
              der(x) = -x + u;
              y = x;
            end Test;
        """.trimIndent()

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors)
        assertTrue(result.tokens.any { it.type == TokenType.PARAMETER })
        assertTrue(result.tokens.any { it.type == TokenType.INPUT })
        assertTrue(result.tokens.any { it.type == TokenType.OUTPUT })
        assertTrue(result.tokens.any { it.type == TokenType.DER })
    }

    @Test
    fun testTokenizeOperators() {
        val source = "a + b - c * d / e ^ f"

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors)
        assertTrue(result.tokens.any { it.type == TokenType.PLUS })
        assertTrue(result.tokens.any { it.type == TokenType.MINUS })
        assertTrue(result.tokens.any { it.type == TokenType.STAR })
        assertTrue(result.tokens.any { it.type == TokenType.SLASH })
        assertTrue(result.tokens.any { it.type == TokenType.POWER })
    }

    @Test
    fun testTokenizeComparisonOperators() {
        val source = "a == b and c <> d or e < f and g <= h or i > j and k >= m"

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors)
        assertTrue(result.tokens.any { it.type == TokenType.EQ })
        assertTrue(result.tokens.any { it.type == TokenType.NE })
        assertTrue(result.tokens.any { it.type == TokenType.LT })
        assertTrue(result.tokens.any { it.type == TokenType.LE })
        assertTrue(result.tokens.any { it.type == TokenType.GT })
        assertTrue(result.tokens.any { it.type == TokenType.GE })
        assertTrue(result.tokens.any { it.type == TokenType.AND })
        assertTrue(result.tokens.any { it.type == TokenType.OR })
    }

    @Test
    fun testTokenizeComments() {
        val source = """
            // This is a line comment
            Real x; /* This is a
               block comment */
            Real y;
        """.trimIndent()

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors)
        // 注释被过滤掉，不应该出现在tokens中
        assertTrue(result.tokens.none { it.type == TokenType.LINE_COMMENT })
        assertTrue(result.tokens.none { it.type == TokenType.BLOCK_COMMENT })
    }

    @Test
    fun testTokenizeStrings() {
        val source = """String name = "Hello World";"""

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors)
        val stringToken = result.tokens.find { it.type == TokenType.STRING_LITERAL }
        assertNotNull(stringToken)
        assertEquals("\"Hello World\"", stringToken?.lexeme)
    }
}