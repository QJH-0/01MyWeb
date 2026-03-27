<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()

const username = computed(() => authStore.profile?.username ?? '')

onMounted(async () => {
  await authStore.initAuth()
})
</script>

<template>
  <div>
    <header class="topbar">
      <router-link to="/" class="brand">MyWeb</router-link>
      <nav class="topnav">
        <router-link to="/">首页</router-link>
        <router-link v-if="authStore.isAuthenticated" to="/profile">账户中心</router-link>
        <router-link v-else to="/login">登录/注册</router-link>
      </nav>
      <p class="whoami">{{ username ? `当前用户：${username}` : '未登录' }}</p>
    </header>
    <router-view />
  </div>
</template>
