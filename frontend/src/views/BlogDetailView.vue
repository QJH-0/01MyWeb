<template>
  <section class="section">
    <h1>文章详情（占位）</h1>
    <p>当前文章 ID：{{ route.params.id }}</p>
    <p>Markdown 渲染将在 M3 实现。</p>
  </section>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped lang="scss">
.section {
  padding: 28px 20px;
  max-width: 1100px;
  margin: 0 auto;
}
</style>

