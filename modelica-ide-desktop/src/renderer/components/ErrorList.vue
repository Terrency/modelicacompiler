<template>
  <div class="error-panel">
    <div v-if="errors.length === 0" class="empty-state">
      <span class="icon">✓</span>
      No errors
    </div>
    <div v-else class="error-list">
      <div
        v-for="(error, index) in errors"
        :key="index"
        class="error-item"
        @click="$emit('select', error)"
      >
        <span class="error-icon">⚠</span>
        <div class="error-content">
          <div class="error-message">{{ error.message }}</div>
          <div v-if="error.location" class="error-location">
            Line {{ error.location.line }}, Column {{ error.location.column }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  errors: Array<{
    message: string
    location?: { line: number; column: number }
  }>
}>()

defineEmits<{
  select: [error: any]
}>()
</script>

<style scoped>
.error-panel {
  height: 100%;
  overflow: auto;
  background: #1e1e1e;
  color: #d4d4d4;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  color: #89d185;
}

.error-list {
  padding: 8px;
}

.error-item {
  display: flex;
  gap: 8px;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.error-item:hover {
  background: #2d2d2d;
}

.error-icon {
  color: #f48771;
  flex-shrink: 0;
}

.error-content {
  flex: 1;
  min-width: 0;
}

.error-message {
  color: #f48771;
  font-size: 12px;
}

.error-location {
  color: #666;
  font-size: 11px;
  margin-top: 2px;
}
</style>