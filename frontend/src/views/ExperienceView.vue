<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchExperienceContent, type ContentPage } from '../api/content'
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
    const response = await fetchExperienceContent()
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Content load failed')
    }
    page.value = response.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
  } finally {
    loading.value = false
  }
}

const isEmpty = computed(() => !page.value?.title && !page.value?.summary && (page.value?.sections?.length ?? 0) === 0)

onMounted(loadPage)
</script>

<template>
  <main class="page">
    <section class="card">
      <template v-if="loading">
        <Loading title="正在加载经历页内容…" hint="从 /api/content/experience 获取内容" />
      </template>
      <template v-else-if="error">
        <ErrorMessage :message="error" @retry="loadPage" />
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
      </template>
      <template v-else-if="page">
        <h1>{{ page.title || '经历' }}</h1>
        <p class="desc">{{ page.summary || '时间线组件将在这里展示教育、工作与项目经历。' }}</p>
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
        <div style="margin-top: 14px" v-if="isEmpty">
          <EmptyState title="经历页内容为空" hint="当前为 M1 占位数据，可后续补齐 sections。" />
        </div>
      </template>
    </section>
  </main>
</template>

