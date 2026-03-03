within Modelica.Thermal;
package HeatTransfer "Library for thermal heat transfer"
  extends Modelica.Icons.Package;

  annotation (
    Documentation(info="<html>
<p>
This library contains thermal heat transfer components.
</p>
</html>"));

  package Interfaces "Connectors and partial models"
    extends Modelica.Icons.InterfacesPackage;

    connector HeatPort "Thermal port for 1-dim. heat transfer"
      Modelica.SIunits.Temperature T "Port temperature";
      flow Modelica.SIunits.HeatFlowRate Q_flow "Heat flow rate (positive if flowing into the component)";
    end HeatPort;

    connector HeatPort_a "Thermal port for 1-dim. heat transfer (filled)"
      extends HeatPort;
    end HeatPort_a;

    connector HeatPort_b "Thermal port for 1-dim. heat transfer (unfilled)"
      extends HeatPort;
    end HeatPort_b;

    partial model PartialElementaryConditionalHeatPort
      "Partial model to include a conditional HeatPort"
      parameter Boolean useHeatPort=false "= true, if HeatPort is enabled";
      parameter Modelica.SIunits.Temperature T=293.15
        "Fixed device temperature if useHeatPort = false";
      HeatPort_a heatPort(T(start=T)) "Conditional heat port";
    equation
      if not useHeatPort then
        heatPort.T = T;
      end if;
    end PartialElementaryConditionalHeatPort;

  end Interfaces;

  package Components "Thermal components"
    extends Modelica.Icons.Package;

    model HeatCapacitor "Lumped thermal element storing heat"
      extends Interfaces.HeatPort;
      parameter Modelica.SIunits.HeatCapacity C "Heat capacity of element";
      Modelica.SIunits.Temperature T "Temperature of element";
    equation
      T = heatPort.T;
      C*der(T) = heatPort.Q_flow;
    end HeatCapacitor;

    model ThermalConductor "Lumped thermal element transporting heat"
      extends Interfaces.HeatPort_a;
      extends Interfaces.HeatPort_b;
      parameter Modelica.SIunits.ThermalConductance G "Constant thermal conductance";
    equation
      heatPort_a.Q_flow + heatPort_b.Q_flow = 0;
      heatPort_a.Q_flow = G*(heatPort_a.T - heatPort_b.T);
    end ThermalConductor;

    model ThermalResistor "Lumped thermal element transporting heat"
      extends Interfaces.HeatPort_a;
      extends Interfaces.HeatPort_b;
      parameter Modelica.SIunits.ThermalResistance R "Constant thermal resistance";
    equation
      heatPort_a.Q_flow + heatPort_b.Q_flow = 0;
      heatPort_a.Q_flow = (heatPort_a.T - heatPort_b.T)/R;
    end ThermalResistor;

    model ConvectiveConductor "Lumped thermal element for heat convection"
      extends Interfaces.HeatPort_a;
      extends Interfaces.HeatPort_b;
      parameter Modelica.SIunits.CoefficientOfHeatTransfer Gc "Convective thermal conductance";
    equation
      heatPort_a.Q_flow + heatPort_b.Q_flow = 0;
      heatPort_a.Q_flow = Gc*(heatPort_a.T - heatPort_b.T);
    end ConvectiveConductor;

    model BodyRadiation "Lumped thermal element for radiation"
      extends Interfaces.HeatPort_a;
      extends Interfaces.HeatPort_b;
      parameter Real Gr(unit="m2") "Radiation conductance";
    equation
      heatPort_a.Q_flow + heatPort_b.Q_flow = 0;
      heatPort_a.Q_flow = Gr*Modelica.Constants.sigma*(heatPort_a.T^4 - heatPort_b.T^4);
    end BodyRadiation;

  end Components;

  package Sources "Thermal sources"
    extends Modelica.Icons.SourcesPackage;

    model FixedTemperature "Fixed temperature boundary condition"
      parameter Modelica.SIunits.Temperature T "Fixed temperature at port";
      Interfaces.HeatPort_b port;
    equation
      port.T = T;
    end FixedTemperature;

    model PrescribedTemperature "Prescribed temperature boundary condition"
      Interfaces.HeatPort_b port;
      Modelica.Blocks.Interfaces.RealInput T(unit="K") "Prescribed temperature";
    equation
      port.T = T;
    end PrescribedTemperature;

    model FixedHeatFlow "Fixed heat flow boundary condition"
      parameter Modelica.SIunits.HeatFlowRate Q_flow "Fixed heat flow rate at port";
      Interfaces.HeatPort_b port;
    equation
      port.Q_flow = -Q_flow;
    end FixedHeatFlow;

    model PrescribedHeatFlow "Prescribed heat flow boundary condition"
      Interfaces.HeatPort_b port;
      Modelica.Blocks.Interfaces.RealInput Q_flow(unit="W") "Prescribed heat flow rate";
    equation
      port.Q_flow = -Q_flow;
    end PrescribedHeatFlow;

  end Sources;

end HeatTransfer;