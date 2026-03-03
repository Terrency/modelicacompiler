within ;
model BouncingBall "A bouncing ball model"
  extends Modelica.Icons.Example;

  // Parameters
  parameter Real e = 0.7 "Coefficient of restitution";
  parameter Modelica.SIunits.Acceleration g = 9.81 "Gravity";

  // State variables
  Modelica.SIunits.Height h(start=1.0) "Height of ball";
  Modelica.SIunits.Velocity v(start=0) "Velocity of ball";

  // State event
  Boolean flying(start=true) "True if ball is flying";

equation
  when h < 0 then
    flying = false;
    reinit(v, -e * pre(v));
    reinit(h, 0);
    flying = true;
  end when;

  der(h) = v;
  der(v) = if flying then -g else 0;

  annotation (
    Documentation(info="<html>
<p>
This model simulates a ball bouncing on a flat surface.
The ball starts at height h=1m with zero velocity.
When the ball hits the ground (h=0), the velocity is reversed
and reduced by the coefficient of restitution.
</p>
</html>"),
    experiment(StopTime=5, Interval=0.01));
end BouncingBall;