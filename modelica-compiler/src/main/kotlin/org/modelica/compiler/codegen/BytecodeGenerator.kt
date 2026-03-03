package org.modelica.compiler.codegen

import org.modelica.compiler.ir.*
import org.modelica.compiler.ir.nodes.*
import org.objectweb.asm.*
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import java.io.File
import java.io.FileOutputStream

/**
 * 字节码生成器
 *
 * 使用ASM库将IR转换为Java字节码
 */
class BytecodeGenerator {

    /**
     * 生成字节码结果
     */
    data class GenerationResult(
        val classes: Map<String, ByteArray>,
        val errors: List<String>
    ) {
        val hasErrors: Boolean get() = errors.isNotEmpty()
    }

    /**
     * 从IR模块生成字节码
     */
    fun generate(module: IRModule): GenerationResult {
        val errors = mutableListOf<String>()
        val classes = mutableMapOf<String, ByteArray>()

        try {
            module.classes.forEach { irClass ->
                val bytecode = generateClass(irClass)
                classes[irClass.name] = bytecode
            }
        } catch (e: Exception) {
            errors.add("Code generation error: ${e.message}")
        }

        return GenerationResult(classes, errors)
    }

    /**
     * 生成单个类的字节码
     */
    private fun generateClass(irClass: IRClass): ByteArray {
        val className = irClass.name.replace(".", "/")
        val superClassName = irClass.superClass.replace(".", "/")

        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)

        // 开始类定义
        classWriter.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC,
            className,
            null,
            superClassName,
            irClass.interfaces.toTypedArray()
        )

        // 生成字段
        irClass.fields.forEach { field ->
            generateField(classWriter, field)
        }

        // 生成默认构造函数
        generateDefaultConstructor(classWriter, superClassName)

        // 生成方法
        irClass.methods.forEach { method ->
            generateMethod(classWriter, method, className)
        }

        classWriter.visitEnd()
        return classWriter.toByteArray()
    }

    /**
     * 生成字段
     */
    private fun generateField(classWriter: ClassWriter, field: IRField) {
        val access = when {
            field.isStatic && field.isFinal -> Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
            field.isStatic -> Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC
            field.isFinal -> Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL
            else -> Opcodes.ACC_PUBLIC
        }

        val fieldVisitor = classWriter.visitField(
            access,
            field.name,
            mapTypeToDescriptor(field.type),
            null,
            null
        )
        fieldVisitor.visitEnd()
    }

    /**
     * 生成默认构造函数
     */
    private fun generateDefaultConstructor(classWriter: ClassWriter, superClassName: String) {
        val methodVisitor = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )

        methodVisitor.visitCode()
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
        methodVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            superClassName,
            "<init>",
            "()V",
            false
        )
        methodVisitor.visitInsn(Opcodes.RETURN)
        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()
    }

    /**
     * 生成方法
     */
    private fun generateMethod(classWriter: ClassWriter, method: IRMethod, className: String) {
        val access = when {
            method.isStatic && method.isConstructor -> Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC
            method.isStatic -> Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC
            method.isConstructor -> Opcodes.ACC_PUBLIC
            else -> Opcodes.ACC_PUBLIC
        }

        val descriptor = buildMethodDescriptor(method)
        val methodVisitor = classWriter.visitMethod(
            access,
            method.name,
            descriptor,
            null,
            null
        )

        generateMethodBody(methodVisitor, method, className)
    }

    /**
     * 生成方法体
     */
    private fun generateMethodBody(
        methodVisitor: MethodVisitor,
        method: IRMethod,
        className: String
    ) {
        methodVisitor.visitCode()

        val localVariables = mutableMapOf<String, Int>()
        var nextLocal = if (method.isStatic) 0 else 1

        // 注册参数
        method.parameters.forEach { param ->
            localVariables[param.name] = nextLocal
            nextLocal += getTypeSize(param.type)
        }

        // 生成语句
        method.body.forEach { stmt ->
            generateStatement(methodVisitor, stmt, localVariables, className, nextLocal)
        }

        // 如果方法没有返回语句，添加默认返回
        if (method.body.isEmpty() || method.body.last() !is IRStatement.Return) {
            when (method.returnType) {
                "void" -> methodVisitor.visitInsn(Opcodes.RETURN)
                "int", "boolean", "byte", "char", "short" -> {
                    methodVisitor.visitInsn(Opcodes.ICONST_0)
                    methodVisitor.visitInsn(Opcodes.IRETURN)
                }
                "long" -> {
                    methodVisitor.visitInsn(Opcodes.LCONST_0)
                    methodVisitor.visitInsn(Opcodes.LRETURN)
                }
                "float" -> {
                    methodVisitor.visitInsn(Opcodes.FCONST_0)
                    methodVisitor.visitInsn(Opcodes.FRETURN)
                }
                "double" -> {
                    methodVisitor.visitInsn(Opcodes.DCONST_0)
                    methodVisitor.visitInsn(Opcodes.DRETURN)
                }
                else -> {
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                    methodVisitor.visitInsn(Opcodes.ARETURN)
                }
            }
        }

        methodVisitor.visitMaxs(0, 0) // 自动计算
        methodVisitor.visitEnd()
    }

    /**
     * 生成语句
     */
    private fun generateStatement(
        mv: MethodVisitor,
        stmt: IRStatement,
        locals: MutableMap<String, Int>,
        className: String,
        nextLocal: Int
    ) {
        when (stmt) {
            is IRStatement.Assignment -> {
                generateAssignment(mv, stmt, locals, className)
            }
            is IRStatement.ExpressionStatement -> {
                generateExpression(mv, stmt.expression, locals, className)
                // 弹出栈顶值（如果有的话）
                val type = getExpressionType(stmt.expression, locals)
                if (type != "void") {
                    if (type == "long" || type == "double") {
                        mv.visitInsn(Opcodes.POP2)
                    } else {
                        mv.visitInsn(Opcodes.POP)
                    }
                }
            }
            is IRStatement.Return -> {
                stmt.value?.let {
                    generateExpression(mv, it, locals, className)
                }
                when (stmt.value) {
                    null -> mv.visitInsn(Opcodes.RETURN)
                    else -> {
                        val type = getExpressionType(stmt.value, locals)
                        when (type) {
                            "int", "boolean", "byte", "char", "short" -> mv.visitInsn(Opcodes.IRETURN)
                            "long" -> mv.visitInsn(Opcodes.LRETURN)
                            "float" -> mv.visitInsn(Opcodes.FRETURN)
                            "double" -> mv.visitInsn(Opcodes.DRETURN)
                            else -> mv.visitInsn(Opcodes.ARETURN)
                        }
                    }
                }
            }
            is IRStatement.If -> {
                generateIf(mv, stmt, locals, className, nextLocal)
            }
            is IRStatement.ForLoop -> {
                generateForLoop(mv, stmt, locals, className, nextLocal)
            }
            is IRStatement.WhileLoop -> {
                generateWhileLoop(mv, stmt, locals, className, nextLocal)
            }
            is IRStatement.VariableDeclaration -> {
                val index = locals.size + nextLocal
                locals[stmt.name] = index
                stmt.initialValue?.let {
                    generateExpression(mv, it, locals, className)
                } ?: run {
                    // 默认值
                    when (stmt.type) {
                        "int", "boolean", "byte", "char", "short" -> mv.visitInsn(Opcodes.ICONST_0)
                        "long" -> mv.visitInsn(Opcodes.LCONST_0)
                        "float" -> mv.visitInsn(Opcodes.FCONST_0)
                        "double" -> mv.visitInsn(Opcodes.DCONST_0)
                        else -> mv.visitInsn(Opcodes.ACONST_NULL)
                    }
                }
                storeLocal(mv, index, stmt.type)
            }
            is IRStatement.Break, is IRStatement.Continue -> {
                // 循环控制语句需要特殊处理
            }
        }
    }

    /**
     * 生成赋值语句
     */
    private fun generateAssignment(
        mv: MethodVisitor,
        stmt: IRStatement.Assignment,
        locals: MutableMap<String, Int>,
        className: String
    ) {
        when (stmt.target) {
            is IRExpression.Variable -> {
                generateExpression(mv, stmt.value, locals, className)
                val index = locals[stmt.target.name] ?: 0
                val type = getExpressionType(stmt.value, locals)
                storeLocal(mv, index, type)
            }
            is IRExpression.FieldAccess -> {
                when (stmt.target.target) {
                    is IRExpression.This -> {
                        generateExpression(mv, stmt.value, locals, className)
                        val type = getExpressionType(stmt.value, locals)
                        mv.visitFieldInsn(
                            Opcodes.PUTFIELD,
                            className,
                            stmt.target.fieldName,
                            mapTypeToDescriptor(type)
                        )
                    }
                    else -> {
                        generateExpression(mv, stmt.target.target, locals, className)
                        generateExpression(mv, stmt.value, locals, className)
                        val type = getExpressionType(stmt.value, locals)
                        mv.visitFieldInsn(
                            Opcodes.PUTFIELD,
                            getExpressionType(stmt.target.target, locals).replace(".", "/"),
                            stmt.target.fieldName,
                            mapTypeToDescriptor(type)
                        )
                    }
                }
            }
            is IRExpression.ArrayAccess -> {
                generateExpression(mv, stmt.target.array, locals, className)
                stmt.target.indices.forEach { generateExpression(mv, it, locals, className) }
                generateExpression(mv, stmt.value, locals, className)
                val elementType = getExpressionType(stmt.target.array, locals).removeSuffix("[]")
                when (elementType) {
                    "int" -> mv.visitInsn(Opcodes.IASTORE)
                    "long" -> mv.visitInsn(Opcodes.LASTORE)
                    "float" -> mv.visitInsn(Opcodes.FASTORE)
                    "double" -> mv.visitInsn(Opcodes.DASTORE)
                    "boolean" -> mv.visitInsn(Opcodes.BASTORE)
                    "byte" -> mv.visitInsn(Opcodes.BASTORE)
                    "char" -> mv.visitInsn(Opcodes.CASTORE)
                    "short" -> mv.visitInsn(Opcodes.SASTORE)
                    else -> mv.visitInsn(Opcodes.AASTORE)
                }
            }
            else -> {
                // 其他情况：生成值但不存储
                generateExpression(mv, stmt.value, locals, className)
            }
        }
    }

    /**
     * 生成If语句
     */
    private fun generateIf(
        mv: MethodVisitor,
        stmt: IRStatement.If,
        locals: MutableMap<String, Int>,
        className: String,
        nextLocal: Int
    ) {
        val elseLabel = Label()
        val endLabel = Label()

        generateExpression(mv, stmt.condition, locals, className)
        mv.visitJumpInsn(Opcodes.IFEQ, elseLabel)

        // then分支
        stmt.thenBranch.forEach { generateStatement(mv, it, locals, className, nextLocal) }
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)

        // else分支
        mv.visitLabel(elseLabel)
        stmt.elseBranch.forEach { generateStatement(mv, it, locals, className, nextLocal) }

        mv.visitLabel(endLabel)
    }

    /**
     * 生成For循环
     */
    private fun generateForLoop(
        mv: MethodVisitor,
        stmt: IRStatement.ForLoop,
        locals: MutableMap<String, Int>,
        className: String,
        nextLocal: Int
    ) {
        val startLabel = Label()
        val endLabel = Label()

        // 初始化循环变量
        val iteratorIndex = locals.size + nextLocal
        locals[stmt.iterator] = iteratorIndex

        generateExpression(mv, stmt.start, locals, className)
        mv.visitVarInsn(Opcodes.ISTORE, iteratorIndex)

        // 循环开始
        mv.visitLabel(startLabel)

        // 检查条件
        mv.visitVarInsn(Opcodes.ILOAD, iteratorIndex)
        generateExpression(mv, stmt.end, locals, className)
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, endLabel)

        // 循环体
        val bodyLocals = locals.toMutableMap()
        stmt.body.forEach { generateStatement(mv, it, bodyLocals, className, nextLocal + 1) }

        // 递增
        mv.visitIincInsn(iteratorIndex, 1)
        mv.visitJumpInsn(Opcodes.GOTO, startLabel)

        mv.visitLabel(endLabel)
    }

    /**
     * 生成While循环
     */
    private fun generateWhileLoop(
        mv: MethodVisitor,
        stmt: IRStatement.WhileLoop,
        locals: MutableMap<String, Int>,
        className: String,
        nextLocal: Int
    ) {
        val startLabel = Label()
        val endLabel = Label()

        mv.visitLabel(startLabel)
        generateExpression(mv, stmt.condition, locals, className)
        mv.visitJumpInsn(Opcodes.IFEQ, endLabel)

        stmt.body.forEach { generateStatement(mv, it, locals, className, nextLocal) }
        mv.visitJumpInsn(Opcodes.GOTO, startLabel)

        mv.visitLabel(endLabel)
    }

    /**
     * 生成表达式
     */
    private fun generateExpression(
        mv: MethodVisitor,
        expr: IRExpression,
        locals: Map<String, Int>,
        className: String
    ) {
        when (expr) {
            is IRExpression.IntegerLiteral -> {
                when (expr.value) {
                    -1L -> mv.visitInsn(Opcodes.ICONST_M1)
                    in 0..5 -> mv.visitInsn(Opcodes.ICONST_0 + expr.value.toInt())
                    in Byte.MIN_VALUE..Byte.MAX_VALUE -> mv.visitIntInsn(Opcodes.BIPUSH, expr.value.toInt())
                    in Short.MIN_VALUE..Short.MAX_VALUE -> mv.visitIntInsn(Opcodes.SIPUSH, expr.value.toInt())
                    else -> mv.visitLdcInsn(expr.value.toInt())
                }
            }
            is IRExpression.RealLiteral -> {
                when (expr.value) {
                    0.0 -> mv.visitInsn(Opcodes.DCONST_0)
                    1.0 -> mv.visitInsn(Opcodes.DCONST_1)
                    else -> mv.visitLdcInsn(expr.value)
                }
            }
            is IRExpression.StringLiteral -> {
                mv.visitLdcInsn(expr.value)
            }
            is IRExpression.BooleanLiteral -> {
                mv.visitInsn(if (expr.value) Opcodes.ICONST_1 else Opcodes.ICONST_0)
            }
            is IRExpression.Null -> {
                mv.visitInsn(Opcodes.ACONST_NULL)
            }
            is IRExpression.This -> {
                mv.visitVarInsn(Opcodes.ALOAD, 0)
            }
            is IRExpression.Variable -> {
                val index = locals[expr.name] ?: 0
                mv.visitVarInsn(Opcodes.ILOAD, index) // 简化，假设是int
            }
            is IRExpression.Binary -> {
                generateBinaryExpression(mv, expr, locals, className)
            }
            is IRExpression.Unary -> {
                generateExpression(mv, expr.operand, locals, className)
                when (expr.operator) {
                    "-" -> {
                        mv.visitInsn(Opcodes.INEG)
                    }
                    "not" -> {
                        val trueLabel = Label()
                        val endLabel = Label()
                        mv.visitJumpInsn(Opcodes.IFNE, trueLabel)
                        mv.visitInsn(Opcodes.ICONST_1)
                        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                        mv.visitLabel(trueLabel)
                        mv.visitInsn(Opcodes.ICONST_0)
                        mv.visitLabel(endLabel)
                    }
                }
            }
            is IRExpression.FunctionCall -> {
                generateFunctionCall(mv, expr, locals, className)
            }
            is IRExpression.MethodCall -> {
                generateExpression(mv, expr.target, locals, className)
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                val descriptor = buildMethodDescriptorFromArgs(expr.arguments.map { getExpressionType(it, locals) }, "void")
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    getExpressionType(expr.target, locals).replace(".", "/"),
                    expr.methodName,
                    descriptor,
                    false
                )
            }
            is IRExpression.FieldAccess -> {
                when (expr.target) {
                    is IRExpression.This -> {
                        mv.visitFieldInsn(
                            Opcodes.GETFIELD,
                            className,
                            expr.fieldName,
                            "D" // 简化，假设是double
                        )
                    }
                    else -> {
                        generateExpression(mv, expr.target, locals, className)
                        mv.visitFieldInsn(
                            Opcodes.GETFIELD,
                            getExpressionType(expr.target, locals).replace(".", "/"),
                            expr.fieldName,
                            "D"
                        )
                    }
                }
            }
            is IRExpression.ArrayAccess -> {
                generateExpression(mv, expr.array, locals, className)
                expr.indices.forEach { generateExpression(mv, it, locals, className) }
                mv.visitInsn(Opcodes.DALOAD) // 简化，假设是double数组
            }
            is IRExpression.ArrayLiteral -> {
                mv.visitIntInsn(Opcodes.BIPUSH, expr.elements.size)
                mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Double")
                expr.elements.forEachIndexed { index, element ->
                    mv.visitInsn(Opcodes.DUP)
                    mv.visitIntInsn(Opcodes.BIPUSH, index)
                    generateExpression(mv, element, locals, className)
                    mv.visitInsn(Opcodes.DASTORE)
                }
            }
            is IRExpression.NewObject -> {
                mv.visitTypeInsn(Opcodes.NEW, expr.className.replace(".", "/"))
                mv.visitInsn(Opcodes.DUP)
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                val descriptor = buildMethodDescriptorFromArgs(
                    expr.arguments.map { getExpressionType(it, locals) },
                    "void"
                )
                mv.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    expr.className.replace(".", "/"),
                    "<init>",
                    descriptor,
                    false
                )
            }
            is IRExpression.NewArray -> {
                expr.dimensions.forEach { generateExpression(mv, it, locals, className) }
                mv.visitTypeInsn(Opcodes.ANEWARRAY, expr.elementType.replace(".", "/"))
            }
            is IRExpression.Cast -> {
                generateExpression(mv, expr.target, locals, className)
                mv.visitTypeInsn(Opcodes.CHECKCAST, expr.targetType.replace(".", "/"))
            }
            is IRExpression.Conditional -> {
                val elseLabel = Label()
                val endLabel = Label()
                generateExpression(mv, expr.condition, locals, className)
                mv.visitJumpInsn(Opcodes.IFEQ, elseLabel)
                generateExpression(mv, expr.thenExpression, locals, className)
                mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                mv.visitLabel(elseLabel)
                generateExpression(mv, expr.elseExpression, locals, className)
                mv.visitLabel(endLabel)
            }
            is IRExpression.Range -> {
                // 创建范围对象
                mv.visitTypeInsn(Opcodes.NEW, "org/modelica/runtime/Range")
                mv.visitInsn(Opcodes.DUP)
                generateExpression(mv, expr.start, locals, className)
                generateExpression(mv, expr.end, locals, className)
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/modelica/runtime/Range", "<init>", "(II)V", false)
            }
        }
    }

    /**
     * 生成二元表达式
     */
    private fun generateBinaryExpression(
        mv: MethodVisitor,
        expr: IRExpression.Binary,
        locals: Map<String, Int>,
        className: String
    ) {
        generateExpression(mv, expr.left, locals, className)
        generateExpression(mv, expr.right, locals, className)

        when (expr.operator) {
            "+" -> mv.visitInsn(Opcodes.DADD)
            "-" -> mv.visitInsn(Opcodes.DSUB)
            "*" -> mv.visitInsn(Opcodes.DMUL)
            "/" -> mv.visitInsn(Opcodes.DDIV)
            "%" -> mv.visitInsn(Opcodes.DREM)
            "==" -> {
                val trueLabel = Label()
                val endLabel = Label()
                mv.visitInsn(Opcodes.DCMPG)
                mv.visitJumpInsn(Opcodes.IFEQ, trueLabel)
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                mv.visitLabel(trueLabel)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitLabel(endLabel)
            }
            "!=" -> {
                val trueLabel = Label()
                val endLabel = Label()
                mv.visitInsn(Opcodes.DCMPG)
                mv.visitJumpInsn(Opcodes.IFNE, trueLabel)
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                mv.visitLabel(trueLabel)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitLabel(endLabel)
            }
            "<" -> {
                val trueLabel = Label()
                val endLabel = Label()
                mv.visitInsn(Opcodes.DCMPG)
                mv.visitJumpInsn(Opcodes.IFLT, trueLabel)
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                mv.visitLabel(trueLabel)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitLabel(endLabel)
            }
            "<=" -> {
                val trueLabel = Label()
                val endLabel = Label()
                mv.visitInsn(Opcodes.DCMPG)
                mv.visitJumpInsn(Opcodes.IFLE, trueLabel)
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                mv.visitLabel(trueLabel)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitLabel(endLabel)
            }
            ">" -> {
                val trueLabel = Label()
                val endLabel = Label()
                mv.visitInsn(Opcodes.DCMPG)
                mv.visitJumpInsn(Opcodes.IFGT, trueLabel)
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                mv.visitLabel(trueLabel)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitLabel(endLabel)
            }
            ">=" -> {
                val trueLabel = Label()
                val endLabel = Label()
                mv.visitInsn(Opcodes.DCMPG)
                mv.visitJumpInsn(Opcodes.IFGE, trueLabel)
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitJumpInsn(Opcodes.GOTO, endLabel)
                mv.visitLabel(trueLabel)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitLabel(endLabel)
            }
            "and" -> mv.visitInsn(Opcodes.IAND)
            "or" -> mv.visitInsn(Opcodes.IOR)
            "^" -> {
                // 调用Math.pow
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Math",
                    "pow",
                    "(DD)D",
                    false
                )
            }
        }
    }

    /**
     * 生成函数调用
     */
    private fun generateFunctionCall(
        mv: MethodVisitor,
        expr: IRExpression.FunctionCall,
        locals: Map<String, Int>,
        className: String
    ) {
        // 内置函数处理
        when (expr.function) {
            "sin" -> {
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false)
            }
            "cos" -> {
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false)
            }
            "tan" -> {
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "tan", "(D)D", false)
            }
            "sqrt" -> {
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false)
            }
            "abs" -> {
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false)
            }
            "exp" -> {
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "exp", "(D)D", false)
            }
            "log" -> {
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "log", "(D)D", false)
            }
            "der" -> {
                // 导数函数：简化处理
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
            }
            "connect" -> {
                // 连接函数：生成连接调用
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "org/modelica/runtime/Connector",
                    "connect",
                    "(Ljava/lang/Object;Ljava/lang/Object;)V",
                    false
                )
            }
            else -> {
                // 用户定义函数
                expr.arguments.forEach { generateExpression(mv, it, locals, className) }
                val descriptor = buildMethodDescriptorFromArgs(
                    expr.arguments.map { getExpressionType(it, locals) },
                    "void"
                )
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    className,
                    expr.function,
                    descriptor,
                    false
                )
            }
        }
    }

    // ==================== 辅助方法 ====================

    private fun mapTypeToDescriptor(type: String): String {
        return when (type) {
            "void" -> "V"
            "boolean" -> "Z"
            "byte" -> "B"
            "char" -> "C"
            "short" -> "S"
            "int" -> "I"
            "long" -> "J"
            "float" -> "F"
            "double" -> "D"
            else -> if (type.endsWith("[]")) {
                "[" + mapTypeToDescriptor(type.dropLast(2))
            } else {
                "L${type.replace(".", "/")};"
            }
        }
    }

    private fun buildMethodDescriptor(method: IRMethod): String {
        val params = method.parameters.joinToString("") { mapTypeToDescriptor(it.type) }
        return "($params)${mapTypeToDescriptor(method.returnType)}"
    }

    private fun buildMethodDescriptorFromArgs(argTypes: List<String>, returnType: String): String {
        val params = argTypes.joinToString("") { mapTypeToDescriptor(it) }
        return "($params)${mapTypeToDescriptor(returnType)}"
    }

    private fun getTypeSize(type: String): Int {
        return when (type) {
            "long", "double" -> 2
            else -> 1
        }
    }

    private fun getExpressionType(expr: IRExpression, locals: Map<String, Int>): String {
        return when (expr) {
            is IRExpression.IntegerLiteral -> "int"
            is IRExpression.RealLiteral -> "double"
            is IRExpression.StringLiteral -> "java/lang/String"
            is IRExpression.BooleanLiteral -> "boolean"
            is IRExpression.Null -> "null"
            is IRExpression.This -> "this"
            is IRExpression.Variable -> "double" // 简化
            is IRExpression.Binary -> when (expr.operator) {
                "+", "-", "*", "/", "%", "^" -> "double"
                "==", "!=", "<", "<=", ">", ">=", "and", "or" -> "boolean"
                else -> "double"
            }
            is IRExpression.Unary -> getExpressionType(expr.operand, locals)
            is IRExpression.FunctionCall -> "double" // 简化
            is IRExpression.MethodCall -> "void"
            is IRExpression.FieldAccess -> "double"
            is IRExpression.ArrayAccess -> "double"
            is IRExpression.ArrayLiteral -> "java/lang/Object[]"
            is IRExpression.NewObject -> expr.className
            is IRExpression.NewArray -> "${expr.elementType}[]"
            is IRExpression.Cast -> expr.targetType
            is IRExpression.Conditional -> getExpressionType(expr.thenExpression, locals)
            is IRExpression.Range -> "org/modelica/runtime/Range"
        }
    }

    private fun storeLocal(mv: MethodVisitor, index: Int, type: String) {
        when (type) {
            "int", "boolean", "byte", "char", "short" -> mv.visitVarInsn(Opcodes.ISTORE, index)
            "long" -> mv.visitVarInsn(Opcodes.LSTORE, index)
            "float" -> mv.visitVarInsn(Opcodes.FSTORE, index)
            "double" -> mv.visitVarInsn(Opcodes.DSTORE, index)
            else -> mv.visitVarInsn(Opcodes.ASTORE, index)
        }
    }

    companion object {
        /**
         * 将字节码写入文件
         */
        fun writeClasses(classes: Map<String, ByteArray>, outputDir: File) {
            classes.forEach { (name, bytecode) ->
                val classFile = File(outputDir, "${name.replace('.', '/')}.class")
                classFile.parentFile.mkdirs()
                FileOutputStream(classFile).use { it.write(bytecode) }
            }
        }
    }
}