const data = require('./library-data-v2.json');

const findNode = (nodes, path) => {
  for (const node of nodes) {
    if (node.path === path) return node;
    if (node.children) {
      const found = findNode(node.children, path);
      if (found) return found;
    }
  }
  return null;
};

// 查找 Blocks.Examples
const blocksExamples = findNode([data], 'Modelica.Blocks.Examples');
if (blocksExamples) {
  console.log('Blocks.Examples:');
  console.log('  path:', blocksExamples.path);
  console.log('  type:', blocksExamples.type);
  console.log('  has content:', blocksExamples.content ? 'yes' : 'no');
  console.log('  children count:', blocksExamples.children?.length || 0);
  console.log('  children:');
  blocksExamples.children?.forEach(child => {
    console.log('    -', child.name, '(' + child.type + ')', child.content ? 'has content' : 'no content');
  });
}

// 查找 Blocks.Continuous
const blocksContinuous = findNode([data], 'Modelica.Blocks.Continuous');
if (blocksContinuous) {
  console.log('\nBlocks.Continuous:');
  console.log('  path:', blocksContinuous.path);
  console.log('  type:', blocksContinuous.type);
  console.log('  has content:', blocksContinuous.content ? 'yes' : 'no');
  console.log('  children count:', blocksContinuous.children?.length || 0);
  console.log('  children (first 5):');
  blocksContinuous.children?.slice(0, 5).forEach(child => {
    console.log('    -', child.name, '(' + child.type + ')', child.content ? 'has content' : 'no content');
  });
}

// 查找 PID_Controller
const pidController = findNode([data], 'Modelica.Blocks.Examples.PID_Controller');
if (pidController) {
  console.log('\nPID_Controller:');
  console.log('  path:', pidController.path);
  console.log('  type:', pidController.type);
  console.log('  has content:', pidController.content ? 'yes' : 'no');
  console.log('  children count:', pidController.children?.length || 0);
  if (pidController.content) {
    console.log('  content preview:', pidController.content.substring(0, 100) + '...');
  }
}