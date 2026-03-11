const data = require('./library-data-v2.json');

function findNode(nodes, name, path = '') {
  for (const node of nodes) {
    const currentPath = path ? `${path}/${node.name}` : node.name;
    if (node.name === name) {
      return { node, path: currentPath };
    }
    if (node.children) {
      const found = findNode(node.children, name, currentPath);
      if (found) return found;
    }
  }
  return null;
}

console.log('=== Searching for PID_Controller ===\n');

const result = findNode([data], 'PID_Controller');
if (result) {
  const { node, path } = result;
  console.log('✅ PID_Controller found!');
  console.log('Path:', path);
  console.log('Type:', node.type);
  console.log('Has content:', node.content ? 'Yes' : 'No');
  if (node.content) {
    console.log('Content length:', node.content.length);
    console.log('Has end statement:', node.content.includes('end PID_Controller'));
    console.log('\nFirst 100 chars:');
    console.log(node.content.substring(0, 100));
    console.log('\nLast 100 chars:');
    console.log(node.content.slice(-100));
  }
} else {
  console.log('❌ PID_Controller not found');

  // 尝试搜索 Examples package
  console.log('\n=== Searching for Examples package ===\n');
  const examples = findNode([data], 'Examples');
  if (examples) {
    const { node, path } = examples;
    console.log('✅ Examples found!');
    console.log('Path:', path);
    console.log('Type:', node.type);
    console.log('Children count:', node.children ? node.children.length : 0);
    if (node.children && node.children.length > 0) {
      console.log('\nFirst 10 children:');
      node.children.slice(0, 10).forEach(child => {
        console.log(`  - ${child.type} ${child.name} (has content: ${child.content ? 'Yes' : 'No'})`);
      });
    }
  } else {
    console.log('❌ Examples not found');
  }
}