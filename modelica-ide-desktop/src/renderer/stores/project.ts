import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'

// 树节点接口
interface TreeNode {
  id: string
  name: string
  type: 'package' | 'class' | 'model' | 'function' | 'record' | 'block' | 'connector' | 'type' | 'file'
  path: string
  content?: string
  children: TreeNode[]
  isExpanded: boolean
  isLibrary: boolean
  isNested?: boolean  // 是否是嵌套定义
}

interface OutputMessage {
  type: 'info' | 'success' | 'warning' | 'error'
  text: string
  timestamp: string
}

interface CompileError {
  message: string
  location?: {
    line: number
    column: number
  }
}

// 构建树形结构 - 优化版本：包节点直接显示内容
function buildTree(files: { path: string; name: string; content: string; isLibrary: boolean }[]): TreeNode[] {
  const root: TreeNode = {
    id: 'root',
    name: 'root',
    type: 'package',
    path: '',
    children: [],
    isExpanded: true,
    isLibrary: false
  }

  // 先按路径分组，识别package.mo文件
  const packageMap = new Map<string, { content: string; isLibrary: boolean }>()
  const otherFiles: typeof files = []

  files.forEach(file => {
    // 检查是否是package.mo文件
    if (file.path.endsWith('/package.mo')) {
      const packagePath = file.path.replace('/package.mo', '')
      packageMap.set(packagePath, { content: file.content, isLibrary: file.isLibrary })
    } else {
      otherFiles.push(file)
    }
  })

  // 构建树结构
  const processFile = (file: typeof files[0]) => {
    const parts = file.path.split('/')
    let current = root

    parts.forEach((part, index) => {
      const isLast = index === parts.length - 1
      const currentPath = parts.slice(0, index + 1).join('/')
      const existingChild = current.children.find(c => c.name === part)

      if (existingChild) {
        current = existingChild
      } else {
        // 检查当前路径是否是包（有package.mo）
        const packageInfo = packageMap.get(currentPath)

        const newNode: TreeNode = {
          id: currentPath,
          name: part,
          type: isLast ? 'class' : 'package',
          path: currentPath,
          // 如果是包，直接赋予package.mo的内容
          content: packageInfo ? packageInfo.content : (isLast ? file.content : undefined),
          children: [],
          isExpanded: index === 0, // 默认展开第一层
          isLibrary: packageInfo ? packageInfo.isLibrary : file.isLibrary
        }
        current.children.push(newNode)
        current = newNode
      }
    })
  }

  // 处理所有文件
  otherFiles.forEach(processFile)

  // 处理只有package.mo的包（没有其他文件）
  packageMap.forEach((info, path) => {
    const parts = path.split('/')
    let current = root

    parts.forEach((part, index) => {
      const currentPath = parts.slice(0, index + 1).join('/')
      const existingChild = current.children.find(c => c.name === part)

      if (existingChild) {
        current = existingChild
      } else {
        const newNode: TreeNode = {
          id: currentPath,
          name: part,
          type: 'package',
          path: currentPath,
          content: index === parts.length - 1 ? info.content : undefined,
          children: [],
          isExpanded: index === 0,
          isLibrary: info.isLibrary
        }
        current.children.push(newNode)
        current = newNode
      }
    })
  })

  return root.children
}

// Modelica Standard Library 文件
const MSL_FILES = [
  {
    path: 'Modelica/package.mo',
    name: 'Modelica',
    content: `within ;
package Modelica "Modelica Standard Library - Version 4.0.0"
  extends Modelica.Icons.Package;

  annotation (
    version = "4.0.0",
    versionDate = "2020-06-25",
    Documentation(info = "<html>
<p>Package <strong>Modelica</strong> is a <strong>standardized</strong> and <strong>free</strong> library
that is developed together with the Modelica language from the
Modelica Association.
</p>
</html>"));
end Modelica;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Icons.mo',
    name: 'Icons',
    content: `within Modelica;
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

  partial package VariantsPackage "Icon for variants packages"
    extends Package;
  end VariantsPackage;

  partial package ExamplesPackage "Icon for examples packages"
    extends Package;
  end ExamplesPackage;

  partial model RotationalSensor "Icon for rotational sensors"
    annotation(Icon(graphics={Ellipse(extent={{-70,70},{70,-70}})}));
  end RotationalSensor;

  partial model TranslationalSensor "Icon for translational sensors"
    annotation(Icon(graphics={Ellipse(extent={{-70,70},{70,-70}})}));
  end TranslationalSensor;

  partial record Record "Icon for records"
    annotation(Icon(graphics={Rectangle(extent={{-100,50},{100,-50}})}));
  end Record;

end Icons;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Constants.mo',
    name: 'Constants',
    content: `within Modelica;
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
  final constant Real epsilon0 = 8.8541878128e-12 "Vacuum permittivity";
  final constant Real mu0 = 1.25663706212e-6 "Vacuum permeability";
  final constant Real small = 1e-60 "Smallest number";

  annotation (Documentation(info="<html><p>Mathematical and physical constants.</p></html>"));
end Constants;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Math/package.mo',
    name: 'Math',
    content: `within Modelica;
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
end Math;`,
    isLibrary: true
  },
  {
    path: 'Modelica/SIunits.mo',
    name: 'SIunits',
    content: `within Modelica;
package SIunits "Type definitions based on SI units"
  extends Modelica.Icons.Package;

  // Base units
  type Length = Real(final quantity="Length", final unit="m");
  type Mass = Real(final quantity="Mass", final unit="kg");
  type Time = Real(final quantity="Time", final unit="s");
  type Current = Real(final quantity="Current", final unit="A");
  type Temperature = Real(final quantity="Temperature", final unit="K");
  type AmountOfSubstance = Real(final quantity="AmountOfSubstance", final unit="mol");
  type LuminousIntensity = Real(final quantity="LuminousIntensity", final unit="cd");

  // Derived units
  type Angle = Real(final quantity="Angle", final unit="rad");
  type SolidAngle = Real(final quantity="SolidAngle", final unit="sr");
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
  type Current = Real(final quantity="Current", final unit="A");
  type Resistance = Real(final quantity="Resistance", final unit="Ohm");
  type Conductance = Real(final quantity="Conductance", final unit="S");
  type Capacitance = Real(final quantity="Capacitance", final unit="F");
  type Inductance = Real(final quantity="Inductance", final unit="H");

  // Thermal
  type HeatFlowRate = Real(final quantity="HeatFlowRate", final unit="W");
  type HeatCapacity = Real(final quantity="HeatCapacity", final unit="J/K");
  type ThermalConductance = Real(final quantity="ThermalConductance", final unit="W/K");
  type ThermalResistance = Real(final quantity="ThermalResistance", final unit="K/W");
  type CoefficientOfHeatTransfer = Real(final quantity="CoefficientOfHeatTransfer", final unit="W/(m2.K)");

  // Mechanical
  type Torque = Real(final quantity="Torque", final unit="N.m");
  type Inertia = Real(final quantity="Inertia", final unit="kg.m2");
  type Position = Length;
  type RotationalSpringConstant = Real(final quantity="RotationalSpringConstant", final unit="N.m/rad");
  type RotationalDampingConstant = Real(final quantity="RotationalDampingConstant", final unit="N.m.s/rad");

  annotation (Documentation(info="<html><p>SI unit type definitions.</p></html>"));
end SIunits;`,
    isLibrary: true
  },
  // Blocks package
  {
    path: 'Modelica/Blocks/package.mo',
    name: 'Blocks',
    content: `within Modelica;
package Blocks "Library of basic input/output control blocks"
  extends Modelica.Icons.Package;

  package Interfaces "Connectors and partial models"
    extends Modelica.Icons.InterfacesPackage;
    connector RealInput = input Real;
    connector RealOutput = output Real;
    connector BooleanInput = input Boolean;
    connector BooleanOutput = output Boolean;
    connector IntegerInput = input Integer;
    connector IntegerOutput = output Integer;
    partial block SO "Single Output"
      extends Modelica.Icons.Block;
      RealOutput y;
    end SO;
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
    block Feedback "Output = u1 - u2"
      extends Modelica.Icons.Block;
      Interfaces.RealInput u1;
      Interfaces.RealInput u2;
      Interfaces.RealOutput y;
    equation
      y = u1 - u2;
    end Feedback;
  end Math;

  package Sources "Signal source blocks"
    extends Modelica.Icons.SourcesPackage;
    block Constant "Generate constant signal"
      extends Interfaces.SO;
      parameter Real k(start=1);
    equation
      y = k;
    end Constant;
    block Step "Generate step signal"
      extends Interfaces.SO;
      parameter Real height=1;
      parameter Real offset=0;
      parameter Real startTime=0;
    equation
      y = offset + (if time < startTime then 0 else height);
    end Step;
    block Sine "Generate sine signal"
      extends Interfaces.SO;
      parameter Real amplitude=1;
      parameter Real freqHz=1;
      parameter Real phase=0;
      parameter Real offset=0;
      parameter Real startTime=0;
    equation
      y = offset + (if time < startTime then 0 else
         amplitude * Modelica.Math.sin(2*Modelica.Constants.pi*freqHz*(time - startTime) + phase));
    end Sine;
    block Ramp "Generate ramp signal"
      extends Interfaces.SO;
      parameter Real height=1;
      parameter Real duration=1;
      parameter Real offset=0;
      parameter Real startTime=0;
    equation
      y = offset + (if time < startTime then 0 else
         if time < startTime + duration then (time - startTime)*height/duration else height);
    end Ramp;
  end Sources;
end Blocks;`,
    isLibrary: true
  },
  // Electrical package
  {
    path: 'Modelica/Electrical/package.mo',
    name: 'Electrical',
    content: `within Modelica;
package Electrical "Library for electrical models"
  extends Modelica.Icons.Package;
end Electrical;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Electrical/Analog/package.mo',
    name: 'Analog',
    content: `within Modelica.Electrical;
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
    model ConstantCurrent "Constant current source"
      Interfaces.PositivePin p;
      Interfaces.NegativePin n;
      parameter Modelica.SIunits.Current I(start=1);
    equation
      p.i = -I;
      n.i = I;
      p.v = n.v;
    end ConstantCurrent;
  end Sources;
end Analog;`,
    isLibrary: true
  },
  // Mechanics package
  {
    path: 'Modelica/Mechanics/package.mo',
    name: 'Mechanics',
    content: `within Modelica;
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
end Mechanics;`,
    isLibrary: true
  },
  // Thermal package
  {
    path: 'Modelica/Thermal/package.mo',
    name: 'Thermal',
    content: `within Modelica;
package Thermal "Library for thermal systems"
  extends Modelica.Icons.Package;
end Thermal;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Thermal/HeatTransfer/package.mo',
    name: 'HeatTransfer',
    content: `within Modelica.Thermal;
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
end HeatTransfer;`,
    isLibrary: true
  },
  // Fluid package
  {
    path: 'Modelica/Fluid/package.mo',
    name: 'Fluid',
    content: `within Modelica;
package Fluid "Library for fluid systems"
  extends Modelica.Icons.Package;
end Fluid;`,
    isLibrary: true
  },
  // Media package
  {
    path: 'Modelica/Media/package.mo',
    name: 'Media',
    content: `within Modelica;
package Media "Library for media properties"
  extends Modelica.Icons.Package;
end Media;`,
    isLibrary: true
  },
  // StateGraph package
  {
    path: 'Modelica/StateGraph/package.mo',
    name: 'StateGraph',
    content: `within Modelica;
package StateGraph "Library for state graphs"
  extends Modelica.Icons.Package;
end StateGraph;`,
    isLibrary: true
  },
  // Examples
  {
    path: 'Examples/package.mo',
    name: 'Examples',
    content: `within ;
package Examples "Example models"
  extends Modelica.Icons.Package;
  annotation (Documentation(info="<html><p>Example models demonstrating Modelica.</p></html>"));
end Examples;`,
    isLibrary: true
  },
  {
    path: 'Examples/HelloWorld.mo',
    name: 'HelloWorld',
    content: `within Examples;
model HelloWorld "The simplest Modelica model"
  extends Modelica.Icons.Example;

  Real x(start=1) "A state variable";

equation
  der(x) = -x;

  annotation (
    Documentation(info="<html>
<p>This is the simplest possible Modelica model with a single
differential equation: dx/dt = -x</p>
</html>"),
    experiment(StopTime=5, Interval=0.01));
end HelloWorld;`,
    isLibrary: true
  },
  {
    path: 'Examples/SimplePendulum.mo',
    name: 'SimplePendulum',
    content: `within Examples;
model SimplePendulum "A simple pendulum model"
  extends Modelica.Icons.Example;

  parameter Real L = 1.0 "Pendulum length (m)";
  parameter Real g = 9.81 "Gravity (m/s2)";
  parameter Real theta0 = 0.1 "Initial angle (rad)";

  Real theta(start=theta0) "Pendulum angle";
  Real omega(start=0) "Angular velocity";

equation
  der(theta) = omega;
  der(omega) = -(g/L) * sin(theta);

  annotation (
    Documentation(info="<html><p>Simple pendulum dynamics.</p></html>"),
    experiment(StopTime=10, Interval=0.01));
end SimplePendulum;`,
    isLibrary: true
  },
  {
    path: 'Examples/BouncingBall.mo',
    name: 'BouncingBall',
    content: `within Examples;
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
end BouncingBall;`,
    isLibrary: true
  },
  {
    path: 'Examples/DCMotor.mo',
    name: 'DCMotor',
    content: `within Examples;
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
end DCMotor;`,
    isLibrary: true
  },
  {
    path: 'Examples/LorenzSystem.mo',
    name: 'LorenzSystem',
    content: `within Examples;
model LorenzSystem "Lorenz attractor"
  extends Modelica.Icons.Example;

  parameter Real sigma = 10.0;
  parameter Real rho = 28.0;
  parameter Real beta = 8.0/3.0;

  Real x(start=1.0);
  Real y(start=1.0);
  Real z(start=1.0);

equation
  der(x) = sigma * (y - x);
  der(y) = x * (rho - z) - y;
  der(z) = x * y - beta * z;

  annotation (experiment(StopTime=30, Interval=0.01));
end LorenzSystem;`,
    isLibrary: true
  }
]

export const useProjectStore = defineStore('project', () => {
  // State
  const treeNodes = ref<TreeNode[]>([])
  const openFiles = ref<TreeNode[]>([])
  const currentFileIndex = ref<number>(-1)
  const outputMessages = ref<OutputMessage[]>([])
  const errors = ref<CompileError[]>([])
  const libraryLoaded = ref<boolean>(false)
  const expandedNodes = ref<Set<string>>(new Set())

  // Computed
  const currentFile = computed(() =>
    currentFileIndex.value >= 0 ? openFiles.value[currentFileIndex.value] : null
  )

  // 初始化 - 加载MSL
  async function loadStandardLibrary() {
    if (libraryLoaded.value) return

    try {
      // 加载新的 Modelica 层次结构数据
      const libraryData = await import('../data/library-data-v2.json')

      // 转换为 TreeNode 格式
      function convertToTreeNode(data: any): TreeNode {
        return {
          id: data.path,
          name: data.name,
          type: data.type || 'package',
          path: data.path,
          content: data.content,
          children: data.children ? data.children.map(convertToTreeNode) : [],
          isExpanded: false,
          isLibrary: data.isLibrary !== false,
          isNested: data.isNested
        }
      }

      treeNodes.value = [convertToTreeNode(libraryData.default || libraryData)]

      // 默认展开 Modelica
      expandedNodes.value.add('Modelica')

      libraryLoaded.value = true

      console.log('Modelica Standard Library loaded successfully')
    } catch (error) {
      console.error('Failed to load Modelica Standard Library:', error)
      // 回退到旧的内联数据
      treeNodes.value = buildTree(MSL_FILES)
      libraryLoaded.value = true
    }
  }

  // 查找节点
  function findNodeByPath(nodes: TreeNode[], path: string): TreeNode | null {
    for (const node of nodes) {
      if (node.path === path) return node
      if (node.children.length > 0) {
        const found = findNodeByPath(node.children, path)
        if (found) return found
      }
    }
    return null
  }

  // 切换节点展开状态
  function toggleNode(node: TreeNode) {
    if (node.children.length === 0) return

    if (expandedNodes.value.has(node.path)) {
      expandedNodes.value.delete(node.path)
    } else {
      expandedNodes.value.add(node.path)
    }
  }

  // 检查节点是否展开
  function isNodeExpanded(node: TreeNode): boolean {
    return expandedNodes.value.has(node.path)
  }

  // 打开文件
  function openFile(node: TreeNode) {
    if (!node.content) return // 不是文件节点

    const index = openFiles.value.findIndex(f => f.path === node.path)
    if (index >= 0) {
      currentFileIndex.value = index
    } else {
      openFiles.value.push(node)
      currentFileIndex.value = openFiles.value.length - 1
    }
  }

  // 关闭文件
  function closeFile(node: TreeNode) {
    const index = openFiles.value.findIndex(f => f.path === node.path)
    if (index >= 0) {
      openFiles.value.splice(index, 1)
      if (currentFileIndex.value >= openFiles.value.length) {
        currentFileIndex.value = openFiles.value.length - 1
      }
    }
  }

  // 设置当前文件
  function setCurrentFile(node: TreeNode) {
    const index = openFiles.value.findIndex(f => f.path === node.path)
    if (index >= 0) {
      currentFileIndex.value = index
    }
  }

  // 更新文件内容
  function updateFileContent(path: string, content: string) {
    const file = openFiles.value.find(f => f.path === path)
    if (file) {
      file.content = content
    }
  }

  // 创建新文件
  function createFile() {
    const newFile: TreeNode = {
      id: `untitled-${Date.now()}`,
      name: `untitled-${openFiles.value.length + 1}.mo`,
      type: 'class',
      path: `untitled-${Date.now()}.mo`,
      content: `model NewModel
  // Add your model here
  Real x(start=1);
equation
  der(x) = -x;  // Example equation
end NewModel;
`,
      children: [],
      isExpanded: false,
      isLibrary: false
    }
    treeNodes.value.push(newFile)
    openFile(newFile)
  }

  // 添加输出消息
  function addOutput(text: string, type: OutputMessage['type'] = 'info') {
    const timestamp = new Date().toLocaleTimeString()
    outputMessages.value.push({ type, text, timestamp })
  }

  // 添加错误
  function addError(message: string, location?: { line: number; column: number }) {
    errors.value.push({ message, location })
  }

  // 清除输出
  function clearOutput() {
    outputMessages.value = []
  }

  // 清除错误
  function clearErrors() {
    errors.value = []
  }

  return {
    treeNodes,
    openFiles,
    currentFile,
    outputMessages,
    errors,
    libraryLoaded,
    expandedNodes,
    loadStandardLibrary,
    toggleNode,
    isNodeExpanded,
    openFile,
    closeFile,
    setCurrentFile,
    updateFileContent,
    createFile,
    addOutput,
    addError,
    clearOutput,
    clearErrors
  }
})