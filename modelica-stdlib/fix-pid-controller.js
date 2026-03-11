#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('=== 修复 library-data-v2.json 中的 PID_Controller ===\n');

// 读取源文件
const packagePath = './Modelica/Blocks/package.mo';
const packageContent = fs.readFileSync(packagePath, 'utf-8');

// 提取 PID_Controller 的完整内容
const cleanCode = packageContent.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

// 查找 PID_Controller
const pidMatch = cleanCode.match(/model\s+PID_Controller\b[^;]*;/);
if (!pidMatch) {
  console.log('❌ 未找到 PID_Controller');
  process.exit(1);
}

const pidStartIndex = pidMatch.index;
let depth = 0;
let pidEndIndex = pidStartIndex;

for (let i = pidStartIndex; i < cleanCode.length; i++) {
  const keywordMatch = cleanCode.substring(i).match(/^(package|class|model|function|record|block|connector)\s+[A-Z]/);
  if (keywordMatch) {
    if (i > pidStartIndex) {
      depth++;
    }
    i += keywordMatch[0].length - 1;
    continue;
  }

  if (cleanCode.substring(i, i + 3) === 'end') {
    const endMatch = cleanCode.substring(i).match(/^end\s+PID_Controller\s*;/);
    if (endMatch) {
      pidEndIndex = i + endMatch[0].length;
      break;
    } else {
      depth--;
    }
  }
}

const pidContent = cleanCode.substring(pidStartIndex, pidEndIndex).trim();

console.log('提取的 PID_Controller 内容长度:', pidContent.length);
console.log('包含 end PID_Controller:', pidContent.includes('end PID_Controller'));

// 读取 library-data-v2.json
const dataPath = './library-data-v2.json';
const data = JSON.parse(fs.readFileSync(dataPath, 'utf-8'));

// 查找 PID_Controller 节点
function findAndUpdateNode(nodes) {
  for (const node of nodes) {
    if (node.name === 'PID_Controller') {
      console.log('\n✅ 找到 PID_Controller 节点');
      console.log('   旧内容长度:', node.content ? node.content.length : 'null');
      node.content = pidContent;
      console.log('   新内容长度:', node.content.length);
      return true;
    }
    if (node.children) {
      if (findAndUpdateNode(node.children)) {
        return true;
      }
    }
  }
  return false;
}

if (findAndUpdateNode([data])) {
  // 写回文件
  fs.writeFileSync(dataPath, JSON.stringify(data, null, 2), 'utf-8');
  console.log('\n✅ library-data-v2.json 已更新');
  console.log('文件大小:', fs.statSync(dataPath).size, 'bytes');
} else {
  console.log('\n❌ 未找到 PID_Controller 节点');
}