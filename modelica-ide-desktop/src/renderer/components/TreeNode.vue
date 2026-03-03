<template>
  <div class="tree-node">
    <div
      class="node-content"
      :class="{
        active: activePath === node.path,
        'is-library': node.isLibrary,
        'is-file': hasContent
      }"
      :style="{ paddingLeft: `${depth * 16 + 8}px` }"
      @click="handleClick"
    >
      <!-- 展开/折叠图标 -->
      <span
        v-if="hasChildren"
        class="toggle-icon"
        @click.stop="handleToggle"
      >
        {{ isExpanded ? '▼' : '▶' }}
      </span>
      <span v-else class="toggle-placeholder"></span>

      <!-- 类型图标 -->
      <span class="node-icon">
        {{ getNodeIcon() }}
      </span>

      <!-- 节点名称 -->
      <span class="node-name">{{ node.name }}</span>
    </div>

    <!-- 子节点 -->
    <div v-if="hasChildren && isExpanded" class="node-children">
      <TreeNode
        v-for="child in node.children"
        :key="child.path"
        :node="child"
        :depth="depth + 1"
        :expanded-ids="expandedIds"
        :active-path="activePath"
        @toggle="(n) => emit('toggle', n)"
        @select="(n) => emit('select', n)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface TreeNodeData {
  id: string
  name: string
  type: string
  path: string
  content?: string
  children: TreeNodeData[]
  isExpanded: boolean
  isLibrary: boolean
}

const props = defineProps<{
  node: TreeNodeData
  depth: number
  expandedIds: Set<string>
  activePath: string | null
}>()

const emit = defineEmits<{
  toggle: [node: TreeNodeData]
  select: [node: TreeNodeData]
}>()

const hasChildren = computed(() => props.node.children.length > 0)
const hasContent = computed(() => props.node.content !== undefined)
const isExpanded = computed(() => props.expandedIds.has(props.node.path))

function getNodeIcon(): string {
  if (hasChildren.value) {
    // 包/目录
    if (props.node.name === 'Modelica') return '📚'
    if (props.node.name === 'Examples') return '📝'
    if (props.node.name === 'Math') return '🔢'
    if (props.node.name === 'Icons') return '🎨'
    if (props.node.name === 'Constants') return '📐'
    if (props.node.name === 'SIunits') return '📏'
    return '📁'
  }
  // 文件
  if (props.node.name === 'HelloWorld') return '👋'
  if (props.node.name === 'SimplePendulum') return '🔄'
  if (props.node.name === 'BouncingBall') return '⚽'
  if (props.node.name === 'DCMotor') return '⚡'
  if (props.node.name === 'LorenzSystem') return '🌀'
  if (props.node.isLibrary) return '📦'
  return '📄'
}

function handleToggle() {
  emit('toggle', props.node)
}

function handleClick() {
  if (hasContent.value) {
    emit('select', props.node)
  } else if (hasChildren.value) {
    handleToggle()
  }
}
</script>

<style scoped>
.tree-node {
  user-select: none;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 13px;
  border-radius: 4px;
  margin: 1px 4px;
  transition: background-color 0.15s;
}

.node-content:hover {
  background: #2a2d2e;
}

.node-content.active {
  background: #37373d;
}

.node-content.is-library .node-name {
  color: #9cdcfe;
}

.node-content.is-file .node-name {
  color: #d4d4d4;
}

.toggle-icon {
  width: 12px;
  font-size: 8px;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.toggle-icon:hover {
  color: #ccc;
}

.toggle-placeholder {
  width: 12px;
  flex-shrink: 0;
}

.node-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-children {
  /* 子节点样式 */
}
</style>