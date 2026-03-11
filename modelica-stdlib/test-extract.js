const fs = require('fs');
const path = require('path');

// 读取 package.mo 文件
const packagePath = './Modelica/Blocks/package.mo';
const content = fs.readFileSync(packagePath, 'utf-8');

// 提取嵌套定义的完整代码
function extractNestedDefinition(code, definitionName, definitionType) {
  // 移除注释
  const cleanCode = code.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

  console.log('Looking for:', definitionType, definitionName);

  // 查找定义的开始
  const definitionStartPattern = new RegExp(
    `(?:^|[;\\n])\\s*(?:public\\s+|protected\\s+)?(?:partial\\s+|encapsulated\\s+)?${definitionType}\\s+${definitionName}\\b[^;]*;`,
    'g'
  );
  const match = definitionStartPattern.exec(cleanCode);

  if (!match) {
    console.log('No match found');
    return null;
  }

  console.log('Match found at index:', match.index);
  console.log('Match text:', match[0]);

  // 找到定义声明的开始位置
  let startIndex = match.index;
  // 如果匹配包含了前面的分隔符，调整起始位置
  const firstChar = cleanCode[startIndex];
  if (firstChar === ';' || firstChar === '\n') {
    startIndex++;
  }

  // 找到对应的 end 语句
  let depth = 1; // 已经进入了一个定义
  let endIndex = startIndex + match[0].length;

  for (let i = endIndex; i < cleanCode.length; i++) {
    // 检查是否进入新的 package/class/model 等
    const keywordMatch = cleanCode.substring(i).match(
      new RegExp(`^(?:package|class|model|function|record|block|connector|type|operator)\\s+[A-Z]`)
    );
    if (keywordMatch) {
      depth++;
      console.log('Depth increased to:', depth, 'at position:', i, 'keyword:', keywordMatch[0]);
      i += keywordMatch[0].length - 1; // 跳过匹配的部分
      continue;
    }

    // 检查是否遇到 end
    if (cleanCode.substring(i, i + 3) === 'end') {
      // 确认这是 end 关键字（后面跟着空格或标识符）
      const afterEnd = cleanCode.substring(i + 3, i + 4);
      if (afterEnd === ' ' || afterEnd === '\t' || afterEnd === '\n' || /[A-Za-z]/.test(afterEnd)) {
        depth--;
        console.log('Depth decreased to:', depth, 'at position:', i);
        if (depth === 0) {
          // 找到匹配的 end，继续找到分号
          let semicolonIndex = cleanCode.indexOf(';', i);
          if (semicolonIndex !== -1) {
            endIndex = semicolonIndex + 1;
          } else {
            endIndex = i + 20; // 估算 end 语句的长度
          }
          break;
        }
      }
    }
  }

  const extracted = cleanCode.substring(startIndex, endIndex).trim();
  console.log('Extracted length:', extracted.length);
  console.log('First 100 chars:', extracted.substring(0, 100));
  console.log('Last 100 chars:', extracted.slice(-100));

  return extracted;
}

// 测试提取 PID_Controller
console.log('=== Testing PID_Controller extraction ===\n');
const result = extractNestedDefinition(content, 'PID_Controller', 'model');

if (result) {
  console.log('\n✅ Success! Length:', result.length);
  console.log('Has end statement:', result.includes('end PID_Controller'));
} else {
  console.log('\n❌ Failed to extract PID_Controller');
}