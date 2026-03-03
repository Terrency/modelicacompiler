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
        // Main Modelica package
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
        // Icons
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

  partial block Block "Icon for blocks"
    annotation(Icon(graphics={Rectangle(
      lineColor={128,128,128},
      fillColor={248,248,248},
      extent={{-100,-100},{100,100}})}));
  end Block;

  partial package InterfacesPackage "Icon for interfaces packages"
    extends Package;
  end InterfacesPackage;

  partial package SourcesPackage "Icon for sources packages"
    extends Package;
  end SourcesPackage;

end Icons;
            """.trimIndent(),
            size = 1200
        ),
        // Constants
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
  final constant Real gamma = 0.57721566490153 "Euler's constant";

  // Constants of nature
  final constant Real N_A = 6.02214076e23 "Avogadro constant";
  final constant Real k = 1.380649e-23 "Boltzmann constant";
  final constant Real R = N_A*k "Molar gas constant";
  final constant Real c = 299792458 "Speed of light";
  final constant Real g_n = 9.80665 "Standard gravity";
  final constant Real sigma = 5.670374419e-8 "Stefan-Boltzmann constant";
  final constant Real small = 1e-60 "Smallest number";

  annotation (Documentation(info="<html><p>Mathematical and physical constants.</p></html>"));
end Constants;
            """.trimIndent(),
            size = 800
        ),
        // Math package
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

  function log "Natural logarithm"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = log(u);
  end log;

  function log10 "Base 10 logarithm"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = log10(u);
  end log10;

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

  function tan "Tangent function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = tan(u);
  end tan;

  function sinh "Hyperbolic sine"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = sinh(u);
  end sinh;

  function cosh "Hyperbolic cosine"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = cosh(u);
  end cosh;

  function tanh "Hyperbolic tangent"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = tanh(u);
  end tanh;

  function asin "Inverse sine"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = asin(u);
  end asin;

  function acos "Inverse cosine"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = acos(u);
  end acos;

  function atan "Inverse tangent"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = atan(u);
  end atan;

  function atan2 "Four quadrant inverse tangent"
    extends Modelica.Icons.Function;
    input Real u1;
    input Real u2;
    output Real y;
  external "builtin" y = atan2(u1, u2);
  end atan2;

  function sqrt "Square root"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = sqrt(u);
  end sqrt;

  annotation (Documentation(info="<html><p>Standard mathematical functions.</p></html>"));
end Math;
            """.trimIndent(),
            size = 2500
        ),
        // SIunits
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
  type AmountOfSubstance = Real(final quantity="AmountOfSubstance", final unit="mol");

  // Derived units
  type Angle = Real(final quantity="Angle", final unit="rad");
  type Velocity = Real(final quantity="Velocity", final unit="m/s");
  type Acceleration = Real(final quantity="Acceleration", final unit="m/s2");
  type Force = Real(final quantity="Force", final unit="N");
  type Pressure = Real(final quantity="Pressure", final unit="Pa");
  type AbsolutePressure = Pressure;
  type Energy = Real(final quantity="Energy", final unit="J");
  type Power = Real(final quantity="Power", final unit="W");
  type Frequency = Real(final quantity="Frequency", final unit="Hz");

  // Angular quantities
  type AngularVelocity = Real(final quantity="AngularVelocity", final unit="rad/s");
  type AngularAcceleration = Real(final quantity="AngularAcceleration", final unit="rad/s2");

  // Area and Volume
  type Area = Real(final quantity="Area", final unit="m2");
  type Volume = Real(final quantity="Volume", final unit="m3");

  // Electrical
  type Voltage = Real(final quantity="Voltage", final unit="V");
  type Resistance = Real(final quantity="Resistance", final unit="Ohm");
  type Conductance = Real(final quantity="Conductance", final unit="S");
  type Capacitance = Real(final quantity="Capacitance", final unit="F");
  type Inductance = Real(final quantity="Inductance", final unit="H");

  // Thermal
  type HeatFlowRate = Real(final quantity="HeatFlowRate", final unit="W");
  type HeatCapacity = Real(final quantity="HeatCapacity", final unit="J/K");
  type ThermalConductance = Real(final quantity="ThermalConductance", final unit="W/K");
  type ThermalResistance = Real(final quantity="ThermalResistance", final unit="K/W");

  // Mechanical
  type Torque = Real(final quantity="Torque", final unit="N.m");
  type Inertia = Real(final quantity="Inertia", final unit="kg.m2");
  type Position = Length;
  type RotationalSpringConstant = Real(final quantity="RotationalSpringConstant", final unit="N.m/rad");
  type RotationalDampingConstant = Real(final quantity="RotationalDampingConstant", final unit="N.m.s/rad");

  annotation (Documentation(info="<html><p>SI unit type definitions.</p></html>"));
end SIunits;
            """.trimIndent(),
            size = 2000
        ),
        // Blocks package
        ProjectFile(
            id = "msl_blocks",
            name = "Blocks",
            path = "Modelica/Blocks/package.mo",
            content = """
within Modelica;
package Blocks "Library of basic input/output control blocks"
  extends Modelica.Icons.Package;

  package Interfaces "Connectors and partial models"
    extends Modelica.Icons.InterfacesPackage;
    connector RealInput = input Real;
    connector RealOutput = output Real;
    connector BooleanInput = input Boolean;
    connector BooleanOutput = output Boolean;
    partial block SISO "Single Input Single Output"
      extends Modelica.Icons.Block;
      RealInput u;
      RealOutput y;
    end SISO;
    partial block SI2SO "Two Inputs Single Output"
      extends Modelica.Icons.Block;
      RealInput u1;
      RealInput u2;
      RealOutput y;
    end SI2SO;
  end Interfaces;

  package Math "Mathematical blocks"
    extends Modelica.Icons.Package;
    block Gain "Output = k * input"
      extends Interfaces.SISO;
      parameter Real k(start=1);
    equation
      y = k * u;
    end Gain;
    block Add "Output = k1*u1 + k2*u2"
      extends Interfaces.SI2SO;
      parameter Real k1=1;
      parameter Real k2=1;
    equation
      y = k1*u1 + k2*u2;
    end Add;
    block Product "Output = u1 * u2"
      extends Interfaces.SI2SO;
    equation
      y = u1 * u2;
    end Product;
  end Math;

  package Sources "Signal source blocks"
    extends Modelica.Icons.SourcesPackage;
    block Constant "Generate constant signal"
      extends Interfaces.RealOutput;
      parameter Real k(start=1);
    equation
      y = k;
    end Constant;
    block Step "Generate step signal"
      extends Interfaces.RealOutput;
      parameter Real height=1;
      parameter Real offset=0;
      parameter Real startTime=0;
    equation
      y = offset + (if time < startTime then 0 else height);
    end Step;
    block Sine "Generate sine signal"
      extends Interfaces.RealOutput;
      parameter Real amplitude=1;
      parameter Real freqHz=1;
      parameter Real phase=0;
      parameter Real offset=0;
    equation
      y = offset + amplitude * Modelica.Math.sin(2*Modelica.Constants.pi*freqHz*time + phase);
    end Sine;
  end Sources;
end Blocks;
            """.trimIndent(),
            size = 2500
        ),
        // Electrical package
        ProjectFile(
            id = "msl_electrical",
            name = "Electrical",
            path = "Modelica/Electrical/package.mo",
            content = """
within Modelica;
package Electrical "Library for electrical models"
  extends Modelica.Icons.Package;
end Electrical;
            """.trimIndent(),
            size = 150
        ),
        // Electrical.Analog
        ProjectFile(
            id = "msl_electrical_analog",
            name = "Analog",
            path = "Modelica/Electrical/Analog/package.mo",
            content = """
within Modelica.Electrical;
package Analog "Library for analog electrical models"
  extends Modelica.Icons.Package;

  package Interfaces "Connectors and partial models"
    extends Modelica.Icons.InterfacesPackage;
    connector Pin "Pin of an electrical component"
      Modelica.SIunits.Voltage v;
      flow Modelica.SIunits.Current i;
    end Pin;
    connector PositivePin "Positive pin"
      Modelica.SIunits.Voltage v;
      flow Modelica.SIunits.Current i;
    end PositivePin;
    connector NegativePin "Negative pin"
      Modelica.SIunits.Voltage v;
      flow Modelica.SIunits.Current i;
    end NegativePin;
    partial model OnePort "Component with two pins"
      extends Modelica.Icons.Block;
      PositivePin p;
      NegativePin n;
      Modelica.SIunits.Voltage v;
      Modelica.SIunits.Current i;
    equation
      v = p.v - n.v;
      0 = p.i + n.i;
      i = p.i;
    end OnePort;
  end Interfaces;

  package Basic "Basic electrical components"
    extends Modelica.Icons.Package;
    model Resistor "Ideal linear resistor"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Resistance R(start=1);
    equation
      v = R*i;
    end Resistor;
    model Capacitor "Ideal linear capacitor"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Capacitance C(start=1);
    equation
      i = C*der(v);
    end Capacitor;
    model Inductor "Ideal linear inductor"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Inductance L(start=1);
    equation
      v = L*der(i);
    end Inductor;
  end Basic;

  package Sources "Electrical sources"
    extends Modelica.Icons.SourcesPackage;
    model ConstantVoltage "Constant voltage source"
      Interfaces.PositivePin p;
      Interfaces.NegativePin n;
      parameter Modelica.SIunits.Voltage V(start=1);
    equation
      p.v - n.v = V;
      p.i + n.i = 0;
    end ConstantVoltage;
  end Sources;
end Analog;
            """.trimIndent(),
            size = 2000
        ),
        // Mechanics package
        ProjectFile(
            id = "msl_mechanics",
            name = "Mechanics",
            path = "Modelica/Mechanics/package.mo",
            content = """
within Modelica;
package Mechanics "Library for mechanical systems"
  extends Modelica.Icons.Package;

  package Rotational "Rotational mechanics"
    extends Modelica.Icons.Package;

    package Interfaces "Connectors"
      extends Modelica.Icons.InterfacesPackage;
      connector Flange_a "Flange with positive torque"
        Modelica.SIunits.Angle phi;
        flow Modelica.SIunits.Torque tau;
      end Flange_a;
      connector Flange_b "Flange with negative torque"
        Modelica.SIunits.Angle phi;
        flow Modelica.SIunits.Torque tau;
      end Flange_b;
    end Interfaces;

    package Components "Rotational components"
      extends Modelica.Icons.Package;
      model Inertia "Rotational inertia"
        Interfaces.Flange_a flange_a;
        Interfaces.Flange_b flange_b;
        parameter Modelica.SIunits.Inertia J(start=1);
        Modelica.SIunits.AngularVelocity w;
      equation
        w = der(flange_a.phi);
        flange_a.phi = flange_b.phi;
        flange_a.tau + flange_b.tau = J*der(w);
      end Inertia;
      model Spring "Linear rotational spring"
        Interfaces.Flange_a flange_a;
        Interfaces.Flange_b flange_b;
        parameter Modelica.SIunits.RotationalSpringConstant c(start=1);
      equation
        flange_a.tau = c*(flange_a.phi - flange_b.phi);
        flange_a.tau + flange_b.tau = 0;
      end Spring;
      model Damper "Linear rotational damper"
        Interfaces.Flange_a flange_a;
        Interfaces.Flange_b flange_b;
        parameter Modelica.SIunits.RotationalDampingConstant d(start=1);
      equation
        flange_a.tau = d*der(flange_a.phi - flange_b.phi);
        flange_a.tau + flange_b.tau = 0;
      end Damper;
    end Components;
  end Rotational;

  package Translational "Translational mechanics"
    extends Modelica.Icons.Package;

    package Interfaces "Connectors"
      extends Modelica.Icons.InterfacesPackage;
      connector Flange_a "Flange with positive force"
        Modelica.SIunits.Position s;
        flow Modelica.SIunits.Force f;
      end Flange_a;
      connector Flange_b "Flange with negative force"
        Modelica.SIunits.Position s;
        flow Modelica.SIunits.Force f;
      end Flange_b;
    end Interfaces;
  end Translational;
end Mechanics;
            """.trimIndent(),
            size = 2500
        ),
        // Thermal package
        ProjectFile(
            id = "msl_thermal",
            name = "Thermal",
            path = "Modelica/Thermal/package.mo",
            content = """
within Modelica;
package Thermal "Library for thermal systems"
  extends Modelica.Icons.Package;
end Thermal;
            """.trimIndent(),
            size = 150
        ),
        // Thermal.HeatTransfer
        ProjectFile(
            id = "msl_thermal_heattransfer",
            name = "HeatTransfer",
            path = "Modelica/Thermal/HeatTransfer/package.mo",
            content = """
within Modelica.Thermal;
package HeatTransfer "Library for thermal heat transfer"
  extends Modelica.Icons.Package;

  package Interfaces "Connectors"
    extends Modelica.Icons.InterfacesPackage;
    connector HeatPort "Thermal port"
      Modelica.SIunits.Temperature T;
      flow Modelica.SIunits.HeatFlowRate Q_flow;
    end HeatPort;
    connector HeatPort_a = HeatPort;
    connector HeatPort_b = HeatPort;
  end Interfaces;

  package Components "Thermal components"
    extends Modelica.Icons.Package;
    model HeatCapacitor "Lumped thermal element"
      extends Interfaces.HeatPort;
      parameter Modelica.SIunits.HeatCapacity C;
      Modelica.SIunits.Temperature T;
    equation
      T = heatPort.T;
      C*der(T) = heatPort.Q_flow;
    end HeatCapacitor;
    model ThermalConductor "Thermal conductor"
      Interfaces.HeatPort_a heatPort_a;
      Interfaces.HeatPort_b heatPort_b;
      parameter Modelica.SIunits.ThermalConductance G;
    equation
      heatPort_a.Q_flow + heatPort_b.Q_flow = 0;
      heatPort_a.Q_flow = G*(heatPort_a.T - heatPort_b.T);
    end ThermalConductor;
  end Components;

  package Sources "Thermal sources"
    extends Modelica.Icons.SourcesPackage;
    model FixedTemperature "Fixed temperature"
      parameter Modelica.SIunits.Temperature T;
      Interfaces.HeatPort_b port;
    equation
      port.T = T;
    end FixedTemperature;
    model FixedHeatFlow "Fixed heat flow"
      parameter Modelica.SIunits.HeatFlowRate Q_flow;
      Interfaces.HeatPort_b port;
    equation
      port.Q_flow = -Q_flow;
    end FixedHeatFlow;
  end Sources;
end HeatTransfer;
            """.trimIndent(),
            size = 1800
        ),
        // Fluid package
        ProjectFile(
            id = "msl_fluid",
            name = "Fluid",
            path = "Modelica/Fluid/package.mo",
            content = """
within Modelica;
package Fluid "Library for fluid systems"
  extends Modelica.Icons.Package;
end Fluid;
            """.trimIndent(),
            size = 150
        ),
        // Media package
        ProjectFile(
            id = "msl_media",
            name = "Media",
            path = "Modelica/Media/package.mo",
            content = """
within Modelica;
package Media "Library for media properties"
  extends Modelica.Icons.Package;
end Media;
            """.trimIndent(),
            size = 150
        ),
        // StateGraph package
        ProjectFile(
            id = "msl_stategraph",
            name = "StateGraph",
            path = "Modelica/StateGraph/package.mo",
            content = """
within Modelica;
package StateGraph "Library for state graphs"
  extends Modelica.Icons.Package;
end StateGraph;
            """.trimIndent(),
            size = 150
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