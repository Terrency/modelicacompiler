within Modelica;
package Media "Library for media properties"
  extends Modelica.Icons.Package;

  annotation (
    Documentation(info="<html>
<p>
This library contains media property models.
</p>
</html>"));

  package Interfaces "Interfaces for media models"
    extends Modelica.Icons.InterfacesPackage;

    partial package PartialMedium "Partial medium model"
      extends Modelica.Icons.Package;

      constant String mediumName = "unusablePartialMedium" "Name of the medium";
      constant String substanceNames[:] = {mediumName} "Names of the mixture substances";
      constant String extraPropertiesNames[:] = fill("", 0) "Names of extra properties";
      constant Boolean singleState = false "True if u and d are independent of pressure";
      constant Boolean reducedX = true "True if the medium has the reduced X property";
      constant Boolean fixedX = false "True if the medium has the fixed X property";
      constant AbsolutePressure p_default = 101325 "Default pressure";
      constant Temperature T_default = 293.15 "Default temperature";

      replaceable record ThermodynamicState
        "Thermodynamic state variables"
        extends Modelica.Icons.Record;
      end ThermodynamicState;

      replaceable partial model BaseProperties
        "Base properties of medium"
        extends Modelica.Icons.Block;

        InputAbsolutePressure p "Absolute pressure of medium";
        InputMassFraction[nX] X(start = reference_X) "Mass fractions";
        InputSpecificEnthalpy h "Specific enthalpy of medium";

        Density d "Density of medium";
        Temperature T "Temperature of medium";
        MassFraction[nX] Xi(start = reference_X[1:nX])
          "Structurally independent mass fractions";

        parameter Boolean preferredMediumStates = false
          "True if StateSelect.prefer shall be used for the independent property variables";

      protected
        constant Integer nX = size(substanceNames, 1) "Number of substances";
        constant MassFraction reference_X[nX] = fill(1/nX, nX) "Reference mass fractions";

        connector InputAbsolutePressure = input Modelica.SIunits.AbsolutePressure;
        connector InputMassFraction = input Modelica.SIunits.MassFraction;
        connector InputSpecificEnthalpy = input Modelica.SIunits.SpecificEnthalpy;

      equation
        Xi = X[1:nX];
      end BaseProperties;

      replaceable partial function setState_pTX
        "Return thermodynamic state from p, T, and X"
        extends Modelica.Icons.Function;
        input AbsolutePressure p "Pressure";
        input Temperature T "Temperature";
        input MassFraction X[:] = reference_X "Mass fractions";
        output ThermodynamicState state "Thermodynamic state";
      end setState_pTX;

      replaceable partial function setState_phX
        "Return thermodynamic state from p, h, and X"
        extends Modelica.Icons.Function;
        input AbsolutePressure p "Pressure";
        input SpecificEnthalpy h "Specific enthalpy";
        input MassFraction X[:] = reference_X "Mass fractions";
        output ThermodynamicState state "Thermodynamic state";
      end setState_phX;

      replaceable partial function temperature
        "Return temperature from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output Temperature T "Temperature";
      end temperature;

      replaceable partial function pressure
        "Return pressure from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output AbsolutePressure p "Pressure";
      end pressure;

      replaceable partial function density
        "Return density from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output Density d "Density";
      end density;

      replaceable partial function specificEnthalpy
        "Return specific enthalpy from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output SpecificEnthalpy h "Specific enthalpy";
      end specificEnthalpy;

      replaceable partial function specificEntropy
        "Return specific entropy from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output SpecificEntropy s "Specific entropy";
      end specificEntropy;

      type AbsolutePressure = Modelica.SIunits.AbsolutePressure (
        min = 0,
        max = 1.e8,
        nominal = 1.e5,
        start = 1.e5);
      type Density = Modelica.SIunits.Density (
        min = 0,
        max = 1.e5,
        nominal = 1,
        start = 1);
      type DynamicViscosity = Modelica.SIunits.DynamicViscosity (
        min = 0,
        max = 1.e8,
        nominal = 1.e-3,
        start = 1.e-3);
      type EnthalpyFlowRate = Modelica.SIunits.EnthalpyFlowRate (
        nominal = 1.e3,
        start = 0);
      type MassFraction = Real (
        quantity = "MassFraction",
        final unit = "1",
        min = 0,
        max = 1,
        nominal = 0.1,
        start = 0.1);
      type MolarMass = Modelica.SIunits.MolarMass (
        min = 0.001,
        max = 0.25,
        nominal = 0.032,
        start = 0.032);
      type MolarVolume = Modelica.SIunits.MolarVolume (
        min = 1e-6,
        max = 1.0e6,
        nominal = 1.0,
        start = 1.0);
      type SpecificEnthalpy = Modelica.SIunits.SpecificEnthalpy (
        min = -1.e10,
        max = 1.e10,
        nominal = 1.e6,
        start = 1.e6);
      type SpecificEntropy = Modelica.SIunits.SpecificEntropy (
        min = -1.e5,
        max = 1.e5,
        nominal = 1.e3,
        start = 1.e3);
      type SpecificHeatCapacity = Modelica.SIunits.SpecificHeatCapacity (
        min = 0,
        max = 1.e7,
        nominal = 1.e3,
        start = 1.e3);
      type Temperature = Modelica.SIunits.Temperature (
        min = 1,
        max = 1.e4,
        nominal = 300,
        start = 300);
      type ThermalConductivity = Modelica.SIunits.ThermalConductivity (
        min = 0,
        max = 500,
        nominal = 1,
        start = 1);

    end PartialMedium;

  end Interfaces;

  package Air "Air as an ideal gas"
    extends Modelica.Icons.VariantsPackage;

    package IdealGas "Ideal gas air model"
      extends Interfaces.PartialMedium(
        mediumName = "Air",
        substanceNames = {"Air"},
        singleState = false);

      redeclare record ThermodynamicState
        "Thermodynamic state variables"
        extends Modelica.Icons.Record;
        AbsolutePressure p "Absolute pressure of medium";
        Temperature T "Temperature of medium";
        MassFraction X[1] "Mass fraction";
      end ThermodynamicState;

      constant SpecificHeatCapacity cp = 1005.45 "Specific heat capacity at constant pressure";
      constant SpecificHeatCapacity cv = 717.95 "Specific heat capacity at constant volume";
      constant MolarMass MM = 0.0289651159 "Molar mass";
      constant Real R_gas = 287.05 "Specific gas constant";

      redeclare function setState_pTX
        "Return thermodynamic state from p, T, and X"
        extends Modelica.Icons.Function;
        input AbsolutePressure p "Pressure";
        input Temperature T "Temperature";
        input MassFraction X[:] = reference_X "Mass fractions";
        output ThermodynamicState state "Thermodynamic state";
      algorithm
        state.p := p;
        state.T := T;
        state.X := X[1:1];
      end setState_pTX;

      redeclare function setState_phX
        "Return thermodynamic state from p, h, and X"
        extends Modelica.Icons.Function;
        input AbsolutePressure p "Pressure";
        input SpecificEnthalpy h "Specific enthalpy";
        input MassFraction X[:] = reference_X "Mass fractions";
        output ThermodynamicState state "Thermodynamic state";
      protected
        Temperature T "Temperature";
      algorithm
        T := h / cp;
        state.p := p;
        state.T := T;
        state.X := X[1:1];
      end setState_phX;

      redeclare function temperature
        "Return temperature from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output Temperature T "Temperature";
      algorithm
        T := state.T;
      end temperature;

      redeclare function pressure
        "Return pressure from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output AbsolutePressure p "Pressure";
      algorithm
        p := state.p;
      end pressure;

      redeclare function density
        "Return density from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output Density d "Density";
      algorithm
        d := state.p / (R_gas * state.T);
      end density;

      redeclare function specificEnthalpy
        "Return specific enthalpy from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output SpecificEnthalpy h "Specific enthalpy";
      algorithm
        h := cp * state.T;
      end specificEnthalpy;

      redeclare function specificEntropy
        "Return specific entropy from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output SpecificEntropy s "Specific entropy";
      algorithm
        s := cv * Modelica.Math.log(state.T) + R_gas * Modelica.Math.log(state.p);
      end specificEntropy;

    end IdealGas;

  end Air;

  package Water "Water as an incompressible fluid"
    extends Modelica.Icons.VariantsPackage;

    package ConstantPropertyLiquidWater
      "Water as an incompressible fluid with constant properties"
      extends Interfaces.PartialMedium(
        mediumName = "Water",
        substanceNames = {"Water"},
        singleState = true);

      redeclare record ThermodynamicState
        "Thermodynamic state variables"
        extends Modelica.Icons.Record;
        Temperature T "Temperature of medium";
        AbsolutePressure p "Absolute pressure of medium";
      end ThermodynamicState;

      constant SpecificHeatCapacity cp = 4184 "Specific heat capacity at constant pressure";
      constant Density rho = 995.586 "Density";
      constant DynamicViscosity eta = 0.000891 "Dynamic viscosity";
      constant ThermalConductivity lambda_const = 0.6065 "Thermal conductivity";

      redeclare function setState_pTX
        "Return thermodynamic state from p, T, and X"
        extends Modelica.Icons.Function;
        input AbsolutePressure p "Pressure";
        input Temperature T "Temperature";
        input MassFraction X[:] = reference_X "Mass fractions";
        output ThermodynamicState state "Thermodynamic state";
      algorithm
        state.T := T;
        state.p := p;
      end setState_pTX;

      redeclare function setState_phX
        "Return thermodynamic state from p, h, and X"
        extends Modelica.Icons.Function;
        input AbsolutePressure p "Pressure";
        input SpecificEnthalpy h "Specific enthalpy";
        input MassFraction X[:] = reference_X "Mass fractions";
        output ThermodynamicState state "Thermodynamic state";
      algorithm
        state.T := h / cp;
        state.p := p;
      end setState_phX;

      redeclare function temperature
        "Return temperature from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output Temperature T "Temperature";
      algorithm
        T := state.T;
      end temperature;

      redeclare function pressure
        "Return pressure from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output AbsolutePressure p "Pressure";
      algorithm
        p := state.p;
      end pressure;

      redeclare function density
        "Return density from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output Density d "Density";
      algorithm
        d := rho;
      end density;

      redeclare function specificEnthalpy
        "Return specific enthalpy from thermodynamic state"
        extends Modelica.Icons.Function;
        input ThermodynamicState state "Thermodynamic state";
        output SpecificEnthalpy h "Specific enthalpy";
      algorithm
        h := cp * state.T;
      end specificEnthalpy;

    end ConstantPropertyLiquidWater;

  end Water;

end Media;