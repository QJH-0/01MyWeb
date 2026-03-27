<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const username = computed(() => authStore.profile?.username ?? '')

const links = computed(() => {
  const mainLinks = [
    { to: '/', label: '首页' },
    { to: '/about', label: '关于' },
    { to: '/experience', label: '经历' },
    { to: '/contact', label: '联系' },
    { to: '/projects', label: '项目' },
    { to: '/blog', label: '博客' },
    { to: '/ai', label: 'AI' },
  ] as const

  const accountLink = authStore.isAuthenticated
    ? [{ to: '/profile', label: '账户中心' }]
    : [{ to: '/login', label: '登录/注册' }]

  return [...mainLinks, ...accountLink]
})
</script>

<template>
  <header class="topbar">
    <router-link to="/" class="brand">MyWeb</router-link>

    <nav class="topnav" aria-label="主导航">
      <router-link
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        :aria-current="route.path === link.to ? 'page' : undefined"
      >
        {{ link.label }}
      </router-link>
    </nav>

    <p class="whoami">{{ username ? `当前用户：${username}` : '未登录' }}</p>
  </header>
</template>

