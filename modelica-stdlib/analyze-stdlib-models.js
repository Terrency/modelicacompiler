#!/usr/bin/env node

/**
 * Modelica 标准库模型清单生成器
 *
 * 功能：
 * 1. 扫描所有 .mo 文件
 * 2. 提取所有模型定义（model, class, function, package, record, block, connector, type）
 * 3. 识别可运行模型（有 equation 段的 model）
 * 4. 生成详细清单
 */

const fs = require('fs');
const path = require('path');

const STDLIB_ROOT = path.join(__dirname, 'Modelica');
const OUTPUT_DIR = __dirname;

console.log('╔══════════════════════════════════════════════════╗');
console.log('║   Modelica 标准库模型清单生成器                ║');
console.log('╚══════════════════════════════════════════════════╝\n');

/**
 * 模型信息
 */
class ModelInfo {
  constructor(name, type, filePath, hasEquation = false, hasAlgorithm = false) {
    this.name = name;
    this.type = type;
    this.filePath = filePath;
    this.hasEquation = hasEquation;
    this.hasAlgorithm = hasAlgorithm;
    this.isExecutable = hasEquation; // 有 equation 的模型是可执行的
  }
}

/**
 * 递归扫描目录，查找所有 .mo 文件
 */
function scanModelicaFiles(dir, fileList = []) {
  if (!fs.existsSync(dir)) return fileList;

  const files = fs.readdirSync(dir);

  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      scanModelicaFiles(filePath, fileList);
    } else if (file.endsWith('.mo')) {
      fileList.push(filePath);
    }
  }

  return fileList;
}

/**
 * 提取文件中的所有模型定义
 */
function extractModels(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8');
  const models = [];

  // 移除注释
  const cleanContent = content
    .replace(/\/\/.*$/gm, '')
    .replace(/\/\*[\s\S]*?\*\//g, '');

  // 查找所有顶层定义
  const pattern = /(?:^|[;\n])\s*(?:(public|protected)\s+)?(?:(partial|encapsulated|expandable)\s+)?(package|class|model|function|record|block|connector|type|operator|operator\s+function|operator\s+record)\s+([A-Z][a-zA-Z0-9_]*)/gm;

  let match;
  while ((match = pattern.exec(cleanContent)) !== null) {
    const type = match[3];
    const name = match[4];
    const startIndex = match.index;

    // 提取完整定义（到 end name;）
    const endPattern = new RegExp(`end\\s+${name}\\s*;`, 'g');
    endPattern.lastIndex = startIndex;
    const endMatch = endPattern.exec(cleanContent);

    if (endMatch) {
      const definition = cleanContent.substring(startIndex, endMatch.index + endMatch[0].length);

      // 检查是否有 equation 段
      const hasEquation = /\bequation\b/.test(definition);

      // 检查是否有 algorithm 段
      const hasAlgorithm = /\balgorithm\b/.test(definition);

      models.push(new ModelInfo(name, type, filePath, hasEquation, hasAlgorithm));
    }
  }

  return models;
}

/**
 * 分析标准库
 */
function analyzeStdlib() {
  console.log('扫描标准库目录:', STDLIB_ROOT);

  const moFiles = scanModelicaFiles(STDLIB_ROOT);
  console.log(`找到 ${moFiles.length} 个 .mo 文件\n`);

  const allModels = [];
  const stats = {
    total: 0,
    byType: {},
    executable: 0,
    byPackage: {}
  };

  console.log('分析模型定义...');
  moFiles.forEach((file, index) => {
    if ((index + 1) % 100 === 0) {
      console.log(`  处理进度: ${index + 1}/${moFiles.length}`);
    }

    const models = extractModels(file);
    allModels.push(...models);
  });

  console.log(`\n提取了 ${allModels.length} 个模型定义\n`);

  // 统计
  allModels.forEach(model => {
    stats.total++;

    // 按类型统计
    if (!stats.byType[model.type]) {
      stats.byType[model.type] = 0;
    }
    stats.byType[model.type]++;

    // 可执行模型
    if (model.isExecutable) {
      stats.executable++;
    }

    // 按包统计
    const relativePath = path.relative(STDLIB_ROOT, model.filePath);
    const packagePath = relativePath.split(path.sep).slice(0, -1).join('.');
    if (packagePath) {
      if (!stats.byPackage[packagePath]) {
        stats.byPackage[packagePath] = [];
      }
      stats.byPackage[packagePath].push(model);
    }
  });

  return { allModels, stats };
}

/**
 * 生成清单文件
 */
function generateManifests(allModels, stats) {
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 生成清单文件                                    ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  // 1. 所有模型清单
  const allModelsFile = path.join(OUTPUT_DIR, 'STDLIB-ALL-MODELS.md');
  let allModelsContent = `# Modelica 标准库 - 所有模型清单

生成时间: ${new Date().toISOString()}

## 统计概览

- **总模型数**: ${stats.total}
- **可执行模型**: ${stats.executable}
- **文件数**: ${stats.byType}

### 按类型统计

| 类型 | 数量 |
|------|------|
`;

  Object.entries(stats.byType)
    .sort((a, b) => b[1] - a[1])
    .forEach(([type, count]) => {
      allModelsContent += `| ${type} | ${count} |\n`;
    });

  allModelsContent += `\n## 所有模型列表\n\n`;

  // 按类型分组
  const byType = {};
  allModels.forEach(model => {
    if (!byType[model.type]) {
      byType[model.type] = [];
    }
    byType[model.type].push(model);
  });

  Object.keys(byType).sort().forEach(type => {
    allModelsContent += `### ${type} (${byType[type].length} 个)\n\n`;

    byType[type]
      .sort((a, b) => a.name.localeCompare(b.name))
      .slice(0, 100) // 限制每个类型最多显示100个
      .forEach(model => {
        const relativePath = path.relative(STDLIB_ROOT, model.filePath);
        const packagePath = relativePath.split(path.sep).slice(0, -1).join('.');
        const fullName = packagePath ? `${packagePath}.${model.name}` : model.name;
        allModelsContent += `- ${fullName}\n`;
      });

    if (byType[type].length > 100) {
      allModelsContent += `\n  ... 还有 ${byType[type].length - 100} 个\n`;
    }

    allModelsContent += `\n`;
  });

  fs.writeFileSync(allModelsFile, allModelsContent, 'utf-8');
  console.log(`✅ 所有模型清单: ${path.basename(allModelsFile)}`);

  // 2. 可执行模型清单
  const executableModels = allModels.filter(m => m.isExecutable);
  const executableFile = path.join(OUTPUT_DIR, 'STDLIB-EXECUTABLE-MODELS.md');

  let executableContent = `# Modelica 标准库 - 可执行模型清单

生成时间: ${new Date().toISOString()}

## 统计概览

- **可执行模型总数**: ${executableModels.length}
- **定义**: 有 \`equation\` 段的 \`model\` 定义

## 可执行模型列表

`;

  // 按包分组
  const byPackage = {};
  executableModels.forEach(model => {
    const relativePath = path.relative(STDLIB_ROOT, model.filePath);
    const packagePath = relativePath.split(path.sep).slice(0, -1).join('.');
    const packageName = packagePath || 'Root';

    if (!byPackage[packageName]) {
      byPackage[packageName] = [];
    }
    byPackage[packageName].push(model);
  });

  Object.keys(byPackage).sort().forEach(packageName => {
    const models = byPackage[packageName];
    executableContent += `### ${packageName} (${models.length} 个)\n\n`;

    models
      .sort((a, b) => a.name.localeCompare(b.name))
      .forEach(model => {
        const fullName = packageName === 'Root' ? model.name : `${packageName}.${model.name}`;
        executableContent += `- ${fullName}\n`;
      });

    executableContent += `\n`;
  });

  fs.writeFileSync(executableFile, executableContent, 'utf-8');
  console.log(`✅ 可执行模型清单: ${path.basename(executableFile)}`);

  // 3. JSON 格式的清单
  const jsonFile = path.join(OUTPUT_DIR, 'stdlib-manifest.json');
  const manifest = {
    generated: new Date().toISOString(),
    statistics: {
      total: stats.total,
      executable: stats.executable,
      byType: stats.byType
    },
    models: allModels.map(m => ({
      name: m.name,
      type: m.type,
      file: path.relative(STDLIB_ROOT, m.filePath),
      hasEquation: m.hasEquation,
      hasAlgorithm: m.hasAlgorithm,
      isExecutable: m.isExecutable
    })),
    executableModels: executableModels.map(m => ({
      name: m.name,
      file: path.relative(STDLIB_ROOT, m.filePath)
    }))
  };

  fs.writeFileSync(jsonFile, JSON.stringify(manifest, null, 2), 'utf-8');
  console.log(`✅ JSON 清单: ${path.basename(jsonFile)}`);
}

// 主流程
async function main() {
  const startTime = Date.now();

  const { allModels, stats } = analyzeStdlib();

  generateManifests(allModels, stats);

  const endTime = Date.now();
  const duration = ((endTime - startTime) / 1000).toFixed(2);

  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║   分析完成                                      ║');
  console.log('╚══════════════════════════════════════════════════╝');
  console.log(`总耗时: ${duration} 秒`);
  console.log(`\n统计结果:`);
  console.log(`  - 总模型数: ${stats.total}`);
  console.log(`  - 可执行模型: ${stats.executable}`);
  console.log(`  - 模型类型: ${Object.keys(stats.byType).length} 种`);
}

main().catch(error => {
  console.error('❌ 分析失败:', error);
  process.exit(1);
});