#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('=== PID_Controller 数据生成诊断 ===\n');

// 读取源文件
const packagePath = './Modelica/Blocks/package.mo';
const packageContent = fs.readFileSync(packagePath, 'utf-8');

console.log('1. 检查源文件');
console.log('   文件长度:', packageContent.length);
console.log('   包含 PID_Controller:', packageContent.includes('model PID_Controller'));
console.log('   包含 end PID_Controller:', packageContent.includes('end PID_Controller'));

// 移除注释
const cleanCode = packageContent.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

console.log('\n2. 检查 Examples package');
const examplesMatch = cleanCode.match(/package\s+Examples\b[^;]*;/);
if (examplesMatch) {
  console.log('   ✅ Examples package 声明找到');
  console.log('   声明:', examplesMatch[0].replace(/\n/g, ' ').substring(0, 80));
} else {
  console.log('   ❌ Examples package 声明未找到');
}

// 查找 Examples package 的完整内容
console.log('\n3. 提取 Examples package 完整内容');
const examplesStartMatch = cleanCode.match(/package\s+Examples\b[^;]*;/);
if (examplesStartMatch) {
  let startIndex = examplesStartMatch.index + examplesStartMatch[0].length;
  let depth = 1;
  let endIndex = startIndex;

  for (let i = startIndex; i < cleanCode.length; i++) {
    const keywordMatch = cleanCode.substring(i).match(/^(package|class|model|function|record|block|connector)\s+\w/);
    if (keywordMatch) {
      depth++;
      i += keywordMatch[0].length - 1;
      continue;
    }

    if (cleanCode.substring(i, i + 3) === 'end') {
      const endMatch = cleanCode.substring(i).match(/^end\s+Examples\s*;/);
      if (endMatch) {
        endIndex = i + endMatch[0].length;
        console.log('   ✅ 找到 end Examples');
        console.log('   位置:', i);
        break;
      } else {
        depth--;
      }
    }
  }

  const examplesContent = cleanCode.substring(startIndex, endIndex).trim();
  console.log('   Examples 内容长度:', examplesContent.length);
  console.log('   包含 PID_Controller:', examplesContent.includes('model PID_Controller'));
  console.log('   包含 end PID_Controller:', examplesContent.includes('end PID_Controller'));

  // 在 Examples 内容中查找 PID_Controller
  console.log('\n4. 在 Examples 中查找 PID_Controller');
  const pidMatch = examplesContent.match(/model\s+PID_Controller/);
  if (pidMatch) {
    console.log('   ✅ 找到 PID_Controller 定义');

    // 提取 PID_Controller 的完整内容
    const pidStartIndex = pidMatch.index;
    let pidDepth = 0;
    let pidEndIndex = pidStartIndex;

    for (let i = pidStartIndex; i < examplesContent.length; i++) {
      const keywordMatch = examplesContent.substring(i).match(/^(package|class|model|function|record|block|connector)\s+[A-Z]/);
      if (keywordMatch) {
        if (i > pidStartIndex) {
          pidDepth++;
        }
        i += keywordMatch[0].length - 1;
        continue;
      }

      if (examplesContent.substring(i, i + 3) === 'end') {
        const endMatch = examplesContent.substring(i).match(/^end\s+PID_Controller\s*;/);
        if (endMatch) {
          pidEndIndex = i + endMatch[0].length;
          console.log('   ✅ 找到 end PID_Controller');
          console.log('   位置:', i);
          break;
        } else {
          pidDepth--;
        }
      }
    }

    const pidContent = examplesContent.substring(pidStartIndex, pidEndIndex).trim();
    console.log('   PID_Controller 内容长度:', pidContent.length);
    console.log('   包含 end PID_Controller:', pidContent.includes('end PID_Controller'));
    console.log('\n   前 100 字符:');
    console.log('   ', pidContent.substring(0, 100));
    console.log('\n   后 100 字符:');
    console.log('   ', pidContent.slice(-100));
  } else {
    console.log('   ❌ 未找到 PID_Controller 定义');
  }
}

console.log('\n=== 诊断完成 ===');