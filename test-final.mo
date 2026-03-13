model TestFinal
  final parameter Real x = 1.0;
  parameter Real y = 2.0;
equation
  der(x) = -y;
end TestFinal;
