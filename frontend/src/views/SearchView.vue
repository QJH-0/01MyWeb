<script setup lang="ts">
/**
 * 全站搜索：关键词 + 类型筛选；后端/ES 不可用时展示示例结果并提示「搜索服务不可用」类降级。
 */
import axios from 'axios'
import DOMPurify from 'dompurify'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchSearch, type SearchItem } from '../api/search'
import BackendOfflineBanner from '../components/common/BackendOfflineBanner.vue'
import EmptyState from '../components/common/EmptyState.vue'
import Loading from '../components/common/Loading.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const offline = ref(false)
const traceId = ref('')
const items = ref<SearchItem[]>([])
const total = ref(0)
const page = ref(0)
const limit = ref(10)

const qLocal = ref('')
const typeLocal = ref<'blog' | 'project' | ''>('')

const fallbackItems: SearchItem[] = [
  {
    sourceType: 'blog',
    sourceId: 1,
    title: '示例结果（离线）',
    url: '/blog/1',
    summary: '搜索服务不可用时展示的占位条目，用于保持版式可预览。',
    highlights: [],
  },
  {
    sourceType: 'project',
    sourceId: 1,
    title: '示例项目（离线）',
    url: '/projects/1',
    summary: '连接恢复后将显示真实检索结果。',
    highlights: [],
  },
]

const hasQuery = computed(() => {
  const q = route.query.q
  return typeof q === 'string' && q.trim().length > 0
})

function safeHighlight(html: string) {
  return DOMPurify.sanitize(html, { ALLOWED_TAGS: ['mark', 'em', 'strong'], ALLOWED_ATTR: [] })
}

function resultLink(item: SearchItem) {
  return item.url.startsWith('/') ? item.url : `/${item.url}`
}

async function load() {
  const rawQ = route.query.q
  const q = typeof rawQ === 'string' ? rawQ.trim() : ''
  if (!q) {
    items.value = []
    total.value = 0
    offline.value = false
    error.value = null
    loading.value = false
    return
  }

  const rawType = route.query.type
  const typeParam =
    rawType === 'blog' || rawType === 'project' ? (rawType as 'blog' | 'project') : undefined
  const pageParam = Number(route.query.page ?? 0)
  const limitParam = Number(route.query.limit ?? 10)

  loading.value = true
  error.value = null
  offline.value = false
  try {
    const response = await fetchSearch({
      q,
      type: typeParam,
      page: Number.isFinite(pageParam) && pageParam >= 0 ? pageParam : 0,
      limit: Number.isFinite(limitParam) && limitParam > 0 ? Math.min(100, limitParam) : 10,
    })
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Search failed')
    }
    items.value = response.data.list
    total.value = response.data.total
    page.value = response.data.page
    limit.value = response.data.limit
  } catch (e) {
    if (axios.isAxiosError(e) && (!e.response || e.response.status >= 500)) {
      offline.value = true
      error.value = e.response?.data?.error ?? e.message
      items.value = fallbackItems
      total.value = fallbackItems.length
    } else {
      const code = axios.isAxiosError(e) ? (e.response?.data as { error?: string } | undefined)?.error : undefined
      error.value = code ?? (e instanceof Error ? e.message : 'Search failed')
      items.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  const q = qLocal.value.trim()
  if (!q) {
    void router.push({ path: '/search', query: {} })
    return
  }
  const query: Record<string, string | number> = { q, page: 0, limit: limit.value }
  if (typeLocal.value) {
    query.type = typeLocal.value
  }
  void router.push({ path: '/search', query })
}

function syncFromRoute() {
  const rawQ = route.query.q
  qLocal.value = typeof rawQ === 'string' ? rawQ : ''
  const rawType = route.query.type
  typeLocal.value = rawType === 'blog' || rawType === 'project' ? rawType : ''
  const lp = Number(route.query.limit ?? 10)
  limit.value = Number.isFinite(lp) && lp > 0 ? Math.min(100, lp) : 10
}

onMounted(() => {
  syncFromRoute()
  void load()
})

watch(
  () => route.fullPath,
  () => {
    syncFromRoute()
    void load()
  },
)
</script>

<template>
  <main class="page">
    <section class="card">
      <div class="page-head">
        <div>
          <h1>搜索</h1>
          <p class="desc">检索已发布的博客与公开项目；后端或 Elasticsearch 不可用时展示降级示例。</p>
        </div>
      </div>

      <div class="toolbar">
        <label class="field grow">
          <span class="label">关键词</span>
          <input v-model.trim="qLocal" class="text-input" placeholder="输入关键词" @keydown.enter.prevent="applyFilters" />
        </label>
        <label class="field">
          <span class="label">类型</span>
          <select v-model="typeLocal" class="select" @change="applyFilters">
            <option value="">全部</option>
            <option value="blog">博客</option>
            <option value="project">项目</option>
          </select>
        </label>
        <label class="field">
          <span class="label">每页</span>
          <select v-model.number="limit" class="select" @change="applyFilters">
            <option :value="10">10</option>
            <option :value="20">20</option>
          </select>
        </label>
        <div class="tool-actions">
          <button class="submit-btn" type="button" @click="applyFilters">搜索</button>
        </div>
      </div>

      <template v-if="loading">
        <Loading title="正在搜索…" hint="GET /api/search" />
      </template>

      <template v-else>
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="load" />

        <template v-if="!hasQuery">
          <p class="muted">输入关键词后按回车或点击搜索。</p>
        </template>

        <template v-else-if="!offline && items.length === 0">
          <EmptyState title="无匹配结果" hint="尝试更换关键词或减少筛选条件。" />
        </template>

        <ul v-else class="results" aria-label="搜索结果">
          <li v-for="item in items" :key="`${item.sourceType}-${item.sourceId}`" class="result">
            <router-link class="title" :to="resultLink(item)">{{ item.title }}</router-link>
            <p class="meta">
              <span class="chip">{{ item.sourceType }}</span>
              <span class="muted">#{{ item.sourceId }}</span>
            </p>
            <p class="summary">{{ item.summary }}</p>
            <div v-if="item.highlights.length" class="hl">
              <p v-for="(h, i) in item.highlights" :key="i" class="hl-line" v-html="safeHighlight(h)" />
            </div>
          </li>
        </ul>

        <p v-if="hasQuery && !offline" class="pager-meta">共 {{ total }} 条</p>
        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
      </template>
    </section>
  </main>
</template>

<style scoped>
.page-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 120px;
}

.field.grow {
  flex: 1 1 220px;
}

.label {
  font-size: 12px;
  font-weight: 700;
  color: #5c6b7e;
}

.text-input,
.select {
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(203, 216, 231, 0.95);
  font: inherit;
}

.tool-actions {
  display: flex;
  gap: 8px;
}

.submit-btn {
  padding: 8px 16px;
  border-radius: 10px;
  border: none;
  background: var(--accent, #4f74a3);
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.muted {
  color: #6a7f96;
  font-size: 14px;
}

.results {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.result {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(203, 216, 231, 0.95);
  background: rgba(255, 255, 255, 0.65);
}

.title {
  font-weight: 800;
  color: var(--accent, #4f74a3);
  text-decoration: none;
}

.title:hover {
  text-decoration: underline;
}

.meta {
  margin: 6px 0;
  display: flex;
  gap: 8px;
  align-items: center;
}

.chip {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(79, 116, 163, 0.12);
  font-weight: 700;
}

.summary {
  margin: 0;
  font-size: 14px;
  color: var(--text-main);
}

.hl {
  margin-top: 8px;
  font-size: 13px;
  color: #3d4d63;
}

.hl-line {
  margin: 4px 0;
}

.pager-meta {
  margin-top: 12px;
  font-size: 13px;
  color: #6a7f96;
}

.trace {
  margin-top: 10px;
  font-size: 12px;
  color: #8a9aaf;
}
</style>
