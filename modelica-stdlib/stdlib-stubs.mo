// Modelica 标准库存根定义
// 包含基础类型和接口的最小定义，用于支持模型编译

package Modelica

  // ==================== Icons 包 ====================
  package Icons
    model Example
      "Icon for an example model"
    end Example;

    model ExamplesPackage
      "Icon for packages containing executable examples"
    end ExamplesPackage;

    package Package
    end Package;
  end Icons;

  // ==================== SIunits 包 ====================
  package SIunits
    type Angle = Real;
    type Time = Real;
    type Frequency = Real;
    type Velocity = Real;
    type Acceleration = Real;
    type Force = Real;
    type Torque = Real;
    type Mass = Real;
    type Length = Real;
    type Area = Real;
    type Volume = Real;
    type Pressure = Real;
    type Temperature = Real;
    type Energy = Real;
    type Power = Real;
    type ElectricCurrent = Real;
    type ElectricVoltage = Real;
    type ElectricResistance = Real;
    type ElectricCapacitance = Real;
    type ElectricInductance = Real;
  end SIunits;

  // ==================== Blocks 包 ====================
  package Blocks
    // Interfaces
    package Interfaces
      connector RealInput = input Real;
      connector RealOutput = output Real;
      connector BooleanInput = input Boolean;
      connector BooleanOutput = output Boolean;
      connector IntegerInput = input Integer;
      connector IntegerOutput = output Integer;

      connector RealVectorInput = input Real[:];
      connector IntegerVectorInput = input Integer[:];
      connector BooleanVectorInput = input Boolean[:];
      connector RealVectorOutput = output Real[:];

      block SO
        "Single Output continuous control block"
        RealOutput y;
      end SO;

      block MO
        "Multiple Output continuous control block"
        RealOutput y[:];
      end MO;

      block SISO
        "Single Input Single Output continuous control block"
        RealInput u;
        RealOutput y;
      end SISO;

      block SI2SO
        "2 Single Input / 1 Single Output continuous control block"
        RealInput u1;
        RealInput u2;
        RealOutput y;
      end SI2SO;

      block SIMO
        "Single Input Multiple Output continuous control block"
        RealInput u;
        RealOutput y[:];
      end SIMO;

      block MIMO
        "Multiple Input Multiple Output continuous control block"
        RealInput u[:];
        RealOutput y[:];
      end MIMO;

      block SignalSource
        "Base class for signal sources"
        RealOutput y;
      end SignalSource;

      block DiscreteBlock
        "Base class for discrete blocks"
        parameter Real samplePeriod;
        RealOutput y;
      end DiscreteBlock;
    end Interfaces;

    // Math 包
    package Math
      block Gain
        "Output the product of a gain value with the input signal"
        parameter Real k = 1;
        Interfaces.RealInput u;
        Interfaces.RealOutput y;
      equation
        y = k * u;
      end Gain;

      block Add
        "Output the sum of the two inputs"
        parameter Real k1 = 1;
        parameter Real k2 = 1;
        Interfaces.RealInput u1;
        Interfaces.RealInput u2;
        Interfaces.RealOutput y;
      equation
        y = k1 * u1 + k2 * u2;
      end Add;

      block Sum
        "Output the sum of the elements of the input vector"
        parameter Real k[:];
        Interfaces.RealInput u[:];
        Interfaces.RealOutput y;
      equation
        y = sum(k[i] * u[i] for i in 1:size(u,1));
      end Sum;

      block Product
        "Output product of the two inputs"
        Interfaces.RealInput u1;
        Interfaces.RealInput u2;
        Interfaces.RealOutput y;
      equation
        y = u1 * u2;
      end Product;

      block Feedback
        "Output difference between commanded and feedback input"
        Interfaces.RealInput u1;
        Interfaces.RealInput u2;
        Interfaces.RealOutput y;
      equation
        y = u1 - u2;
      end Feedback;
    end Math;

    // Continuous 包
    package Continuous
      block Integrator
        "Output the integral of the input signal"
        parameter Real k = 1;
        Interfaces.RealInput u;
        Interfaces.RealOutput y;
      initial equation
        y = 0;
      equation
        der(y) = k * u;
      end Integrator;

      block Derivative
        "Approximated derivative block"
        parameter Real k = 1;
        parameter Real T = 0.01;
        Interfaces.RealInput u;
        Interfaces.RealOutput y;
        Real x;
      equation
        T * der(x) + x = u;
        y = k * der(x);
      end Derivative;

      block FirstOrder
        "First order transfer function"
        parameter Real k = 1;
        parameter Real T = 1;
        Interfaces.RealInput u;
        Interfaces.RealOutput y;
      initial equation
        y = 0;
      equation
        T * der(y) + y = k * u;
      end FirstOrder;

      block SecondOrder
        "Second order transfer function"
        parameter Real k = 1;
        parameter Real w = 1;
        parameter Real D = 0.5;
        Interfaces.RealInput u;
        Interfaces.RealOutput y;
      initial equation
        y = 0;
        der(y) = 0;
      equation
        der(y) + 2*D*w*der(y) + w^2*y = k*w^2*u;
      end SecondOrder;

      block PI
        "Proportional-Integral controller"
        parameter Real k = 1;
        parameter Real Ti = 1;
        Interfaces.RealInput u;
        Interfaces.RealOutput y;
        Real x;
      initial equation
        x = 0;
      equation
        der(x) = u/Ti;
        y = k*(u + x);
      end PI;

      block LimPID
        "PID controller with output limitation"
        parameter Real k = 1;
        parameter Real Ti = 1;
        parameter Real Td = 0.1;
        parameter Real yMax = 100;
        parameter Real yMin = -100;
        Interfaces.RealInput u_s;
        Interfaces.RealInput u_m;
        Interfaces.RealOutput y;
        Real P, I, D;
      initial equation
        I = 0;
      equation
        P = k * (u_s - u_m);
        der(I) = k/Ti * (u_s - u_m);
        D = k*Td * der(u_s - u_m);
        y = if P+I+D > yMax then yMax
           else if P+I+D < yMin then yMin
           else P+I+D;
      end LimPID;
    end Continuous;

    // Sources 包
    package Sources
      block Constant
        "Generate constant signal"
        parameter Real k = 1;
        Interfaces.RealOutput y;
      equation
        y = k;
      end Constant;

      block Step
        "Generate step signal"
        parameter Real height = 1;
        parameter Real offset = 0;
        parameter Real startTime = 0;
        Interfaces.RealOutput y;
      equation
        y = offset + (if time < startTime then 0 else height);
      end Step;

      block Ramp
        "Generate ramp signal"
        parameter Real height = 1;
        parameter Real duration = 1;
        parameter Real offset = 0;
        parameter Real startTime = 0;
        Interfaces.RealOutput y;
      equation
        y = offset + (if time < startTime then 0
                      else if time < startTime + duration then
                        (time - startTime) * height / duration
                      else height);
      end Ramp;

      block Sine
        "Generate sine signal"
        parameter Real amplitude = 1;
        parameter Real freqHz = 1;
        parameter Real phase = 0;
        parameter Real offset = 0;
        parameter Real startTime = 0;
        Interfaces.RealOutput y;
      equation
        y = offset + (if time < startTime then 0
                      else amplitude * Modelica.Math.sin(2*3.14159265*freqHz*(time-startTime) + phase));
      end Sine;
    end Sources;

    // Types 包
    package Types
      type Init = enumeration(
        NoInit,
        SteadyState,
        InitialState,
        InitialOutput
      );

      type SimpleController = enumeration(
        P,
        PI,
        PD,
        PID
      );
    end Types;
  end Blocks;

  // ==================== Math 函数 ====================
  package Math
    function sin
      input Real u;
      output Real y;
    external "builtin" y = sin(u);
    end sin;

    function cos
      input Real u;
      output Real y;
    external "builtin" y = cos(u);
    end cos;

    function exp
      input Real u;
      output Real y;
    external "builtin" y = exp(u);
    end exp;

    function log
      input Real u;
      output Real y;
    external "builtin" y = log(u);
    end log;

    function sqrt
      input Real u;
      output Real y;
    external "builtin" y = sqrt(u);
    end sqrt;
  end Math;

  // ==================== Constants ====================
  package Constants
    constant Real pi = 3.14159265358979;
    constant Real e = 2.71828182845905;
    constant Real g_n = 9.80665;
  end Constants;

end Modelica;