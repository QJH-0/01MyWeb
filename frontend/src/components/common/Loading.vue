<script setup lang="ts">
/** 异步加载占位；`role="status"` + `aria-live` 便于读屏感知加载中。 */
withDefaults(
  defineProps<{
    title?: string
    hint?: string
  }>(),
  {
    title: '正在加载…',
    hint: '请稍候',
  },
)
</script>

<template>
  <div class="state-card" role="status" aria-live="polite">
    <div class="spinner" aria-hidden="true" />
    <div>
      <p class="state-title">{{ title }}</p>
      <p class="state-hint">{{ hint }}</p>
    </div>
  </div>
</template>

