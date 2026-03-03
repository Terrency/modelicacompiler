package org.modelica.ide.services

import org.modelica.compiler.CompilationOptions
import org.modelica.compiler.ModelicaCompiler
import org.modelica.ide.models.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Modelica Standard Library 内置文件
 */
object ModelicaStandardLibrary {
    val files = listOf(
        ProjectFile(
            id = "msl_package",
            name = "Modelica",
            path = "Modelica/package.mo",
            content = """
within ;
package Modelica "Modelica Standard Library - Version 4.0.0"
  extends Modelica.Icons.Package;

  annotation (
    version = "4.0.0",
    versionDate = "2020-06-25",
    Documentation(info = "<html>
<p>Package <strong>Modelica</strong> is a standardized and free library.</p>
</html>"));
end Modelica;
            """.trimIndent(),
            size = 500
        ),
        ProjectFile(
            id = "msl_icons",
            name = "Icons",
            path = "Modelica/Icons.mo",
            content = """
within Modelica;
package Icons "Icon definitions"
  extends Package;

  partial package Package "Icon for standard packages"
    annotation(Icon(graphics={Rectangle(
      lineColor={200,200,200},
      fillColor={248,248,248},
      extent={{-100,-100},{100,100}},
      radius=25)}));
  end Package;

  partial model Example "Icon for example models"
    annotation(Icon(graphics={Ellipse(
      lineColor={128,128,128},
      fillColor={248,248,248},
      extent={{-80,-80},{80,80}})}));
  end Example;

  partial function Function "Icon for functions"
    annotation(Icon(graphics={Ellipse(
      lineColor={128,128,128},
      fillColor={248,248,248},
      extent={{-100,-100},{100,100}})}));
  end Function;

end Icons;
            """.trimIndent(),
            size = 800
        ),
        ProjectFile(
            id = "msl_constants",
            name = "Constants",
            path = "Modelica/Constants.mo",
            content = """
within Modelica;
package Constants "Mathematical constants and constants of nature"
  extends Modelica.Icons.Package;

  // Mathematical constants
  final constant Real e = 2.71828182845905;
  final constant Real pi = 3.14159265358979;
  final constant Real D2R = pi/180 "Degree to Radian";
  final constant Real R2D = 180/pi "Radian to Degree";

  // Constants of nature
  final constant Real N_A = 6.02214076e23 "Avogadro constant";
  final constant Real k = 1.380649e-23 "Boltzmann constant";
  final constant Real c = 299792458 "Speed of light";
  final constant Real g_n = 9.80665 "Standard gravity";

  annotation (Documentation(info="<html><p>Mathematical and physical constants.</p></html>"));
end Constants;
            """.trimIndent(),
            size = 600
        ),
        ProjectFile(
            id = "msl_math",
            name = "Math",
            path = "Modelica/Math/package.mo",
            content = """
within Modelica;
package Math "Mathematical functions"
  extends Modelica.Icons.Package;

  function exp "Exponential function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = exp(u);
  end exp;

  function sin "Sine function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = sin(u);
  end sin;

  function cos "Cosine function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = cos(u);
  end cos;

  function sqrt "Square root"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = sqrt(u);
  end sqrt;

  annotation (Documentation(info="<html><p>Standard mathematical functions.</p></html>"));
end Math;
            """.trimIndent(),
            size = 1200
        ),
        ProjectFile(
            id = "msl_siunits",
            name = "SIunits",
            path = "Modelica/SIunits.mo",
            content = """
within Modelica;
package SIunits "Type definitions based on SI units"
  extends Modelica.Icons.Package;

  // Base units
  type Length = Real(final quantity="Length", final unit="m");
  type Mass = Real(final quantity="Mass", final unit="kg");
  type Time = Real(final quantity="Time", final unit="s");
  type Current = Real(final quantity="Current", final unit="A");
  type Temperature = Real(final quantity="Temperature", final unit="K");

  // Derived units
  type Angle = Real(final quantity="Angle", final unit="rad");
  type Velocity = Real(final quantity="Velocity", final unit="m/s");
  type Acceleration = Real(final quantity="Acceleration", final unit="m/s2");
  type Force = Real(final quantity="Force", final unit="N");
  type Energy = Real(final quantity="Energy", final unit="J");
  type Power = Real(final quantity="Power", final unit="W");

  // Angular quantities
  type AngularVelocity = Real(final quantity="AngularVelocity", final unit="rad/s");

  annotation (Documentation(info="<html><p>SI unit type definitions.</p></html>"));
end SIunits;
            """.trimIndent(),
            size = 900
        ),
        // Examples
        ProjectFile(
            id = "ex_helloworld",
            name = "HelloWorld",
            path = "Examples/HelloWorld.mo",
            content = """
within ;
model HelloWorld "The simplest Modelica model"
  extends Modelica.Icons.Example;

  Real x(start=1) "A state variable";

equation
  der(x) = -x;

  annotation (
    Documentation(info="<html><p>Simplest Modelica model: dx/dt = -x</p></html>"),
    experiment(StopTime=5, Interval=0.01));
end HelloWorld;
            """.trimIndent(),
            size = 400
        ),
        ProjectFile(
            id = "ex_pendulum",
            name = "SimplePendulum",
            path = "Examples/SimplePendulum.mo",
            content = """
within ;
model SimplePendulum "A simple pendulum model"
  extends Modelica.Icons.Example;

  parameter Real L = 1.0 "Pendulum length (m)";
  parameter Real g = 9.81 "Gravity (m/s2)";
  parameter Real theta0 = 0.1 "Initial angle (rad)";

  Real theta(start=theta0) "Pendulum angle";
  Real omega(start=0) "Angular velocity";

equation
  der(theta) = omega;
  der(omega) = -(g/L) * Modelica.Math.sin(theta);

  annotation (
    Documentation(info="<html><p>Simple pendulum dynamics.</p></html>"),
    experiment(StopTime=10, Interval=0.01));
end SimplePendulum;
            """.trimIndent(),
            size = 600
        ),
        ProjectFile(
            id = "ex_bouncing",
            name = "BouncingBall",
            path = "Examples/BouncingBall.mo",
            content = """
within ;
model BouncingBall "A bouncing ball model"
  extends Modelica.Icons.Example;

  parameter Real e = 0.7 "Coefficient of restitution";
  parameter Real g = 9.81 "Gravity";

  Real h(start=1.0) "Height of ball";
  Real v(start=0) "Velocity of ball";
  Boolean flying(start=true) "True if ball is flying";

equation
  when h < 0 then
    flying = false;
    reinit(v, -e * pre(v));
    reinit(h, 0);
    flying = true;
  end when;

  der(h) = v;
  der(v) = if flying then -g else 0;

  annotation (
    Documentation(info="<html><p>Ball bouncing on a surface.</p></html>"),
    experiment(StopTime=5, Interval=0.01));
end BouncingBall;
            """.trimIndent(),
            size = 700
        ),
        ProjectFile(
            id = "ex_dcmotor",
            name = "DCMotor",
            path = "Examples/DCMotor.mo",
            content = """
within ;
model DCMotor "A simple DC motor model"
  extends Modelica.Icons.Example;

  parameter Real R = 1.0 "Resistance (Ohm)";
  parameter Real L = 0.01 "Inductance (H)";
  parameter Real K = 0.1 "Motor constant";
  parameter Real J = 0.01 "Inertia (kg.m2)";
  parameter Real B = 0.001 "Damping";

  Real v "Voltage (V)";
  Real i(start=0) "Current (A)";
  Real w(start=0) "Angular velocity (rad/s)";
  Real tau "Torque (N.m)";

equation
  v = R*i + L*der(i) + K*w;
  tau = K*i;
  J*der(w) = tau - B*w;
  v = if time < 0.5 then 0 else 10;

  annotation (
    Documentation(info="<html><p>DC motor with electrical and mechanical dynamics.</p></html>"),
    experiment(StopTime=2, Interval=0.001));
end DCMotor;
            """.trimIndent(),
            size = 800
        )
    )

    val examples = files.filter { it.path.startsWith("Examples/") }
    val library = files.filter { it.path.startsWith("Modelica/") }
}

/**
 * 编译服务
 */
class CompilerService {
    private val compiler = ModelicaCompiler(
        CompilationOptions(verbose = false)
    )

    /**
     * 编译代码
     */
    fun compile(code: String, fileName: String? = null): CompileResult {
        return try {
            val result = compiler.compile(code, fileName)

            val errors = result.lexerErrors.map { parseError(it) } +
                    result.parserErrors.map { parseError(it) } +
                    result.semanticErrors.map { parseError(it) }

            CompileResult(
                success = result.success,
                errors = errors,
                output = result.codegenErrors,
                generatedFiles = result.outputClasses.keys.toList()
            )
        } catch (e: Exception) {
            CompileResult(
                success = false,
                errors = listOf(CompileError("Compilation failed: ${e.message}"))
            )
        }
    }

    private fun parseError(errorStr: String): CompileError {
        val lineMatch = Regex("at (\\d+):(\\d+)").find(errorStr)
        return CompileError(
            message = errorStr,
            line = lineMatch?.groupValues?.get(1)?.toIntOrNull(),
            column = lineMatch?.groupValues?.get(2)?.toIntOrNull()
        )
    }
}

/**
 * 项目服务
 */
class ProjectService {
    private val projects = ConcurrentHashMap<String, Project>()
    private val projectCounter = java.util.concurrent.atomic.AtomicLong(0)

    init {
        // 创建默认项目，包含MSL
        val defaultProject = Project(
            id = "default",
            name = "Modelica Project",
            description = "Project with Modelica Standard Library",
            files = ModelicaStandardLibrary.files
        )
        projects["default"] = defaultProject
    }

    fun getAllProjects(): List<Project> = projects.values.toList()

    fun getProject(id: String): Project? = projects[id]

    fun createProject(request: CreateProjectRequest): Project {
        val id = "proj_${projectCounter.incrementAndGet()}"
        val project = Project(
            id = id,
            name = request.name,
            description = request.description,
            files = ModelicaStandardLibrary.files
        )
        projects[id] = project
        return project
    }

    fun updateProject(id: String, request: UpdateProjectRequest): Project? {
        val existing = projects[id] ?: return null
        val updated = existing.copy(
            name = request.name ?: existing.name,
            description = request.description ?: existing.description,
            updatedAt = System.currentTimeMillis()
        )
        projects[id] = updated
        return updated
    }

    fun deleteProject(id: String): Boolean {
        return projects.remove(id) != null
    }
}

/**
 * 文件服务
 */
class FileService(private val projectService: ProjectService) {
    private val fileCounter = java.util.concurrent.atomic.AtomicLong(1000)

    fun getFile(projectId: String, fileId: String): ProjectFile? {
        val project = projectService.getProject(projectId) ?: return null
        return project.files.find { it.id == fileId }
    }

    fun createFile(projectId: String, request: CreateFileRequest): ProjectFile? {
        val project = projectService.getProject(projectId) ?: return null
        val fileId = "file_${fileCounter.incrementAndGet()}"
        val file = ProjectFile(
            id = fileId,
            name = request.name,
            path = request.path ?: request.name,
            content = request.content
        )

        val updatedProject = project.copy(
            files = project.files + file,
            updatedAt = System.currentTimeMillis()
        )
        projectService.updateProject(projectId, UpdateProjectRequest())

        return file
    }

    fun updateFile(projectId: String, fileId: String, request: UpdateFileRequest): ProjectFile? {
        val project = projectService.getProject(projectId) ?: return null
        val fileIndex = project.files.indexOfFirst { it.id == fileId }
        if (fileIndex < 0) return null

        val updatedFile = project.files[fileIndex].copy(
            content = request.content,
            lastModified = System.currentTimeMillis()
        )

        val updatedFiles = project.files.toMutableList()
        updatedFiles[fileIndex] = updatedFile

        return updatedFile
    }

    fun deleteFile(projectId: String, fileId: String): Boolean {
        val project = projectService.getProject(projectId) ?: return false
        val fileIndex = project.files.indexOfFirst { it.id == fileId }
        if (fileIndex < 0) return false

        val updatedFiles = project.files.toMutableList()
        updatedFiles.removeAt(fileIndex)

        return true
    }
}