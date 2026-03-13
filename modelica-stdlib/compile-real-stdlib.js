#!/usr/bin/env node

/**
 * 编译真实的标准库模型
 * 从Modelica标准库的.mo文件直接编译
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const STDLIB_DIR = __dirname;
const MODELICA_DIR = path.join(__dirname, 'Modelica');
const OUTPUT_DIR = path.join(__dirname, 'compiled-real');
const COMPILER_JAR = path.join(__dirname, '../modelica-compiler/build/libs/modelica-compiler-1.0.0-SNAPSHOT.jar');

console.log('╔══════════════════════════════════════════════════╗');
console.log('║   编译真实标准库模型                            ║');
console.log('╚══════════════════════════════════════════════════╝\n');

// 检查编译器
if (!fs.existsSync(COMPILER_JAR)) {
  console.error('❌ 编译器 JAR 未找到:', COMPILER_JAR);
  process.exit(1);
}

// 清理输出目录
if (fs.existsSync(OUTPUT_DIR)) {
  fs.rmSync(OUTPUT_DIR, { recursive: true });
}
fs.mkdirSync(OUTPUT_DIR, { recursive: true });

/**
 * 查找所有.mo文件
 */
function findMoFiles(dir, files = []) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      findMoFiles(fullPath, files);
    } else if (entry.name.endsWith('.mo')) {
      files.push(fullPath);
    }
  }
  return files;
}

/**
 * 尝试编译单个文件
 */
function compileMoFile(moFile) {
  const relativePath = path.relative(STDLIB_DIR, moFile);
  try {
    const cmd = `java -jar "${COMPILER_JAR}" file "${moFile}"`;
    const result = execSync(cmd, {
      cwd: OUTPUT_DIR,
      encoding: 'utf-8',
      timeout: 10000,
      stdio: 'pipe'
    });

    if (result.includes('successful')) {
      return { success: true, file: relativePath };
    } else {
      return { success: false, file: relativePath, error: result };
    }
  } catch (error) {
    return {
      success: false,
      file: relativePath,
      error: error.stdout || error.stderr || error.message
    };
  }
}

// 主流程
console.log('查找 .mo 文件...');
const moFiles = findMoFiles(MODELICA_DIR);
console.log(`找到 ${moFiles.length} 个 .mo 文件\n`);

console.log('开始编译...\n');

const results = {
  success: [],
  failed: [],
  errors: new Map()
};

const startTime = Date.now();

// 编译前50个文件作为测试
const testFiles = moFiles.slice(0, 50);
console.log(`测试编译前 ${testFiles.length} 个文件...\n`);

for (const moFile of testFiles) {
  const result = compileMoFile(moFile);
  if (result.success) {
    results.success.push(result.file);
    console.log(`✅ ${result.file}`);
  } else {
    results.failed.push(result.file);
    results.errors.set(result.file, result.error);
    console.log(`❌ ${result.file}`);
  }
}

const endTime = Date.now();
const duration = ((endTime - startTime) / 1000).toFixed(2);

// 统计结果
console.log('\n╔══════════════════════════════════════════════════╗');
console.log('║   编译结果统计                                  ║');
console.log('╚══════════════════════════════════════════════════╝\n');

console.log(`总耗时: ${duration} 秒`);
console.log(`成功: ${results.success.length} 个`);
console.log(`失败: ${results.failed.length} 个`);
console.log(`成功率: ${((results.success.length / testFiles.length) * 100).toFixed(1)}%\n`);

if (results.failed.length > 0) {
  console.log('\n失败的文件:');
  results.failed.slice(0, 10).forEach(file => {
    console.log(`  - ${file}`);
    const error = results.errors.get(file);
    if (error) {
      const errorLines = error.split('\n').slice(0, 3);
      errorLines.forEach(line => console.log(`    ${line}`));
    }
  });

  if (results.failed.length > 10) {
    console.log(`  ... 还有 ${results.failed.length - 10} 个失败`);
  }
}

// 保存详细报告
const report = {
  timestamp: new Date().toISOString(),
  totalFiles: testFiles.length,
  successCount: results.success.length,
  failedCount: results.failed.length,
  duration: duration,
  successFiles: results.success,
  failedFiles: results.failed,
  errors: Object.fromEntries(results.errors)
};

fs.writeFileSync(
  path.join(STDLIB_DIR, 'compile-report.json'),
  JSON.stringify(report, null, 2)
);

console.log('\n详细报告已保存到: compile-report.json');