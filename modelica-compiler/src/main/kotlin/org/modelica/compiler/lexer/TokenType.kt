package org.modelica.compiler.lexer

/**
 * Modelica语言的Token类型枚举
 * 基于Modelica语言规范定义所有关键字、操作符和字面量类型
 */
enum class TokenType(val lexeme: String = "", val description: String) {
    // 关键字 - 类定义
    CLASS("class", "类定义关键字"),
    MODEL("model", "模型定义关键字"),
    RECORD("record", "记录定义关键字"),
    BLOCK("block", "块定义关键字"),
    CONNECTOR("connector", "连接器定义关键字"),
    TYPE("type", "类型定义关键字"),
    PACKAGE("package", "包定义关键字"),
    FUNCTION("function", "函数定义关键字"),
    ENCAPSULATED("encapsulated", "封装关键字"),
    PARTIAL("partial", "部分关键字"),
    FINAL("final", "最终关键字"),
    ABSTRACT("abstract", "抽象关键字"),

    // 关键字 - 访问控制
    PUBLIC("public", "公共访问"),
    PROTECTED("protected", "受保护访问"),

    // 关键字 - 类型前缀
    FLOW("flow", "流变量前缀"),
    STREAM("stream", "流变量前缀"),
    DISCRETE("discrete", "离散变量前缀"),
    PARAMETER("parameter", "参数前缀"),
    CONSTANT("constant", "常量前缀"),
    INPUT("input", "输入前缀"),
    OUTPUT("output", "输出前缀"),

    // 关键字 - 内置类型
    REAL("Real", "实数类型"),
    INTEGER("Integer", "整数类型"),
    BOOLEAN("Boolean", "布尔类型"),
    STRING("String", "字符串类型"),

    // 关键字 - 控制结构
    IF("if", "条件语句"),
    THEN("then", "条件语句then"),
    ELSE("else", "条件语句else"),
    ELSEIF("elseif", "条件语句elseif"),
    END_IF("end if", "条件语句结束"),
    FOR("for", "循环语句"),
    IN("in", "循环in关键字"),
    LOOP("loop", "循环loop关键字"),
    END_FOR("end for", "循环语句结束"),
    WHILE("while", "while循环"),
    END_WHILE("end while", "while循环结束"),
    WHEN("when", "when语句"),
    END_WHEN("end when", "when语句结束"),
    BREAK("break", "break语句"),
    RETURN("return", "return语句"),

    // 关键字 - 方程和算法
    EQUATION("equation", "方程段"),
    ALGORITHM("algorithm", "算法段"),
    INITIAL("initial", "初始关键字"),
    CONNECT("connect", "连接语句"),

    // 关键字 - 其他
    ANNOTATION("annotation", "注解关键字"),
    EXTERNAL("external", "外部函数关键字"),
    IMPORT("import", "导入关键字"),
    EXTENDS("extends", "继承关键字"),
    CONSTRAINT("constraint", "约束关键字"),
    DER("der", "导数关键字"),
    REDECLARE("redeclare", "重声明关键字"),
    REPLACEABLE("replaceable", "可替换关键字"),
    EACH("each", "each关键字"),
    INNER("inner", "inner关键字"),
    OUTER("outer", "outer关键字"),

    // 布尔字面量
    TRUE("true", "布尔真值"),
    FALSE("false", "布尔假值"),

    // 标识符和字面量
    IDENTIFIER("", "标识符"),
    INTEGER_LITERAL("", "整数字面量"),
    REAL_LITERAL("", "实数字面量"),
    STRING_LITERAL("", "字符串字面量"),

    // 操作符 - 算术
    PLUS("+", "加法运算符"),
    MINUS("-", "减法运算符"),
    STAR("*", "乘法运算符"),
    SLASH("/", "除法运算符"),
    POWER("^", "幂运算符"),

    // 操作符 - 比较
    EQ("==", "等于运算符"),
    NE("<>", "不等于运算符"),
    LT("<", "小于运算符"),
    LE("<=", "小于等于运算符"),
    GT(">", "大于运算符"),
    GE(">=", "大于等于运算符"),

    // 操作符 - 逻辑
    AND("and", "逻辑与"),
    OR("or", "逻辑或"),
    NOT("not", "逻辑非"),

    // 操作符 - 赋值
    ASSIGN(":=", "赋值运算符"),
    COLON_EQ(":=", "赋值运算符(别名)"),

    // 操作符 - 数组
    LBRACKET("[", "左方括号"),
    RBRACKET("]", "右方括号"),
    LBRACE("{", "左花括号"),
    RBRACE("}", "右花括号"),

    // 分隔符
    LPAREN("(", "左圆括号"),
    RPAREN(")", "右圆括号"),
    COMMA(",", "逗号"),
    SEMICOLON(";", "分号"),
    COLON(":", "冒号"),
    DOT(".", "点号"),

    // 特殊符号
    ARROW("->", "箭头"),
    EQUALS("=", "等于号(方程)"),

    // 注释和空白
    LINE_COMMENT("", "单行注释"),
    BLOCK_COMMENT("", "多行注释"),
    WHITESPACE("", "空白字符"),

    // 特殊
    EOF("", "文件结束"),
    ERROR("", "错误Token");

    companion object {
        /** 关键字映射表 */
        val keywords: Map<String, TokenType> by lazy {
            entries.filter { it.lexeme.isNotEmpty() && it.description.contains("关键字") }
                .associateBy { it.lexeme }
        }

        /** 判断字符串是否为关键字 */
        fun isKeyword(text: String): Boolean = keywords.containsKey(text)

        /** 获取关键字对应的TokenType */
        fun getKeyword(text: String): TokenType? = keywords[text]
    }
}