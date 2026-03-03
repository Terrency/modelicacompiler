package org.modelica.ide

import org.modelica.compiler.CompilationOptions
import org.modelica.compiler.CompilationResult
import org.modelica.compiler.ModelicaCompiler
import java.io.File

/**
 * 编译器桥接服务
 *
 * 为Electron桌面IDE提供编译器访问接口
 */
class CompilerBridge {

    private var compiler: ModelicaCompiler? = null

    /**
     * 初始化编译器
     */
    fun initialize(outputDir: String): Boolean {
        return try {
            val options = CompilationOptions(
                outputDirectory = File(outputDir),
                verbose = true
            )
            compiler = ModelicaCompiler(options)
            true
        } catch (e: Exception) {
            System.err.println("Failed to initialize compiler: ${e.message}")
            false
        }
    }

    /**
     * 编译代码
     *
     * @param source Modelica源代码
     * @param fileName 文件名（可选）
     * @return 编译结果JSON字符串
     */
    fun compile(source: String, fileName: String? = null): CompileResponse {
        val comp = compiler ?: return CompileResponse(
            success = false,
            errors = listOf("Compiler not initialized"),
            outputFiles = emptyList()
        )

        return try {
            val result = comp.compile(source, fileName)

            CompileResponse(
                success = result.success,
                errors = result.allErrors,
                outputFiles = result.outputClasses.keys.toList()
            )
        } catch (e: Exception) {
            CompileResponse(
                success = false,
                errors = listOf("Compilation error: ${e.message}"),
                outputFiles = emptyList()
            )
        }
    }

    /**
     * 编译文件
     */
    fun compileFile(filePath: String): CompileResponse {
        val comp = compiler ?: return CompileResponse(
            success = false,
            errors = listOf("Compiler not initialized"),
            outputFiles = emptyList()
        )

        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return CompileResponse(
                    success = false,
                    errors = listOf("File not found: $filePath"),
                    outputFiles = emptyList()
                )
            }

            val result = comp.compileFile(file)

            CompileResponse(
                success = result.success,
                errors = result.allErrors,
                outputFiles = result.outputClasses.keys.toList()
            )
        } catch (e: Exception) {
            CompileResponse(
                success = false,
                errors = listOf("Compilation error: ${e.message}"),
                outputFiles = emptyList()
            )
        }
    }

    /**
     * 获取语法高亮信息
     */
    fun getSyntaxInfo(source: String): SyntaxInfoResponse {
        val lexer = org.modelica.compiler.lexer.ModelicaLexer(source)
        val result = lexer.tokenize()

        val tokens = result.tokens.map { token ->
            TokenInfo(
                type = token.type.name,
                lexeme = token.lexeme,
                line = token.location.line,
                column = token.location.column,
                start = token.location.offset,
                end = token.location.offset + token.lexeme.length
            )
        }

        val errors = result.errors.map { error ->
            ErrorInfo(
                message = error.message,
                line = error.location.line,
                column = error.location.column
            )
        }

        return SyntaxInfoResponse(tokens, errors)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val bridge = CompilerBridge()

            if (args.isEmpty()) {
                println("Usage: CompilerBridge <command> [args...]")
                println("Commands:")
                println("  compile <source>  - Compile source code")
                println("  file <path>       - Compile file")
                println("  syntax <source>   - Get syntax info")
                return
            }

            when (args[0]) {
                "compile" -> {
                    bridge.initialize(".")
                    if (args.size > 1) {
                        val result = bridge.compile(args[1])
                        println(result.toJson())
                    }
                }
                "file" -> {
                    bridge.initialize(".")
                    if (args.size > 1) {
                        val result = bridge.compileFile(args[1])
                        println(result.toJson())
                    }
                }
                "syntax" -> {
                    if (args.size > 1) {
                        val result = bridge.getSyntaxInfo(args[1])
                        println(result.toJson())
                    }
                }
                else -> {
                    println("Unknown command: ${args[0]}")
                }
            }
        }
    }
}

/**
 * 编译响应
 */
data class CompileResponse(
    val success: Boolean,
    val errors: List<String>,
    val outputFiles: List<String>
) {
    fun toJson(): String {
        val errorsJson = errors.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
        val filesJson = outputFiles.joinToString(",") { "\"$it\"" }
        return """{"success":$success,"errors":[$errorsJson],"outputFiles":[$filesJson]}"""
    }
}

/**
 * 语法信息响应
 */
data class SyntaxInfoResponse(
    val tokens: List<TokenInfo>,
    val errors: List<ErrorInfo>
) {
    fun toJson(): String {
        val tokensJson = tokens.joinToString(",") { it.toJson() }
        val errorsJson = errors.joinToString(",") { it.toJson() }
        return """{"tokens":[$tokensJson],"errors":[$errorsJson]}"""
    }
}

/**
 * Token信息
 */
data class TokenInfo(
    val type: String,
    val lexeme: String,
    val line: Int,
    val column: Int,
    val start: Int,
    val end: Int
) {
    fun toJson(): String {
        return """{"type":"$type","lexeme":"${lexeme.replace("\"", "\\\"")}","line":$line,"column":$column,"start":$start,"end":$end}"""
    }
}

/**
 * 错误信息
 */
data class ErrorInfo(
    val message: String,
    val line: Int,
    val column: Int
) {
    fun toJson(): String {
        return """{"message":"${message.replace("\"", "\\\"")}","line":$line,"column":$column}"""
    }
}