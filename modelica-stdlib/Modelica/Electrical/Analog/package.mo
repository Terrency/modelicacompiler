within Modelica.Electrical;
package Analog "Library for analog electrical models"
  extends Modelica.Icons.Package;

  annotation (
    Documentation(info="<html>
<p>
This library contains analog electrical components.
</p>
</html>"));

  package Interfaces "Connectors and partial models"
    extends Modelica.Icons.InterfacesPackage;

    connector Pin "Pin of an electrical component"
      Modelica.SIunits.Voltage v "Potential at the pin";
      flow Modelica.SIunits.Current i "Current flowing into the pin";
    end Pin;

    connector PositivePin "Positive pin of an electrical component"
      Modelica.SIunits.Voltage v "Potential at the pin";
      flow Modelica.SIunits.Current i "Current flowing into the pin";
    end PositivePin;

    connector NegativePin "Negative pin of an electrical component"
      Modelica.SIunits.Voltage v "Potential at the pin";
      flow Modelica.SIunits.Current i "Current flowing into the pin";
    end NegativePin;

    partial model TwoPort "Component with two electrical ports"
      extends Modelica.Icons.Block;
      PositivePin p1 "Positive electrical pin of port 1";
      NegativePin n1 "Negative electrical pin of port 1";
      PositivePin p2 "Positive electrical pin of port 2";
      NegativePin n2 "Negative electrical pin of port 2";
      Modelica.SIunits.Voltage v1 "Voltage drop of port 1";
      Modelica.SIunits.Voltage v2 "Voltage drop of port 2";
      Modelica.SIunits.Current i1 "Current flowing from pos. to neg. pin of port 1";
      Modelica.SIunits.Current i2 "Current flowing from pos. to neg. pin of port 2";
    equation
      v1 = p1.v - n1.v;
      v2 = p2.v - n2.v;
      0 = p1.i + n1.i;
      0 = p2.i + n2.i;
      i1 = p1.i;
      i2 = p2.i;
    end TwoPort;

    partial model OnePort "Component with two electrical pins"
      extends Modelica.Icons.Block;
      PositivePin p "Positive electrical pin";
      NegativePin n "Negative electrical pin";
      Modelica.SIunits.Voltage v "Voltage drop between the two pins (= p.v - n.v)";
      Modelica.SIunits.Current i "Current flowing from pin p to pin n";
    equation
      v = p.v - n.v;
      0 = p.i + n.i;
      i = p.i;
    end OnePort;

    partial model VoltageSource "Interface for voltage sources"
      extends OnePort;
      parameter Modelica.SIunits.Voltage offset=0 "Voltage offset";
      parameter Modelica.SIunits.Time startTime=0 "Time offset";
    protected
      Modelica.SIunits.Voltage vSource;
    equation
      v = offset + vSource;
    end VoltageSource;

    partial model CurrentSource "Interface for current sources"
      extends OnePort;
      parameter Modelica.SIunits.Current offset=0 "Current offset";
      parameter Modelica.SIunits.Time startTime=0 "Time offset";
    protected
      Modelica.SIunits.Current iSource;
    equation
      i = offset + iSource;
    end CurrentSource;

  end Interfaces;

  package Basic "Basic electrical components"
    extends Modelica.Icons.Package;

    model Resistor "Ideal linear electrical resistor"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Resistance R(start=1) "Resistance";
    equation
      v = R*i;
    end Resistor;

    model Conductor "Ideal linear electrical conductor"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Conductance G(start=1) "Conductance";
    equation
      i = G*v;
    end Conductor;

    model Capacitor "Ideal linear electrical capacitor"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Capacitance C(start=1) "Capacitance";
    equation
      i = C*der(v);
    end Capacitor;

    model Inductor "Ideal linear electrical inductor"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Inductance L(start=1) "Inductance";
    equation
      v = L*der(i);
    end Inductor;

    model SaturatingInductor "Inductor with saturation"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Inductance L(start=1) "Inductance at zero current";
      parameter Modelica.SIunits.Current Inom(start=1) "Nominal current";
      parameter Real N=1 "Exponent of saturation curve";
    equation
      v = L*der(i)/(1 + abs(i/Inom)^N);
    end SaturatingInductor;

    model ResistorHeating "Resistor with heating port"
      extends Interfaces.OnePort;
      parameter Modelica.SIunits.Resistance R_ref(start=1) "Resistance at reference temperature";
      parameter Modelica.SIunits.Temperature T_ref=300.15 "Reference temperature";
      parameter Real alpha=0 "Temperature coefficient of resistance";
      Modelica.Thermal.HeatTransfer.Interfaces.HeatPort_a heatPort;
      Modelica.SIunits.Resistance R "Actual resistance";
      Modelica.SIunits.Power LossPower "Loss power";
    equation
      R = R_ref*(1 + alpha*(heatPort.T - T_ref));
      v = R*i;
      LossPower = v*i;
      heatPort.Q_flow = -LossPower;
    end ResistorHeating;

    model EMF "Electromotoric force (electric energy to mechanical energy)"
      extends Modelica.Icons.RotationalSensor;
      parameter Real k(start=1) "Transformation coefficient";
      Interfaces.PositivePin p;
      Interfaces.NegativePin n;
      Modelica.Mechanics.Rotational.Interfaces.Flange_b flange;
      Modelica.SIunits.Voltage v "Voltage drop between the two pins";
      Modelica.SIunits.Current i "Current flowing from positive to negative pin";
      Modelica.SIunits.AngularVelocity w "Angular velocity of flange";
    equation
      v = p.v - n.v;
      0 = p.i + n.i;
      i = p.i;
      w = der(flange.phi);
      v = k*w;
      flange.tau = -k*i;
    end EMF;

  end Basic;

  package Sources "Time-dependend and controlled voltage/current sources"
    extends Modelica.Icons.SourcesPackage;

    model ConstantVoltage "Source of constant voltage"
      extends Interfaces.VoltageSource;
      parameter Modelica.SIunits.Voltage V(start=1) "Value of constant voltage";
    equation
      vSource = V;
    end ConstantVoltage;

    model ConstantCurrent "Source of constant current"
      extends Interfaces.CurrentSource;
      parameter Modelica.SIunits.Current I(start=1) "Value of constant current";
    equation
      iSource = I;
    end ConstantCurrent;

    model StepVoltage "Voltage step source"
      extends Interfaces.VoltageSource;
      parameter Modelica.SIunits.Voltage V=1 "Height of step";
    equation
      vSource = if time < startTime then 0 else V;
    end StepVoltage;

    model StepCurrent "Current step source"
      extends Interfaces.CurrentSource;
      parameter Modelica.SIunits.Current I=1 "Height of step";
    equation
      iSource = if time < startTime then 0 else I;
    end StepCurrent;

    model SineVoltage "Sine voltage source"
      extends Interfaces.VoltageSource;
      parameter Modelica.SIunits.Voltage V(start=1) "Amplitude of sine wave";
      parameter Modelica.SIunits.Frequency freqHz(start=1) "Frequency of sine wave";
      parameter Modelica.SIunits.Angle phase=0 "Phase of sine wave";
    equation
      vSource = V*Modelica.Math.sin(2*Modelica.Constants.pi*freqHz*(time - startTime) + phase);
    end SineVoltage;

    model SineCurrent "Sine current source"
      extends Interfaces.CurrentSource;
      parameter Modelica.SIunits.Current I(start=1) "Amplitude of sine wave";
      parameter Modelica.SIunits.Frequency freqHz(start=1) "Frequency of sine wave";
      parameter Modelica.SIunits.Angle phase=0 "Phase of sine wave";
    equation
      iSource = I*Modelica.Math.sin(2*Modelica.Constants.pi*freqHz*(time - startTime) + phase);
    end SineCurrent;

  end Sources;

end Analog;