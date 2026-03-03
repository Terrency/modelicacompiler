within Modelica;
package Blocks "Library of basic input/output control blocks"
  extends Modelica.Icons.Package;

  annotation (
    Documentation(info="<html>
<p>
This library contains basic input/output control blocks.
</p>
</html>"));

  package Interfaces "Connectors and partial models"
    extends Modelica.Icons.InterfacesPackage;

    connector RealInput = input Real "'input Real' as connector";
    connector RealOutput = output Real "'output Real' as connector";
    connector BooleanInput = input Boolean "'input Boolean' as connector";
    connector BooleanOutput = output Boolean "'output Boolean' as connector";
    connector IntegerInput = input Integer "'input Integer' as connector";
    connector IntegerOutput = output Integer "'output Integer' as connector";

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

    partial block SI2SO "Two Single Input / Single Output block"
      extends Modelica.Icons.Block;
      RealInput u1;
      RealInput u2;
      RealOutput y;
    end SI2SO;

    partial block MISO "Multiple Input Single Output block"
      extends Modelica.Icons.Block;
      parameter Integer nin=1 "Number of inputs";
      RealInput u[nin];
      RealOutput y;
    end MISO;

    partial block SignalSource "Base class for signal sources"
      extends Modelica.Icons.SourcesPackage;
      parameter Real offset=0 "Offset of output signal";
      parameter Real startTime=0 "Output = offset for time < startTime";
    end SignalSource;

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

    block Add3 "Output the sum of the three inputs"
      extends Modelica.Icons.Block;
      parameter Real k1=+1 "Gain of input 1";
      parameter Real k2=+1 "Gain of input 2";
      parameter Real k3=+1 "Gain of input 3";
      Interfaces.RealInput u1;
      Interfaces.RealInput u2;
      Interfaces.RealInput u3;
      Interfaces.RealOutput y;
    equation
      y = k1*u1 + k2*u2 + k3*u3;
    end Add3;

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

    block Atan2 "Output atan(u1/u2)"
      extends Interfaces.SI2SO;
    equation
      y = Modelica.Math.atan2(u1, u2);
    end Atan2;

    block Sinh "Output the hyperbolic sine of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.sinh(u);
    end Sinh;

    block Cosh "Output the hyperbolic cosine of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.cosh(u);
    end Cosh;

    block Tanh "Output the hyperbolic tangent of the input"
      extends Interfaces.SISO;
    equation
      y = Modelica.Math.tanh(u);
    end Tanh;

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

    block Power "Output the power of the input"
      extends Interfaces.SISO;
      parameter Real exponent=2 "Exponent of the power function";
    equation
      y = u^exponent;
    end Power;

    block Sum "Output the sum of the elements of the input vector"
      extends Interfaces.MISO;
    equation
      y = sum(u);
    end Sum;

    block Feedback "Output difference between commanded and feedback input"
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
      parameter Real k(start=1) "Constant output value";
    equation
      y = k;
    end Constant;

    block Step "Generate step signal"
      extends Interfaces.SignalSource;
      parameter Real height=1 "Height of step";
    equation
      y = offset + (if time < startTime then 0 else height);
    end Step;

    block Ramp "Generate ramp signal"
      extends Interfaces.SignalSource;
      parameter Real height=1 "Height of ramp";
      parameter Real duration(min=Modelica.Constants.small, start=1) "Duration of ramp";
    equation
      y = offset + (if time < startTime then 0 else
         if time < (startTime + duration) then
         (time - startTime)*height/duration else height);
    end Ramp;

    block Sine "Generate sine signal"
      extends Interfaces.SignalSource;
      parameter Real amplitude=1 "Amplitude of sine wave";
      parameter Real freqHz=1 "Frequency of sine wave";
      parameter Real phase=0 "Phase of sine wave";
    equation
      y = offset + (if time < startTime then 0 else
         amplitude * Modelica.Math.sin(2*Modelica.Constants.pi*freqHz*(time - startTime) + phase));
    end Sine;

    block Cosine "Generate cosine signal"
      extends Interfaces.SignalSource;
      parameter Real amplitude=1 "Amplitude of cosine wave";
      parameter Real freqHz=1 "Frequency of cosine wave";
      parameter Real phase=0 "Phase of cosine wave";
    equation
      y = offset + (if time < startTime then 0 else
         amplitude * Modelica.Math.cos(2*Modelica.Constants.pi*freqHz*(time - startTime) + phase));
    end Cosine;

    block ExpSine "Generate exponentially decaying sine signal"
      extends Interfaces.SignalSource;
      parameter Real amplitude=1 "Amplitude of sine wave";
      parameter Real freqHz=1 "Frequency of sine wave";
      parameter Real phase=0 "Phase of sine wave";
      parameter Real damping=1 "Damping coefficient of sine wave";
    equation
      y = offset + (if time < startTime then 0 else
         amplitude * Modelica.Math.exp(-(time - startTime)/damping) *
         Modelica.Math.sin(2*Modelica.Constants.pi*freqHz*(time - startTime) + phase));
    end ExpSine;

    block Pulse "Generate pulse signal"
      extends Interfaces.SignalSource;
      parameter Real amplitude=1 "Amplitude of pulse";
      parameter Real width=50 "Width of pulse in % of period";
      parameter Real period(final min=Modelica.Constants.small, start=1) "Time for one period";
      parameter Integer nperiod=-1 "Number of periods (< 0 means infinite)";
    protected
      parameter Real t0(fixed=false);
    initial equation
      t0 = startTime;
    equation
      y = offset + (if time < startTime or (nperiod > 0 and time > t0 + nperiod*period) then 0 else
         if (time - t0) - integer((time - t0)/period)*period < width*period/100 then
         amplitude else 0);
    end Pulse;

    block SawTooth "Generate saw tooth signal"
      extends Interfaces.SignalSource;
      parameter Real amplitude=1 "Amplitude of saw tooth";
      parameter Real period=1 "Time for one period";
    equation
      y = offset + (if time < startTime then 0 else
         amplitude * (time - startTime - integer((time - startTime)/period)*period)/period);
    end SawTooth;

    block Triangular "Generate triangular signal"
      extends Interfaces.SignalSource;
      parameter Real amplitude=1 "Amplitude of triangular wave";
      parameter Real period=1 "Time for one period";
    protected
      Real t_rel;
      Real t_p;
    equation
      t_rel = time - startTime;
      t_p = t_rel - integer(t_rel/period)*period;
      y = offset + (if time < startTime then 0 else
         if t_p < period/2 then
         2*amplitude*t_p/period else
         2*amplitude*(period - t_p)/period);
    end Triangular;

    block BooleanConstant "Generate constant Boolean signal"
      extends Modelica.Icons.Block;
      parameter Boolean k=true "Constant output value";
      Interfaces.BooleanOutput y;
    equation
      y = k;
    end BooleanConstant;

    block BooleanStep "Generate Boolean step signal"
      extends Modelica.Icons.Block;
      parameter Boolean startValue=false "Output before startTime";
      parameter Real startTime=0 "Time instant of step";
      Interfaces.BooleanOutput y;
    equation
      y = if time < startTime then startValue else not startValue;
    end BooleanStep;

    block IntegerConstant "Generate constant Integer signal"
      extends Modelica.Icons.Block;
      parameter Integer k=0 "Constant output value";
      Interfaces.IntegerOutput y;
    equation
      y = k;
    end IntegerConstant;

  end Sources;

  package Nonlinear "Nonlinear blocks"
    extends Modelica.Icons.Package;

    block Limiter "Limit the range of a signal"
      extends Interfaces.SISO;
      parameter Real uMax=1 "Upper limits of input signals";
      parameter Real uMin=-uMax "Lower limits of input signals";
    equation
      y = smooth(0, if u > uMax then uMax else if u < uMin then uMin else u);
    end Limiter;

    block DeadZone "Provide a region of zero output"
      extends Interfaces.SISO;
      parameter Real uMax=1 "Upper limits of dead zones";
      parameter Real uMin=-uMax "Lower limits of dead zones";
    equation
      y = smooth(0, if u > uMax then u - uMax else if u < uMin then u - uMin else 0);
    end DeadZone;

    block Hysteresis "Transform Real to Boolean with Hysteresis"
      extends Modelica.Icons.Block;
      parameter Real uLow=0 "If y=true and u<=uLow, switch to y=false";
      parameter Real uHigh=1 "If y=false and u>=uHigh, switch to y=true";
      parameter Boolean pre_y_start=false "Value of pre(y) at initial time";
      Interfaces.RealInput u;
      Interfaces.BooleanOutput y;
    initial equation
      pre(y) = pre_y_start;
    equation
      y = u >= uHigh or pre(y) and u > uLow;
    end Hysteresis;

  end Nonlinear;

  block RealExpression "Set output signal to a time varying Real expression"
    extends Modelica.Icons.Block;
    Interfaces.RealOutput y;
    parameter Real y_start=0 "Initial value of output signal";
  equation
    y = y_start;
  end RealExpression;

  block BooleanExpression "Set output signal to a time varying Boolean expression"
    extends Modelica.Icons.Block;
    Interfaces.BooleanOutput y;
    parameter Boolean y_start=false "Initial value of output signal";
  equation
    y = y_start;
  end BooleanExpression;

end Blocks;