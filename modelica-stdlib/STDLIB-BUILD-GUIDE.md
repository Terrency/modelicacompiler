# Modelica 标准库编译指南

## 概述

本项目成功实现了 Modelica 标准库的编译和打包，生成了可重用的 JAR 文件。

## 编译结果

### JAR 文件信息

- **文件名**: `modelica-stdlib.jar`
- **大小**: 7.0 KB
- **包含类**: 20 个
- **位置**: `modelica-stdlib/modelica-stdlib.jar`

### 包含的类

#### 基础接口 (Icons)
- `Modelica_Icons_Example` - 示例图标
- `Modelica_Icons_ExamplesPackage` - 示例包图标

#### 连接器接口 (Blocks.Interfaces)
- `Modelica_Blocks_Interfaces_RealInput` - Real 输入连接器
- `Modelica_Blocks_Interfaces_RealOutput` - Real 输出连接器
- `Modelica_Blocks_Interfaces_BooleanInput` - Boolean 输入连接器
- `Modelica_Blocks_Interfaces_BooleanOutput` - Boolean 输出连接器
- `Modelica_Blocks_Interfaces_IntegerInput` - Integer 输入连接器
- `Modelica_Blocks_Interfaces_IntegerOutput` - Integer 输出连接器
- `Modelica_Blocks_Interfaces_SO` - 单输出块
- `Modelica_Blocks_Interfaces_SISO` - 单输入单输出块
- `Modelica_Blocks_Interfaces_SI2SO` - 双输入单输出块
- `Modelica_Blocks_Interfaces_SignalSource` - 信号源基类

#### 数学块 (Blocks.Math)
- `Modelica_Blocks_Math_Gain` - 增益块
- `Modelica_Blocks_Math_Add` - 加法块
- `Modelica_Blocks_Math_Product` - 乘法块
- `Modelica_Blocks_Math_Feedback` - 反馈块

#### 连续控制块 (Blocks.Continuous)
- `Modelica_Blocks_Continuous_Integrator` - 积分器
- `Modelica_Blocks_Continuous_FirstOrder` - 一阶传递函数

#### 信号源 (Blocks.Sources)
- `Modelica_Blocks_Sources_Constant` - 常数信号源

#### 测试模型
- `SimpleTest` - 简单测试模型

## 编译方法

### 使用编译脚本

```bash
cd modelica-stdlib
node build-stdlib-v2.js
```

### 手动编译

```bash
# 编译标准库存根
java -jar ../modelica-ide-desktop/native/libs/native-1.0.0-SNAPSHOT.jar file stdlib-stubs-simple.mo

# 创建 JAR 文件
cd compiled-stdlib
jar cfm ../modelica-stdlib.jar META-INF/MANIFEST.MF *.class
```

## 使用方法

### 在 Java 项目中使用

将 `modelica-stdlib.jar` 添加到类路径：

```bash
java -cp modelica-stdlib.jar:your-app.jar com.yourapp.Main
```

### 在编译时使用

编译依赖标准库的模型时，将 JAR 添加到类路径：

```bash
java -cp modelica-stdlib.jar:modelica-compiler.jar \
  org.modelica.ide.CompilerBridge \
  compile "your-model.mo"
```

### 在 Gradle 项目中使用

```kotlin
dependencies {
    implementation(files("path/to/modelica-stdlib.jar"))
}
```

## 技术限制

### 当前编译器支持的语法

✅ **支持的特性**:
- `model` 定义
- `class` 定义
- 基本类型: `Real`, `Integer`, `Boolean`, `String`
- 参数定义: `parameter`
- 方程段: `equation`
- 导数: `der()`
- 基本表达式: `+`, `-`, `*`, `/`
- 简单变量声明

❌ **不支持的特性**:
- `package` 嵌套定义
- `type` 定义 (如 `type Angle = Real`)
- `connector` 定义
- `block` 定义
- `function` 定义
- `external` 函数调用
- `initial equation` 段
- `if` 表达式
- `for` 循环
- 数组操作

### 解决方案

为了支持完整的标准库，需要：

1. **扩展解析器**:
   - 支持 `package` 嵌套语法
   - 支持 `type` 别名定义
   - 支持 `connector` 定义
   - 支持 `block` 和 `function` 定义

2. **扩展语义分析**:
   - 处理类型别名
   - 支持继承 (`extends`)
   - 支持导入 (`import`)

3. **扩展代码生成**:
   - 生成正确的包结构
   - 支持 `external` 函数调用

## 未来改进

### 短期目标

1. 支持 `package` 嵌套定义
2. 支持 `type` 别名
3. 支持 `connector` 定义
4. 支持 `initial equation`

### 中期目标

1. 支持 `extends` 继承
2. 支持 `import` 导入
3. 支持数组操作
4. 支持更多数学函数

### 长期目标

1. 编译完整的 Modelica 标准库
2. 支持泛型类型
3. 支持元模型编程
4. 生成优化的字节码

## 验证

### 查看 JAR 内容

```bash
jar tf modelica-stdlib.jar
```

### 反编译类文件

```bash
javap -c compiled-stdlib/Modelica_Blocks_Math_Gain.class
```

### 测试使用

```bash
# 创建测试模型
cat > test.mo << 'EOF'
model TestStdlib
  Real x;
  Real y;
equation
  der(x) = -x;
  y = x * 2;
end TestStdlib;
EOF

# 编译测试模型
java -jar ../modelica-ide-desktop/native/libs/native-1.0.0-SNAPSHOT.jar file test.mo
```

## 文件结构

```
modelica-stdlib/
├── stdlib-stubs-simple.mo      # 简化的标准库存根
├── stdlib-stubs.mo              # 完整的标准库存根（待支持）
├── build-stdlib-v2.js           # 编译脚本 v2
├── build-stdlib.js              # 编译脚本 v1
├── compiled-stdlib/             # 编译输出目录
│   ├── META-INF/
│   │   └── MANIFEST.MF
│   └── *.class                  # 编译生成的类文件
└── modelica-stdlib.jar          # 最终的 JAR 文件
```

## 贡献

欢迎贡献代码来扩展编译器的功能！

### 开发流程

1. Fork 项目
2. 创建特性分支
3. 提交改进
4. 创建 Pull Request

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或联系项目维护者。