#!/usr/bin/env node

/**
 * Modelica 标准库编译脚本 - 分层编译方案
 *
 * 策略：
 * 1. 先编译 stdlib-stubs.mo 生成基础类型字节码
 * 2. 使用基础类型作为依赖，编译简单模型
 * 3. 逐步编译更复杂的模型
 * 4. 打包成 JAR 文件
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const STDLIB_ROOT = path.join(__dirname, 'Modelica');
const OUTPUT_DIR = path.join(__dirname, 'compiled-stdlib');
const JAR_OUTPUT = path.join(__dirname, 'modelica-stdlib.jar');
const COMPILER_JAR = path.join(__dirname, '../modelica-ide-desktop/native/libs/native-1.0.0-SNAPSHOT.jar');

console.log('=== Modelica 标准库编译器 (分层编译) ===\n');

// 检查编译器
if (!fs.existsSync(COMPILER_JAR)) {
  console.error('❌ 编译器 JAR 未找到:', COMPILER_JAR);
  process.exit(1);
}

// 创建输出目录
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

/**
 * 编译单个文件
 */
function compileFile(moFile, description = '') {
  const fileName = path.basename(moFile);
  const desc = description ? ` (${description})` : '';

  try {
    const cmd = `java -jar "${COMPILER_JAR}" file "${moFile}"`;
    const output = execSync(cmd, {
      cwd: OUTPUT_DIR,
      encoding: 'utf-8',
      timeout: 10000,
      stdio: ['pipe', 'pipe', 'pipe']
    });

    // 检查是否生成了 .class 文件
    const className = fileName.replace('.mo', '.class');
    const classPath = path.join(OUTPUT_DIR, className);

    if (fs.existsSync(classPath)) {
      console.log(`✅ ${fileName}${desc}`);
      return { success: true, file: fileName };
    } else {
      console.log(`⚠️  ${fileName}${desc} - 未生成字节码`);
      return { success: false, file: fileName, reason: 'no_bytecode' };
    }
  } catch (error) {
    const errorMsg = error.stderr || error.message;
    console.log(`❌ ${fileName}${desc} - 编译错误`);
    if (errorMsg.includes('Unknown') || errorMsg.includes('error')) {
      // 只显示第一行错误
      const firstLine = errorMsg.split('\n')[0];
      console.log(`   ${firstLine}`);
    }
    return { success: false, file: fileName, reason: 'compile_error' };
  }
}

/**
 * 编译目录中的所有 .mo 文件
 */
function compileDirectory(dir, description = '', maxFiles = null) {
  const files = fs.readdirSync(dir)
    .filter(f => f.endsWith('.mo'))
    .map(f => path.join(dir, f));

  const toCompile = maxFiles ? files.slice(0, maxFiles) : files;
  const results = { success: 0, failed: 0, errors: [] };

  console.log(`\n编译 ${description} (${toCompile.length} 个文件):`);
  console.log('='.repeat(60));

  for (const file of toCompile) {
    const result = compileFile(file);
    if (result.success) {
      results.success++;
    } else {
      results.failed++;
      results.errors.push(result);
    }
  }

  console.log('='.repeat(60));
  console.log(`结果: ✅ ${results.success} 成功, ❌ ${results.failed} 失败\n`);

  return results;
}

/**
 * 创建 JAR 文件
 */
function createJar() {
  console.log('\n=== 创建 JAR 文件 ===\n');

  // 统计 .class 文件
  const classFiles = [];
  function findClassFiles(dir) {
    if (!fs.existsSync(dir)) return;
    const files = fs.readdirSync(dir);
    for (const file of files) {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);
      if (stat.isDirectory()) {
        findClassFiles(filePath);
      } else if (file.endsWith('.class')) {
        classFiles.push(filePath);
      }
    }
  }

  findClassFiles(OUTPUT_DIR);

  if (classFiles.length === 0) {
    console.log('⚠️  没有找到字节码文件，无法创建 JAR');
    return false;
  }

  console.log(`找到 ${classFiles.length} 个字节码文件`);

  // 创建 MANIFEST.MF
  const manifestDir = path.join(OUTPUT_DIR, 'META-INF');
  if (!fs.existsSync(manifestDir)) {
    fs.mkdirSync(manifestDir, { recursive: true });
  }

  const manifestContent = `Manifest-Version: 1.0
Created-By: Modelica Standard Library Compiler v2.0
Implementation-Title: Modelica Standard Library
Implementation-Version: 3.2.3
`;

  fs.writeFileSync(path.join(manifestDir, 'MANIFEST.MF'), manifestContent, 'utf-8');

  // 使用 jar 命令打包
  try {
    const jarCmd = `jar cfm "${JAR_OUTPUT}" "${path.join(manifestDir, 'MANIFEST.MF')}" -C "${OUTPUT_DIR}" .`;
    execSync(jarCmd, { stdio: 'pipe' });

    const stats = fs.statSync(JAR_OUTPUT);
    const sizeMB = (stats.size / 1024 / 1024).toFixed(2);

    console.log(`\n✅ JAR 文件已创建: ${JAR_OUTPUT}`);
    console.log(`   文件大小: ${sizeMB} MB`);
    console.log(`   包含类文件: ${classFiles.length} 个`);

    return true;
  } catch (error) {
    console.error('❌ 创建 JAR 失败:', error.message);
    return false;
  }
}

/**
 * 清理旧的编译结果
 */
function cleanOutput() {
  if (fs.existsSync(OUTPUT_DIR)) {
    console.log('清理旧的编译结果...');
    const files = fs.readdirSync(OUTPUT_DIR);
    for (const file of files) {
      if (file !== '.gitkeep') {
        const filePath = path.join(OUTPUT_DIR, file);
        if (fs.statSync(filePath).isDirectory()) {
          fs.rmSync(filePath, { recursive: true });
        } else {
          fs.unlinkSync(filePath);
        }
      }
    }
    console.log('清理完成\n');
  }
}

// 主流程
async function main() {
  const startTime = Date.now();

  console.log('编译器:', COMPILER_JAR);
  console.log('输出目录:', OUTPUT_DIR);
  console.log('JAR 输出:', JAR_OUTPUT);
  console.log('');

  // 清理旧结果
  cleanOutput();

  // 第 1 层：编译标准库存根（基础类型）
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 第 1 层：基础类型和接口 (stdlib-stubs.mo)        ║');
  console.log('╚══════════════════════════════════════════════════╝');

  const stubsFile = path.join(__dirname, 'stdlib-stubs.mo');
  if (!fs.existsSync(stubsFile)) {
    console.error('❌ stdlib-stubs.mo 未找到');
    process.exit(1);
  }

  const layer1Result = compileFile(stubsFile, '标准库存根');

  if (!layer1Result.success) {
    console.error('\n❌ 基础类型编译失败，无法继续');
    console.log('\n尝试编译简单模型...\n');
  }

  // 第 2 层：编译简单连接器
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 第 2 层：简单连接器和接口                        ║');
  console.log('╚══════════════════════════════════════════════════╝');

  const interfacesDir = path.join(STDLIB_ROOT, 'Blocks/Interfaces');
  if (fs.existsSync(interfacesDir)) {
    compileDirectory(interfacesDir, 'Blocks.Interfaces');
  }

  // 第 3 层：编译简单数学块
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 第 3 层：简单数学块                              ║');
  console.log('╚══════════════════════════════════════════════════╝');

  const mathDir = path.join(STDLIB_ROOT, 'Blocks/Math');
  if (fs.existsSync(mathDir)) {
    compileDirectory(mathDir, 'Blocks.Math', 20); // 只编译前20个
  }

  // 第 4 层：编译连续控制块
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 第 4 层：连续控制块                              ║');
  console.log('╚══════════════════════════════════════════════════╝');

  const continuousDir = path.join(STDLIB_ROOT, 'Blocks/Continuous');
  if (fs.existsSync(continuousDir)) {
    compileDirectory(continuousDir, 'Blocks.Continuous', 20);
  }

  // 第 5 层：编译信号源
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 第 5 层：信号源                                  ║');
  console.log('╚══════════════════════════════════════════════════╝');

  const sourcesDir = path.join(STDLIB_ROOT, 'Blocks/Sources');
  if (fs.existsSync(sourcesDir)) {
    compileDirectory(sourcesDir, 'Blocks.Sources', 20);
  }

  // 创建 JAR 文件
  const jarCreated = createJar();

  // 统计结果
  const endTime = Date.now();
  const duration = ((endTime - startTime) / 1000).toFixed(2);

  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║ 编译完成                                        ║');
  console.log('╚══════════════════════════════════════════════════╝');
  console.log(`总耗时: ${duration} 秒`);

  if (jarCreated) {
    console.log('\n✅ 标准库 JAR 构建成功！');
    console.log(`   位置: ${JAR_OUTPUT}`);
  } else {
    console.log('\n⚠️  部分模型编译成功，但 JAR 创建失败');
  }

  console.log('\n提示: 当前编译器仍有限制，复杂模型可能需要进一步改进。');
}

main().catch(error => {
  console.error('❌ 编译失败:', error);
  process.exit(1);
});