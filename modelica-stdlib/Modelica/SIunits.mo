within Modelica;
package SIunits "Type definitions based on SI units"
  extends Modelica.Icons.Package;

  // Base units
  type Length = Real(final quantity="Length", final unit="m");
  type Mass = Real(final quantity="Mass", final unit="kg");
  type Time = Real(final quantity="Time", final unit="s");
  type ElectricCurrent = Real(final quantity="ElectricCurrent", final unit="A");
  type ThermodynamicTemperature = Real(final quantity="ThermodynamicTemperature", final unit="K");
  type Temperature = ThermodynamicTemperature;
  type AmountOfSubstance = Real(final quantity="AmountOfSubstance", final unit="mol");
  type LuminousIntensity = Real(final quantity="LuminousIntensity", final unit="cd");

  // Derived units
  type Angle = Real(final quantity="Angle", final unit="rad", displayUnit="deg");
  type SolidAngle = Real(final quantity="SolidAngle", final unit="sr");
  type Frequency = Real(final quantity="Frequency", final unit="Hz");
  type Force = Real(final quantity="Force", final unit="N");
  type Pressure = Real(final quantity="Pressure", final unit="Pa", displayUnit="bar");
  type Energy = Real(final quantity="Energy", final unit="J");
  type Power = Real(final quantity="Power", final unit="W");
  type ElectricCharge = Real(final quantity="ElectricCharge", final unit="C");
  type ElectricPotential = Real(final quantity="ElectricPotential", final unit="V");
  type ElectricCapacitance = Real(final quantity="ElectricCapacitance", final unit="F");
  type ElectricResistance = Real(final quantity="ElectricResistance", final unit="Ohm");
  type ElectricConductance = Real(final quantity="ElectricConductance", final unit="S");
  type MagneticFlux = Real(final quantity="MagneticFlux", final unit="Wb");
  type MagneticFluxDensity = Real(final quantity="MagneticFluxDensity", final unit="T");
  type Inductance = Real(final quantity="Inductance", final unit="H");

  // Mechanical quantities
  type Velocity = Real(final quantity="Velocity", final unit="m/s");
  type Acceleration = Real(final quantity="Acceleration", final unit="m/s2");
  type AngularVelocity = Real(final quantity="AngularVelocity", final unit="rad/s");
  type AngularAcceleration = Real(final quantity="AngularAcceleration", final unit="rad/s2");
  type Torque = Real(final quantity="Torque", final unit="N.m");
  type MomentOfInertia = Real(final quantity="MomentOfInertia", final unit="kg.m2");

  // Thermal quantities
  type HeatFlowRate = Real(final quantity="HeatFlowRate", final unit="W");
  type ThermalConductivity = Real(final quantity="ThermalConductivity", final unit="W/(m.K)");
  type HeatCapacity = Real(final quantity="HeatCapacity", final unit="J/K");
  type SpecificHeatCapacity = Real(final quantity="SpecificHeatCapacity", final unit="J/(kg.K)");

  // Area and Volume
  type Area = Real(final quantity="Area", final unit="m2");
  type Volume = Real(final quantity="Volume", final unit="m3");

  // Density
  type Density = Real(final quantity="Density", final unit="kg/m3");

  // Dynamic viscosity
  type DynamicViscosity = Real(final quantity="DynamicViscosity", final unit="Pa.s");

  // Kinematic viscosity
  type KinematicViscosity = Real(final quantity="KinematicViscosity", final unit="m2/s");

  annotation (
    Documentation(info="<html>
<p>This package provides type definitions based on SI units according to ISO 31-1992.</p>
</html>"));
end SIunits;