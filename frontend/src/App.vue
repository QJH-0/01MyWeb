<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth } from './api/health'

const health = ref<any>(null)
const loading = ref(false)
const errorMessage = ref<string | null>(null)

onMounted(async () => {
  loading.value = true
  errorMessage.value = null
  try {
    health.value = await getHealth()
  } catch (e) {
    errorMessage.value = '健康检查尚未联通（后端未就绪或代理未配置）'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="app-root">
    <header class="topbar">
      <div class="brand">myweb</div>
      <nav class="nav">
        <router-link to="/">首页</router-link>
        <router-link to="/about">关于我</router-link>
        <router-link to="/experience">经历</router-link>
        <router-link to="/contact">联系</router-link>
        <router-link to="/admin/projects">项目管理</router-link>
        <span class="nav-spacer" />
        <span class="health" v-if="loading">加载中...</span>
        <span class="health" v-else-if="health && health.success">
          健康检查：OK
        </span>
        <span class="health" v-else-if="errorMessage">{{ errorMessage }}</span>
      </nav>
    </header>

    <main class="content">
      <router-view />
    </main>

    <footer class="footer">© {{ new Date().getFullYear() }} myweb</footer>
  </div>
</template>

<style scoped lang="scss">
.app-root {
  min-height: 100svh;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(247, 245, 243, 0.95);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(6px);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  font-weight: 600;
  color: var(--text-primary);
}

.nav {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.nav-spacer {
  flex: 1 1 auto;
}

.nav a {
  color: var(--text-muted);
  text-decoration: none;
  padding: 6px 10px;
  border-radius: 8px;
  transition: background 150ms ease;
}

.nav a.router-link-active {
  background: rgba(125, 157, 156, 0.15);
  color: var(--text-primary);
}

.health {
  color: var(--text-secondary);
  font-size: 14px;
}

.content {
  flex: 1 1 auto;
}

.footer {
  padding: 20px;
  text-align: center;
  color: var(--text-muted);
  border-top: 1px solid var(--border);
}
</style>
