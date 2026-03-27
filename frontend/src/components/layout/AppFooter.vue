<script setup lang="ts">
const year = new Date().getFullYear()
</script>

<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <p class="app-footer__brand">MyWeb</p>
      <p class="app-footer__meta">© {{ year }} · Built with Vue + Spring Boot</p>
    </div>
  </footer>
</template>

