<script setup lang="ts">
/**
 * 与“硬错误”区分：提示当前为降级/离线渲染，避免用户误以为写操作已成功持久化。
 */
defineProps<{
  details?: string
}>()

defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="state-card state-card--error" role="status" aria-live="polite">
    <div class="state-icon" aria-hidden="true">!</div>
    <div class="state-body">
      <p class="state-title">后端连接失败</p>
      <p class="state-hint">已使用默认内容渲染页面（展示可用，写操作不会误报成功）。</p>
      <p v-if="details" class="state-hint">{{ details }}</p>
      <div class="action-row">
        <button class="ghost-btn" type="button" @click="$emit('retry')">重试连接</button>
      </div>
    </div>
  </div>
</template>

