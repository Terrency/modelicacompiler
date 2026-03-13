// 测试block定义
block Gain
  parameter Real k = 1;
  Real u;
  Real y;
equation
  y = k * u;
end Gain;

block Integrator
  Real u;
  Real y;
  parameter Real k = 1;
equation
  der(y) = k * u;
end Integrator;