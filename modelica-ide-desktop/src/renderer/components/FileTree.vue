<template>
  <div class="file-tree">
    <TreeNode
      v-for="node in nodes"
      :key="node.path"
      :node="node"
      :depth="0"
      :expanded-ids="expandedIds"
      :active-path="activePath"
      @toggle="handleToggle"
      @select="handleSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import TreeNode from './TreeNode.vue'

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
  nodes: TreeNodeData[]
  expandedIds: Set<string>
  activePath: string | null
}>()

const emit = defineEmits<{
  toggle: [node: TreeNodeData]
  select: [node: TreeNodeData]
}>()

function handleToggle(node: TreeNodeData) {
  emit('toggle', node)
}

function handleSelect(node: TreeNodeData) {
  emit('select', node)
}
</script>

<style scoped>
.file-tree {
  overflow: auto;
  flex: 1;
}
</style>