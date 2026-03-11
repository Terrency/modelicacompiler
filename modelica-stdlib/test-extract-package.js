const fs = require('fs');
const path = require('path');

// 读取 package.mo 文件
const packagePath = './Modelica/Blocks/package.mo';
const packageContent = fs.readFileSync(packagePath, 'utf-8');

// 提取嵌套包的内容
function extractNestedPackage(code, packageName) {
  // 移除注释
  const cleanCode = code.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

  // 查找嵌套包的开始
  const packageStartPattern = new RegExp('package\\s+' + packageName + '\\b[^;]*;', 'g');
  const match = packageStartPattern.exec(cleanCode);

  if (!match) return null;

  console.log('Package start found at index:', match.index);
  console.log('Match text:', match[0]);

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
      console.log('Depth increased to:', depth, 'at position:', i, 'keyword:', keywordMatch[0]);
      i += keywordMatch[0].length - 1; // 跳过匹配的部分
      continue;
    }

    // 检查是否遇到 end
    if (cleanCode.substring(i, i + 3) === 'end') {
      depth--;
      console.log('Depth decreased to:', depth, 'at position:', i, 'text:', cleanCode.substring(i, i + 20));
      if (depth === 0) {
        // 找到匹配的 end
        endIndex = i;
        break;
      }
    }
  }

  const extracted = cleanCode.substring(startIndex, endIndex).trim();
  console.log('\nExtracted content length:', extracted.length);
  console.log('First 100 chars:', extracted.substring(0, 100));
  console.log('Last 100 chars:', extracted.slice(-100));

  return extracted;
}

console.log('=== Testing extractNestedPackage for Examples ===\n');
const examplesContent = extractNestedPackage(packageContent, 'Examples');

if (examplesContent) {
  console.log('\n✅ Success! Length:', examplesContent.length);
  console.log('Has "end Examples":', examplesContent.includes('end Examples'));
} else {
  console.log('\n❌ Failed to extract Examples');
}