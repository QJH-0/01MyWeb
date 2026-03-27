<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const navOpen = ref(false)

const username = computed(() => authStore.profile?.username ?? '')

watch(
  () => route.fullPath,
  () => {
    navOpen.value = false
  },
)

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

  const adminLinks =
    authStore.isAuthenticated && authStore.profile?.permissions.includes('PERM_ADMIN_PANEL')
      ? [{ to: '/admin/projects', label: '管理' }]
      : []

  const accountLink = authStore.isAuthenticated
    ? [{ to: '/profile', label: '账户中心' }]
    : [{ to: '/login', label: '登录/注册' }]

  return [...mainLinks, ...adminLinks, ...accountLink]
})
</script>

<template>
  <header class="topbar">
    <router-link to="/" class="brand">MyWeb</router-link>

    <button
      class="nav-toggle"
      type="button"
      :aria-expanded="navOpen"
      aria-controls="primary-nav"
      @click="navOpen = !navOpen"
    >
      <span class="nav-toggle__icon" aria-hidden="true" />
      <span class="nav-toggle__label">{{ navOpen ? '收起' : '菜单' }}</span>
    </button>

    <nav id="primary-nav" class="topnav" :class="{ open: navOpen }" aria-label="主导航">
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

