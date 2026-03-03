import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'

// 树节点接口
interface TreeNode {
  id: string
  name: string
  type: 'package' | 'class' | 'function' | 'file'
  path: string
  content?: string
  children: TreeNode[]
  isExpanded: boolean
  isLibrary: boolean
}

interface OutputMessage {
  type: 'info' | 'success' | 'warning' | 'error'
  text: string
  timestamp: string
}

interface CompileError {
  message: string
  location?: {
    line: number
    column: number
  }
}

// 构建树形结构
function buildTree(files: { path: string; name: string; content: string; isLibrary: boolean }[]): TreeNode[] {
  const root: TreeNode = {
    id: 'root',
    name: 'root',
    type: 'package',
    path: '',
    children: [],
    isExpanded: true,
    isLibrary: false
  }

  files.forEach(file => {
    const parts = file.path.split('/')
    let current = root

    parts.forEach((part, index) => {
      const isLast = index === parts.length - 1
      const existingChild = current.children.find(c => c.name === part)

      if (existingChild) {
        current = existingChild
      } else {
        const newNode: TreeNode = {
          id: file.path,
          name: part,
          type: isLast ? 'class' : 'package',
          path: parts.slice(0, index + 1).join('/'),
          content: isLast ? file.content : undefined,
          children: [],
          isExpanded: index === 0, // 默认展开第一层
          isLibrary: file.isLibrary
        }
        current.children.push(newNode)
        current = newNode
      }
    })
  })

  return root.children
}

// Modelica Standard Library 文件
const MSL_FILES = [
  {
    path: 'Modelica/package.mo',
    name: 'Modelica',
    content: `within ;
package Modelica "Modelica Standard Library - Version 4.0.0"
  extends Modelica.Icons.Package;

  annotation (
    version = "4.0.0",
    versionDate = "2020-06-25",
    Documentation(info = "<html>
<p>Package <strong>Modelica</strong> is a <strong>standardized</strong> and <strong>free</strong> library
that is developed together with the Modelica language from the
Modelica Association.
</p>
</html>"));
end Modelica;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Icons.mo',
    name: 'Icons',
    content: `within Modelica;
package Icons "Icon definitions"
  extends Package;

  partial package Package "Icon for standard packages"
    annotation(Icon(graphics={Rectangle(
      lineColor={200,200,200},
      fillColor={248,248,248},
      extent={{-100,-100},{100,100}},
      radius=25)}));
  end Package;

  partial model Example "Icon for example models"
    annotation(Icon(graphics={Ellipse(
      lineColor={128,128,128},
      fillColor={248,248,248},
      extent={{-80,-80},{80,80}})}));
  end Example;

  partial function Function "Icon for functions"
    annotation(Icon(graphics={Ellipse(
      lineColor={128,128,128},
      fillColor={248,248,248},
      extent={{-100,-100},{100,100}})}));
  end Function;

  partial block Block "Icon for blocks"
    annotation(Icon(graphics={Rectangle(
      lineColor={128,128,128},
      fillColor={248,248,248},
      extent={{-100,-100},{100,100}})}));
  end Block;

end Icons;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Constants.mo',
    name: 'Constants',
    content: `within Modelica;
package Constants "Mathematical constants and constants of nature"
  extends Modelica.Icons.Package;

  // Mathematical constants
  final constant Real e = 2.71828182845905;
  final constant Real pi = 3.14159265358979;
  final constant Real D2R = pi/180 "Degree to Radian";
  final constant Real R2D = 180/pi "Radian to Degree";
  final constant Real gamma = 0.57721566490153 "Euler's constant";

  // Constants of nature
  final constant Real N_A = 6.02214076e23 "Avogadro constant";
  final constant Real k = 1.380649e-23 "Boltzmann constant";
  final constant Real R = N_A*k "Molar gas constant";
  final constant Real c = 299792458 "Speed of light";
  final constant Real g_n = 9.80665 "Standard gravity";

  annotation (Documentation(info="<html><p>Mathematical and physical constants.</p></html>"));
end Constants;`,
    isLibrary: true
  },
  {
    path: 'Modelica/Math/package.mo',
    name: 'Math',
    content: `within Modelica;
package Math "Mathematical functions"
  extends Modelica.Icons.Package;

  function exp "Exponential function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = exp(u);
  end exp;

  function log "Natural logarithm"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = log(u);
  end log;

  function sin "Sine function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = sin(u);
  end sin;

  function cos "Cosine function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = cos(u);
  end cos;

  function tan "Tangent function"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = tan(u);
  end tan;

  function sqrt "Square root"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = sqrt(u);
  end sqrt;

  function abs "Absolute value"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = abs(u);
  end abs;

  function asin "Inverse sine"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = asin(u);
  end asin;

  function acos "Inverse cosine"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = acos(u);
  end acos;

  function atan "Inverse tangent"
    extends Modelica.Icons.Function;
    input Real u;
    output Real y;
  external "builtin" y = atan(u);
  end atan;

  annotation (Documentation(info="<html><p>Standard mathematical functions.</p></html>"));
end Math;`,
    isLibrary: true
  },
  {
    path: 'Modelica/SIunits.mo',
    name: 'SIunits',
    content: `within Modelica;
package SIunits "Type definitions based on SI units"
  extends Modelica.Icons.Package;

  // Base units
  type Length = Real(final quantity="Length", final unit="m");
  type Mass = Real(final quantity="Mass", final unit="kg");
  type Time = Real(final quantity="Time", final unit="s");
  type Current = Real(final quantity="Current", final unit="A");
  type Temperature = Real(final quantity="Temperature", final unit="K");

  // Derived units
  type Angle = Real(final quantity="Angle", final unit="rad");
  type Velocity = Real(final quantity="Velocity", final unit="m/s");
  type Acceleration = Real(final quantity="Acceleration", final unit="m/s2");
  type Force = Real(final quantity="Force", final unit="N");
  type Pressure = Real(final quantity="Pressure", final unit="Pa");
  type Energy = Real(final quantity="Energy", final unit="J");
  type Power = Real(final quantity="Power", final unit="W");
  type Frequency = Real(final quantity="Frequency", final unit="Hz");

  // Angular quantities
  type AngularVelocity = Real(final quantity="AngularVelocity", final unit="rad/s");
  type AngularAcceleration = Real(final quantity="AngularAcceleration", final unit="rad/s2");

  // Area and Volume
  type Area = Real(final quantity="Area", final unit="m2");
  type Volume = Real(final quantity="Volume", final unit="m3");

  annotation (Documentation(info="<html><p>SI unit type definitions.</p></html>"));
end SIunits;`,
    isLibrary: true
  },
  // Examples
  {
    path: 'Examples/package.mo',
    name: 'Examples',
    content: `within ;
package Examples "Example models"
  extends Modelica.Icons.Package;
  annotation (Documentation(info="<html><p>Example models demonstrating Modelica.</p></html>"));
end Examples;`,
    isLibrary: true
  },
  {
    path: 'Examples/HelloWorld.mo',
    name: 'HelloWorld',
    content: `within Examples;
model HelloWorld "The simplest Modelica model"
  extends Modelica.Icons.Example;

  Real x(start=1) "A state variable";

equation
  der(x) = -x;

  annotation (
    Documentation(info="<html>
<p>This is the simplest possible Modelica model with a single
differential equation: dx/dt = -x</p>
</html>"),
    experiment(StopTime=5, Interval=0.01));
end HelloWorld;`,
    isLibrary: true
  },
  {
    path: 'Examples/SimplePendulum.mo',
    name: 'SimplePendulum',
    content: `within Examples;
model SimplePendulum "A simple pendulum model"
  extends Modelica.Icons.Example;

  parameter Real L = 1.0 "Pendulum length (m)";
  parameter Real g = 9.81 "Gravity (m/s2)";
  parameter Real theta0 = 0.1 "Initial angle (rad)";

  Real theta(start=theta0) "Pendulum angle";
  Real omega(start=0) "Angular velocity";

equation
  der(theta) = omega;
  der(omega) = -(g/L) * sin(theta);

  annotation (
    Documentation(info="<html><p>Simple pendulum dynamics.</p></html>"),
    experiment(StopTime=10, Interval=0.01));
end SimplePendulum;`,
    isLibrary: true
  },
  {
    path: 'Examples/BouncingBall.mo',
    name: 'BouncingBall',
    content: `within Examples;
model BouncingBall "A bouncing ball model"
  extends Modelica.Icons.Example;

  parameter Real e = 0.7 "Coefficient of restitution";
  parameter Real g = 9.81 "Gravity";

  Real h(start=1.0) "Height of ball";
  Real v(start=0) "Velocity of ball";
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
    Documentation(info="<html><p>Ball bouncing on a surface.</p></html>"),
    experiment(StopTime=5, Interval=0.01));
end BouncingBall;`,
    isLibrary: true
  },
  {
    path: 'Examples/DCMotor.mo',
    name: 'DCMotor',
    content: `within Examples;
model DCMotor "A simple DC motor model"
  extends Modelica.Icons.Example;

  parameter Real R = 1.0 "Resistance (Ohm)";
  parameter Real L = 0.01 "Inductance (H)";
  parameter Real K = 0.1 "Motor constant";
  parameter Real J = 0.01 "Inertia (kg.m2)";
  parameter Real B = 0.001 "Damping";

  Real v "Voltage (V)";
  Real i(start=0) "Current (A)";
  Real w(start=0) "Angular velocity (rad/s)";
  Real tau "Torque (N.m)";

equation
  v = R*i + L*der(i) + K*w;
  tau = K*i;
  J*der(w) = tau - B*w;
  v = if time < 0.5 then 0 else 10;

  annotation (
    Documentation(info="<html><p>DC motor with electrical and mechanical dynamics.</p></html>"),
    experiment(StopTime=2, Interval=0.001));
end DCMotor;`,
    isLibrary: true
  },
  {
    path: 'Examples/LorenzSystem.mo',
    name: 'LorenzSystem',
    content: `within Examples;
model LorenzSystem "Lorenz attractor"
  extends Modelica.Icons.Example;

  parameter Real sigma = 10.0;
  parameter Real rho = 28.0;
  parameter Real beta = 8.0/3.0;

  Real x(start=1.0);
  Real y(start=1.0);
  Real z(start=1.0);

equation
  der(x) = sigma * (y - x);
  der(y) = x * (rho - z) - y;
  der(z) = x * y - beta * z;

  annotation (experiment(StopTime=30, Interval=0.01));
end LorenzSystem;`,
    isLibrary: true
  }
]

export const useProjectStore = defineStore('project', () => {
  // State
  const treeNodes = ref<TreeNode[]>([])
  const openFiles = ref<TreeNode[]>([])
  const currentFileIndex = ref<number>(-1)
  const outputMessages = ref<OutputMessage[]>([])
  const errors = ref<CompileError[]>([])
  const libraryLoaded = ref<boolean>(false)
  const expandedNodes = ref<Set<string>>(new Set())

  // Computed
  const currentFile = computed(() =>
    currentFileIndex.value >= 0 ? openFiles.value[currentFileIndex.value] : null
  )

  // 初始化 - 加载MSL
  function loadStandardLibrary() {
    if (libraryLoaded.value) return

    treeNodes.value = buildTree(MSL_FILES)

    // 默认展开 Modelica 和 Examples
    expandedNodes.value.add('Modelica')
    expandedNodes.value.add('Examples')
    expandedNodes.value.add('Modelica/Math')

    libraryLoaded.value = true

    // 打开 HelloWorld 作为默认文件
    const helloWorld = findNodeByPath(treeNodes.value, 'Examples/HelloWorld.mo')
    if (helloWorld) {
      openFile(helloWorld)
    }
  }

  // 查找节点
  function findNodeByPath(nodes: TreeNode[], path: string): TreeNode | null {
    for (const node of nodes) {
      if (node.path === path) return node
      if (node.children.length > 0) {
        const found = findNodeByPath(node.children, path)
        if (found) return found
      }
    }
    return null
  }

  // 切换节点展开状态
  function toggleNode(node: TreeNode) {
    if (node.children.length === 0) return

    if (expandedNodes.value.has(node.path)) {
      expandedNodes.value.delete(node.path)
    } else {
      expandedNodes.value.add(node.path)
    }
  }

  // 检查节点是否展开
  function isNodeExpanded(node: TreeNode): boolean {
    return expandedNodes.value.has(node.path)
  }

  // 打开文件
  function openFile(node: TreeNode) {
    if (!node.content) return // 不是文件节点

    const index = openFiles.value.findIndex(f => f.path === node.path)
    if (index >= 0) {
      currentFileIndex.value = index
    } else {
      openFiles.value.push(node)
      currentFileIndex.value = openFiles.value.length - 1
    }
  }

  // 关闭文件
  function closeFile(node: TreeNode) {
    const index = openFiles.value.findIndex(f => f.path === node.path)
    if (index >= 0) {
      openFiles.value.splice(index, 1)
      if (currentFileIndex.value >= openFiles.value.length) {
        currentFileIndex.value = openFiles.value.length - 1
      }
    }
  }

  // 设置当前文件
  function setCurrentFile(node: TreeNode) {
    const index = openFiles.value.findIndex(f => f.path === node.path)
    if (index >= 0) {
      currentFileIndex.value = index
    }
  }

  // 更新文件内容
  function updateFileContent(path: string, content: string) {
    const file = openFiles.value.find(f => f.path === path)
    if (file) {
      file.content = content
    }
  }

  // 创建新文件
  function createFile() {
    const newFile: TreeNode = {
      id: `untitled-${Date.now()}`,
      name: `untitled-${openFiles.value.length + 1}.mo`,
      type: 'class',
      path: `untitled-${Date.now()}.mo`,
      content: `model NewModel
  // Add your model here
  Real x(start=1);
equation
  der(x) = -x;  // Example equation
end NewModel;
`,
      children: [],
      isExpanded: false,
      isLibrary: false
    }
    treeNodes.value.push(newFile)
    openFile(newFile)
  }

  // 添加输出消息
  function addOutput(text: string, type: OutputMessage['type'] = 'info') {
    const timestamp = new Date().toLocaleTimeString()
    outputMessages.value.push({ type, text, timestamp })
  }

  // 添加错误
  function addError(message: string, location?: { line: number; column: number }) {
    errors.value.push({ message, location })
  }

  // 清除输出
  function clearOutput() {
    outputMessages.value = []
  }

  // 清除错误
  function clearErrors() {
    errors.value = []
  }

  return {
    treeNodes,
    openFiles,
    currentFile,
    outputMessages,
    errors,
    libraryLoaded,
    expandedNodes,
    loadStandardLibrary,
    toggleNode,
    isNodeExpanded,
    openFile,
    closeFile,
    setCurrentFile,
    updateFileContent,
    createFile,
    addOutput,
    addError,
    clearOutput,
    clearErrors
  }
})