<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchAboutContent, type ContentPage } from '../api/content'
import BackendOfflineBanner from '../components/common/BackendOfflineBanner.vue'
import EmptyState from '../components/common/EmptyState.vue'
import ErrorMessage from '../components/common/ErrorMessage.vue'
import Loading from '../components/common/Loading.vue'

const loading = ref(true)
const error = ref<string | null>(null)
const offline = ref(false)
const traceId = ref('')
const page = ref<ContentPage | null>(null)

const fallbackSkills = ['Java', 'Spring Boot', 'Vue 3', 'TypeScript', 'MySQL', 'Docker']
const fallbackStacks = [
  { title: '后端', items: ['Spring Boot', 'Spring Security', 'JWT', 'MySQL', 'Redis'] },
  { title: '前端', items: ['Vue 3', 'Vite', 'Vue Router', 'Pinia', 'SCSS'] },
  { title: '工程化', items: ['Maven', 'ESLint/Prettier', 'Docker Compose', 'Nginx'] },
] as const

const fallbackPage: ContentPage = {
  title: '关于我',
  summary: '这里将展示个人介绍、技能标签和技术栈概览。',
  sections: [],
  updatedAt: new Date(0).toISOString(),
}

async function loadPage() {
  loading.value = true
  error.value = null
  offline.value = false
  try {
    const response = await fetchAboutContent()
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Content load failed')
    }
    page.value = response.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
    offline.value = true
    page.value = fallbackPage
  } finally {
    loading.value = false
  }
}

const isEmpty = computed(() => !page.value?.title && !page.value?.summary && (page.value?.sections?.length ?? 0) === 0)

const skills = computed(() => {
  const sections = page.value?.sections
  if (!sections || !Array.isArray(sections)) return fallbackSkills
  const skillSection = sections.find(
    (section) =>
      typeof section === 'object' &&
      section !== null &&
      'type' in section &&
      (section as { type?: unknown }).type === 'skills',
  ) as { items?: unknown } | undefined
  const items = skillSection?.items
  if (!Array.isArray(items)) return fallbackSkills
  const normalized = items.filter((value): value is string => typeof value === 'string' && value.trim().length > 0)
  return normalized.length ? normalized : fallbackSkills
})

onMounted(loadPage)
</script>

<template>
  <main class="page">
    <section class="card">
      <template v-if="loading">
        <Loading title="正在加载关于页内容…" hint="从 /api/content/about 获取内容" />
      </template>
      <template v-else-if="error">
        <ErrorMessage :message="error" @retry="loadPage" />
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
      </template>
      <template v-else-if="page">
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="loadPage" />
        <h1>{{ page.title || '关于我' }}</h1>
        <p class="desc">{{ page.summary || '这里将展示个人介绍、技能标签和技术栈概览。' }}</p>
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>

        <div class="section">
          <h2 class="h2">技能标签</h2>
          <ul class="tags" aria-label="技能标签">
            <li v-for="item in skills" :key="item" class="tag">{{ item }}</li>
          </ul>
        </div>

        <div class="section">
          <h2 class="h2">技术栈概览</h2>
          <div class="stack-grid">
            <div v-for="block in fallbackStacks" :key="block.title" class="stack-card">
              <p class="stack-title">{{ block.title }}</p>
              <ul class="stack-list">
                <li v-for="it in block.items" :key="it">{{ it }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div style="margin-top: 14px" v-if="isEmpty">
          <EmptyState title="关于页内容为空" hint="当前为 M1 占位数据，可后续补齐 sections。" />
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.card > :deep(.state-card) {
  margin-bottom: 14px;
}

.h2 {
  margin: 18px 0 10px;
  font-size: 16px;
}

.section {
  margin-top: 10px;
}

.tags {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag {
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(79, 116, 163, 0.06);
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 800;
}

.stack-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stack-card {
  border: 1px solid rgba(203, 216, 231, 0.85);
  background: rgba(255, 255, 255, 0.85);
  border-radius: 14px;
  padding: 14px;
}

.stack-title {
  margin: 0 0 8px;
  font-weight: 900;
  color: var(--text-main);
}

.stack-list {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  display: grid;
  gap: 6px;
}

@media (max-width: 760px) {
  .stack-grid {
    grid-template-columns: 1fr;
  }
}
</style>

