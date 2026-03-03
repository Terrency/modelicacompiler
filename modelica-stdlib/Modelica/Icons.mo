within Modelica;
package Icons "Icon definitions"
  extends Package;

  partial package Package "Icon for standard packages"
    annotation(Icon(coordinateSystem(preserveAspectRatio=false,
      extent={{-100,-100},{100,100}}),
      graphics={Rectangle(lineColor={200,200,200},
        fillColor={248,248,248},
        fillPattern=FillPattern.HorizontalCylinder,
        extent={{-100,-100},{100,100}},
        radius=25),
        Rectangle(lineColor={128,128,128},
          extent={{-100,-100},{100,100}},
          radius=25)}));
  end Package;

  partial model Example "Icon for an example model"
    annotation(Icon(coordinateSystem(preserveAspectRatio=false,
      extent={{-100,-100},{100,100}}),
      graphics={Rectangle(lineColor={128,128,128},
        fillColor={248,248,248},
        fillPattern=FillPattern.HorizontalCylinder,
        extent={{-100,-100},{100,100}},
        radius=25),
        Ellipse(lineColor={128,128,128},
          fillColor={248,248,248},
          fillPattern=FillPattern.HorizontalCylinder,
          extent={{-80,-80},{80,80}})}));
  end Example;

  partial function Function "Icon for functions"
    annotation(Icon(coordinateSystem(preserveAspectRatio=false,
      extent={{-100,-100},{100,100}}),
      graphics={Text(lineColor={0,0,255},
        extent={{-150,110},{150,150}},
        textString="%name"),
        Ellipse(lineColor={128,128,128},
          fillColor={248,248,248},
          fillPattern=FillPattern.HorizontalCylinder,
          extent={{-100,-100},{100,100}})}));
  end Function;

  annotation(Documentation(info="<html>
<p>This package contains definitions for icons.</p>
</html>"));
end Icons;