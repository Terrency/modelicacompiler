package org.modelica.compiler

import org.modelica.compiler.ast.nodes.Program
import org.modelica.compiler.codegen.BytecodeGenerator
import org.modelica.compiler.lexer.LexerResult
import org.modelica.compiler.lexer.ModelicaLexer
import org.modelica.compiler.parser.ModelicaParser
import org.modelica.compiler.parser.ParseResult
import org.modelica.compiler.ir.IRBuilder
import org.modelica.compiler.semantic.SemanticAnalyzer
import org.modelica.compiler.semantic.SemanticResult
import java.io.File

/**
 * 编译结果
 */
data class CompilationResult(
    val success: Boolean,
    val lexerErrors: List<String> = emptyList(),
    val parserErrors: List<String> = emptyList(),
    val semanticErrors: List<String> = emptyList(),
    val codegenErrors: List<String> = emptyList(),
    val outputClasses: Map<String, ByteArray> = emptyMap()
) {
    val allErrors: List<String>
        get() = lexerErrors + parserErrors + semanticErrors + codegenErrors

    val hasErrors: Boolean
        get() = allErrors.isNotEmpty()
}

/**
 * 编译选项
 */
data class CompilationOptions(
    val outputDirectory: File = File("."),
    val verbose: Boolean = false,
    val emitAst: Boolean = false,
    val emitIr: Boolean = false
)

/**
 * Modelica编译器主类
 *
 * 完整的编译流程：
 * 1. 词法分析 (Lexer)
 * 2. 语法分析 (Parser)
 * 3. 语义分析 (Semantic Analyzer)
 * 4. 中间表示生成 (IR Builder)
 * 5. 字节码生成 (Bytecode Generator)
 */
class ModelicaCompiler(
    private val options: CompilationOptions = CompilationOptions()
) {
    private val irBuilder = IRBuilder()
    private val bytecodeGenerator = BytecodeGenerator()

    /**
     * 编译源代码字符串
     */
    fun compile(source: String, fileName: String? = null): CompilationResult {
        if (options.verbose) {
            println("=== Starting compilation ===")
            if (fileName != null) println("File: $fileName")
        }

        // 阶段1：词法分析
        if (options.verbose) println("\n--- Phase 1: Lexical Analysis ---")
        val lexerResult = ModelicaLexer.tokenize(source, fileName)
        if (lexerResult.hasErrors) {
            return CompilationResult(
                success = false,
                lexerErrors = lexerResult.errorMessages
            )
        }
        if (options.verbose) {
            println("Generated ${lexerResult.tokens.size} tokens")
        }

        // 阶段2：语法分析
        if (options.verbose) println("\n--- Phase 2: Syntax Analysis ---")
        val parseResult = ModelicaParser.parse(lexerResult.tokens)
        if (parseResult.hasErrors) {
            return CompilationResult(
                success = false,
                lexerErrors = lexerResult.errorMessages,
                parserErrors = parseResult.errors.map { it.toString() }
            )
        }
        val program = parseResult.root!!
        if (options.verbose) {
            println("Parsed ${program.classes.size} classes")
        }

        // 可选：输出AST
        if (options.emitAst) {
            val astFile = File(options.outputDirectory, "${fileName?.removeSuffix(".mo") ?: "output"}.ast")
            astFile.writeText(prettyPrintAst(program))
            if (options.verbose) println("AST written to: ${astFile.absolutePath}")
        }

        // 阶段3：语义分析
        if (options.verbose) println("\n--- Phase 3: Semantic Analysis ---")
        val semanticResult = SemanticAnalyzer().analyze(program)
        if (!semanticResult.success) {
            return CompilationResult(
                success = false,
                lexerErrors = lexerResult.errorMessages,
                parserErrors = parseResult.errors.map { it.toString() },
                semanticErrors = semanticResult.errors.map { it.toString() }
            )
        }
        if (options.verbose) {
            println("Semantic analysis completed successfully")
        }

        // 阶段4：IR生成
        if (options.verbose) println("\n--- Phase 4: IR Generation ---")
        val irModule = irBuilder.build(program)
        if (options.verbose) {
            println("Generated IR for ${irModule.classes.size} classes")
        }

        // 可选：输出IR
        if (options.emitIr) {
            val irFile = File(options.outputDirectory, "${fileName?.removeSuffix(".mo") ?: "output"}.ir")
            irFile.writeText(prettyPrintIr(irModule))
            if (options.verbose) println("IR written to: ${irFile.absolutePath}")
        }

        // 阶段5：字节码生成
        if (options.verbose) println("\n--- Phase 5: Bytecode Generation ---")
        val codegenResult = bytecodeGenerator.generate(irModule)
        if (codegenResult.hasErrors) {
            return CompilationResult(
                success = false,
                lexerErrors = lexerResult.errorMessages,
                parserErrors = parseResult.errors.map { it.toString() },
                semanticErrors = semanticResult.errors.map { it.toString() },
                codegenErrors = codegenResult.errors
            )
        }

        // 写入class文件
        BytecodeGenerator.writeClasses(codegenResult.classes, options.outputDirectory)
        if (options.verbose) {
            println("Generated ${codegenResult.classes.size} class files")
            codegenResult.classes.keys.forEach { className ->
                println("  - $className.class")
            }
        }

        if (options.verbose) println("\n=== Compilation successful ===")

        return CompilationResult(
            success = true,
            outputClasses = codegenResult.classes
        )
    }

    /**
     * 编译文件
     */
    fun compileFile(file: File): CompilationResult {
        val source = file.readText()
        return compile(source, file.name)
    }

    /**
     * 编译多个文件
     */
    fun compileFiles(files: List<File>): CompilationResult {
        // 合并所有源文件
        val combinedSource = files.joinToString("\n\n") { it.readText() }
        return compile(combinedSource, files.firstOrNull()?.name)
    }

    /**
     * 简单打印AST（用于调试）
     */
    private fun prettyPrintAst(program: Program): String {
        val sb = StringBuilder()
        sb.appendLine("Program:")
        program.imports.forEach { imp ->
            sb.appendLine("  Import: ${imp.path}${imp.alias?.let { " as $it" } ?: ""}")
        }
        program.classes.forEach { cls ->
            sb.appendLine("  Class: ${cls.name} (${cls.classType})")
            cls.composition.elements.forEach { elem ->
                when (elem) {
                    is org.modelica.compiler.ast.nodes.ComponentDeclaration -> {
                        val type = elem.type.name
                        elem.components.forEach { comp ->
                            sb.appendLine("    - $type ${comp.name}")
                        }
                    }
                    is org.modelica.compiler.ast.nodes.ExtendsClause -> {
                        sb.appendLine("    extends ${elem.baseClass}")
                    }
                    else -> {}
                }
            }
            cls.composition.equations.forEach { eq ->
                sb.appendLine("    equation: $eq")
            }
        }
        return sb.toString()
    }

    /**
     * 简单打印IR（用于调试）
     */
    private fun prettyPrintIr(module: org.modelica.compiler.ir.IRModule): String {
        val sb = StringBuilder()
        sb.appendLine("IR Module: ${module.name}")
        module.classes.forEach { cls ->
            sb.appendLine("  Class: ${cls.name}")
            cls.fields.forEach { field ->
                sb.appendLine("    field: ${field.type} ${field.name}")
            }
            cls.methods.forEach { method ->
                sb.appendLine("    method: ${method.returnType} ${method.name}(${method.parameters.joinToString(", ") { "${it.type} ${it.name}" }})")
            }
        }
        return sb.toString()
    }

    companion object {
        /**
         * 命令行入口
         */
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) {
                println("Usage: ModelicaCompiler <source.mo> [-o <outputDir>] [-v] [--emit-ast] [--emit-ir]")
                println()
                println("Options:")
                println("  -o <dir>     Output directory (default: current directory)")
                println("  -v           Verbose output")
                println("  --emit-ast   Emit AST to file")
                println("  --emit-ir    Emit IR to file")
                return
            }

            var inputFile: File? = null
            var outputDir = File(".")
            var verbose = false
            var emitAst = false
            var emitIr = false

            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "-o" -> {
                        outputDir = File(args[++i])
                    }
                    "-v" -> verbose = true
                    "--emit-ast" -> emitAst = true
                    "--emit-ir" -> emitIr = true
                    else -> {
                        if (!args[i].startsWith("-")) {
                            inputFile = File(args[i])
                        }
                    }
                }
                i++
            }

            if (inputFile == null) {
                System.err.println("Error: No input file specified")
                return
            }

            if (!inputFile.exists()) {
                System.err.println("Error: File not found: ${inputFile.absolutePath}")
                return
            }

            val options = CompilationOptions(
                outputDirectory = outputDir,
                verbose = verbose,
                emitAst = emitAst,
                emitIr = emitIr
            )

            val compiler = ModelicaCompiler(options)
            val result = compiler.compileFile(inputFile)

            if (result.hasErrors) {
                System.err.println("Compilation failed:")
                result.allErrors.forEach { System.err.println("  $it") }
                System.exit(1)
            } else {
                println("Compilation successful: ${result.outputClasses.size} classes generated")
            }
        }
    }
}