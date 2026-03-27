<script setup lang="ts">
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

