within Modelica;
package Mechanics "Library for mechanical systems"
  extends Modelica.Icons.Package;

  annotation (
    Documentation(info="<html>
<p>
This library contains mechanical components.
</p>
</html>"));

  package Rotational "Library for rotational mechanics"
    extends Modelica.Icons.Package;

    annotation (
      Documentation(info="<html>
<p>
This library contains rotational mechanical components.
</p>
</html>"));

    package Interfaces "Connectors and partial models"
      extends Modelica.Icons.InterfacesPackage;

      connector Flange "Flange of a rotational component"
        Modelica.SIunits.Angle phi "Absolute rotation angle of flange";
        flow Modelica.SIunits.Torque tau "Cut torque in the flange";
      end Flange;

      connector Flange_a "Flange with positive torque direction"
        Modelica.SIunits.Angle phi "Absolute rotation angle of flange";
        flow Modelica.SIunits.Torque tau "Cut torque in the flange";
      end Flange_a;

      connector Flange_b "Flange with negative torque direction"
        Modelica.SIunits.Angle phi "Absolute rotation angle of flange";
        flow Modelica.SIunits.Torque tau "Cut torque in the flange";
      end Flange_b;

      partial model PartialTwoFlanges "Component with two rotational flanges"
        extends Modelica.Icons.Block;
        Flange_a flange_a "Flange of left shaft";
        Flange_b flange_b "Flange of right shaft";
      end PartialTwoFlanges;

      partial model PartialCompliant "Compliant rotational component"
        extends PartialTwoFlanges;
        Modelica.SIunits.Angle phi_rel "Relative rotation angle (= flange_b.phi - flange_a.phi)";
        Modelica.SIunits.Torque tau "Torque between flanges";
      equation
        phi_rel = flange_b.phi - flange_a.phi;
        flange_b.tau = tau;
        flange_a.tau = -tau;
      end PartialCompliant;

      partial model PartialAbsoluteSensor "Absolute sensor"
        extends Modelica.Icons.RotationalSensor;
        Flange_a flange;
      equation
        flange.tau = 0;
      end PartialAbsoluteSensor;

    end Interfaces;

    package Components "Rotational components"
      extends Modelica.Icons.Package;

      model Inertia "Rotational inertia"
        extends Interfaces.PartialTwoFlanges;
        parameter Modelica.SIunits.Inertia J(start=1) "Moment of inertia";
        Modelica.SIunits.AngularVelocity w "Angular velocity";
        Modelica.SIunits.AngularAcceleration a "Angular acceleration";
      equation
        w = der(flange_a.phi);
        a = der(w);
        flange_a.phi = flange_b.phi;
        flange_a.tau + flange_b.tau = J*a;
      end Inertia;

      model Spring "Linear rotational spring"
        extends Interfaces.PartialCompliant;
        parameter Modelica.SIunits.RotationalSpringConstant c(start=1) "Spring constant";
        parameter Modelica.SIunits.Angle phi_rel0=0 "Unstretched spring angle";
      equation
        tau = c*(phi_rel - phi_rel0);
      end Spring;

      model Damper "Linear rotational damper"
        extends Interfaces.PartialCompliant;
        parameter Modelica.SIunits.RotationalDampingConstant d(start=1) "Damping constant";
      equation
        tau = d*der(phi_rel);
      end Damper;

      model SpringDamper "Linear rotational spring and damper in parallel"
        extends Interfaces.PartialCompliant;
        parameter Modelica.SIunits.RotationalSpringConstant c(start=1) "Spring constant";
        parameter Modelica.SIunits.RotationalDampingConstant d(start=1) "Damping constant";
        parameter Modelica.SIunits.Angle phi_rel0=0 "Unstretched spring angle";
      equation
        tau = c*(phi_rel - phi_rel0) + d*der(phi_rel);
      end SpringDamper;

      model IdealGear "Ideal gear without inertia"
        extends Interfaces.PartialTwoFlanges;
        parameter Real ratio(start=1) "Transmission ratio (flange_a.phi/flange_b.phi)";
      equation
        flange_a.phi = ratio*flange_b.phi;
        flange_a.tau = -ratio*flange_b.tau;
      end IdealGear;

      model IdealGearR2T "Ideal gear transforming rotational into translational motion"
        extends Modelica.Icons.TranslationalSensor;
        parameter Real ratio(start=1) "Transmission ratio (flangeR.phi/flangeT.s)";
        Interfaces.Flange_a flangeR "Rotational flange";
        Modelica.Mechanics.Translational.Interfaces.Flange_a flangeT "Translational flange";
      equation
        flangeR.phi = ratio*flangeT.s;
        flangeR.tau = -ratio*flangeT.f;
      end IdealGearR2T;

    end Components;

    package Sources "Sources for rotational mechanics"
      extends Modelica.Icons.SourcesPackage;

      model Torque "Input signal acting as torque on a flange"
        extends Modelica.Icons.RotationalSensor;
        Modelica.Blocks.Interfaces.RealInput tau(unit="N.m") "Torque as input signal";
        Interfaces.Flange_b flange;
      equation
        flange.tau = -tau;
      end Torque;

      model ConstantTorque "Constant torque"
        extends Modelica.Icons.RotationalSensor;
        parameter Modelica.SIunits.Torque tau_constant(start=1) "Constant torque";
        Interfaces.Flange_b flange;
      equation
        flange.tau = -tau_constant;
      end ConstantTorque;

      model Speed "Input signal acting as speed on a flange"
        extends Modelica.Icons.RotationalSensor;
        Modelica.Blocks.Interfaces.RealInput w(unit="rad/s") "Angular velocity as input signal";
        parameter Boolean useSupport=false "= true, if support flange enabled";
        Interfaces.Flange_b flange;
        Interfaces.Support support if useSupport;
      protected
        Interfaces.Support internalSupport(phi=0) if not useSupport;
        Interfaces.Support supportActual;
      equation
        connect(internalSupport, supportActual);
        connect(support, supportActual);
        der(flange.phi) = w;
        flange.phi = supportActual.phi + flange.phi;
      end Speed;

      model ConstantSpeed "Constant speed"
        extends Modelica.Icons.RotationalSensor;
        parameter Modelica.SIunits.AngularVelocity w_fixed(start=1) "Fixed speed";
        Interfaces.Flange_b flange;
      equation
        der(flange.phi) = w_fixed;
      end ConstantSpeed;

    end Sources;

  end Rotational;

  package Translational "Library for translational mechanics"
    extends Modelica.Icons.Package;

    annotation (
      Documentation(info="<html>
<p>
This library contains translational mechanical components.
</p>
</html>"));

    package Interfaces "Connectors and partial models"
      extends Modelica.Icons.InterfacesPackage;

      connector Flange "Flange of a translational component"
        Modelica.SIunits.Position s "Absolute position of flange";
        flow Modelica.SIunits.Force f "Cut force in the flange";
      end Flange;

      connector Flange_a "Flange with positive force direction"
        Modelica.SIunits.Position s "Absolute position of flange";
        flow Modelica.SIunits.Force f "Cut force in the flange";
      end Flange_a;

      connector Flange_b "Flange with negative force direction"
        Modelica.SIunits.Position s "Absolute position of flange";
        flow Modelica.SIunits.Force f "Cut force in the flange";
      end Flange_b;

    end Interfaces;

  end Translational;

end Mechanics;