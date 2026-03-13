model TestComplex
  parameter Real k(unit="1")=1 "Integrator gain";
  parameter Boolean use_reset = false "= true, if reset port enabled"
    annotation(Evaluate=true, HideResult=true);
  parameter Real y_start=0 "Initial value"
    annotation(Dialog(group="Initialization"));
end TestComplex;
