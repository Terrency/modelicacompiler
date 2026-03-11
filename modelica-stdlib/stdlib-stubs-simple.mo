// Modelica 标准库简化存根
// 只包含编译器当前支持的语法

model Modelica_Icons_Example
  "Icon for an example model"
end Modelica_Icons_Example;

model Modelica_Icons_ExamplesPackage
  "Icon for packages containing executable examples"
end Modelica_Icons_ExamplesPackage;

model Modelica_Blocks_Interfaces_RealInput
  Real u;
end Modelica_Blocks_Interfaces_RealInput;

model Modelica_Blocks_Interfaces_RealOutput
  Real y;
end Modelica_Blocks_Interfaces_RealOutput;

model Modelica_Blocks_Interfaces_BooleanInput
  Boolean u;
end Modelica_Blocks_Interfaces_BooleanInput;

model Modelica_Blocks_Interfaces_BooleanOutput
  Boolean y;
end Modelica_Blocks_Interfaces_BooleanOutput;

model Modelica_Blocks_Interfaces_IntegerInput
  Integer u;
end Modelica_Blocks_Interfaces_IntegerInput;

model Modelica_Blocks_Interfaces_IntegerOutput
  Integer y;
end Modelica_Blocks_Interfaces_IntegerOutput;

model Modelica_Blocks_Interfaces_SO
  Real y;
end Modelica_Blocks_Interfaces_SO;

model Modelica_Blocks_Interfaces_SISO
  Real u;
  Real y;
end Modelica_Blocks_Interfaces_SISO;

model Modelica_Blocks_Interfaces_SI2SO
  Real u1;
  Real u2;
  Real y;
end Modelica_Blocks_Interfaces_SI2SO;

model Modelica_Blocks_Interfaces_SignalSource
  Real y;
end Modelica_Blocks_Interfaces_SignalSource;

model Modelica_Blocks_Math_Gain
  parameter Real k = 1;
  Real u;
  Real y;
equation
  y = k * u;
end Modelica_Blocks_Math_Gain;

model Modelica_Blocks_Math_Add
  parameter Real k1 = 1;
  parameter Real k2 = 1;
  Real u1;
  Real u2;
  Real y;
equation
  y = k1 * u1 + k2 * u2;
end Modelica_Blocks_Math_Add;

model Modelica_Blocks_Math_Product
  Real u1;
  Real u2;
  Real y;
equation
  y = u1 * u2;
end Modelica_Blocks_Math_Product;

model Modelica_Blocks_Math_Feedback
  Real u1;
  Real u2;
  Real y;
equation
  y = u1 - u2;
end Modelica_Blocks_Math_Feedback;

model Modelica_Blocks_Continuous_Integrator
  parameter Real k = 1;
  Real u;
  Real y;
equation
  der(y) = k * u;
end Modelica_Blocks_Continuous_Integrator;

model Modelica_Blocks_Continuous_FirstOrder
  parameter Real k = 1;
  parameter Real T = 1;
  Real u;
  Real y;
equation
  T * der(y) + y = k * u;
end Modelica_Blocks_Continuous_FirstOrder;

model Modelica_Blocks_Sources_Constant
  parameter Real k = 1;
  Real y;
equation
  y = k;
end Modelica_Blocks_Sources_Constant;

model SimpleTest
  Real x;
equation
  der(x) = -x;
end SimpleTest;