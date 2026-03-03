within Modelica;
package Constants "Mathematical constants and constants of nature"
  extends Modelica.Icons.Package;

  // Mathematical constants
  final constant Real e = Modelica.Math.exp(1.0);
  final constant Real pi = 2*Modelica.Math.asin(1.0);
  final constant Real D2R = pi/180 "Degree to Radian";
  final constant Real R2D = 180/pi "Radian to Degree";
  final constant Real gamma = 0.57721566490153286060
    "Euler's constant";

  // Constants of nature
  final constant Real N_A = 6.02214076e23
    "Avogadro constant (mol^-1)";
  final constant Real k = 1.380649e-23
    "Boltzmann constant (J/K)";
  final constant Real R = N_A*k
    "Molar gas constant (J/(mol*K))";
  final constant Real sigma = 5.670374419e-8
    "Stefan-Boltzmann constant (W/(m2*K4))";
  final constant Real c = 299792458
    "Speed of light in vacuum (m/s)";
  final constant Real epsilon0 = 8.8541878128e-12
    "Electric constant (F/m)";
  final constant Real mu0 = 4*pi*1e-7
    "Magnetic constant (H/m)";
  final constant Real g_n = 9.80665
    "Standard acceleration of gravity on earth (m/s2)";

  annotation (
    Documentation(info = "<html>
<p>
This package provides often needed constants.
</p>
</html>"));
end Constants;