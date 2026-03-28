<script setup lang="ts">
/** 兜底 404，对应 history 模式的 catch-all 路由。 */
</script>

<template>
  <main class="page">
    <section class="card">
      <h1>页面未找到</h1>
      <p class="desc">你访问的页面不存在或已被移动。</p>
      <div class="action-row">
        <router-link to="/" class="submit-btn">返回首页</router-link>
      </div>
    </section>
  </main>
</template>

