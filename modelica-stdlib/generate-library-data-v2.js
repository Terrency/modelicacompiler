#!/usr/bin/env node
/**
 * 从 Modelica Standard Library 生成前端数据格式（版本 2）
 *
 * 正确解析 Modelica 的包层次结构：
 * - 解析 package.mo 中的嵌套定义
 * - 解析 package.order 文件
 * - 合并独立 .mo 文件
 */

const fs = require('fs');
const path = require('path');

const STDLIB_ROOT = path.join(__dirname, 'Modelica');
const OUTPUT_FILE = path.join(__dirname, 'library-data-v2.json');

/**
 * 解析 Modelica 代码，提取顶层定义
 */
function parseModelicaCode(code, packageName) {
  const items = [];

  // 移除注释
  code = code.replace(/\/\/.*$/gm, '');
  code = code.replace(/\/\*[\s\S]*?\*\//g, '');

  // 移除字符串字面量
  code = code.replace(/"[^"]*"/g, '');

  // 匹配顶层 package/class/model/function/record/block/connector 定义
  // 必须在行首或有明确的分隔符
  const patterns = [
    /(?:^|[;\n])\s*(?:public\s+|protected\s+)?(?:partial\s+|encapsulated\s+)?(package|class|model|function|record|block|connector|type|operator|operator\s+function|operator\s+record)\s+([A-Z][a-zA-Z0-9_]*)/gm,
  ];

  const found = new Set();

  for (const pattern of patterns) {
    let match;
    while ((match = pattern.exec(code)) !== null) {
      const type = match[1];
      const name = match[2];
      if (!found.has(name)) {
        found.add(name);
        items.push({ name, type });
      }
    }
  }

  return items;
}

/**
 * 提取嵌套定义的完整代码（包括定义语句和 end 语句）
 */
function extractNestedDefinition(code, definitionName, definitionType) {
  // 移除注释
  const cleanCode = code.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

  // 查找定义的开始
  const definitionStartPattern = new RegExp(
    `(?:^|[;\\n])\\s*(?:public\\s+|protected\\s+)?(?:partial\\s+|encapsulated\\s+)?${definitionType}\\s+${definitionName}\\b[^;]*;`,
    'g'
  );
  const match = definitionStartPattern.exec(cleanCode);

  if (!match) return null;

  // 找到定义声明的开始位置
  let startIndex = match.index;
  // 如果匹配包含了前面的分隔符，调整起始位置
  const firstChar = cleanCode[startIndex];
  if (firstChar === ';' || firstChar === '\n') {
    startIndex++;
  }

  // 找到对应的 end 语句
  let depth = 0; // 初始深度为 0，因为我们已经在嵌套上下文中
  let endIndex = startIndex + match[0].length;

  for (let i = endIndex; i < cleanCode.length; i++) {
    // 检查是否进入新的 package/class/model 等
    const keywordMatch = cleanCode.substring(i).match(
      new RegExp(`^(?:package|class|model|function|record|block|connector|type|operator)\\s+[A-Z]`)
    );
    if (keywordMatch) {
      depth++;
      i += keywordMatch[0].length - 1; // 跳过匹配的部分
      continue;
    }

    // 检查是否遇到 end
    if (cleanCode.substring(i, i + 3) === 'end') {
      // 检查 end 后面是否跟着当前定义名
      const endMatch = cleanCode.substring(i).match(new RegExp(`^end\\s+${definitionName}\\s*;`));
      if (endMatch && depth === 0) {
        // 这是我们需要的 end，继续找到分号
        let semicolonIndex = cleanCode.indexOf(';', i);
        if (semicolonIndex !== -1) {
          endIndex = semicolonIndex + 1;
        } else {
          endIndex = i + endMatch[0].length;
        }
        break;
      } else if (!endMatch) {
        // 这是嵌套定义的 end，减少深度
        depth--;
      }
    }
  }

  return cleanCode.substring(startIndex, endIndex).trim();
}

/**
 * 提取嵌套包的内容（只提取包内部的定义，不包括 package 和 end 语句）
 */
function extractNestedPackage(code, packageName) {
  // 移除注释
  const cleanCode = code.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

  // 查找嵌套包的开始
  const packageStartPattern = new RegExp('package\\s+' + packageName + '\\b[^;]*;', 'g');
  const match = packageStartPattern.exec(cleanCode);

  if (!match) return null;

  // 找到包声明的结束位置（分号后）
  let startIndex = match.index + match[0].length;

  // 找到对应的 end 语句
  let depth = 1; // 已经进入了一个 package
  let endIndex = startIndex;

  for (let i = startIndex; i < cleanCode.length; i++) {
    // 检查是否进入新的 package/class/model 等
    const keywordMatch = cleanCode.substring(i).match(/^(package|class|model|function|record|block|connector)\s+\w/);
    if (keywordMatch) {
      depth++;
      i += keywordMatch[0].length - 1; // 跳过匹配的部分
      continue;
    }

    // 检查是否遇到 end
    if (cleanCode.substring(i, i + 3) === 'end') {
      // 检查 end 后面是否跟着当前包名
      const endMatch = cleanCode.substring(i).match(new RegExp(`^end\\s+${packageName}\\s*;`));
      if (endMatch) {
        // 这是我们需要的 end
        endIndex = i;
        break;
      } else {
        // 这是嵌套定义的 end，减少深度
        depth--;
      }
    }
  }

  return cleanCode.substring(startIndex, endIndex).trim();
}

/**
 * 解析独立 .mo 文件的类型
 */
function parseMoFileType(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf-8');
    // 移除注释
    const cleanContent = content.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

    // 匹配顶层定义（within 语句后的第一个定义）
    const match = cleanContent.match(/within\s+[\w.]+;\s*(?:public\s+|protected\s+)?(?:partial\s+|encapsulated\s+)?(package|class|model|function|record|block|connector|type|operator|operator\s+function|operator\s+record)\s+(\w+)/);

    if (match) {
      return { type: match[1], name: match[2] };
    }
  } catch (error) {
    console.error(`Error parsing ${filePath}:`, error.message);
  }

  return { type: 'file', name: path.basename(filePath, '.mo') };
}

/**
 * 读取 package.order 文件
 */
function readPackageOrder(dirPath) {
  const orderFile = path.join(dirPath, 'package.order');
  if (fs.existsSync(orderFile)) {
    return fs.readFileSync(orderFile, 'utf-8')
      .split('\n')
      .map(line => line.trim())
      .filter(line => line && !line.startsWith('#'));
  }
  return null;
}

/**
 * 递归扫描包
 */
function scanPackage(dirPath, basePath) {
  const packageMoPath = path.join(dirPath, 'package.mo');

  if (!fs.existsSync(packageMoPath)) {
    return null;
  }

  const packageContent = fs.readFileSync(packageMoPath, 'utf-8');
  const packageName = path.basename(dirPath);

  // 解析 package.mo 中的嵌套定义
  const nestedItems = parseModelicaCode(packageContent, packageName);

  // 读取 package.order
  const packageOrder = readPackageOrder(dirPath);

  // 获取目录中的所有条目
  const entries = fs.readdirSync(dirPath, { withFileTypes: true });

  // 收集所有子项
  const children = [];
  const processedNames = new Set();

  // 如果有 package.order，按照顺序处理
  if (packageOrder) {
    for (const name of packageOrder) {
      if (processedNames.has(name)) continue;
      processedNames.add(name);

      // 检查是否是嵌套定义
      const nestedItem = nestedItems.find(item => item.name === name);
      if (nestedItem) {
        // 检查是否有对应的目录
        const subDirPath = path.join(dirPath, name);
        if (fs.existsSync(subDirPath) && fs.statSync(subDirPath).isDirectory()) {
          // 这是一个子包
          const childPackage = scanPackage(subDirPath, `${basePath}/${name}`);
          if (childPackage) {
            children.push(childPackage);
          }
        } else {
          // 这是一个嵌套定义的类/函数/包
          if (nestedItem.type === 'package') {
            // 如果是嵌套的包，需要进一步解析其内容
            const nestedPackageContent = extractNestedPackage(packageContent, name);
            const nestedPackageChildren = [];

            if (nestedPackageContent) {
              const nestedItemsInPackage = parseModelicaCode(nestedPackageContent, name);
              for (const item of nestedItemsInPackage) {
                // 提取嵌套定义的完整代码内容
                const itemContent = extractNestedDefinition(nestedPackageContent, item.name, item.type);

                nestedPackageChildren.push({
                  path: `${basePath}/${name}/${item.name}`,
                  name: item.name,
                  type: item.type,
                  content: itemContent,
                  isNested: true,
                  isLibrary: true,
                  children: []
                });
              }
            }

            children.push({
              path: `${basePath}/${name}`,
              name: name,
              type: 'package',
              content: null, // 包节点不包含内容，只是容器
              isNested: true,
              isLibrary: true,
              children: nestedPackageChildren
            });
          } else {
            // 其他类型的嵌套定义（model, function, class 等）
            // 提取完整的代码内容
            const nestedContent = extractNestedDefinition(packageContent, name, nestedItem.type);

            children.push({
              path: `${basePath}/${name}`,
              name: name,
              type: nestedItem.type,
              content: nestedContent,
              isNested: true,
              isLibrary: true,
              children: []
            });
          }
        }
      } else {
        // 检查是否是独立的 .mo 文件
        const moFile = path.join(dirPath, `${name}.mo`);
        if (fs.existsSync(moFile)) {
          // 解析 .mo 文件的真实类型
          const fileInfo = parseMoFileType(moFile);
          const moContent = fs.readFileSync(moFile, 'utf-8');

          // 如果是 package，递归扫描其内容
          if (fileInfo.type === 'package') {
            const nestedInMo = parseModelicaCode(moContent, name);
            const subChildren = [];

            for (const nestedItem of nestedInMo) {
              // 提取嵌套定义的完整代码内容
              const itemContent = extractNestedDefinition(moContent, nestedItem.name, nestedItem.type);

              subChildren.push({
                path: `${basePath}/${name}/${nestedItem.name}`,
                name: nestedItem.name,
                type: nestedItem.type,
                content: nestedItem.type === 'package' ? null : itemContent, // 包节点不包含内容
                isNested: true,
                isLibrary: true,
                children: []
              });
            }

            children.push({
              path: `${basePath}/${name}`,
              name: name,
              type: 'package',
              content: null, // 包节点不包含内容，只是容器
              isNested: false,
              isLibrary: true,
              children: subChildren
            });
          } else {
            // 非 package 类型
            children.push({
              path: `${basePath}/${name}`,
              name: name,
              type: fileInfo.type,
              content: moContent,
              isNested: false,
              isLibrary: true,
              children: []
            });
          }
        } else {
          // 可能是子目录
          const subDirPath = path.join(dirPath, name);
          if (fs.existsSync(subDirPath) && fs.statSync(subDirPath).isDirectory()) {
            const childPackage = scanPackage(subDirPath, `${basePath}/${name}`);
            if (childPackage) {
              children.push(childPackage);
            }
          }
        }
      }
    }
  }

  // 处理没有在 package.order 中的项
  for (const entry of entries) {
    const name = entry.name;

    if (name === 'package.mo' || name === 'package.order') continue;
    if (processedNames.has(name.replace('.mo', ''))) continue;

    if (entry.isDirectory()) {
      const subDirPath = path.join(dirPath, name);
      const childPackage = scanPackage(subDirPath, `${basePath}/${name}`);
      if (childPackage) {
        children.push(childPackage);
      }
    } else if (entry.isFile() && name.endsWith('.mo')) {
      const itemName = name.replace('.mo', '');
      const moFilePath = path.join(dirPath, name);

      // 解析 .mo 文件的真实类型
      const fileInfo = parseMoFileType(moFilePath);

      // 如果是 package，递归扫描其内容
      if (fileInfo.type === 'package') {
        const moContent = fs.readFileSync(moFilePath, 'utf-8');
        const nestedItems = parseModelicaCode(moContent, itemName);

        const subChildren = [];
        const processedSubNames = new Set();

        // 处理嵌套定义
        for (const nestedItem of nestedItems) {
          processedSubNames.add(nestedItem.name);

          // 检查是否有对应的子目录
          const subDirPath = path.join(dirPath, itemName);
          if (fs.existsSync(subDirPath) && fs.statSync(subDirPath).isDirectory()) {
            const nestedDir = path.join(subDirPath, nestedItem.name);
            if (fs.existsSync(nestedDir) && fs.statSync(nestedDir).isDirectory()) {
              const childPackage = scanPackage(nestedDir, `${basePath}/${itemName}/${nestedItem.name}`);
              if (childPackage) {
                subChildren.push(childPackage);
                continue;
              }
            }
          }

          // 否则作为嵌套定义
          subChildren.push({
            path: `${basePath}/${itemName}/${nestedItem.name}`,
            name: nestedItem.name,
            type: nestedItem.type,
            content: null,
            isNested: true,
            isLibrary: true,
            children: []
          });
        }

        children.push({
          path: `${basePath}/${itemName}`,
          name: itemName,
          type: 'package',
          content: null, // 包节点不包含内容，只是容器
          isNested: false,
          isLibrary: true,
          children: subChildren
        });
      } else {
        // 非 package 类型，直接作为文件
        children.push({
          path: `${basePath}/${itemName}`,
          name: itemName,
          type: fileInfo.type,
          content: fs.readFileSync(moFilePath, 'utf-8'),
          isNested: false,
          isLibrary: true,
          children: []
        });
      }
    }
  }

  return {
    path: basePath,
    name: packageName,
    type: 'package',
    content: null, // 包节点不包含内容，只是容器
    isNested: false,
    isLibrary: true,
    children: children
  };
}

/**
 * 主函数
 */
function main() {
  console.log('Scanning Modelica Standard Library (Version 2)...');
  console.log('Root:', STDLIB_ROOT);

  const startTime = Date.now();
  const libraryData = scanPackage(STDLIB_ROOT, 'Modelica');
  const endTime = Date.now();

  console.log(`\nScan completed in ${endTime - startTime}ms`);

  // 统计信息
  let totalPackages = 0;
  let totalClasses = 0;
  let totalFunctions = 0;
  let totalFiles = 0;

  function countItems(node) {
    if (!node) return;

    if (node.type === 'package') {
      totalPackages++;
    } else if (node.type === 'class' || node.type === 'model' || node.type === 'record' || node.type === 'block') {
      totalClasses++;
    } else if (node.type === 'function') {
      totalFunctions++;
    } else if (node.type === 'file') {
      totalFiles++;
    }

    if (node.children) {
      node.children.forEach(countItems);
    }
  }

  countItems(libraryData);

  console.log(`Total packages: ${totalPackages}`);
  console.log(`Total classes/models: ${totalClasses}`);
  console.log(`Total functions: ${totalFunctions}`);
  console.log(`Total files: ${totalFiles}`);

  // 写入输出文件
  console.log(`\nWriting to ${OUTPUT_FILE}...`);
  fs.writeFileSync(OUTPUT_FILE, JSON.stringify(libraryData, null, 2));

  const fileSizeMB = (fs.statSync(OUTPUT_FILE).size / 1024 / 1024).toFixed(2);
  console.log(`Output file size: ${fileSizeMB} MB`);
  console.log('Done!');
}

main();