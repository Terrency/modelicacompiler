#!/usr/bin/env node

/**
 * Modelica 标准库编译脚本 v2
 * 分层编译 + JAR 打包
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const STDLIB_DIR = path.join(__dirname);
const OUTPUT_DIR = path.join(__dirname, 'compiled-stdlib');
const JAR_OUTPUT = path.join(__dirname, 'modelica-stdlib.jar');
const COMPILER_JAR = path.join(__dirname, '../modelica-ide-desktop/native/libs/native-1.0.0-SNAPSHOT.jar');

console.log('╔══════════════════════════════════════════════════╗');
console.log('║   Modelica 标准库编译器 v2.0                    ║');
console.log('╚══════════════════════════════════════════════════╝\n');

// 检查编译器
if (!fs.existsSync(COMPILER_JAR)) {
  console.error('❌ 编译器 JAR 未找到:', COMPILER_JAR);
  process.exit(1);
}

console.log('编译器:', COMPILER_JAR);
console.log('输出目录:', OUTPUT_DIR);
console.log('JAR 输出:', JAR_OUTPUT);
console.log('');

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

    // 检查是否成功
    if (output.includes('Compilation successful')) {
      // 移动生成的 .class 文件到输出目录
      const className = fileName.replace('.mo', '.class');
      const currentDir = process.cwd();
      const classPath = path.join(currentDir, className);
      const targetPath = path.join(OUTPUT_DIR, className);

      // 如果class文件在当前目录，移动到输出目录
      if (fs.existsSync(classPath) && classPath !== targetPath) {
        fs.renameSync(classPath, targetPath);
      }

      // 检查目标文件是否存在
      if (fs.existsSync(targetPath)) {
        console.log(`✅ ${fileName}${desc}`);
        return { success: true, file: fileName };
      } else {
        // 检查输出目录中是否有对应的class文件
        const classFiles = fs.readdirSync(OUTPUT_DIR).filter(f => f.endsWith('.class'));
        if (classFiles.length > 0) {
          console.log(`✅ ${fileName}${desc}`);
          return { success: true, file: fileName };
        }
        return { success: false, file: fileName, reason: 'no_bytecode' };
      }
    } else {
      return { success: false, file: fileName, reason: 'no_bytecode' };
    }
  } catch (error) {
    return { success: false, file: fileName, reason: 'compile_error' };
  }
}

/**
 * 创建 JAR 文件
 */
function createJar() {
  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║   创建 JAR 文件                                  ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

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
    const sizeKB = (stats.size / 1024).toFixed(2);

    console.log(`\n✅ JAR 文件已创建: ${path.basename(JAR_OUTPUT)}`);
    console.log(`   文件大小: ${sizeKB} KB`);
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

  // 清理旧结果
  cleanOutput();

  // 第 1 层：编译标准库简化存根
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 第 1 层：基础类型和接口                          ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  const stubsFile = path.join(STDLIB_DIR, 'stdlib-stubs-simple.mo');
  if (!fs.existsSync(stubsFile)) {
    console.error('❌ stdlib-stubs-simple.mo 未找到');
    process.exit(1);
  }

  const result = compileFile(stubsFile, '标准库存根');

  if (result.success) {
    console.log('\n✅ 基础类型编译成功！');
  } else {
    console.error('\n❌ 基础类型编译失败');
    process.exit(1);
  }

  // 创建 JAR 文件
  const jarCreated = createJar();

  // 统计结果
  const endTime = Date.now();
  const duration = ((endTime - startTime) / 1000).toFixed(2);

  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║   编译完成                                      ║');
  console.log('╚══════════════════════════════════════════════════╝');
  console.log(`总耗时: ${duration} 秒`);

  if (jarCreated) {
    console.log('\n✅ 标准库 JAR 构建成功！');
    console.log(`   位置: ${JAR_OUTPUT}`);

    // 验证 JAR 文件
    console.log('\n验证 JAR 文件...');
    try {
      const jarContent = execSync(`jar tf "${JAR_OUTPUT}"`, { encoding: 'utf-8' });
      const classes = jarContent.split('\n').filter(line => line.endsWith('.class'));
      console.log(`   JAR 包含 ${classes.length} 个类文件`);
      console.log('   示例类:');
      classes.slice(0, 5).forEach(c => console.log(`     - ${c}`));
      if (classes.length > 5) {
        console.log(`     ... 还有 ${classes.length - 5} 个类`);
      }
    } catch (error) {
      console.log('   ⚠️  无法验证 JAR 内容');
    }
  }

  console.log('\n提示: 当前编译器支持基本语法，后续可扩展更多特性。');
}

main().catch(error => {
  console.error('❌ 编译失败:', error);
  process.exit(1);
});