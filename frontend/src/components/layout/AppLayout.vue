<script setup lang="ts">
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

