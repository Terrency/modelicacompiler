package org.modelica.compiler.parser

import org.modelica.compiler.ast.nodes.*
import org.modelica.compiler.lexer.Token
import org.modelica.compiler.lexer.TokenType

/**
 * Modelica语法分析器
 *
 * 使用递归下降方法解析Token序列生成AST
 */
class ModelicaParser(
    private val tokens: List<Token>
) {
    /** 当前Token位置 */
    private var current: Int = 0

    /** 收集的错误列表 */
    private val errors: MutableList<ParseError> = mutableListOf()

    /** 当前Token */
    private val peek: Token get() = tokens.getOrNull(current) ?: tokens.last()

    /** 前一个Token */
    private val previous: Token get() = tokens[current - 1]

    /** 是否到达末尾 */
    private val isAtEnd: Boolean get() = peek.type == TokenType.EOF

    /**
     * 解析整个程序，返回AST根节点
     */
    fun parse(): ParseResult<Program> {
        val classes = mutableListOf<ClassDefinition>()
        val imports = mutableListOf<ImportClause>()

        while (!isAtEnd) {
            try {
                when {
                    check(TokenType.IMPORT) -> imports.add(parseImport())
                    checkClassPrefix() -> classes.add(parseClass())
                    else -> {
                        errors.add(ParseError.unexpected(peek))
                        advance()
                    }
                }
            } catch (e: ParseError) {
                errors.add(e)
                synchronize()
            }
        }

        val program = Program(classes, imports)
        return ParseResult(program, errors.toList())
    }

    // ==================== 类定义解析 ====================

    /**
     * 检查是否为类定义前缀
     */
    private fun checkClassPrefix(): Boolean {
        return check(TokenType.CLASS, TokenType.MODEL, TokenType.RECORD,
            TokenType.BLOCK, TokenType.CONNECTOR, TokenType.TYPE,
            TokenType.PACKAGE, TokenType.FUNCTION) ||
                (check(TokenType.ENCAPSULATED, TokenType.PARTIAL, TokenType.FINAL,
                    TokenType.ABSTRACT, TokenType.ENCAPSULATED) && checkNextClassPrefix())
    }

    private fun checkNextClassPrefix(): Boolean {
        for (i in 1..3) {
            if (tokens.getOrNull(current + i)?.type?.let {
                it in listOf(TokenType.CLASS, TokenType.MODEL, TokenType.RECORD,
                    TokenType.BLOCK, TokenType.CONNECTOR, TokenType.TYPE,
                    TokenType.PACKAGE, TokenType.FUNCTION)
            } == true) {
                return true
            }
        }
        return false
    }

    /**
     * 解析类定义
     */
    private fun parseClass(): ClassDefinition {
        // 解析类前缀
        val prefixes = parseClassPrefixes()

        // 解析类类型
        val classType = parseClassType()

        // 解析类名
        val name = consume(TokenType.IDENTIFIER, "Expected class name")

        // 解析类特化（可选）
        val specialization = if (match(TokenType.EQUALS)) {
            parseClassSpecialization()
        } else null

        // 解析类描述（可选）
        val description = parseDescription()

        // 解析组合子句
        val composition = if (match(TokenType.EQUALS)) {
            // 短类定义
            val baseClass = parseExpression()
            consume(TokenType.SEMICOLON, "Expected ';' after short class definition")
            ClassComposition(short = baseClass)
        } else {
            // 完整类定义
            parseComposition()
        }

        // 解析结束关键字
        val endKeyword = consume(TokenType.END, "Expected 'end'")

        // 解析结束类名
        val endName = consume(TokenType.IDENTIFIER, "Expected class name after 'end'")
        if (endName.lexeme != name.lexeme) {
            errors.add(ParseError(
                "End name '$endName' does not match class name '$name'",
                endName.location
            ))
        }

        consume(TokenType.SEMICOLON, "Expected ';' after end name")

        return ClassDefinition(
            prefixes = prefixes,
            classType = classType,
            name = name.lexeme,
            specialization = specialization,
            description = description,
            composition = composition,
            location = name.location
        )
    }

    private fun parseClassPrefixes(): ClassPrefixes {
        var encapsulated = false
        var partial = false
        var final = false
        var abstract = false

        while (true) {
            when {
                match(TokenType.ENCAPSULATED) -> encapsulated = true
                match(TokenType.PARTIAL) -> partial = true
                match(TokenType.FINAL) -> final = true
                match(TokenType.ABSTRACT) -> abstract = true
                else -> break
            }
        }

        return ClassPrefixes(encapsulated, partial, final, abstract)
    }

    private fun parseClassType(): ClassType {
        return when {
            match(TokenType.CLASS) -> ClassType.CLASS
            match(TokenType.MODEL) -> ClassType.MODEL
            match(TokenType.RECORD) -> ClassType.RECORD
            match(TokenType.BLOCK) -> ClassType.BLOCK
            match(TokenType.CONNECTOR) -> ClassType.CONNECTOR
            match(TokenType.TYPE) -> ClassType.TYPE
            match(TokenType.PACKAGE) -> ClassType.PACKAGE
            match(TokenType.FUNCTION) -> ClassType.FUNCTION
            else -> throw ParseError("Expected class type", peek.location)
        }
    }

    private fun parseClassSpecialization(): ClassSpecialization? {
        // 简化实现：解析类型特化
        val baseType = parseType()
        return ClassSpecialization(baseType)
    }

    private fun parseComposition(): ClassComposition {
        val elements = mutableListOf<Element>()
        val equations = mutableListOf<Equation>()
        val algorithms = mutableListOf<AlgorithmSection>()

        while (!check(TokenType.END) && !isAtEnd) {
            when {
                // 支持嵌套的类定义（包括package）
                checkClassPrefix() -> {
                    val nestedClass = parseClass()
                    elements.add(NestedClassElement(nestedClass))
                }
                check(TokenType.PUBLIC, TokenType.PROTECTED) -> {
                    val section = parsePublicProtectedSection()
                    elements.addAll(section)
                }
                check(TokenType.EQUATION) -> {
                    equations.addAll(parseEquationSection())
                }
                check(TokenType.ALGORITHM) -> {
                    algorithms.add(parseAlgorithmSection())
                }
                check(TokenType.INITIAL) -> {
                    if (checkNext(TokenType.EQUATION)) {
                        equations.addAll(parseInitialEquationSection())
                    } else {
                        algorithms.add(parseInitialAlgorithmSection())
                    }
                }
                check(TokenType.ANNOTATION) -> {
                    advance() // 消耗 ANNOTATION token
                    parseAnnotation()
                }
                checkImportOrElement() -> {
                    elements.add(parseElement())
                }
                else -> {
                    errors.add(ParseError.unexpected(peek))
                    advance()
                }
            }
        }

        return ClassComposition(elements, equations, algorithms)
    }

    private fun checkImportOrElement(): Boolean {
        return check(TokenType.IMPORT, TokenType.EXTENDS) ||
                checkTypePrefix() ||
                check(TokenType.IDENTIFIER)
    }

    // ==================== 元素解析 ====================

    private fun parsePublicProtectedSection(): List<Element> {
        val isPublic = match(TokenType.PUBLIC)
        if (!isPublic) match(TokenType.PROTECTED)

        val elements = mutableListOf<Element>()
        while (!check(TokenType.PUBLIC, TokenType.PROTECTED, TokenType.EQUATION,
            TokenType.ALGORITHM, TokenType.INITIAL, TokenType.END, TokenType.ANNOTATION) &&
            checkImportOrElement() && !isAtEnd) {
            elements.add(parseElement())
        }
        return elements
    }

    private fun parseElement(): Element {
        return when {
            match(TokenType.IMPORT) -> parseImport()
            match(TokenType.EXTENDS) -> parseExtends()
            checkTypePrefix() -> parseComponentDeclaration()
            check(TokenType.IDENTIFIER) -> parseComponentDeclaration()
            else -> throw ParseError("Expected element", peek.location)
        }
    }

    private fun checkTypePrefix(): Boolean {
        return check(TokenType.FLOW, TokenType.STREAM, TokenType.DISCRETE,
            TokenType.PARAMETER, TokenType.CONSTANT, TokenType.INPUT, TokenType.OUTPUT,
            TokenType.REAL, TokenType.INTEGER, TokenType.BOOLEAN, TokenType.STRING)
    }

    private fun parseImport(): ImportClause {
        val name = parseIdentifierPath()
        val alias = if (match(TokenType.ASSIGN)) {
            consume(TokenType.IDENTIFIER, "Expected import alias").lexeme
        } else null

        consume(TokenType.SEMICOLON, "Expected ';' after import")

        return ImportClause(name, alias)
    }

    private fun parseExtends(): ExtendsClause {
        val baseClass = parseIdentifierPath()
        // 简化：跳过类修改
        if (match(TokenType.LPAREN)) {
            var depth = 1
            while (depth > 0 && !isAtEnd) {
                if (match(TokenType.LPAREN)) depth++
                else if (match(TokenType.RPAREN)) depth--
                else advance()
            }
        }
        consume(TokenType.SEMICOLON, "Expected ';' after extends")
        return ExtendsClause(baseClass)
    }

    private fun parseComponentDeclaration(): ComponentDeclaration {
        // 解析类型前缀
        val prefixes = parseTypePrefixes()

        // 解析类型
        val type = parseType()

        // 解析组件列表
        val components = parseComponentList()

        // 解析可选的 annotation
        val hasAnnotation = match(TokenType.ANNOTATION)
        if (hasAnnotation) {
            parseAnnotation()
            // annotation 后面的分号已经被 parseAnnotation() 消耗了
        } else {
            consume(TokenType.SEMICOLON, "Expected ';' after component declaration")
        }

        return ComponentDeclaration(prefixes, type, components)
    }

    private fun parseTypePrefixes(): TypePrefixes {
        var flow = false
        var stream = false
        var discrete = false
        var parameter = false
        var constant = false
        var input = false
        var output = false

        while (true) {
            when {
                match(TokenType.FLOW) -> flow = true
                match(TokenType.STREAM) -> stream = true
                match(TokenType.DISCRETE) -> discrete = true
                match(TokenType.PARAMETER) -> parameter = true
                match(TokenType.CONSTANT) -> constant = true
                match(TokenType.INPUT) -> input = true
                match(TokenType.OUTPUT) -> output = true
                else -> break
            }
        }

        return TypePrefixes(flow, stream, discrete, parameter, constant, input, output)
    }

    private fun parseType(): TypeSpec {
        val name = parseIdentifierPath()

        // 解析数组维度
        val dimensions = mutableListOf<ArrayDimension>()
        while (match(TokenType.LBRACKET)) {
            val dim = if (check(TokenType.COLON)) {
                advance()
                ArrayDimension.Unknown
            } else {
                val size = parseExpression()
                ArrayDimension.Known(size)
            }
            consume(TokenType.RBRACKET, "Expected ']'")
            dimensions.add(dim)
        }

        return TypeSpec(name, dimensions)
    }

    private fun parseComponentList(): List<ComponentItem> {
        val components = mutableListOf<ComponentItem>()
        components.add(parseComponentItem())

        while (match(TokenType.COMMA)) {
            components.add(parseComponentItem())
        }

        return components
    }

    private fun parseComponentItem(): ComponentItem {
        val name = consume(TokenType.IDENTIFIER, "Expected component name").lexeme

        // 解析数组维度
        val dimensions = mutableListOf<ArrayDimension>()
        while (match(TokenType.LBRACKET)) {
            val dim = if (check(TokenType.COLON)) {
                advance()
                ArrayDimension.Unknown
            } else {
                val size = parseExpression()
                ArrayDimension.Known(size)
            }
            consume(TokenType.RBRACKET, "Expected ']'")
            dimensions.add(dim)
        }

        // 解析修饰
        val modification = if (match(TokenType.LPAREN)) {
            parseModification()
        } else if (match(TokenType.EQUALS)) {
            // 值修改
            val value = parseExpression()
            Modification.Value(value)
        } else null

        // 解析描述
        val description = parseDescription()

        return ComponentItem(name, dimensions, modification, description)
    }

    private fun parseModification(): Modification {
        // 解析修改内容，支持嵌套修改
        val args = mutableListOf<Argument>()
        while (!check(TokenType.RPAREN) && !isAtEnd) {
            if (match(TokenType.IDENTIFIER)) {
                val name = previous.lexeme
                when {
                    // 嵌套修改: name(...)
                    match(TokenType.LPAREN) -> {
                        val nestedMod = parseModification()
                        args.add(Argument.ComponentModification(name, nestedMod))
                    }
                    // 命名参数: name = value
                    match(TokenType.EQUALS) -> {
                        val value = parseExpression()
                        args.add(Argument.Named(name, value))
                    }
                    else -> {
                        // 可能是数组索引或其他情况，暂时跳过
                        args.add(Argument.Named(name, null))
                    }
                }
            } else {
                val value = parseExpression()
                args.add(Argument.Positional(value))
            }
            if (!match(TokenType.COMMA)) break
        }
        consume(TokenType.RPAREN, "Expected ')' after modification")
        return Modification.Arguments(args)
    }

    // ==================== 方程解析 ====================

    private fun parseEquationSection(): List<Equation> {
        consume(TokenType.EQUATION, "Expected 'equation'")
        val equations = mutableListOf<Equation>()

        while (!check(TokenType.EQUATION, TokenType.ALGORITHM, TokenType.INITIAL,
            TokenType.PUBLIC, TokenType.PROTECTED, TokenType.END, TokenType.ANNOTATION) &&
            !isAtEnd) {
            equations.add(parseEquation())
        }

        return equations
    }

    private fun parseInitialEquationSection(): List<Equation> {
        consume(TokenType.INITIAL, "Expected 'initial'")
        consume(TokenType.EQUATION, "Expected 'equation'")
        val equations = mutableListOf<Equation>()

        while (!check(TokenType.EQUATION, TokenType.ALGORITHM, TokenType.INITIAL,
            TokenType.PUBLIC, TokenType.PROTECTED, TokenType.END, TokenType.ANNOTATION) &&
            !isAtEnd) {
            val equation = parseEquation()
            // Set isInitial flag for initial equations
            equations.add(when (equation) {
                is Equation.Simple -> equation.copy(isInitial = true)
                is Equation.Connect -> equation.copy(isInitial = true)
                is Equation.For -> equation.copy(isInitial = true)
                is Equation.If -> equation.copy(isInitial = true)
                is Equation.When -> equation.copy(isInitial = true)
            })
        }

        return equations
    }

    private fun parseEquation(): Equation {
        // 检查是否为连接方程
        if (match(TokenType.CONNECT)) {
            return parseConnectEquation()
        }

        // 检查是否为for方程
        if (match(TokenType.FOR)) {
            return parseForEquation()
        }

        // 检查是否为if方程
        if (match(TokenType.IF)) {
            return parseIfEquation()
        }

        // 简单方程：表达式 = 表达式
        val left = parseExpression()
        consume(TokenType.EQUALS, "Expected '=' in equation")
        val right = parseExpression()
        consume(TokenType.SEMICOLON, "Expected ';' after equation")

        return Equation.Simple(left, right)
    }

    private fun parseConnectEquation(): Equation {
        consume(TokenType.LPAREN, "Expected '(' after 'connect'")
        val left = parseExpression()
        consume(TokenType.COMMA, "Expected ',' in connect")
        val right = parseExpression()
        consume(TokenType.RPAREN, "Expected ')' after connect arguments")

        // 解析可选的 annotation
        val hasAnnotation = match(TokenType.ANNOTATION)
        if (hasAnnotation) {
            parseAnnotation()
            // annotation 后面的分号已经被 parseAnnotation() 消耗了
        } else {
            consume(TokenType.SEMICOLON, "Expected ';' after connect")
        }

        return Equation.Connect(left, right)
    }

    private fun parseForEquation(): Equation {
        val iterator = consume(TokenType.IDENTIFIER, "Expected iterator variable").lexeme
        consume(TokenType.IN, "Expected 'in' in for equation")
        val range = parseExpression()
        consume(TokenType.LOOP, "Expected 'loop'")

        val equations = mutableListOf<Equation>()
        while (!check(TokenType.END_FOR) && !isAtEnd) {
            equations.add(parseEquation())
        }
        consume(TokenType.END_FOR, "Expected 'end for'")
        consume(TokenType.SEMICOLON, "Expected ';' after 'end for'")

        return Equation.For(iterator, range, equations)
    }

    private fun parseIfEquation(): Equation {
        val condition = parseExpression()
        consume(TokenType.THEN, "Expected 'then'")

        val thenEquations = mutableListOf<Equation>()
        while (!check(TokenType.ELSEIF, TokenType.ELSE, TokenType.END_IF) && !isAtEnd) {
            thenEquations.add(parseEquation())
        }

        val elseIfBranches = mutableListOf<Pair<Expression, List<Equation>>>()
        while (match(TokenType.ELSEIF)) {
            val elseIfCondition = parseExpression()
            consume(TokenType.THEN, "Expected 'then'")
            val elseIfEquations = mutableListOf<Equation>()
            while (!check(TokenType.ELSEIF, TokenType.ELSE, TokenType.END_IF) && !isAtEnd) {
                elseIfEquations.add(parseEquation())
            }
            elseIfBranches.add(elseIfCondition to elseIfEquations)
        }

        val elseEquations = if (match(TokenType.ELSE)) {
            val equations = mutableListOf<Equation>()
            while (!check(TokenType.END_IF) && !isAtEnd) {
                equations.add(parseEquation())
            }
            equations
        } else null

        consume(TokenType.END_IF, "Expected 'end if'")
        consume(TokenType.SEMICOLON, "Expected ';' after 'end if'")

        return Equation.If(condition, thenEquations, elseIfBranches, elseEquations)
    }

    // ==================== 算法解析 ====================

    private fun parseAlgorithmSection(): AlgorithmSection {
        consume(TokenType.ALGORITHM, "Expected 'algorithm'")
        val statements = mutableListOf<Statement>()

        while (!check(TokenType.EQUATION, TokenType.ALGORITHM, TokenType.INITIAL,
            TokenType.PUBLIC, TokenType.PROTECTED, TokenType.END, TokenType.ANNOTATION) &&
            !isAtEnd) {
            statements.add(parseStatement())
        }

        return AlgorithmSection(statements, isInitial = false)
    }

    private fun parseInitialAlgorithmSection(): AlgorithmSection {
        consume(TokenType.INITIAL, "Expected 'initial'")
        consume(TokenType.ALGORITHM, "Expected 'algorithm'")
        val statements = mutableListOf<Statement>()

        while (!check(TokenType.EQUATION, TokenType.ALGORITHM, TokenType.INITIAL,
            TokenType.PUBLIC, TokenType.PROTECTED, TokenType.END, TokenType.ANNOTATION) &&
            !isAtEnd) {
            statements.add(parseStatement())
        }

        return AlgorithmSection(statements, isInitial = true)
    }

    private fun parseStatement(): Statement {
        return when {
            match(TokenType.IF) -> parseIfStatement()
            match(TokenType.FOR) -> parseForStatement()
            match(TokenType.WHILE) -> parseWhileStatement()
            match(TokenType.WHEN) -> parseWhenStatement()
            match(TokenType.BREAK) -> {
                consume(TokenType.SEMICOLON, "Expected ';' after 'break'")
                Statement.Break
            }
            match(TokenType.RETURN) -> {
                consume(TokenType.SEMICOLON, "Expected ';' after 'return'")
                Statement.Return(null)
            }
            else -> parseAssignmentOrFunctionCall()
        }
    }

    private fun parseAssignmentOrFunctionCall(): Statement {
        val expr = parseExpression()

        return if (match(TokenType.ASSIGN)) {
            val value = parseExpression()
            consume(TokenType.SEMICOLON, "Expected ';' after assignment")
            Statement.Assignment(expr, value)
        } else {
            consume(TokenType.SEMICOLON, "Expected ';' after function call")
            Statement.FunctionCall(expr)
        }
    }

    private fun parseIfStatement(): Statement.If {
        val condition = parseExpression()
        consume(TokenType.THEN, "Expected 'then'")

        val thenStatements = mutableListOf<Statement>()
        while (!check(TokenType.ELSEIF, TokenType.ELSE, TokenType.END_IF) && !isAtEnd) {
            thenStatements.add(parseStatement())
        }

        val elseIfBranches = mutableListOf<Pair<Expression, List<Statement>>>()
        while (match(TokenType.ELSEIF)) {
            val elseIfCondition = parseExpression()
            consume(TokenType.THEN, "Expected 'then'")
            val elseIfStatements = mutableListOf<Statement>()
            while (!check(TokenType.ELSEIF, TokenType.ELSE, TokenType.END_IF) && !isAtEnd) {
                elseIfStatements.add(parseStatement())
            }
            elseIfBranches.add(elseIfCondition to elseIfStatements)
        }

        val elseStatements = if (match(TokenType.ELSE)) {
            val statements = mutableListOf<Statement>()
            while (!check(TokenType.END_IF) && !isAtEnd) {
                statements.add(parseStatement())
            }
            statements
        } else null

        consume(TokenType.END_IF, "Expected 'end if'")
        consume(TokenType.SEMICOLON, "Expected ';' after 'end if'")

        return Statement.If(condition, thenStatements, elseIfBranches, elseStatements)
    }

    private fun parseForStatement(): Statement.For {
        val iterator = consume(TokenType.IDENTIFIER, "Expected iterator variable").lexeme
        consume(TokenType.IN, "Expected 'in' in for statement")
        val range = parseExpression()
        consume(TokenType.LOOP, "Expected 'loop'")

        val statements = mutableListOf<Statement>()
        while (!check(TokenType.END_FOR) && !isAtEnd) {
            statements.add(parseStatement())
        }
        consume(TokenType.END_FOR, "Expected 'end for'")
        consume(TokenType.SEMICOLON, "Expected ';' after 'end for'")

        return Statement.For(iterator, range, statements)
    }

    private fun parseWhileStatement(): Statement.While {
        val condition = parseExpression()
        consume(TokenType.LOOP, "Expected 'loop'")

        val statements = mutableListOf<Statement>()
        while (!check(TokenType.END_WHILE) && !isAtEnd) {
            statements.add(parseStatement())
        }
        consume(TokenType.END_WHILE, "Expected 'end while'")
        consume(TokenType.SEMICOLON, "Expected ';' after 'end while'")

        return Statement.While(condition, statements)
    }

    private fun parseWhenStatement(): Statement.When {
        val condition = parseExpression()
        consume(TokenType.THEN, "Expected 'then'")

        val statements = mutableListOf<Statement>()
        while (!check(TokenType.ELSE, TokenType.END_WHEN) && !isAtEnd) {
            statements.add(parseStatement())
        }

        val elseWhen = if (match(TokenType.ELSE)) {
            if (check(TokenType.WHEN)) {
                advance()
                parseWhenStatement()
            } else null
        } else null

        if (elseWhen == null) {
            consume(TokenType.END_WHEN, "Expected 'end when'")
            consume(TokenType.SEMICOLON, "Expected ';' after 'end when'")
        }

        return Statement.When(condition, statements, elseWhen)
    }

    // ==================== 表达式解析 ====================

    private fun parseExpression(): Expression = parseOrExpression()

    private fun parseOrExpression(): Expression {
        var left = parseAndExpression()
        while (match(TokenType.OR)) {
            val right = parseAndExpression()
            left = Expression.Binary(left, BinaryOperator.OR, right)
        }
        return left
    }

    private fun parseAndExpression(): Expression {
        var left = parseNotExpression()
        while (match(TokenType.AND)) {
            val right = parseNotExpression()
            left = Expression.Binary(left, BinaryOperator.AND, right)
        }
        return left
    }

    private fun parseNotExpression(): Expression {
        return if (match(TokenType.NOT)) {
            val operand = parseNotExpression()
            Expression.Unary(UnaryOperator.NOT, operand)
        } else {
            parseComparisonExpression()
        }
    }

    private fun parseComparisonExpression(): Expression {
        var left = parseAdditiveExpression()

        while (check(TokenType.EQ, TokenType.NE, TokenType.LT, TokenType.LE,
            TokenType.GT, TokenType.GE)) {
            val op = when (advance().type) {
                TokenType.EQ -> BinaryOperator.EQ
                TokenType.NE -> BinaryOperator.NE
                TokenType.LT -> BinaryOperator.LT
                TokenType.LE -> BinaryOperator.LE
                TokenType.GT -> BinaryOperator.GT
                TokenType.GE -> BinaryOperator.GE
                else -> throw ParseError("Expected comparison operator", previous.location)
            }
            val right = parseAdditiveExpression()
            left = Expression.Binary(left, op, right)
        }

        return left
    }

    private fun parseAdditiveExpression(): Expression {
        var left = parseMultiplicativeExpression()

        while (check(TokenType.PLUS, TokenType.MINUS)) {
            val op = if (advance().type == TokenType.PLUS) BinaryOperator.ADD else BinaryOperator.SUB
            val right = parseMultiplicativeExpression()
            left = Expression.Binary(left, op, right)
        }

        return left
    }

    private fun parseMultiplicativeExpression(): Expression {
        var left = parsePowerExpression()

        while (check(TokenType.STAR, TokenType.SLASH)) {
            val op = if (advance().type == TokenType.STAR) BinaryOperator.MUL else BinaryOperator.DIV
            val right = parsePowerExpression()
            left = Expression.Binary(left, op, right)
        }

        return left
    }

    private fun parsePowerExpression(): Expression {
        val left = parseUnaryExpression()
        return if (match(TokenType.POWER)) {
            val right = parsePowerExpression() // 右结合
            Expression.Binary(left, BinaryOperator.POWER, right)
        } else {
            left
        }
    }

    private fun parseUnaryExpression(): Expression {
        return when {
            match(TokenType.MINUS) -> {
                val operand = parseUnaryExpression()
                Expression.Unary(UnaryOperator.NEG, operand)
            }
            match(TokenType.PLUS) -> parseUnaryExpression()
            else -> parsePostfixExpression()
        }
    }

    private fun parsePostfixExpression(): Expression {
        var expr = parsePrimaryExpression()

        while (true) {
            when {
                match(TokenType.DOT) -> {
                    val name = consume(TokenType.IDENTIFIER, "Expected member name").lexeme
                    expr = Expression.MemberAccess(expr, name)
                }
                match(TokenType.LBRACKET) -> {
                    val indices = mutableListOf<Expression>()
                    indices.add(parseExpression())
                    while (match(TokenType.COMMA)) {
                        indices.add(parseExpression())
                    }
                    consume(TokenType.RBRACKET, "Expected ']'")
                    expr = Expression.ArrayAccess(expr, indices)
                }
                match(TokenType.LPAREN) -> {
                    val args = mutableListOf<Expression>()
                    if (!check(TokenType.RPAREN)) {
                        args.add(parseExpression())
                        while (match(TokenType.COMMA)) {
                            args.add(parseExpression())
                        }
                    }
                    consume(TokenType.RPAREN, "Expected ')'")
                    expr = Expression.FunctionCall(expr, args)
                }
                else -> break
            }
        }

        return expr
    }

    private fun parsePrimaryExpression(): Expression {
        return when {
            match(TokenType.INTEGER_LITERAL) -> Expression.IntegerLiteral(previous.lexeme.toLong())
            match(TokenType.REAL_LITERAL) -> Expression.RealLiteral(previous.lexeme.toDouble())
            match(TokenType.STRING_LITERAL) -> Expression.StringLiteral(previous.lexeme.removeSurrounding("\""))
            match(TokenType.TRUE) -> Expression.BooleanLiteral(true)
            match(TokenType.FALSE) -> Expression.BooleanLiteral(false)
            match(TokenType.DER) -> {
                consume(TokenType.LPAREN, "Expected '(' after 'der'")
                val arg = parseExpression()
                consume(TokenType.RPAREN, "Expected ')' after der argument")
                Expression.Der(arg)
            }
            match(TokenType.IDENTIFIER) -> Expression.Identifier(previous.lexeme)
            match(TokenType.LPAREN) -> {
                val expr = parseExpression()
                consume(TokenType.RPAREN, "Expected ')'")
                expr
            }
            match(TokenType.LBRACE) -> {
                val elements = mutableListOf<Expression>()
                if (!check(TokenType.RBRACE)) {
                    elements.add(parseExpression())
                    while (match(TokenType.COMMA)) {
                        elements.add(parseExpression())
                    }
                }
                consume(TokenType.RBRACE, "Expected '}'")
                Expression.ArrayLiteral(elements)
            }
            else -> throw ParseError("Expected expression", peek.location)
        }
    }

    // ==================== 辅助方法 ====================

    private fun parseIdentifierPath(): String {
        val parts = mutableListOf<String>()

        // 接受标识符或内置类型关键字
        val firstPart = when {
            match(TokenType.IDENTIFIER) -> previous.lexeme
            match(TokenType.REAL) -> previous.lexeme
            match(TokenType.INTEGER) -> previous.lexeme
            match(TokenType.BOOLEAN) -> previous.lexeme
            match(TokenType.STRING) -> previous.lexeme
            else -> throw ParseError("Expected identifier or built-in type", peek.location)
        }
        parts.add(firstPart)

        while (match(TokenType.DOT)) {
            parts.add(consume(TokenType.IDENTIFIER, "Expected identifier").lexeme)
        }

        return parts.joinToString(".")
    }

    private fun parseDescription(): String? {
        return if (match(TokenType.STRING_LITERAL)) {
            previous.lexeme.removeSurrounding("\"")
        } else null
    }

    private fun parseAnnotation() {
        // ANNOTATION token 已经被 match() 消耗了
        // 跳过注解内容
        var depth = 0
        do {
            when {
                match(TokenType.LPAREN) -> depth++
                match(TokenType.RPAREN) -> depth--
                else -> advance()
            }
        } while (depth > 0 && !isAtEnd)

        // 消耗可选的分号
        match(TokenType.SEMICOLON)
    }

    // ==================== Token消费辅助方法 ====================

    private fun advance(): Token {
        if (!isAtEnd) current++
        return previous
    }

    private fun check(vararg types: TokenType): Boolean {
        return peek.type in types
    }

    private fun checkNext(vararg types: TokenType): Boolean {
        return tokens.getOrNull(current + 1)?.type in types
    }

    private fun match(vararg types: TokenType): Boolean {
        return if (check(*types)) {
            advance()
            true
        } else false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw ParseError.expected(type, peek).also { message }
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd) {
            when (peek.type) {
                TokenType.SEMICOLON -> {
                    advance()
                    return
                }
                TokenType.CLASS, TokenType.MODEL, TokenType.RECORD,
                TokenType.BLOCK, TokenType.CONNECTOR, TokenType.TYPE,
                TokenType.PACKAGE, TokenType.FUNCTION,
                TokenType.EQUATION, TokenType.ALGORITHM,
                TokenType.PUBLIC, TokenType.PROTECTED,
                TokenType.END -> return
                else -> advance()
            }
        }
    }

    companion object {
        /**
         * 便捷方法：解析Token列表
         */
        fun parse(tokens: List<Token>): ParseResult<Program> {
            return ModelicaParser(tokens).parse()
        }
    }
}