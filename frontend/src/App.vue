<script setup lang="ts">
/**
 * 根组件：首屏从本地 token 恢复会话（拉取 `/api/auth/me`），实际布局由 AppLayout 承担。
 */
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth'
import AppLayout from './components/layout/AppLayout.vue'

const authStore = useAuthStore()

onMounted(async () => {
  await authStore.initAuth()
})
</script>

<template>
  <AppLayout />
</template>
