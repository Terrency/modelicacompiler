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
    input Real u "Angle in rad";
    output Real y;
  external "builtin" y = sin(u);
  end sin;

  function cos "Cosine function"
    extends Modelica.Icons.Function;
    input Real u "Angle in rad";
    output Real y;
  external "builtin" y = cos(u);
  end cos;

  function tan "Tangent function"
    extends Modelica.Icons.Function;
    input Real u "Angle in rad";
    output Real y;
  external "builtin" y = tan(u);
  end tan;

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

  function sqrt "Square root"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = sqrt(u);
  end sqrt;

  function abs "Absolute value"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = abs(u);
  end abs;

  function sign "Sign function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  algorithm
    y := if u > 0 then 1 else if u < 0 then -1 else 0;
  end sign;

  function floor "Floor function"
    extends Modelica.Icons.Function;
    input Real u;
    output Integer y;
  external "builtin" y = floor(u);
  end floor;

  function ceil "Ceiling function"
    extends Modelica.Icons.Function;
    input Real u;
    output Integer y;
  external "builtin" y = ceil(u);
  end ceil;

  function integer "Convert Real to Integer"
    extends Modelica.Icons.Function;
    input Real u;
    output Integer y;
  external "builtin" y = integer(u);
  end integer;

  annotation (
    Documentation(info="<html>
<p>This package contains standard mathematical functions.</p>
</html>"));
end Math;