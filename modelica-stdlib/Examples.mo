within Modelica;
package Blocks "Library of basic input/output control blocks"
  extends Modelica.Icons.Package;

  package Interfaces "Connector and block definitions"
    extends Modelica.Icons.Package;

    connector RealInput = input Real;
    connector RealOutput = output Real;
    connector BooleanInput = input Boolean;
    connector BooleanOutput = output Boolean;
    connector IntegerInput = input Integer;
    connector IntegerOutput = output Integer;

    partial block SO "Single Output continuous control block"
      extends Modelica.Icons.Block;
      RealOutput y;
    end SO;

    partial block SI "Single Input continuous control block"
      extends Modelica.Icons.Block;
      RealInput u;
    end SI;

    partial block SISO "Single Input Single Output continuous control block"
      extends Modelica.Icons.Block;
      RealInput u;
      RealOutput y;
    end SISO;

    partial block MIMO "Multiple Input Multiple Output continuous control block"
      extends Modelica.Icons.Block;
      parameter Integer nin=1 "Number of inputs";
      parameter Integer nout=1 "Number of outputs";
      RealInput u[nin];
      RealOutput y[nout];
    end MIMO;

  end Interfaces;

  package Math "Mathematical blocks"
    extends Modelica.Icons.Package;

    block Gain "Output the product of a gain value with the input signal"
      extends Interfaces.SISO;
      parameter Real k(start=1, unit="1") "Gain value multiplied with input signal";
    equation
      y = k * u;
    end Gain;

    block Add "Output the sum of the two inputs"
      extends Interfaces.SI2SO;
      parameter Real k1=+1 "Gain of upper input";
      parameter Real k2=+1 "Gain of lower input";
    equation
      y = k1*u1 + k2*u2;
    end Add;

    block Feedback "Output difference between commanded and feedback input"
      extends Interfaces.SI2SO;
    equation
      y = u1 - u2;
    end Feedback;

    block Product "Output product of the two inputs"
      extends Interfaces.SI2SO;
    equation
      y = u1 * u2;
    end Product;

    block Division "Output first input divided by second input"
      extends Interfaces.SI2SO;
    equation
      y = u1 / u2;
    end Division;

    block Abs "Output the absolute value of the input"
      extends Interfaces.SISO;
    equation
      y = abs(u);
    end Abs;

    block Sign "Output the sign of the input"
      extends Interfaces.SISO;
    equation
      y = sign(u);
    end Sign;

    block Sqrt "Output the square root of the input"
      extends Interfaces.SISO;
    equation
      y = sqrt(u);
    end Sqrt;

    block Sin "Output the sine of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.sin(u);
    end Sin;

    block Cos "Output the cosine of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.cos(u);
    end Cos;

    block Tan "Output the tangent of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.tan(u);
    end Tan;

    block Asin "Output the arc sine of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.asin(u);
    end Asin;

    block Acos "Output the arc cosine of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.acos(u);
    end Acos;

    block Atan "Output the arc tangent of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.atan(u);
    end Atan;

    block Exp "Output the exponential of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.exp(u);
    end Exp;

    block Log "Output the natural logarithm of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.log(u);
    end Log;

    block Log10 "Output the base 10 logarithm of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.log10(u);
    end Log10;

    partial block SI2SO "2 Single Input / 1 Single Output block"
      extends Modelica.Icons.Block;
      RealInput u1;
      RealInput u2;
      RealOutput y;
    end SI2SO;

  end Math;

  block Constant "Generate constant signal"
    extends Interfaces.SO;
    parameter Real k(start=1) "Constant output value";
  equation
    y = k;
  end Constant;

  block Step "Generate step signal"
    extends Interfaces.SO;
    parameter Real height=1 "Height of step";
    parameter Real offset=0 "Offset of output signal";
    parameter Real startTime=0 "Output = offset for time < startTime";
  equation
    y = offset + (if time < startTime then 0 else height);
  end Step;

  block Ramp "Generate ramp signal"
    extends Interfaces.SO;
    parameter Real height=1 "Height of ramp";
    parameter Real duration(min=0.0, start=1) "Duration of ramp";
    parameter Real offset=0 "Offset of output signal";
    parameter Real startTime=0 "Output = offset for time < startTime";
  equation
    y = offset + (if time < startTime then 0 else
       if time < (startTime + duration) then
       (time - startTime)*height/duration else height);
  end Ramp;

  block Sine "Generate sine signal"
    extends Interfaces.SO;
    parameter Real amplitude=1 "Amplitude of sine wave";
    parameter Real freqHz=1 "Frequency of sine wave";
    parameter Real phase=0 "Phase of sine wave";
    parameter Real offset=0 "Offset of output signal";
    parameter Real startTime=0 "Output = offset for time < startTime";
  equation
    y = offset + (if time < startTime then 0 else
       amplitude * Modelica.Math.sin(2*Modelica.Constants.pi*freqHz*(time - startTime) + phase));
  end Sine;

  block Pulse "Generate pulse signal"
    extends Interfaces.SO;
    parameter Real amplitude=1 "Amplitude of pulse";
    parameter Real width=50 "Width of pulse in % of period";
    parameter Real period(final min=0, start=1) "Time for one period";
    parameter Real offset=0 "Offset of output signal";
    parameter Real startTime=0 "Output = offset for time < startTime";
  equation
    y = offset + (if time < startTime then 0 else
       if (time - startTime) - integer((time - startTime)/period)*period < width*period/100 then
       amplitude else 0);
  end Pulse;

  annotation (
    Documentation(info="<html>
<p>
This library contains basic input/output control blocks.
</p>
</html>"));
end Blocks;