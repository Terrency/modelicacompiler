package org.modelica.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DiagnosticTest {

    private val compiler = ModelicaCompiler()

    @Test
    fun `test SimplePendulum compilation with diagnostics`() {
        val source = """
model SimplePendulum
  parameter Real L = 1.0;
  parameter Real g = 9.81;
  parameter Real theta0 = 0.1;

  Real theta(start=theta0);
  Real omega(start=0);

equation
  der(theta) = omega;
  der(omega) = -(g/L) * sin(theta);
end SimplePendulum;
        """.trimIndent()

        val result = compiler.compile(source, "SimplePendulum.mo")

        println("=== Compilation Result ===")
        println("Success: ${result.success}")
        println("Lexer errors: ${result.lexerErrors}")
        println("Parser errors: ${result.parserErrors}")
        println("Semantic errors: ${result.semanticErrors}")
        println("Codegen errors: ${result.codegenErrors}")
        println("Output classes: ${result.outputClasses.keys}")

        if (!result.success) {
            println("\n=== All Errors ===")
            result.allErrors.forEach { println("  - $it") }
        }

        assertTrue(result.success, "Compilation should succeed: ${result.allErrors}")
    }
}