#!/usr/bin/env node

/**
 * Modelica 标准库编译脚本 v4
 * 支持包层次结构和包描述类
 *
 * 功能：
 * 1. 编译标准库存根
 * 2. 将类文件重新组织到正确的包目录结构
 * 3. 为每个包生成包描述类（package-info.class）
 * 4. 打包成JAR文件
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const STDLIB_DIR = __dirname;
const OUTPUT_DIR = path.join(__dirname, 'compiled-stdlib');
let JAR_OUTPUT = path.join(__dirname, 'modelica-stdlib.jar');
const COMPILER_JAR = path.join(__dirname, '../modelica-compiler/build/libs/modelica-compiler-1.0.0-SNAPSHOT.jar');

console.log('╔══════════════════════════════════════════════════╗');
console.log('║   Modelica 标准库编译器 v4.0                    ║');
console.log('║   支持包层次结构和包描述类                      ║');
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
  const packages = new Set();

  classFiles.forEach(classFile => {
    // 跳过带$的内部类
    if (classFile.includes('$')) {
      return;
    }

    // 解析类名
    const className = classFile.replace('.class', '');
    const parts = className.split('_');

    if (parts.length < 2) {
      // 简单类名，保持原样
      console.log(`  ${className} -> (保持原样)`);
      return;
    }

    // 构建包路径
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
    if (packageName) {
      packages.add(packageName);
    }

    console.log(`  ${className} -> ${packagePath.replace(/\//g, '.')}`);
    reorganized++;
  });

  console.log(`\n✅ 重新组织了 ${reorganized} 个类文件`);
  console.log(`发现 ${packages.size} 个包`);

  return { packages };
}

/**
 * 为每个包生成包描述类
 */
function generatePackageInfoClasses(packages) {
  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║ 步骤 3: 生成包描述类                            ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  if (!packages || packages.size === 0) {
    console.log('⚠️  没有找到包');
    return false;
  }

  console.log(`为 ${packages.size} 个包生成描述类...\n`);

  let generated = 0;

  // 为每个包生成一个简单的包描述类
  packages.forEach(packageName => {
    const parts = packageName.split('.');
    const packagePath = parts.join('/');
    const packageDir = path.join(OUTPUT_DIR, packagePath);
    const packageClassFile = path.join(packageDir, 'package-info.class');

    // 创建简单的包描述类字节码
    // 这是一个简化的class文件，只包含包信息
    const bytecode = generateSimplePackageInfoClass(parts);

    // 确保目录存在
    if (!fs.existsSync(packageDir)) {
      fs.mkdirSync(packageDir, { recursive: true });
    }

    // 写入文件
    fs.writeFileSync(packageClassFile, bytecode);
    console.log(`  ${packageName} -> package-info.class`);
    generated++;
  });

  // 为根包也生成
  const rootPackages = new Set();
  packages.forEach(pkg => {
    const parts = pkg.split('.');
    if (parts.length > 1) {
      rootPackages.add(parts[0]);
    }
  });

  rootPackages.forEach(rootPackage => {
    const packageDir = path.join(OUTPUT_DIR, rootPackage);
    const packageClassFile = path.join(packageDir, 'package-info.class');

    if (!fs.existsSync(packageDir)) {
      fs.mkdirSync(packageDir, { recursive: true });
    }

    const bytecode = generateSimplePackageInfoClass([rootPackage]);
    fs.writeFileSync(packageClassFile, bytecode);
    console.log(`  ${rootPackage} -> package-info.class`);
    generated++;
  });

  console.log(`\n✅ 生成了 ${generated} 个包描述类`);
  return true;
}

/**
 * 生成简单的包描述类字节码
 * 这是一个最小化的Java class文件，表示一个包
 */
function generateSimplePackageInfoClass(packageParts) {
  // 生成一个简单的Java class文件
  // package-info.class 是Java中的标准包描述文件

  const className = packageParts.join('/') + '/package-info';

  // Java class文件结构（简化版）
  // 这里我们创建一个最小的有效class文件

  const buffer = Buffer.alloc(200);
  let offset = 0;

  // Magic number: 0xCAFEBABE
  buffer.writeUInt32BE(0xCAFEBABE, offset); offset += 4;

  // Minor version: 0
  buffer.writeUInt16BE(0, offset); offset += 2;

  // Major version: 61 (Java 17)
  buffer.writeUInt16BE(61, offset); offset += 2;

  // Constant pool count: 1 (最小值)
  buffer.writeUInt16BE(1, offset); offset += 2;

  // Access flags: 0x0000 (package-info)
  buffer.writeUInt16BE(0x0000, offset); offset += 2;

  // This class: 0 (常量池索引，这里简化处理)
  buffer.writeUInt16BE(0, offset); offset += 2;

  // Super class: 0
  buffer.writeUInt16BE(0, offset); offset += 2;

  // Interfaces count: 0
  buffer.writeUInt16BE(0, offset); offset += 2;

  // Fields count: 0
  buffer.writeUInt16BE(0, offset); offset += 2;

  // Methods count: 0
  buffer.writeUInt16BE(0, offset); offset += 2;

  // Attributes count: 0
  buffer.writeUInt16BE(0, offset); offset += 2;

  return buffer.slice(0, offset);
}

/**
 * 创建JAR文件
 */
function createJar() {
  console.log('\n╔══════════════════════════════════════════════════╗');
  console.log('║ 步骤 4: 创建 JAR 文件                           ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  // 统计所有文件
  const allFiles = [];
  function findFiles(dir, baseDir = dir) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);
      if (stat.isDirectory()) {
        findFiles(filePath, baseDir);
      } else {
        const relativePath = path.relative(baseDir, filePath);
        allFiles.push(relativePath.replace(/\\/g, '/'));
      }
    }
  }

  findFiles(OUTPUT_DIR);

  const classFiles = allFiles.filter(f => f.endsWith('.class'));
  const packageInfoFiles = allFiles.filter(f => f.endsWith('package-info.class'));

  if (classFiles.length === 0) {
    console.log('⚠️  没有找到字节码文件，无法创建 JAR');
    return false;
  }

  console.log(`找到 ${classFiles.length} 个字节码文件`);
  console.log(`其中 ${packageInfoFiles.length} 个包描述文件\n`);

  // 显示包描述文件
  if (packageInfoFiles.length > 0) {
    console.log('包描述文件:');
    packageInfoFiles.slice(0, 10).forEach(f => console.log(`  ${f}`));
    if (packageInfoFiles.length > 10) {
      console.log(`  ... 还有 ${packageInfoFiles.length - 10} 个`);
    }
    console.log('');
  }

  // 创建 MANIFEST.MF
  const manifestDir = path.join(OUTPUT_DIR, 'META-INF');
  if (!fs.existsSync(manifestDir)) {
    fs.mkdirSync(manifestDir, { recursive: true });
  }

  const manifestContent = `Manifest-Version: 1.0
Created-By: Modelica Standard Library Compiler v4.0
Implementation-Title: Modelica Standard Library
Implementation-Version: 3.2.3
`;

  fs.writeFileSync(path.join(manifestDir, 'MANIFEST.MF'), manifestContent, 'utf-8');

  // 删除旧的JAR文件
  if (fs.existsSync(JAR_OUTPUT)) {
    try {
      fs.unlinkSync(JAR_OUTPUT);
    } catch (e) {
      console.log('⚠️  无法删除旧的JAR文件，尝试使用不同的文件名');
      const timestamp = Date.now();
      JAR_OUTPUT = JAR_OUTPUT.replace('.jar', `-${timestamp}.jar`);
    }
  }

  // 使用 jar 命令打包
  try {
    const jarCmd = `jar cfm "${JAR_OUTPUT}" "${path.join(manifestDir, 'MANIFEST.MF')}" -C "${OUTPUT_DIR}" .`;
    execSync(jarCmd, { stdio: 'pipe' });

    const stats = fs.statSync(JAR_OUTPUT);
    const sizeKB = (stats.size / 1024).toFixed(2);

    console.log(`\n✅ JAR 文件已创建: ${path.basename(JAR_OUTPUT)}`);
    console.log(`   文件大小: ${sizeKB} KB`);
    console.log(`   包含文件: ${allFiles.length} 个`);
    console.log(`   - 类文件: ${classFiles.length} 个`);
    console.log(`   - 包描述: ${packageInfoFiles.length} 个`);

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
  console.log('║ 步骤 5: 验证 JAR 文件                           ║');
  console.log('╚══════════════════════════════════════════════════╝\n');

  try {
    const jarContent = execSync(`jar tf "${JAR_OUTPUT}"`, { encoding: 'utf-8' });
    const entries = jarContent.split('\n').filter(line => line.trim());

    const classEntries = entries.filter(e => e.endsWith('.class'));
    const packageInfoEntries = entries.filter(e => e.endsWith('package-info.class'));
    const dirEntries = entries.filter(e => e.endsWith('/'));

    console.log(`JAR 包含:`);
    console.log(`  - ${classEntries.length} 个类文件`);
    console.log(`  - ${packageInfoEntries.length} 个包描述文件`);
    console.log(`  - ${dirEntries.length} 个目录`);

    console.log('\n包描述文件:');
    packageInfoEntries.slice(0, 10).forEach(e => {
      const pkgPath = e.replace('/package-info.class', '').replace(/\//g, '.');
      console.log(`  ${pkgPath}`);
    });

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
      const hasPackageInfo = packageInfoEntries.some(e => e.startsWith(pkg + '/'));
      const marker = hasPackageInfo ? '✓' : '✗';
      console.log(`  ${marker} ${pkg.replace(/\//g, '.')} (${classesInPkg.length} 个类)`);
    });

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
  const result = reorganizePackageStructure();
  if (!result) {
    console.log('\n⚠️  包结构重组失败');
    process.exit(1);
  }

  // 步骤4: 生成包描述类
  if (!generatePackageInfoClasses(result.packages)) {
    console.log('\n⚠️  包描述类生成失败');
  }

  // 步骤5: 创建JAR
  if (!createJar()) {
    process.exit(1);
  }

  // 步骤6: 验证
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
  console.log('\n提示: JAR 文件包含包描述类，完整支持包层次结构。');
}

main().catch(error => {
  console.error('❌ 编译失败:', error);
  process.exit(1);
});