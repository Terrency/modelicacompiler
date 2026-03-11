#!/usr/bin/env node
/**
 * 从 Modelica Standard Library 生成前端数据格式
 *
 * 扫描 modelica-stdlib/Modelica 目录，生成 JSON 格式的库数据
 */

const fs = require('fs');
const path = require('path');

const STDLIB_ROOT = path.join(__dirname, 'Modelica');
const OUTPUT_FILE = path.join(__dirname, 'library-data.json');

/**
 * 递归扫描目录，构建库数据
 */
function scanLibrary(dirPath, basePath = '') {
  const items = [];

  try {
    const entries = fs.readdirSync(dirPath, { withFileTypes: true });

    // 先处理 package.mo
    const packageMoPath = path.join(dirPath, 'package.mo');
    let packageContent = null;

    if (fs.existsSync(packageMoPath)) {
      packageContent = fs.readFileSync(packageMoPath, 'utf-8');
    }

    // 处理子目录
    for (const entry of entries) {
      if (!entry.isDirectory()) continue;

      const subDirName = entry.name;
      const subDirPath = path.join(dirPath, subDirName);
      const subBasePath = basePath ? `${basePath}/${subDirName}` : subDirName;

      // 检查是否有 package.mo
      const subPackageMo = path.join(subDirPath, 'package.mo');
      const hasPackageMo = fs.existsSync(subPackageMo);

      if (hasPackageMo) {
        // 这是一个包
        items.push({
          path: subBasePath,
          name: subDirName,
          content: fs.readFileSync(subPackageMo, 'utf-8'),
          isLibrary: true,
          children: scanLibrary(subDirPath, subBasePath)
        });
      } else {
        // 可能包含其他 .mo 文件
        const subItems = scanLibrary(subDirPath, subBasePath);
        if (subItems.length > 0) {
          items.push(...subItems);
        }
      }
    }

    // 处理当前目录下的 .mo 文件（非 package.mo）
    for (const entry of entries) {
      if (!entry.isFile() || !entry.name.endsWith('.mo')) continue;
      if (entry.name === 'package.mo') continue;

      const moName = entry.name.replace('.mo', '');
      const moPath = path.join(dirPath, entry.name);
      const moBasePath = basePath ? `${basePath}/${moName}` : moName;

      items.push({
        path: moBasePath,
        name: moName,
        content: fs.readFileSync(moPath, 'utf-8'),
        isLibrary: true,
        children: []
      });
    }

  } catch (error) {
    console.error(`Error scanning ${dirPath}:`, error.message);
  }

  return items;
}

/**
 * 主函数
 */
function main() {
  console.log('Scanning Modelica Standard Library...');
  console.log('Root:', STDLIB_ROOT);

  const startTime = Date.now();
  const libraryData = scanLibrary(STDLIB_ROOT, 'Modelica');
  const endTime = Date.now();

  console.log(`\nScan completed in ${endTime - startTime}ms`);
  console.log(`Total items: ${libraryData.length}`);

  // 统计信息
  let totalFiles = 0;
  let totalPackages = 0;

  function countItems(items) {
    for (const item of items) {
      if (item.content) {
        totalFiles++;
        if (item.children && item.children.length > 0) {
          totalPackages++;
        }
      }
      if (item.children) {
        countItems(item.children);
      }
    }
  }

  countItems(libraryData);

  console.log(`Total files: ${totalFiles}`);
  console.log(`Total packages: ${totalPackages}`);

  // 写入输出文件
  console.log(`\nWriting to ${OUTPUT_FILE}...`);
  fs.writeFileSync(OUTPUT_FILE, JSON.stringify(libraryData, null, 2));

  const fileSizeMB = (fs.statSync(OUTPUT_FILE).size / 1024 / 1024).toFixed(2);
  console.log(`Output file size: ${fileSizeMB} MB`);
  console.log('Done!');
}

main();