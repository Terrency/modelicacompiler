within Modelica;
package Fluid "Library for fluid systems"
  extends Modelica.Icons.Package;

  annotation (
    Documentation(info="<html>
<p>
This library contains fluid system components.
</p>
</html>"));

  package Interfaces "Connectors and partial models for fluid systems"
    extends Modelica.Icons.InterfacesPackage;

    connector FluidPort "Fluid port connector"
      Modelica.SIunits.AbsolutePressure p "Pressure in the port";
      flow Modelica.SIunits.MassFlowRate m_flow
        "Mass flow rate (positive if flowing into the component)";
      stream Modelica.SIunits.SpecificEnthalpy h_outflow
        "Specific enthalpy of outflowing fluid";
      stream Modelica.SIunits.MassFraction Xi_outflow[Medium.nXi]
        "Independent mixture mass fractions of outflowing fluid";
      stream Modelica.SIunits.ExtraProperty C_outflow[Medium.nC]
        "Extra properties of outflowing fluid";
    end FluidPort;

    connector FluidPort_a "Fluid port with positive flow direction"
      extends FluidPort;
    end FluidPort_a;

    connector FluidPort_b "Fluid port with negative flow direction"
      extends FluidPort;
    end FluidPort_b;

    partial model PartialTwoPortTransport
      "Partial component transporting fluid between two ports"
      extends Modelica.Icons.Block;

      replaceable package Medium = Modelica.Media.Interfaces.PartialMedium
        "Medium model";

      FluidPort_a port_a(redeclare package Medium = Medium);
      FluidPort_b port_b(redeclare package Medium = Medium);

      Modelica.SIunits.AbsolutePressure dp = port_a.p - port_b.p
        "Pressure difference across component";
      Modelica.SIunits.MassFlowRate m_flow = port_a.m_flow
        "Mass flow rate from port_a to port_b";

    end PartialTwoPortTransport;

  end Interfaces;

  package Pipes "Pipe components"
    extends Modelica.Icons.Package;

    model StaticPipe "Pipe with static pressure drop"
      extends Interfaces.PartialTwoPortTransport;

      parameter Modelica.SIunits.Length length "Pipe length";
      parameter Modelica.SIunits.Diameter diameter "Pipe inner diameter";
      parameter Real roughness = 0 "Pipe roughness";
      parameter Modelica.SIunits.Area area = Modelica.Constants.pi * diameter^2 / 4
        "Cross-sectional area";

      Modelica.SIunits.Velocity v "Flow velocity";

    equation
      v = m_flow / (Medium.density(Medium.setState_phX(port_a.p, inStream(port_a.h_outflow))) * area);
      dp = 0.5 * Medium.density(Medium.setState_phX(port_a.p, inStream(port_a.h_outflow))) * v^2 * 0.02 * length / diameter;

      port_a.h_outflow = inStream(port_b.h_outflow);
      port_b.h_outflow = inStream(port_a.h_outflow);

      port_a.Xi_outflow = inStream(port_b.Xi_outflow);
      port_b.Xi_outflow = inStream(port_a.Xi_outflow);

    end StaticPipe;

  end Pipes;

  package Valves "Valve components"
    extends Modelica.Icons.Package;

    model ValveLinear "Linear valve"
      extends Interfaces.PartialTwoPortTransport;

      parameter Real Kv = 1 "Valve flow coefficient";
      Real opening "Valve opening (0-1)";

    equation
      m_flow = Kv * opening * dp;

      port_a.h_outflow = inStream(port_b.h_outflow);
      port_b.h_outflow = inStream(port_a.h_outflow);

    end ValveLinear;

  end Valves;

  package Sources "Source components"
    extends Modelica.Icons.SourcesPackage;

    model Boundary_pT "Boundary with prescribed pressure and temperature"
      replaceable package Medium = Modelica.Media.Interfaces.PartialMedium
        "Medium model";

      parameter Modelica.SIunits.AbsolutePressure p = 101325 "Pressure";
      parameter Modelica.SIunits.Temperature T = 293.15 "Temperature";
      parameter Modelica.SIunits.MassFraction X[Medium.nX] = Medium.reference_X
        "Mass fraction";

      Interfaces.FluidPort_b port(redeclare package Medium = Medium);

    equation
      port.p = p;
      port.h_outflow = Medium.specificEnthalpy(Medium.setState_pTX(p, T, X));
      port.Xi_outflow = X[1:Medium.nXi];

    end Boundary_pT;

    model Boundary_ph "Boundary with prescribed pressure and enthalpy"
      replaceable package Medium = Modelica.Media.Interfaces.PartialMedium
        "Medium model";

      parameter Modelica.SIunits.AbsolutePressure p = 101325 "Pressure";
      parameter Modelica.SIunits.SpecificEnthalpy h = 1e5 "Specific enthalpy";
      parameter Modelica.SIunits.MassFraction X[Medium.nX] = Medium.reference_X
        "Mass fraction";

      Interfaces.FluidPort_b port(redeclare package Medium = Medium);

    equation
      port.p = p;
      port.h_outflow = h;
      port.Xi_outflow = X[1:Medium.nXi];

    end Boundary_ph;

  end Sources;

end Fluid;