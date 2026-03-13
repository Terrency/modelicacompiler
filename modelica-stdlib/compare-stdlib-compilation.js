#!/usr/bin/env node

/**
 * 标准库编译对比工具
 *
 * 功能：
 * 1. 读取所有模型清单
 * 2. 读取已编译JAR中的类文件
 * 3. 对比生成详细报告
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const STDLIB_DIR = __dirname;
const MANIFEST_FILE = path.join(STDLIB_DIR, 'stdlib-manifest.json');
const JAR_FILE = path.join(STDLIB_DIR, 'modelica-stdlib-new.jar');
const OUTPUT_FILE = path.join(STDLIB_DIR, 'STDLIB-COMPILATION-STATUS.md');

console.log('JAR文件路径:', JAR_FILE);
console.log('JAR文件存在:', fs.existsSync(JAR_FILE));

console.log('╔══════════════════════════════════════════════════╗');
console.log('║   标准库编译状态对比                            ║');
console.log('╚══════════════════════════════════════════════════╝\n');

/**
 * 读取模型清单
 */
function loadManifest() {
  console.log('读取模型清单:', MANIFEST_FILE);

  if (!fs.existsSync(MANIFEST_FILE)) {
    console.error('❌ 清单文件未找到，请先运行 analyze-stdlib-models.js');
    process.exit(1);
  }

  const manifest = JSON.parse(fs.readFileSync(MANIFEST_FILE, 'utf-8'));
  console.log(`  总模型数: ${manifest.statistics.total}`);
  console.log(`  可执行模型: ${manifest.statistics.executable}\n`);

  return manifest;
}

/**
 * 读取JAR文件中的类列表
 */
function loadCompiledClasses() {
  console.log('读取已编译JAR:', JAR_FILE);

  if (!fs.existsSync(JAR_FILE)) {
    console.error('❌ JAR文件未找到:', JAR_FILE);
    process.exit(1);
  }

  try {
    const jarContent = execSync(`jar tf "${JAR_FILE}"`, { encoding: 'utf-8' });
    const classFiles = jarContent
      .split('\n')
      .map(line => line.trim()) // 去除首尾空格和回车符
      .filter(line => line.endsWith('.class'))
      .filter(line => !line.includes('$')) // 排除内部类
      .map(line => {
        // Modelica/Blocks/Math/Gain.class -> Modelica.Blocks.Math.Gain
        return line
          .replace('.class', '')
          .replace(/\//g, '.');
      });

    console.log(`  已编译类: ${classFiles.length}\n`);

    if (classFiles.length === 0) {
      console.log('  ⚠️  警告: 未找到编译的类文件');
    }

    return new Set(classFiles);
  } catch (error) {
    console.error('❌ 读取JAR失败:', error.message);
    process.exit(1);
  }
}

/**
 * 对比并生成报告
 */
function compareAndReport(manifest, compiledClasses) {
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 对比分析                                        ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  const allModels = manifest.models;
  const executableModels = manifest.executableModels;

  // 统计
  const stats = {
    total: allModels.length,
    compiled: 0,
    notCompiled: 0,
    byType: {},
    executable: {
      total: executableModels.length,
      compiled: 0,
      notCompiled: 0
    }
  };

  // 已编译和未编译的模型列表
  const compiled = [];
  const notCompiled = [];

  // 检查每个模型
  allModels.forEach(model => {
    const isCompiled = compiledClasses.has(model.name);

    if (isCompiled) {
      compiled.push(model);
      stats.compiled++;
    } else {
      notCompiled.push(model);
      stats.notCompiled++;
    }

    // 按类型统计
    if (!stats.byType[model.type]) {
      stats.byType[model.type] = { total: 0, compiled: 0 };
    }
    stats.byType[model.type].total++;
    if (isCompiled) {
      stats.byType[model.type].compiled++;
    }
  });

  // 检查可执行模型
  executableModels.forEach(model => {
    if (compiledClasses.has(model.name)) {
      stats.executable.compiled++;
    } else {
      stats.executable.notCompiled++;
    }
  });

  // 生成报告
  let report = `# Modelica 标准库编译状态报告

生成时间: ${new Date().toISOString()}

## 总体统计

| 指标 | 数量 | 百分比 |
|------|------|--------|
| 总模型数 | ${stats.total} | 100% |
| 已编译 | ${stats.compiled} | ${(stats.compiled / stats.total * 100).toFixed(2)}% |
| 未编译 | ${stats.notCompiled} | ${(stats.notCompiled / stats.total * 100).toFixed(2)}% |
| 可执行模型总数 | ${stats.executable.total} | 100% |
| 可执行模型已编译 | ${stats.executable.compiled} | ${(stats.executable.compiled / stats.executable.total * 100).toFixed(2)}% |
| 可执行模型未编译 | ${stats.executable.notCompiled} | ${(stats.executable.notCompiled / stats.executable.total * 100).toFixed(2)}% |

## 按类型统计

| 类型 | 总数 | 已编译 | 未编译 | 编译率 |
|------|------|--------|--------|--------|
`;

  Object.entries(stats.byType)
    .sort((a, b) => b[1].total - a[1].total)
    .forEach(([type, data]) => {
      const rate = (data.compiled / data.total * 100).toFixed(2);
      report += `| ${type} | ${data.total} | ${data.compiled} | ${data.total - data.compiled} | ${rate}% |\n`;
    });

  report += `\n## 已编译模型列表 (${stats.compiled} 个)\n\n`;

  compiled.forEach(model => {
    report += `- ${model.name} (${model.type})\n`;
  });

  report += `\n## 未编译模型列表 (${stats.notCompiled} 个)\n\n`;

  // 只显示前100个未编译模型
  notCompiled.slice(0, 100).forEach(model => {
    report += `- ${model.name} (${model.type})\n`;
  });

  if (notCompiled.length > 100) {
    report += `\n... 还有 ${notCompiled.length - 100} 个未编译模型\n`;
  }

  report += `\n## 编译优先级建议\n\n`;

  report += `### 高优先级：可执行模型（有 equation 段）\n\n`;
  report += `共有 ${stats.executable.notCompiled} 个可执行模型未编译，这些模型可以用于仿真测试。\n\n`;

  report += `### 中优先级：基础组件（block, connector）\n\n`;
  const blocks = notCompiled.filter(m => m.type === 'block');
  const connectors = notCompiled.filter(m => m.type === 'connector');
  report += `- block: ${blocks.length} 个未编译\n`;
  report += `- connector: ${connectors.length} 个未编译\n\n`;

  report += `### 低优先级：辅助定义（package, type, function）\n\n`;
  const packages = notCompiled.filter(m => m.type === 'package');
  const functions = notCompiled.filter(m => m.type === 'function');
  const types = notCompiled.filter(m => m.type === 'type');
  report += `- package: ${packages.length} 个未编译\n`;
  report += `- function: ${functions.length} 个未编译\n`;
  report += `- type: ${types.length} 个未编译\n\n`;

  report += `## 下一步工作\n\n`;
  report += `1. 扩展编译器支持更多语法特性\n`;
  report += `2. 编译高优先级的可执行模型\n`;
  report += `3. 编译基础组件以支持模型组合\n`;
  report += `4. 逐步编译完整的标准库\n`;

  fs.writeFileSync(OUTPUT_FILE, report, 'utf-8');
  console.log(`✅ 报告已生成: ${path.basename(OUTPUT_FILE)}`);

  // 输出摘要
  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║ 编译状态摘要                                    ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  console.log(`总模型数: ${stats.total}`);
  console.log(`已编译: ${stats.compiled} (${(stats.compiled / stats.total * 100).toFixed(2)}%)`);
  console.log(`未编译: ${stats.notCompiled} (${(stats.notCompiled / stats.total * 100).toFixed(2)}%)`);
  console.log(`\n可执行模型:`);
  console.log(`  总数: ${stats.executable.total}`);
  console.log(`  已编译: ${stats.executable.compiled}`);
  console.log(`  未编译: ${stats.executable.notCompiled}`);

  return stats;
}

// 主流程
async function main() {
  const manifest = loadManifest();
  const compiledClasses = loadCompiledClasses();
  const stats = compareAndReport(manifest, compiledClasses);
}

main().catch(error => {
  console.error('❌ 对比失败:', error);
  process.exit(1);
});