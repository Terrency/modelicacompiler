// 测试package定义
package Modelica
  package Blocks
    package Math
      model Gain
        Real u;
        Real y;
      equation
        y = u;
      end Gain;
    end Math;
  end Blocks;
end Modelica;