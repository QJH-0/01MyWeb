<script setup lang="ts">
import Card from '../components/common/Card.vue'
import Section from '../components/common/Section.vue'
import TimeLine, { type TimelineItem } from '../components/common/TimeLine.vue'

const items: TimelineItem[] = [
  {
    id: 't1',
    time: '2024 - 至今',
    title: '某公司 · 软件工程',
    description: '负责全栈研发与系统稳定性建设（占位）。'
  },
  {
    id: 't2',
    time: '2022 - 2024',
    title: '某团队 · 后端方向',
    description: '参与搜索与数据管道优化（占位）。'
  },
  {
    id: 't3',
    time: '2020 - 2022',
    title: '学习与项目积累',
    description: '打磨工程化能力与交付思维（占位）。'
  }
]
</script>

<template>
  <Section>
    <Card>
      <div class="title">经历时间线</div>
      <TimeLine :items="items" />
    </Card>
  </Section>
</template>

<style scoped lang="scss">
.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}
</style>

