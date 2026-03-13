model TestConditional
  parameter Boolean use_reset = false;
  Real x;
  Real reset if use_reset;
equation
  der(x) = -x;
end TestConditional;
