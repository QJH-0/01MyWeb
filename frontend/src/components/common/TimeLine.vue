<script setup lang="ts">
export type TimelineItem = {
  id: string
  time: string
  title: string
  description?: string
}

defineProps<{
  items: TimelineItem[]
}>()
</script>

<template>
  <div class="timeline">
    <div class="line" />
    <div v-for="item in items" :key="item.id" class="node">
      <div class="dot" />
      <div class="body">
        <div class="time">{{ item.time }}</div>
        <div class="title">{{ item.title }}</div>
        <div v-if="item.description" class="desc">{{ item.description }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.timeline {
  position: relative;
  margin-top: 10px;
  padding-left: 24px;
}

.line {
  position: absolute;
  left: 10px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: rgba(149, 165, 166, 0.4);
}

.node {
  position: relative;
  padding: 8px 0 12px;
}

.dot {
  position: absolute;
  left: -1px;
  top: 18px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid rgba(125, 157, 156, 0.9);
  background: rgba(125, 157, 156, 0.15);
}

.body {
  padding-left: 10px;
}

.time {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.title {
  font-size: 18px;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.desc {
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>

