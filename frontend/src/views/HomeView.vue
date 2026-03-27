<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHealth, type HealthStatus } from '../api/health'
import { useAuthStore } from '../stores/auth'

const loading = ref(true)
const error = ref<string | null>(null)
const traceId = ref('')
const health = ref<HealthStatus | null>(null)
const router = useRouter()
const authStore = useAuthStore()

async function loadHealth() {
  loading.value = true
  error.value = null
  try {
    const response = await fetchHealth()
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Health check failed')
    }
    health.value = response.data
    traceId.value = response.traceId
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Unknown error'
    error.value = message
  } finally {
    loading.value = false
  }
}

onMounted(loadHealth)
</script>

<template>
  <main class="page">
    <section class="card">
      <h1>MyWeb M0 Baseline</h1>
      <p class="desc">Frontend and backend health link is connected.</p>

      <p v-if="loading" class="state">Checking service health...</p>
      <p v-else-if="error" class="state error">{{ error }}</p>

      <div v-else-if="health" class="grid">
        <p>MySQL: <strong>{{ health.mysql ? 'UP' : 'DOWN' }}</strong></p>
        <p>Redis: <strong>{{ health.redis ? 'UP' : 'DOWN' }}</strong></p>
        <p>Elasticsearch: <strong>{{ health.elasticsearchStatus }}</strong></p>
        <p>MinIO: <strong>{{ health.minio ? 'UP' : 'DOWN' }}</strong></p>
      </div>

      <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
      <div class="action-row">
        <button v-if="authStore.isAuthenticated" class="submit-btn" @click="router.push('/profile')">
          进入账户中心
        </button>
        <button v-else class="submit-btn" @click="router.push('/login')">前往登录</button>
      </div>
    </section>
  </main>
</template>
