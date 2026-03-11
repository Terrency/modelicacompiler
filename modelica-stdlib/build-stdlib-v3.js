#!/usr/bin/env node

/**
 * Modelica 标准库编译脚本 v3
 * 支持包层次结构的JAR打包
 *
 * 功能：
 * 1. 编译标准库存根
 * 2. 将类文件重新组织到正确的包目录结构
 * 3. 打包成JAR文件
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const STDLIB_DIR = __dirname;
const OUTPUT_DIR = path.join(__dirname, 'compiled-stdlib');
const JAR_OUTPUT = path.join(__dirname, 'modelica-stdlib.jar');
const COMPILER_JAR = path.join(__dirname, '../modelica-ide-desktop/native/libs/native-1.0.0-SNAPSHOT.jar');

console.log('╔══════════════════════════════════════════════════╗');
console.log('║   Modelica 标准库编译器 v3.0                    ║');
console.log('║   支持包层次结构的JAR打包                       ║');
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

/**
 * 清理输出目录
 */
function cleanOutput() {
  if (fs.existsSync(OUTPUT_DIR)) {
    console.log('清理旧的编译结果...');
    fs.rmSync(OUTPUT_DIR, { recursive: true });
  }
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  console.log('清理完成\n');
}

/**
 * 编译标准库文件
 */
function compileStdlib() {
  console.log('╔══════════════════════════════════════════════════╗');
  console.log('║ 步骤 1: 编译标准库存根                          ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  const stubsFile = path.join(STDLIB_DIR, 'stdlib-stubs-simple.mo');
  if (!fs.existsSync(stubsFile)) {
    console.error('❌ stdlib-stubs-simple.mo 未找到');
    process.exit(1);
  }

  try {
    const cmd = `java -jar "${COMPILER_JAR}" file "${stubsFile}"`;
    execSync(cmd, {
      cwd: OUTPUT_DIR,
      encoding: 'utf-8',
      timeout: 30000,
      stdio: 'inherit'
    });

    console.log('\n✅ 编译成功！');
    return true;
  } catch (error) {
    console.error('\n❌ 编译失败:', error.message);
    return false;
  }
}

/**
 * 重新组织类文件到包目录结构
 *
 * 将 Modelica_Blocks_Math_Gain.class 转换为
 * Modelica/Blocks/Math/Gain.class
 */
function reorganizePackageStructure() {
  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║ 步骤 2: 重新组织包目录结构                      ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  // 查找所有 .class 文件
  const classFiles = fs.readdirSync(OUTPUT_DIR)
    .filter(file => file.endsWith('.class'));

  if (classFiles.length === 0) {
    console.log('⚠️  没有找到类文件');
    return false;
  }

  console.log(`找到 ${classFiles.length} 个类文件\n`);

  let reorganized = 0;
  const manifest = [];

  classFiles.forEach(classFile => {
    // 跳过带$的内部类
    if (classFile.includes('$')) {
      return;
    }

    // 解析类名：Modelica_Blocks_Math_Gain -> ['Modelica', 'Blocks', 'Math', 'Gain']
    const className = classFile.replace('.class', '');
    const parts = className.split('_');

    if (parts.length < 2) {
      // 简单类名，保持原样
      console.log(`  ${className} -> (保持原样)`);
      manifest.push(className);
      return;
    }

    // 构建包路径：Modelica/Blocks/Math/Gain
    const packagePath = parts.join('/');
    const targetDir = path.join(OUTPUT_DIR, path.dirname(packagePath));
    const targetFile = path.join(OUTPUT_DIR, packagePath + '.class');

    // 创建目录
    if (!fs.existsSync(targetDir)) {
      fs.mkdirSync(targetDir, { recursive: true });
    }

    // 移动文件
    const sourceFile = path.join(OUTPUT_DIR, classFile);
    fs.renameSync(sourceFile, targetFile);

    // 记录包名
    const packageName = parts.slice(0, -1).join('.');
    const simpleClassName = parts[parts.length - 1];
    const fullName = packageName ? `${packageName}.${simpleClassName}` : simpleClassName;
    manifest.push(fullName);

    console.log(`  ${className} -> ${packagePath.replace('/', '.')}`);
    reorganized++;
  });

  console.log(`\n✅ 重新组织了 ${reorganized} 个类文件`);

  // 保存清单
  const manifestFile = path.join(OUTPUT_DIR, 'MANIFEST.txt');
  fs.writeFileSync(manifestFile, manifest.join('\n'), 'utf-8');
  console.log(`清单已保存到: MANIFEST.txt`);

  return true;
}

/**
 * 创建JAR文件
 */
function createJar() {
  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║ 步骤 3: 创建 JAR 文件                           ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  // 统计 .class 文件
  const classFiles = [];
  function findClassFiles(dir, baseDir = dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);
      if (stat.isDirectory()) {
        findClassFiles(filePath, baseDir);
      } else if (file.endsWith('.class')) {
        const relativePath = path.relative(baseDir, filePath);
        classFiles.push(relativePath.replace(/\\/g, '/'));
      }
    }
  }

  findClassFiles(OUTPUT_DIR);

  if (classFiles.length === 0) {
    console.log('⚠️  没有找到字节码文件，无法创建 JAR');
    return false;
  }

  console.log(`找到 ${classFiles.length} 个字节码文件\n`);

  // 显示部分文件
  console.log('示例文件:');
  classFiles.slice(0, 5).forEach(f => console.log(`  ${f}`));
  if (classFiles.length > 5) {
    console.log(`  ... 还有 ${classFiles.length - 5} 个文件`);
  }

  // 创建 MANIFEST.MF
  const manifestDir = path.join(OUTPUT_DIR, 'META-INF');
  if (!fs.existsSync(manifestDir)) {
    fs.mkdirSync(manifestDir, { recursive: true });
  }

  const manifestContent = `Manifest-Version: 1.0
Created-By: Modelica Standard Library Compiler v3.0
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
 * 验证JAR文件
 */
function verifyJar() {
  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║ 步骤 4: 验证 JAR 文件                           ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  try {
    const jarContent = execSync(`jar tf "${JAR_OUTPUT}"`, { encoding: 'utf-8' });
    const entries = jarContent.split('\n').filter(line => line.trim());

    const classEntries = entries.filter(e => e.endsWith('.class'));
    const dirEntries = entries.filter(e => e.endsWith('/'));

    console.log(`JAR 包含:`);
    console.log(`  - ${classEntries.length} 个类文件`);
    console.log(`  - ${dirEntries.length} 个目录`);

    console.log('\n包结构:');
    const packages = new Set();
    classEntries.forEach(entry => {
      const parts = entry.split('/');
      if (parts.length > 1) {
        const packagePath = parts.slice(0, -1).join('/');
        packages.add(packagePath);
      }
    });

    const sortedPackages = Array.from(packages).sort();
    sortedPackages.forEach(pkg => {
      const classesInPkg = classEntries.filter(e => e.startsWith(pkg + '/'));
      console.log(`  ${pkg.replace(/\//g, '.')} (${classesInPkg.length} 个类)`);
    });

    console.log('\n示例类文件:');
    classEntries.slice(0, 10).forEach(c => console.log(`  ${c}`));

    return true;
  } catch (error) {
    console.log('⚠️  无法验证 JAR 内容:', error.message);
    return false;
  }
}

// 主流程
async function main() {
  const startTime = Date.now();

  // 步骤1: 清理
  cleanOutput();

  // 步骤2: 编译
  if (!compileStdlib()) {
    process.exit(1);
  }

  // 步骤3: 重新组织包结构
  if (!reorganizePackageStructure()) {
    console.log('\n⚠️  包结构重组失败');
  }

  // 步骤4: 创建JAR
  if (!createJar()) {
    process.exit(1);
  }

  // 步骤5: 验证
  verifyJar();

  // 统计结果
  const endTime = Date.now();
  const duration = ((endTime - startTime) / 1000).toFixed(2);

  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║   编译完成                                      ║');
  console.log('╚══════════════════════════════════════════════════╝');
  console.log(`总耗时: ${duration} 秒`);

  console.log('\n✅ 标准库 JAR 构建成功！');
  console.log(`   位置: ${JAR_OUTPUT}`);
  console.log('\n提示: JAR 文件已按照 Modelica 包层次结构组织。');
}

main().catch(error => {
  console.error('❌ 编译失败:', error);
  process.exit(1);
});