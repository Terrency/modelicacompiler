# 编译器修复验证指南

## 修复内容

### 1. 编译器命令行参数修复
- **文件**: `src/main/index.ts`
- **问题**: 使用了错误的参数 `--compile`
- **修复**: 改用 `file` 命令并传递临时文件路径

### 2. 词法分析器 bug 修复
- **文件**: `modelica-compiler/src/main/kotlin/org/modelica/compiler/lexer/ModelicaLexer.kt`
- **问题**: 重复调用 `advance()` 导致字符被跳过
- **症状**: `Real` → `eal`, `equation` → `quation`
- **修复**: 删除重复的 `advance()` 调用

### 3. 关键字映射修复
- **文件**: `modelica-compiler/src/main/kotlin/org/modelica/compiler/lexer/TokenType.kt`
- **问题**: 内置类型未被识别为关键字
- **修复**: 简化过滤条件，包含所有非空 lexeme

### 4. 解析器内置类型支持
- **文件**: `modelica-compiler/src/main/kotlin/org/modelica/compiler/parser/ModelicaParser.kt`
- **问题**: `parseIdentifierPath()` 不接受内置类型关键字
- **修复**: 添加对 REAL, INTEGER, BOOLEAN, STRING 的支持

## 验证步骤

### 方法 1: 运行发布包

1. 启动 IDE：
   ```bash
   cd release/ModelicaIDE-win32-x64
   ./ModelicaIDE.exe
   ```

2. 创建新文件并输入以下代码：
   ```modelica
   model HelloWorld
     Real x;
   equation
     der(x) = -x;
   end HelloWorld;
   ```

3. 点击 "Compile" 按钮

4. 检查输出面板，应该看到：
   ```
   === Modelica Compiler ===
   [时间戳] Starting compilation...
   [时间戳] Code length: XX bytes
   [时间戳] Compiler JAR: ...
   [时间戳] Temp file: ...
   [时间戳] Invoking compiler...

   === Starting compilation ===
   File: ...

   --- Phase 1: Lexical Analysis ---
   Generated 18 tokens

   --- Phase 2: Syntax Analysis ---
   Parsed 1 classes

   --- Phase 3: Semantic Analysis ---
   Semantic analysis completed successfully

   --- Phase 4: IR Generation ---
   Generated IR for 1 classes

   --- Phase 5: Bytecode Generation ---
   Generated 1 class files
     - HelloWorld.class

   === Compilation successful ===
   {"success":true,"errors":[],"outputFiles":["HelloWorld"]}

   [时间戳] Compilation succeeded (exit code: 0)
   ```

### 方法 2: 命令行测试

1. 测试词法分析：
   ```bash
   cd modelica-ide-desktop/native/libs
   java -jar native-1.0.0-SNAPSHOT.jar syntax "model Test Real x; equation x = 1; end Test;"
   ```

   应该看到正确的 token 类型：
   - `MODEL` (不是 IDENTIFIER)
   - `REAL` (不是 IDENTIFIER)
   - `EQUATION` (不是 IDENTIFIER)
   - `END` (不是 IDENTIFIER)

2. 测试编译：
   ```bash
   echo 'model HelloWorld Real x; equation der(x) = -x; end HelloWorld;' > test.mo
   java -jar native-1.0.0-SNAPSHOT.jar file test.mo
   ```

   应该看到：
   ```
   === Compilation successful ===
   {"success":true,"errors":[],"outputFiles":["HelloWorld"]}
   ```

## 已更新文件

- ✅ `dist/main/index.js` - 主进程代码
- ✅ `native/libs/native-1.0.0-SNAPSHOT.jar` - 编译器 JAR
- ✅ `release/ModelicaIDE-win32-x64/resources/app/dist/main/index.js` - 发布包主进程
- ✅ `release/ModelicaIDE-win32-x64/resources/app/native/libs/native-1.0.0-SNAPSHOT.jar` - 发布包编译器

## 预期结果

- ✅ 不再出现 "Unknown command: --compile" 错误
- ✅ 词法分析正确识别关键字
- ✅ 能够成功编译包含 `Real`, `Integer` 等类型的模型
- ✅ 编译日志显示详细的编译过程
- ✅ 生成正确的字节码文件

## 如果仍有问题

1. 确认 Java 已安装并在 PATH 中
2. 检查编译器 JAR 是否存在
3. 查看完整的编译日志输出
4. 尝试命令行测试以隔离问题