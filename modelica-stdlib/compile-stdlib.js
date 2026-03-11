#!/usr/bin/env node

/**
 * 批量编译 Modelica 标准库并生成字节码 JAR
 *
 * 功能：
 * 1. 扫描标准库中的所有模型
 * 2. 调用编译器生成字节码
 * 3. 将字节码打包成 JAR 文件
 */

const fs = require('fs');
const path = require('path');
const { execSync, spawn } = require('child_process');

const STDLIB_ROOT = path.join(__dirname, 'Modelica');
const OUTPUT_DIR = path.join(__dirname, 'compiled-stdlib');
const JAR_OUTPUT = path.join(__dirname, 'modelica-stdlib.jar');

console.log('=== Modelica 标准库编译器 ===\n');

// 创建输出目录
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

// 查找编译器 JAR
const compilerJar = path.join(__dirname, '../modelica-ide-desktop/native/libs/native-1.0.0-SNAPSHOT.jar');
if (!fs.existsSync(compilerJar)) {
  console.error('❌ 编译器 JAR 未找到:', compilerJar);
  process.exit(1);
}

console.log('编译器 JAR:', compilerJar);
console.log('标准库根目录:', STDLIB_ROOT);
console.log('输出目录:', OUTPUT_DIR);
console.log('');

/**
 * 递归扫描目录，查找所有 .mo 文件
 */
function scanModelicaFiles(dir, fileList = []) {
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
  const cleanContent = content.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

  // 查找所有顶层定义
  const pattern = /(?:^|[;\n])\s*(?:public\s+|protected\s+)?(?:partial\s+|encapsulated\s+)?(package|class|model|function|record|block|connector|type)\s+([A-Z][a-zA-Z0-9_]*)/gm;

  let match;
  while ((match = pattern.exec(cleanContent)) !== null) {
    const type = match[1];
    const name = match[2];

    // 提取完整定义
    const startIndex = match.index;
    let depth = 0;
    let endIndex = startIndex;

    for (let i = startIndex; i < cleanContent.length; i++) {
      const keywordMatch = cleanContent.substring(i).match(/^(?:package|class|model|function|record|block|connector|type)\s+[A-Z]/);
      if (keywordMatch) {
        if (i > startIndex) depth++;
        i += keywordMatch[0].length - 1;
        continue;
      }

      if (cleanContent.substring(i, i + 3) === 'end') {
        const endMatch = cleanContent.substring(i).match(new RegExp(`^end\\s+${name}\\s*;`));
        if (endMatch) {
          endIndex = i + endMatch[0].length;
          break;
        } else {
          depth--;
        }
      }
    }

    const modelContent = cleanContent.substring(startIndex, endIndex).trim();
    models.push({ name, type, content: modelContent, file: filePath });
  }

  return models;
}

/**
 * 编译单个模型
 */
function compileModel(model, index, total) {
  const tempFile = path.join(OUTPUT_DIR, `${model.name}.mo`);

  try {
    // 写入临时文件
    fs.writeFileSync(tempFile, model.content, 'utf-8');

    // 调用编译器（在输出目录中运行）
    const cmd = `java -jar "${compilerJar}" file "${tempFile}"`;
    const output = execSync(cmd, {
      cwd: OUTPUT_DIR,  // 在输出目录中运行
      encoding: 'utf-8',
      stdio: ['pipe', 'pipe', 'pipe']
    });

    // 检查是否成功
    if (output.includes('Compilation successful')) {
      // 查找生成的 .class 文件
      const classFile = path.join(OUTPUT_DIR, `${model.name}.class`);
      if (fs.existsSync(classFile)) {
        console.log(`[${index}/${total}] ✅ ${model.name} (${model.type})`);
        return { success: true, name: model.name, classFile };
      } else {
        console.log(`[${index}/${total}] ⚠️  ${model.name} - 字节码未生成`);
        return { success: false, name: model.name, reason: 'no_bytecode' };
      }
    } else {
      console.log(`[${index}/${total}] ⚠️  ${model.name} - 语义错误（预期）`);
      return { success: false, name: model.name, reason: 'semantic' };
    }
  } catch (error) {
    // 编译失败（可能是语义错误）
    console.log(`[${index}/${total}] ⚠️  ${model.name} - 编译失败`);
    return { success: false, name: model.name, reason: 'compile' };
  }
}

/**
 * 创建 JAR 文件
 */
function createJar() {
  console.log('\n=== 创建 JAR 文件 ===\n');

  // 查找所有 .class 文件
  const classFiles = [];
  function findClassFiles(dir) {
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
    console.log('⚠️  没有找到字节码文件');
    return false;
  }

  console.log(`找到 ${classFiles.length} 个字节码文件`);

  // 创建 MANIFEST.MF
  const manifestDir = path.join(OUTPUT_DIR, 'META-INF');
  if (!fs.existsSync(manifestDir)) {
    fs.mkdirSync(manifestDir, { recursive: true });
  }

  const manifestContent = `Manifest-Version: 1.0
Created-By: Modelica Standard Library Compiler
`;

  fs.writeFileSync(path.join(manifestDir, 'MANIFEST.MF'), manifestContent, 'utf-8');

  // 使用 jar 命令打包
  try {
    const jarCmd = `jar cfm "${JAR_OUTPUT}" "${path.join(manifestDir, 'MANIFEST.MF')}" -C "${OUTPUT_DIR}" .`;
    execSync(jarCmd, { stdio: 'inherit' });

    console.log(`\n✅ JAR 文件已创建: ${JAR_OUTPUT}`);
    console.log(`文件大小: ${(fs.statSync(JAR_OUTPUT).size / 1024 / 1024).toFixed(2)} MB`);
    return true;
  } catch (error) {
    console.error('❌ 创建 JAR 失败:', error.message);
    return false;
  }
}

// 主流程
async function main() {
  console.log('步骤 1: 扫描标准库文件...');
  const moFiles = scanModelicaFiles(STDLIB_ROOT);
  console.log(`找到 ${moFiles.length} 个 .mo 文件\n`);

  console.log('步骤 2: 提取模型定义...');
  const allModels = [];
  for (const file of moFiles) {
    const models = extractModels(file);
    allModels.push(...models);
  }
  console.log(`提取了 ${allModels.length} 个模型定义\n`);

  // 过滤掉 package 和 type，只编译 model, class, function, record, block, connector
  const compilableModels = allModels.filter(m =>
    ['model', 'class', 'function', 'record', 'block', 'connector'].includes(m.type)
  );

  console.log(`其中 ${compilableModels.length} 个可编译（排除 package 和 type）\n`);

  console.log('步骤 3: 编译模型...');
  const results = {
    success: 0,
    semanticError: 0,
    compileError: 0
  };

  // 只编译前 100 个模型作为测试
  const modelsToCompile = compilableModels.slice(0, 100);
  console.log(`编译前 ${modelsToCompile.length} 个模型...\n`);

  for (let i = 0; i < modelsToCompile.length; i++) {
    const result = compileModel(modelsToCompile[i], i + 1, modelsToCompile.length);
    if (result.success) {
      results.success++;
    } else if (result.reason === 'semantic') {
      results.semanticError++;
    } else {
      results.compileError++;
    }
  }

  console.log('\n编译统计:');
  console.log(`  ✅ 成功: ${results.success}`);
  console.log(`  ⚠️  语义错误: ${results.semanticError} (缺少依赖)`);
  console.log(`  ❌ 编译错误: ${results.compileError}`);

  console.log('\n步骤 4: 创建 JAR 文件...');
  createJar();

  console.log('\n=== 完成 ===');
}

main().catch(console.error);