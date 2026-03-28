<script setup lang="ts">
/**
 * 公开博客列表：分类/标签筛选与分页；接口失败时用内置示例数据降级并提示「后端连接失败」。
 */
import { computed, onMounted, ref } from 'vue'
import { fetchBlogs, type Blog } from '../../api/blogs'
import { toAbsoluteHttpUrl } from '../../utils/url'
import BackendOfflineBanner from '../../components/common/BackendOfflineBanner.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import ErrorMessage from '../../components/common/ErrorMessage.vue'
import Loading from '../../components/common/Loading.vue'

const loading = ref(true)
const error = ref<string | null>(null)
const offline = ref(false)
const traceId = ref('')
const page = ref(0)
const limit = ref(10)
const category = ref('')
const tag = ref('')

type PageData = { list: Blog[]; total: number; page: number; limit: number }
const data = ref<PageData | null>(null)

const fallbackBlogs: Blog[] = [
  {
    id: 1,
    title: '示例文章（离线）',
    slug: 'offline-sample',
    summary: '后端不可用时展示的占位博客条目，便于演示列表布局。',
    content: null,
    category: 'web',
    tags: ['Vue', 'Spring'],
    status: 'PUBLISHED',
    coverUrl: null,
    viewCount: 0,
    publishedAt: new Date(0).toISOString(),
    createdAt: new Date(0).toISOString(),
    updatedAt: new Date(0).toISOString(),
  },
]

function formatPublished(iso: string | null | undefined) {
  if (!iso) return ''
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('zh-CN')
}

async function loadBlogs() {
  loading.value = true
  error.value = null
  offline.value = false

  try {
    const response = await fetchBlogs({
      category: category.value ? category.value : undefined,
      tag: tag.value ? tag.value : undefined,
      page: page.value,
      limit: limit.value,
    })
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Blogs load failed')
    }
    data.value = response.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
    offline.value = true
    data.value = { list: fallbackBlogs, total: fallbackBlogs.length, page: 0, limit: fallbackBlogs.length }
  } finally {
    loading.value = false
  }
}

const blogs = computed(() => data.value?.list ?? [])
const isEmpty = computed(() => !blogs.value.length)
const total = computed(() => data.value?.total ?? 0)
const totalPages = computed(() => (limit.value > 0 ? Math.max(1, Math.ceil(total.value / limit.value)) : 1))

function resetAndLoad() {
  page.value = 0
  void loadBlogs()
}

function goPrev() {
  page.value = Math.max(0, page.value - 1)
  void loadBlogs()
}

function goNext() {
  page.value = Math.min(totalPages.value - 1, page.value + 1)
  void loadBlogs()
}

function coverSrc(b: Blog) {
  return toAbsoluteHttpUrl(b.coverUrl)
}

onMounted(loadBlogs)
</script>

<template>
  <main class="page">
    <section class="card">
      <div class="page-head">
        <div>
          <h1>博客</h1>
          <p class="desc">仅展示已发布文章；后端不可用时使用默认数据降级渲染。</p>
        </div>
        <router-link class="admin-cta" to="/admin/blogs">博客后台</router-link>
      </div>

      <template v-if="loading">
        <Loading title="正在加载博客列表…" hint="从 /api/blogs 获取数据" />
      </template>

      <template v-else>
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="loadBlogs" />

        <div class="toolbar">
          <label class="field">
            <span class="label">分类</span>
            <input
              v-model.trim="category"
              class="text-input"
              placeholder="留空为全部"
              @keydown.enter.prevent="resetAndLoad"
            />
          </label>
          <label class="field">
            <span class="label">标签</span>
            <input
              v-model.trim="tag"
              class="text-input"
              placeholder="精确匹配单个标签"
              @keydown.enter.prevent="resetAndLoad"
            />
          </label>
          <label class="field">
            <span class="label">每页</span>
            <select v-model.number="limit" class="select" @change="resetAndLoad">
              <option :value="10">10</option>
              <option :value="20">20</option>
            </select>
          </label>
          <div class="tool-actions">
            <button class="ghost-btn" type="button" @click="resetAndLoad">刷新</button>
          </div>
        </div>

        <template v-if="error && !offline">
          <ErrorMessage :message="error" @retry="loadBlogs" />
        </template>

        <template v-else-if="isEmpty">
          <EmptyState title="暂无文章" hint="管理员发布文章后将在此展示。" />
        </template>

        <div v-else class="grid" aria-label="博客列表">
          <router-link v-for="b in blogs" :key="b.id" class="item" :to="`/blog/${b.id}`">
            <img
              v-if="coverSrc(b)"
              class="thumb"
              :src="coverSrc(b)!"
              :alt="`「${b.title}」封面`"
              loading="lazy"
            />
            <p class="title">{{ b.title }}</p>
            <p class="summary">{{ b.summary }}</p>
            <div class="meta">
              <span v-if="b.category" class="chip">{{ b.category }}</span>
              <span v-for="t in (b.tags ?? []).slice(0, 3)" :key="t" class="chip muted">{{ t }}</span>
              <span v-if="b.publishedAt" class="date">{{ formatPublished(b.publishedAt) }}</span>
            </div>
          </router-link>
        </div>

        <div v-if="!isEmpty && !offline" class="pager" aria-label="分页">
          <button class="ghost-btn" type="button" :disabled="page <= 0" @click="goPrev">上一页</button>
          <p class="pager-meta">
            第 <strong>{{ page + 1 }}</strong> / {{ totalPages }} 页（共 {{ total }} 篇）
          </p>
          <button class="ghost-btn" type="button" :disabled="page >= totalPages - 1" @click="goNext">下一页</button>
        </div>

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
}

.admin-cta {
  flex-shrink: 0;
  align-self: center;
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid rgba(79, 116, 163, 0.35);
  background: rgba(79, 116, 163, 0.08);
  color: var(--accent, #4f74a3);
  font-weight: 800;
  font-size: 14px;
  text-decoration: none;
  transition:
    border-color 0.2s ease,
    background 0.2s ease;
}

.admin-cta:hover {
  border-color: rgba(79, 116, 163, 0.55);
  background: rgba(79, 116, 163, 0.12);
}

.toolbar {
  margin: 14px 0 10px;
  display: grid;
  grid-template-columns: 1fr 1fr 0.5fr auto;
  gap: 12px;
  align-items: end;
}

.field {
  display: grid;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 800;
}

.text-input,
.select {
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(203, 216, 231, 0.85);
  padding: 0 10px;
  background: rgba(255, 255, 255, 0.88);
}

.tool-actions {
  display: flex;
  justify-content: flex-end;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 10px;
}

.item {
  display: block;
  padding: 14px;
  border: 1px solid rgba(203, 216, 231, 0.85);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  text-decoration: none;
  color: inherit;
}

.item:hover {
  border-color: rgba(79, 116, 163, 0.4);
}

.thumb {
  width: calc(100% + 8px);
  height: 120px;
  margin: -4px -4px 10px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid rgba(203, 216, 231, 0.7);
  background: rgba(79, 116, 163, 0.06);
}

.title {
  margin: 0 0 6px;
  font-weight: 900;
}

.summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

.meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.date {
  font-size: 12px;
  color: #6a7f96;
  margin-left: auto;
}

.chip {
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(79, 116, 163, 0.06);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 900;
  color: var(--text-main);
}

.chip.muted {
  color: var(--text-secondary);
}

.pager {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.pager-meta {
  margin: 0;
  color: var(--text-secondary);
}

@media (max-width: 760px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }

  .date {
    margin-left: 0;
  }
}
</style>
