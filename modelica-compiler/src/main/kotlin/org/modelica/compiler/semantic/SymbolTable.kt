package org.modelica.compiler.semantic

import org.modelica.compiler.ast.nodes.*
import org.modelica.compiler.lexer.SourceLocation
import org.modelica.compiler.semantic.types.ModelicaType

/**
 * 符号表
 *
 * 管理作用域和符号定义
 */
class SymbolTable {
    /** 全局作用域 */
    private val globalScope = Scope(null, ScopeType.GLOBAL)

    /** 当前作用域 */
    private var currentScope: Scope = globalScope

    /** 所有已定义的类 */
    private val classes: MutableMap<String, ClassDefinition> = mutableMapOf()

    /**
     * 进入新作用域
     */
    fun enterScope(type: ScopeType, name: String? = null): Scope {
        val scope = Scope(currentScope, type, name)
        currentScope = scope
        return scope
    }

    /**
     * 退出当前作用域
     */
    fun exitScope() {
        currentScope = currentScope.parent ?: globalScope
    }

    /**
     * 在当前作用域定义符号
     */
    fun define(symbol: Symbol) {
        currentScope.define(symbol)
    }

    /**
     * 查找符号（从当前作用域向上查找）
     */
    fun resolve(name: String): Symbol? {
        var scope: Scope? = currentScope
        while (scope != null) {
            val symbol = scope.resolve(name)
            if (symbol != null) return symbol
            scope = scope.parent
        }
        return null
    }

    /**
     * 仅在当前作用域查找符号
     */
    fun resolveLocal(name: String): Symbol? {
        return currentScope.resolve(name)
    }

    /**
     * 注册类定义
     */
    fun registerClass(name: String, classDef: ClassDefinition) {
        classes[name] = classDef
    }

    /**
     * 获取类定义
     */
    fun getClass(name: String): ClassDefinition? = classes[name]

    /**
     * 获取当前作用域深度
     */
    val scopeDepth: Int get() = currentScope.depth

    /**
     * 获取当前作用域名
     */
    val currentScopeName: String? get() = currentScope.name
}

/**
 * 作用域类型
 */
enum class ScopeType {
    GLOBAL,      // 全局作用域
    CLASS,       // 类作用域
    FUNCTION,    // 函数作用域
    BLOCK,       // 块作用域
    FOR,         // for循环作用域
    EQUATION     // 方程作用域
}

/**
 * 作用域
 */
class Scope(
    val parent: Scope?,
    val type: ScopeType,
    val name: String? = null
) {
    /** 作用域中的符号 */
    private val symbols: MutableMap<String, Symbol> = mutableMapOf()

    /** 作用域深度 */
    val depth: Int = (parent?.depth ?: -1) + 1

    /**
     * 定义符号
     */
    fun define(symbol: Symbol) {
        symbols[symbol.name] = symbol
    }

    /**
     * 查找符号
     */
    fun resolve(name: String): Symbol? = symbols[name]

    /**
     * 获取所有符号
     */
    fun allSymbols(): Collection<Symbol> = symbols.values
}

/**
 * 符号类型
 */
enum class SymbolKind {
    VARIABLE,      // 变量
    PARAMETER,     // 参数
    CONSTANT,      // 常量
    FUNCTION,      // 函数
    CLASS,         // 类
    COMPONENT,     // 组件
    CONNECTOR,     // 连接器
    TYPE,          // 类型
    IMPORT         // 导入
}

/**
 * 符号定义
 */
sealed class Symbol {
    abstract val name: String
    abstract val kind: SymbolKind
    abstract val type: ModelicaType
    abstract val location: SourceLocation?
    abstract val isMutable: Boolean

    /**
     * 变量符号
     */
    data class Variable(
        override val name: String,
        override val type: ModelicaType,
        override val location: SourceLocation?,
        override val isMutable: Boolean = true,
        val isInput: Boolean = false,
        val isOutput: Boolean = false,
        val isFlow: Boolean = false,
        val isStream: Boolean = false,
        val isDiscrete: Boolean = false
    ) : Symbol() {
        override val kind: SymbolKind = SymbolKind.VARIABLE
    }

    /**
     * 参数符号
     */
    data class Parameter(
        override val name: String,
        override val type: ModelicaType,
        override val location: SourceLocation?,
        override val isMutable: Boolean = false
    ) : Symbol() {
        override val kind: SymbolKind = SymbolKind.PARAMETER
    }

    /**
     * 常量符号
     */
    data class Constant(
        override val name: String,
        override val type: ModelicaType,
        override val location: SourceLocation?,
        override val isMutable: Boolean = false
    ) : Symbol() {
        override val kind: SymbolKind = SymbolKind.CONSTANT
    }

    /**
     * 函数符号
     */
    data class Function(
        override val name: String,
        override val type: ModelicaType,
        val parameters: List<Variable>,
        override val location: SourceLocation?,
        val definition: ClassDefinition? = null
    ) : Symbol() {
        override val kind: SymbolKind = SymbolKind.FUNCTION
        override val isMutable: Boolean = false
    }

    /**
     * 类符号
     */
    data class Class(
        override val name: String,
        override val type: ModelicaType,
        override val location: SourceLocation?,
        val definition: ClassDefinition
    ) : Symbol() {
        override val kind: SymbolKind = SymbolKind.CLASS
        override val isMutable: Boolean = false
    }
}