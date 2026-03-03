package org.modelica.compiler

import org.modelica.compiler.lexer.ModelicaLexer
import org.modelica.compiler.parser.ModelicaParser
import org.modelica.compiler.semantic.SemanticAnalyzer
import org.modelica.compiler.ir.IRBuilder
import org.modelica.compiler.codegen.BytecodeGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName

/**
 * Modelica编译器测试套件
 */
class CompilerTest {

    // ==================== 词法分析测试 ====================

    @Test
    @DisplayName("Lexer: 简单模型词法分析")
    fun testLexerSimpleModel() {
        val source = """
            model Simple
              Real x;
            equation
              der(x) = -x;
            end Simple;
        """.trimIndent()

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors, "词法分析不应有错误: ${result.errorMessages}")
        assertTrue(result.tokens.isNotEmpty())

        // 验证关键Token存在
        val tokenTypes = result.tokens.map { it.type.name }
        assertTrue(tokenTypes.contains("MODEL"))
        assertTrue(tokenTypes.contains("REAL"))
        assertTrue(tokenTypes.contains("EQUATION"))
        assertTrue(tokenTypes.contains("DER"))
        assertTrue(tokenTypes.contains("END"))
    }

    @Test
    @DisplayName("Lexer: 数学表达式词法分析")
    fun testLexerMathExpression() {
        val source = "y = a * sin(x) + b * cos(x);"

        val result = ModelicaLexer.tokenize(source)

        assertFalse(result.hasErrors)
        val lexemes = result.tokens.map { it.lexeme }
        assertTrue(lexemes.contains("sin"))
        assertTrue(lexemes.contains("cos"))
    }

    // ==================== 语法分析测试 ====================

    @Test
    @DisplayName("Parser: HelloWorld模型解析")
    fun testParserHelloWorld() {
        val source = """
            model HelloWorld
              Real x(start = 1);
            equation
              der(x) = -x;
            end HelloWorld;
        """.trimIndent()

        val lexerResult = ModelicaLexer.tokenize(source)
        assertFalse(lexerResult.hasErrors)

        val parseResult = ModelicaParser.parse(lexerResult.tokens)

        if (parseResult.hasErrors) {
            parseResult.errors.forEach { println("Parse error: $it") }
        }
        assertFalse(parseResult.hasErrors, "语法分析不应有错误")
        assertNotNull(parseResult.root)

        val program = parseResult.root!!
        assertEquals(1, program.classes.size)
        assertEquals("HelloWorld", program.classes[0].name)
    }

    @Test
    @DisplayName("Parser: 带参数的模型解析")
    fun testParserParameterizedModel() {
        val source = """
            model Pendulum
              parameter Real L = 1.0 "Length";
              parameter Real g = 9.81 "Gravity";
              Real theta(start = 0.1);
              Real omega(start = 0);
            equation
              der(theta) = omega;
              der(omega) = -(g/L) * sin(theta);
            end Pendulum;
        """.trimIndent()

        val lexerResult = ModelicaLexer.tokenize(source)
        val parseResult = ModelicaParser.parse(lexerResult.tokens)

        assertFalse(parseResult.hasErrors, "语法分析不应有错误: ${parseResult.errors}")
        assertNotNull(parseResult.root)
    }

    @Test
    @DisplayName("Parser: 连接方程解析")
    fun testParserConnectEquation() {
        val source = """
            model Circuit
              Real v1, v2, i;
            equation
              connect(v1, v2);
              i = v1 - v2;
            end Circuit;
        """.trimIndent()

        val lexerResult = ModelicaLexer.tokenize(source)
        val parseResult = ModelicaParser.parse(lexerResult.tokens)

        assertFalse(parseResult.hasErrors)
    }

    @Test
    @DisplayName("Parser: 算法段解析")
    fun testParserAlgorithmSection() {
        val source = """
            model WithAlgorithm
              Real x, y;
            algorithm
              x := 1.0;
              y := x * 2.0;
              if y > 1.0 then
                x := 0.0;
              end if;
            end WithAlgorithm;
        """.trimIndent()

        val lexerResult = ModelicaLexer.tokenize(source)
        val parseResult = ModelicaParser.parse(lexerResult.tokens)

        if (parseResult.hasErrors) {
            parseResult.errors.forEach { println("Error: $it") }
        }
        assertFalse(parseResult.hasErrors)
    }

    // ==================== 语义分析测试 ====================

    @Test
    @DisplayName("Semantic: 类型检查")
    fun testSemanticTypeChecking() {
        val source = """
            model TypeCheck
              Real x;
              Integer n;
              Boolean b;
            equation
              x = 1.0;
              n = 1;
              b = true;
            end TypeCheck;
        """.trimIndent()

        val lexerResult = ModelicaLexer.tokenize(source)
        val parseResult = ModelicaParser.parse(lexerResult.tokens)

        assertFalse(parseResult.hasErrors)

        val analyzer = SemanticAnalyzer()
        val semanticResult = analyzer.analyze(parseResult.root!!)

        // 应该没有类型错误
        assertTrue(semanticResult.success || semanticResult.errors.isEmpty())
    }

    // ==================== 完整编译测试 ====================

    @Test
    @DisplayName("Compile: HelloWorld完整编译")
    fun testCompileHelloWorld() {
        val source = """
            model HelloWorld
              Real x(start = 1);
            equation
              der(x) = -x;
            end HelloWorld;
        """.trimIndent()

        val compiler = ModelicaCompiler()
        val result = compiler.compile(source, "HelloWorld.mo")

        assertTrue(result.success, "编译应该成功: ${result.allErrors}")
        assertTrue(result.outputClasses.containsKey("HelloWorld"))
    }

    @Test
    @DisplayName("Compile: 简单振荡器")
    fun testCompileOscillator() {
        val source = """
            model Oscillator
              Real x(start = 1);
              Real y(start = 0);
              parameter Real w = 1.0 "Angular frequency";
            equation
              der(x) = y;
              der(y) = -w * w * x;
            end Oscillator;
        """.trimIndent()

        val compiler = ModelicaCompiler()
        val result = compiler.compile(source)

        assertTrue(result.success, "编译应该成功: ${result.allErrors}")
    }

    @Test
    @DisplayName("Compile: 带函数调用的模型")
    fun testCompileWithFunctions() {
        val source = """
            model WithFunctions
              Real x;
              Real y;
            equation
              x = sin(time) + cos(time);
              y = sqrt(x * x + 1);
            end WithFunctions;
        """.trimIndent()

        val compiler = ModelicaCompiler()
        val result = compiler.compile(source)

        assertTrue(result.success, "编译应该成功: ${result.allErrors}")
    }
}