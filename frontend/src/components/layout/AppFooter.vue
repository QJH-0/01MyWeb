<script setup lang="ts">
/** 页脚版权年使用运行时年，避免每年手动改模板。 */
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

