<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    await authStore.fetchMe()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录状态已失效，请重新登录。'
    await authStore.logoutCurrentUser()
    await router.replace('/login')
  } finally {
    loading.value = false
  }
})

async function logout() {
  await authStore.logoutCurrentUser()
  await router.replace('/login')
}
</script>

<template>
  <main class="page">
    <section class="card">
      <h1>账户中心</h1>
      <p class="desc">当前页面受鉴权保护，仅登录用户可访问。</p>

      <p v-if="loading" class="state">正在加载用户信息...</p>
      <p v-else-if="error" class="state error">{{ error }}</p>

      <template v-else-if="authStore.profile">
        <div class="grid">
          <p>用户 ID: <strong>{{ authStore.profile.userId }}</strong></p>
          <p>用户名: <strong>{{ authStore.profile.username }}</strong></p>
          <p>角色: <strong>{{ authStore.profile.roles.join(', ') }}</strong></p>
        </div>
        <div class="action-row">
          <button class="ghost-btn" @click="router.push('/')">返回首页</button>
          <button class="submit-btn" @click="logout">退出登录</button>
        </div>
      </template>
    </section>
  </main>
</template>
