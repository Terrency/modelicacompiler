# Modelica Compiler

基于GraalVM技术的Modelica编译器项目，包含编译器核心、桌面IDE和在线IDE三个模块。

## 项目结构

```
modelicacompiler/
├── modelica-compiler/          # 模块一：编译器核心
│   └── src/main/kotlin/org/modelica/compiler/
│       ├── lexer/              # 词法分析
│       ├── parser/             # 语法分析
│       ├── ast/                # 抽象语法树
│       ├── semantic/           # 语义分析
│       ├── ir/                 # 中间表示
│       └── codegen/            # 字节码生成
│
├── modelica-ide-desktop/       # 模块二：桌面IDE
│   ├── src/main/               # Electron主进程
│   ├── src/renderer/           # Vue渲染进程
│   └── native/                 # Kotlin编译器桥接
│
└── modelica-ide-online/        # 模块三：在线IDE
    ├── backend/                # Kotlin/Ktor后端
    └── frontend/               # React前端
```

## 技术栈

- **编程语言**: Kotlin
- **编译目标**: Java字节码
- **构建工具**: Gradle (Kotlin DSL)
- **桌面IDE**: Electron + Vue 3 + Monaco Editor
- **在线IDE**: React + Kotlin/Ktor
- **字节码生成**: ASM库

## 快速开始

### 编译器核心

```bash
cd modelica-compiler
../gradlew build
../gradlew run --args="test.mo -o output/"
```

### 桌面IDE

```bash
cd modelica-ide-desktop
npm install
npm run dev
```

### 在线IDE

启动后端：
```bash
cd modelica-ide-online/backend
../../gradlew run
```

启动前端：
```bash
cd modelica-ide-online/frontend
npm install
npm run dev
```

## 编译器架构

### 1. 词法分析 (Lexer)
- 将源代码转换为Token序列
- 支持Modelica所有关键字和操作符
- 位置信息追踪

### 2. 语法分析 (Parser)
- 递归下降解析器
- 构建抽象语法树(AST)
- 支持类、方程、算法等语法结构

### 3. 语义分析 (Semantic Analyzer)
- 符号表管理
- 类型检查
- 作用域分析

### 4. 中间表示 (IR)
- 平台无关的中间表示
- 便于优化和代码生成

### 5. 字节码生成 (Code Generator)
- 使用ASM库生成Java字节码
- 支持GraalVM原生镜像编译

## Modelica语言支持

支持的语法特性：
- 类定义: `model`, `class`, `record`, `block`, `connector`, `function`, `package`
- 变量类型: `Real`, `Integer`, `Boolean`, `String`
- 方程段: `equation`
- 算法段: `algorithm`
- 连接: `connect(a, b)`
- 控制结构: `if`, `for`, `while`, `when`
- 导数: `der(x)`

## 示例代码

```modelica
model SimplePendulum
  parameter Real L = 1 "Pendulum length";
  parameter Real g = 9.81 "Gravity constant";
  Real theta "Angle";
  Real omega "Angular velocity";
equation
  der(theta) = omega;
  der(omega) = -g/L * sin(theta);
end SimplePendulum;
```

## 许可证

MIT License