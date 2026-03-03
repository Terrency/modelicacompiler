within ;
model SimplePendulum "A simple pendulum model"
  extends Modelica.Icons.Example;

  // Parameters
  parameter Modelica.SIunits.Length L = 1.0 "Pendulum length";
  parameter Modelica.SIunits.Acceleration g = 9.81 "Gravity constant";
  parameter Modelica.SIunits.Angle theta0 = 0.1 "Initial angle (rad)";

  // State variables
  Modelica.SIunits.Angle theta(start=theta0) "Pendulum angle";
  Modelica.SIunits.AngularVelocity omega(start=0) "Angular velocity";

equation
  // Equations of motion
  der(theta) = omega;
  der(omega) = -(g/L) * Modelica.Math.sin(theta);

  annotation (
    Documentation(info="<html>
<p>
This is a simple pendulum model demonstrating the basic structure
of a Modelica model with differential equations.
</p>
<p>
The pendulum consists of a point mass at the end of a massless rod.
The equation of motion is derived from Newton's second law:
</p>
<pre>
  d²θ/dt² = -(g/L) * sin(θ)
</pre>
</html>"),
    experiment(StopTime=10, Interval=0.01));
end SimplePendulum;