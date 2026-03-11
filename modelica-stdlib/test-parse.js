const fs = require('fs');
const path = require('path');

// 读取 package.mo 文件
const packagePath = './Modelica/Blocks/package.mo';
const packageContent = fs.readFileSync(packagePath, 'utf-8');

// 解析 Modelica 代码中的顶层定义
function parseModelicaCode(code, packageName) {
  const items = [];

  // 匹配顶层 package/class/model/function/record/block/connector 定义
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

// 解析 package.mo 中的嵌套定义
const nestedItems = parseModelicaCode(packageContent, 'Blocks');
console.log('Nested items in Blocks package:');
nestedItems.forEach(item => console.log('  -', item.type, item.name));

// 检查 Examples 是否在其中
const examples = nestedItems.find(item => item.name === 'Examples');
if (examples) {
  console.log('\n✅ Examples found, type:', examples.type);

  // 提取 Examples package 的内容
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
        depth--;
        if (depth === 0) {
          // 找到匹配的 end
          endIndex = i;
          break;
        }
      }
    }

    return cleanCode.substring(startIndex, endIndex).trim();
  }

  const examplesContent = extractNestedPackage(packageContent, 'Examples');
  console.log('\nExamples package content length:', examplesContent ? examplesContent.length : 'null');

  if (examplesContent) {
    // 解析 Examples 中的嵌套定义
    const examplesItems = parseModelicaCode(examplesContent, 'Examples');
    console.log('\nNested items in Examples package:');
    examplesItems.forEach(item => console.log('  -', item.type, item.name));

    // 检查 PID_Controller 是否在其中
    const pid = examplesItems.find(item => item.name === 'PID_Controller');
    if (pid) {
      console.log('\n✅ PID_Controller found in Examples, type:', pid.type);
    } else {
      console.log('\n❌ PID_Controller not found in Examples');
    }
  }
} else {
  console.log('\n❌ Examples not found');
}