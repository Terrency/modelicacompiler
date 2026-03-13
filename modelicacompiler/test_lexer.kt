package org.modelica.compiler

import org.modelica.compiler.lexer.ModelicaLexer
import org.modelica.compiler.lexer.TokenType

fun main() {
    val source = "model Test Real x; equation x = 1; end Test;"
    println("Source: $source")
    println()

    val result = ModelicaLexer.tokenize(source)

    println("=== Tokens ===")
    result.tokens.forEach { token ->
        println("${token.type.name.padEnd(20)} '${token.lexeme}' at ${token.location}")
    }

    println()
    println("=== Errors ===")
    if (result.hasErrors) {
        result.errors.forEach { error ->
            println(error)
        }
    } else {
        println("No errors")
    }

    println()
    println("=== Keyword Map ===")
    TokenType.keywords.forEach { (key, value) ->
        println("$key -> ${value.name}")
    }
}