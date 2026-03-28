<script setup lang="ts">
/**
 * 经历页：将 `ContentPage.sections` 解析为时间线；接口失败时使用内置 `fallbackItems` 保证版式可预览。
 */
import { computed, onMounted, ref } from 'vue'
import { fetchExperienceContent, type ContentPage } from '../api/content'
import BackendOfflineBanner from '../components/common/BackendOfflineBanner.vue'
import EmptyState from '../components/common/EmptyState.vue'
import ErrorMessage from '../components/common/ErrorMessage.vue'
import Loading from '../components/common/Loading.vue'
import Timeline, { type TimelineItem } from '../components/experience/Timeline.vue'

/** 页面加载状态 */
const loading = ref(true)
const error = ref<string | null>(null)
const offline = ref(false)
const traceId = ref('')
const page = ref<ContentPage | null>(null)

/** 默认时间线数据 */
const fallbackItems: TimelineItem[] = [
  {
    time: '2026',
    title: 'MyWeb 个人站点',
    description: '从工程初始化到模块化闭环推进，逐步完善内容、项目、博客与 AI 能力。',
    tags: ['Vue 3', 'Spring Boot', 'JWT'],
  },
  {
    time: '2025',
    title: '全栈实践与工程化',
    description: '持续打磨工程质量门禁、测试体系与可观测性，沉淀可复用的项目模板。',
    tags: ['Testing', 'Docker', 'Nginx'],
  },
] as const

/** 默认页面数据 */
const fallbackPage: ContentPage = {
  title: '经历',
  summary: '时间线组件将在这里展示教育、工作与项目经历。',
  sections: [],
  updatedAt: new Date(0).toISOString(),
}

/** 加载经历页内容 */
async function loadPage() {
  loading.value = true
  error.value = null
  offline.value = false
  try {
    const response = await fetchExperienceContent()
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

/** 计算属性：判断页面是否为空 */
const isEmpty = computed(() => !page.value?.title && !page.value?.summary && (page.value?.sections?.length ?? 0) === 0)

/**
 * 计算属性：从页面 sections 解析时间线数据。
 * 查找 type 为 'timeline' 的 section，解析其 items 为时间线格式。
 */
const items = computed<TimelineItem[]>(() => {
  const sections = page.value?.sections
  if (!sections || !Array.isArray(sections)) return fallbackItems
  const timelineSection = sections.find(
    (section) =>
      typeof section === 'object' &&
      section !== null &&
      'type' in section &&
      (section as { type?: unknown }).type === 'timeline',
  ) as { items?: unknown } | undefined

  const rawItems = timelineSection?.items
  if (!Array.isArray(rawItems)) return fallbackItems

  const normalized = rawItems
    .map((raw): TimelineItem | null => {
      if (typeof raw !== 'object' || raw === null) return null
      const time = (raw as { time?: unknown }).time
      const title = (raw as { title?: unknown }).title
      const description = (raw as { description?: unknown }).description
      const tags = (raw as { tags?: unknown }).tags

      if (typeof time !== 'string' || typeof title !== 'string' || typeof description !== 'string') return null
      const normalizedTags = Array.isArray(tags) ? tags.filter((t): t is string => typeof t === 'string' && t.trim().length > 0) : undefined
      return { time, title, description, tags: normalizedTags?.length ? normalizedTags : undefined }
    })
    .filter((value): value is TimelineItem => value !== null)

  return normalized.length ? normalized : fallbackItems
})

onMounted(loadPage)
</script>

<template>
  <main class="page">
    <section class="card">
      <template v-if="loading">
        <Loading title="正在加载经历页内容…" hint="从 /api/content/experience 获取内容" />
      </template>
      <template v-else-if="page">
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="loadPage" />
        <h1>{{ page.title || '经历' }}</h1>
        <p class="desc">{{ page.summary || '时间线组件将在这里展示教育、工作与项目经历。' }}</p>
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>

        <Timeline :items="items" />

        <div style="margin-top: 14px" v-if="isEmpty">
          <EmptyState title="经历页内容为空" hint="当前为 M1 占位数据，可后续补齐 sections。" />
        </div>
      </template>
      <template v-else-if="error">
        <ErrorMessage :message="error" @retry="loadPage" />
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
      </template>
    </section>
  </main>
</template>

<style scoped>
.card > :deep(.state-card) {
  margin-bottom: 14px;
}
</style>

