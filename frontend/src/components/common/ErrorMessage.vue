<script setup lang="ts">
/** 可恢复错误：`retry` 事件把重试逻辑留给父组件（通常重新拉取接口）。 */
defineProps<{
  title?: string
  message: string
}>()

defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="state-card state-card--error" role="alert">
    <div class="state-icon" aria-hidden="true">!</div>
    <div class="state-body">
      <p class="state-title">{{ title ?? '加载失败' }}</p>
      <p class="state-hint">{{ message }}</p>
      <div class="action-row">
        <button class="ghost-btn" type="button" @click="$emit('retry')">重试</button>
      </div>
    </div>
  </div>
</template>

