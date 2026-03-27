<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchHomeContent, type ContentPage } from '../api/content'
import EmptyState from '../components/common/EmptyState.vue'
import ErrorMessage from '../components/common/ErrorMessage.vue'
import Loading from '../components/common/Loading.vue'

const loading = ref(true)
const error = ref<string | null>(null)
const traceId = ref('')
const page = ref<ContentPage | null>(null)

async function loadPage() {
  loading.value = true
  error.value = null
  try {
    const response = await fetchHomeContent()
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Content load failed')
    }
    page.value = response.data
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Unknown error'
    error.value = message
  } finally {
    loading.value = false
  }
}

const isEmpty = computed(() => !page.value?.title && !page.value?.summary && (page.value?.sections?.length ?? 0) === 0)

onMounted(loadPage)
</script>

<template>
  <div class="page">
    <section class="card">
      <template v-if="loading">
        <Loading title="正在加载首页内容…" hint="从 /api/content/home 获取内容" />
      </template>

      <template v-else-if="error">
        <ErrorMessage :message="error" @retry="loadPage" />
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
      </template>

      <template v-else-if="page">
        <div class="hero">
          <h1>{{ page.title || 'MyWeb' }}</h1>
          <p class="desc">{{ page.summary || '欢迎来到我的门户主页。' }}</p>
        </div>

        <div class="entry-cards" aria-label="入口区块">
          <router-link to="/projects" class="entry-card">
            <p class="entry-title">项目</p>
            <p class="entry-desc">浏览项目列表与详情（M2 入口）</p>
          </router-link>
          <router-link to="/blog" class="entry-card">
            <p class="entry-title">博客</p>
            <p class="entry-desc">阅读文章与专题（M3 入口）</p>
          </router-link>
          <router-link to="/ai" class="entry-card">
            <p class="entry-title">AI</p>
            <p class="entry-desc">对话与检索增强（M6 入口）</p>
          </router-link>
        </div>

        <div class="inline-meta" v-if="traceId">traceId: {{ traceId }}</div>

        <div style="margin-top: 14px" v-if="isEmpty">
          <EmptyState title="首页内容为空" hint="当前为 M1 占位数据，可后续通过管理端配置。" />
        </div>
      </template>
    </section>
  </div>
</template>
