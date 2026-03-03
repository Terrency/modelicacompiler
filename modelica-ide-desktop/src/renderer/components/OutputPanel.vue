<template>
  <div class="output-panel">
    <div v-if="messages.length === 0" class="empty-state">
      No output yet
    </div>
    <div v-else class="output-list">
      <div
        v-for="(message, index) in messages"
        :key="index"
        class="output-item"
        :class="message.type"
      >
        <span class="timestamp">{{ message.timestamp }}</span>
        <span class="message">{{ message.text }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  messages: Array<{ type: string; text: string; timestamp: string }>
}>()
</script>

<style scoped>
.output-panel {
  height: 100%;
  overflow: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', monospace;
  font-size: 12px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.output-list {
  padding: 8px;
}

.output-item {
  display: flex;
  gap: 8px;
  padding: 2px 0;
}

.output-item.error {
  color: #f48771;
}

.output-item.warning {
  color: #cca700;
}

.output-item.success {
  color: #89d185;
}

.timestamp {
  color: #666;
  flex-shrink: 0;
}
</style>