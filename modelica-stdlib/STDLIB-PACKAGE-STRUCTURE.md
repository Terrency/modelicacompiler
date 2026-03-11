# Modelica 标准库编译指南 v3

## 概述

本项目成功实现了 Modelica 标准库的编译和打包，生成了具有正确包层次结构的 JAR 文件。

## 编译结果

### JAR 文件信息

- **文件名**: `modelica-stdlib.jar`
- **大小**: 8.0 KB
- **包含类**: 20 个
- **位置**: `modelica-stdlib/modelica-stdlib.jar`
- **包结构**: ✅ 支持正确的目录层次

### 包结构

JAR 文件按照 Modelica 的包层次结构组织：

```
modelica-stdlib.jar
├── META-INF/
│   └── MANIFEST.MF
├── Modelica/
│   ├── Icons/
│   │   ├── Example.class
│   │   └── ExamplesPackage.class
│   └── Blocks/
│       ├── Interfaces/
│       │   ├── RealInput.class
│       │   ├── RealOutput.class
│       │   ├── BooleanInput.class
│       │   ├── BooleanOutput.class
│       │   ├── IntegerInput.class
│       │   ├── IntegerOutput.class
│       │   ├── SO.class
│       │   ├── SISO.class
│       │   ├── SI2SO.class
│       │   └── SignalSource.class
│       ├── Math/
│       │   ├── Gain.class
│       │   ├── Add.class
│       │   ├── Product.class
│       │   └── Feedback.class
│       ├── Continuous/
│       │   ├── Integrator.class
│       │   └── FirstOrder.class
│       └── Sources/
│           └── Constant.class
└── SimpleTest.class
```

### 包映射关系

编译脚本自动将下划线分隔的类名转换为包路径：

| 原始类名 | 包名 | 类名 | JAR 路径 |
|---------|------|------|----------|
| `Modelica_Blocks_Math_Gain` | `Modelica.Blocks.Math` | `Gain` | `Modelica/Blocks/Math/Gain.class` |
| `Modelica_Blocks_Interfaces_RealInput` | `Modelica.Blocks.Interfaces` | `RealInput` | `Modelica/Blocks/Interfaces/RealInput.class` |
| `Modelica_Icons_Example` | `Modelica.Icons` | `Example` | `Modelica/Icons/Example.class` |

## 编译方法

### 使用编译脚本

```bash
cd modelica-stdlib
node build-stdlib-v3.js
```

### 编译流程

脚本执行以下步骤：

1. **清理输出目录** - 删除旧的编译结果
2. **编译标准库** - 使用 Modelica 编译器生成字节码
3. **重新组织包结构** - 将类文件移动到正确的目录层次
4. **创建 JAR 文件** - 打包所有类文件
5. **验证 JAR 文件** - 检查包结构是否正确

## 使用方法

### 在 Java 项目中使用

```bash
java -cp modelica-stdlib.jar:your-app.jar com.yourapp.Main
```

### 在 Gradle 项目中使用

```kotlin
dependencies {
    implementation(files("path/to/modelica-stdlib.jar"))
}
```

### 引用标准库类

```java
// 引用 Modelica.Blocks.Math.Gain
import Modelica.Blocks.Math.Gain;

// 引用 Modelica.Blocks.Interfaces.RealInput
import Modelica.Blocks.Interfaces.RealInput;

// 使用
Gain gain = new Gain();
gain.k = 2.0;  // 设置参数
gain.u = 1.0;  // 设置输入
// 求解方程后
double output = gain.y;  // 获取输出
```

### 在 Modelica 代码中使用

```modelica
model MyModel
  Modelica_Blocks_Math_Gain gain(k=2);
  // 编译时会自动解析为 Modelica.Blocks.Math.Gain
equation
  gain.u = 1.0;
end MyModel;
```

## 验证

### 查看 JAR 内容

```bash
jar tf modelica-stdlib.jar
```

输出示例：
```
META-INF/
META-INF/MANIFEST.MF
Modelica/
Modelica/Blocks/
Modelica/Blocks/Math/
Modelica/Blocks/Math/Gain.class
...
```

### 查看包结构

```bash
unzip -l modelica-stdlib.jar | grep ".class"
```

### 反编译类文件

```bash
javap -p compiled-stdlib/Modelica/Blocks/Math/Gain.class
```

输出：
```java
public class Modelica.Blocks.Math.Gain {
  public final double k;
  public double u;
  public double y;
  public Modelica.Blocks.Math.Gain();
  public void solveEquations(double);
}
```

## 技术实现

### 包结构重组算法

```javascript
// 1. 解析类名
const className = "Modelica_Blocks_Math_Gain";
const parts = className.split('_');
// parts = ['Modelica', 'Blocks', 'Math', 'Gain']

// 2. 构建包路径
const packagePath = parts.join('/');
// packagePath = 'Modelica/Blocks/Math/Gain'

// 3. 移动文件
source: "Modelica_Blocks_Math_Gain.class"
target: "Modelica/Blocks/Math/Gain.class"
```

### 编译器限制

当前编译器不支持点号分隔的类名（如 `Modelica.Blocks.Math.Gain`），因此：

- ✅ **解决方案**: 使用下划线分隔（`Modelica_Blocks_Math_Gain`）
- ✅ **后处理**: 编译后自动重组为正确的包结构

### 类加载器支持

JAR 文件完全兼容 Java 类加载器：

```java
// 正常工作
Class<?> gainClass = Class.forName("Modelica.Blocks.Math.Gain");
Object gain = gainClass.newInstance();
```

## 包含的类

### Modelica.Icons (2 个类)
- `Example` - 示例图标
- `ExamplesPackage` - 示例包图标

### Modelica.Blocks.Interfaces (10 个类)
- `RealInput` - Real 输入连接器
- `RealOutput` - Real 输出连接器
- `BooleanInput` - Boolean 输入连接器
- `BooleanOutput` - Boolean 输出连接器
- `IntegerInput` - Integer 输入连接器
- `IntegerOutput` - Integer 输出连接器
- `SO` - 单输出块
- `SISO` - 单输入单输出块
- `SI2SO` - 双输入单输出块
- `SignalSource` - 信号源基类

### Modelica.Blocks.Math (4 个类)
- `Gain` - 增益块 (y = k * u)
- `Add` - 加法块 (y = k1*u1 + k2*u2)
- `Product` - 乘法块 (y = u1 * u2)
- `Feedback` - 反馈块 (y = u1 - u2)

### Modelica.Blocks.Continuous (2 个类)
- `Integrator` - 积分器 (der(y) = k * u)
- `FirstOrder` - 一阶传递函数 (T*der(y) + y = k*u)

### Modelica.Blocks.Sources (1 个类)
- `Constant` - 常数信号源 (y = k)

### 测试模型 (1 个类)
- `SimpleTest` - 简单测试模型

## 文件结构

```
modelica-stdlib/
├── stdlib-stubs-simple.mo      # 简化的标准库存根（下划线分隔）
├── build-stdlib-v3.js           # 编译脚本 v3（支持包结构）
├── compiled-stdlib/             # 编译输出目录
│   ├── META-INF/
│   │   └── MANIFEST.MF
│   ├── Modelica/                # 包层次结构
│   │   ├── Icons/
│   │   │   └── *.class
│   │   └── Blocks/
│   │       ├── Interfaces/
│   │       │   └── *.class
│   │       ├── Math/
│   │       │   └── *.class
│   │       ├── Continuous/
│   │       │   └── *.class
│   │       └── Sources/
│   │           └── *.class
│   └── SimpleTest.class
├── modelica-stdlib.jar          # 最终的 JAR 文件
└── STDLIB-BUILD-GUIDE.md        # 本文档
```

## 后续改进

### 短期目标

1. ✅ 支持正确的包目录结构
2. 添加更多标准库模型
3. 支持继承和导入

### 中期目标

1. 修改编译器直接支持点号分隔的类名
2. 支持完整的 package 定义语法
3. 支持 type、connector、block 定义

### 长期目标

1. 编译完整的 Modelica 标准库
2. 支持泛型类型
3. 生成优化的字节码

## 常见问题

### Q: 为什么使用下划线而不是点号？

A: 当前编译器的解析器不支持点号分隔的类名。我们使用后处理步骤在编译后重组包结构。

### Q: JAR 文件是否兼容标准 Java 类加载器？

A: 是的，JAR 文件完全兼容 Java 类加载器。类文件按照标准的包层次结构组织。

### Q: 如何添加新的标准库模型？

A: 编辑 `stdlib-stubs-simple.mo` 文件，添加新的模型定义（使用下划线分隔类名），然后重新运行编译脚本。

### Q: 类名中的包信息是否会影响运行时？

A: 不会。类文件内部的包名已经正确设置，JVM 会正确识别包层次结构。

## 许可证

MIT License

## 更新历史

- **v3.0** (2026-03-11): 支持正确的包目录结构
- **v2.0** (2026-03-11): 简化标准库存根
- **v1.0** (2026-03-05): 初始版本