<script setup lang="ts">
export interface TimelineItem {
  time: string
  title: string
  description: string
  tags?: string[]
}

defineProps<{
  items: TimelineItem[]
}>()
</script>

<template>
  <ol class="timeline" aria-label="经历时间线">
    <li v-for="item in items" :key="`${item.time}-${item.title}`" class="timeline-item">
      <div class="dot" aria-hidden="true" />
      <div class="content">
        <div class="row">
          <p class="time">{{ item.time }}</p>
          <h3 class="title">{{ item.title }}</h3>
        </div>
        <p class="desc">{{ item.description }}</p>
        <ul v-if="item.tags?.length" class="tags" aria-label="标签">
          <li v-for="tag in item.tags" :key="tag" class="tag">{{ tag }}</li>
        </ul>
      </div>
    </li>
  </ol>
</template>

<style scoped>
.timeline {
  list-style: none;
  padding: 0;
  margin: 16px 0 0;
  display: grid;
  gap: 14px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 16px 1fr;
  gap: 12px;
  align-items: start;
}

.dot {
  width: 12px;
  height: 12px;
  margin-top: 6px;
  border-radius: 999px;
  border: 2px solid rgba(79, 116, 163, 0.55);
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(58, 94, 138, 0.18);
}

.content {
  border: 1px solid rgba(203, 216, 231, 0.85);
  background: rgba(255, 255, 255, 0.85);
  border-radius: 14px;
  padding: 14px;
}

.row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 10px;
}

.time {
  margin: 0;
  font-size: 12px;
  color: #6a7f96;
  font-weight: 700;
}

.title {
  margin: 0;
  font-size: 16px;
}

.desc {
  margin: 8px 0 0;
  color: var(--text-secondary);
}

.tags {
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(79, 116, 163, 0.06);
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}
</style>

