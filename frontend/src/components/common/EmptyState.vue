<script setup lang="ts">
/** 列表/区块为空时的统一空态展示，避免各页重复写样式与文案结构。 */
withDefaults(
  defineProps<{
    title?: string
    hint?: string
  }>(),
  {
    title: '暂无内容',
    hint: '稍后再来看看',
  },
)
</script>

<template>
  <div class="state-card state-card--empty">
    <div class="state-icon" aria-hidden="true">○</div>
    <div class="state-body">
      <p class="state-title">{{ title }}</p>
      <p class="state-hint">{{ hint }}</p>
    </div>
  </div>
</template>

