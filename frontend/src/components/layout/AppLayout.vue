<script setup lang="ts">
/** 站点壳层：顶栏 + 主内容区 + 页脚，保证全站导航一致。 */
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'
</script>

<template>
  <div class="app-shell">
    <AppHeader />
    <main class="app-main" role="main">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>

