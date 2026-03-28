<script setup lang="ts">
/**
 * 公开博客详情：Markdown 经 DOMPurify 过滤后渲染；404 与网络失败分流提示（非 404 才降级示例数据）。
 */
import axios from 'axios'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchBlogDetail, type Blog } from '../../api/blogs'
import { mapApiErrorCodeToMessage } from '../../auth/error-code'
import { renderSafeMarkdown } from '../../utils/markdown'
import { renderMermaidInElement } from '../../utils/mermaidRender'
import { toAbsoluteHttpUrl } from '../../utils/url'
import BackendOfflineBanner from '../../components/common/BackendOfflineBanner.vue'
import ErrorMessage from '../../components/common/ErrorMessage.vue'
import Loading from '../../components/common/Loading.vue'

const route = useRoute()

const loading = ref(true)
const error = ref<string | null>(null)
const offline = ref(false)
const notFound = ref(false)
const traceId = ref('')
const blog = ref<Blog | null>(null)

const id = computed(() => Number(route.params.id))

const fallbackBlog = (nid: number): Blog => ({
  id: nid,
  title: '示例文章（离线）',
  slug: 'offline-sample',
  summary: '后端不可用时的占位详情。',
  content: '当前为 **降级视图**。恢复后端后请使用下方重试加载正文。',
  category: 'web',
  tags: ['Fallback'],
  status: 'PUBLISHED',
  coverUrl: null,
  viewCount: 0,
  publishedAt: new Date(0).toISOString(),
  createdAt: new Date(0).toISOString(),
  updatedAt: new Date(0).toISOString(),
})

function formatPublished(iso: string | null | undefined) {
  if (!iso) return ''
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('zh-CN')
}

const htmlBody = computed(() => renderSafeMarkdown(blog.value?.content ?? ''))
const coverHref = computed(() => toAbsoluteHttpUrl(blog.value?.coverUrl))
const mdRoot = ref<HTMLElement | null>(null)

watch(
  () => htmlBody.value,
  async () => {
    await nextTick()
    await renderMermaidInElement(mdRoot.value)
  },
)

async function loadDetail() {
  if (!Number.isFinite(id.value) || id.value <= 0) {
    notFound.value = true
    offline.value = false
    blog.value = null
    error.value = '无效的文章 ID'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  offline.value = false
  notFound.value = false

  try {
    const response = await fetchBlogDetail(id.value)
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      notFound.value = true
      blog.value = null
      error.value = mapApiErrorCodeToMessage(response.error)
      return
    }
    blog.value = response.data
  } catch (e) {
    if (axios.isAxiosError(e) && e.response?.status === 404) {
      notFound.value = true
      blog.value = null
      const code = (e.response?.data as { error?: string } | undefined)?.error
      error.value = mapApiErrorCodeToMessage(code)
      return
    }
    error.value = e instanceof Error ? e.message : 'Unknown error'
    offline.value = true
    blog.value = fallbackBlog(id.value)
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
watch(
  () => route.params.id,
  () => {
    void loadDetail()
  },
)
</script>

<template>
  <main class="page">
    <section class="card blog-detail-card">
      <template v-if="loading">
        <Loading title="正在加载文章…" hint="从 /api/blogs/:id 获取数据" />
      </template>

      <template v-else-if="notFound">
        <h1>文章不可用</h1>
        <p class="desc">{{ error ?? '该文章不存在、未发布或已删除。' }}</p>
        <router-link class="back-link" to="/blog">返回博客列表</router-link>
      </template>

      <template v-else-if="blog">
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="loadDetail" />

        <img
          v-if="coverHref"
          class="cover"
          :src="coverHref"
          :alt="`「${blog.title}」封面`"
          loading="lazy"
        />

        <h1 class="title">{{ blog.title }}</h1>
        <p class="lead">{{ blog.summary }}</p>

        <div class="meta">
          <span v-if="blog.category" class="chip">{{ blog.category }}</span>
          <span v-for="t in blog.tags ?? []" :key="t" class="chip muted">{{ t }}</span>
          <span v-if="blog.publishedAt" class="muted">发布于 {{ formatPublished(blog.publishedAt) }}</span>
          <span v-if="blog.viewCount != null" class="muted">阅读 {{ blog.viewCount }}</span>
        </div>

        <div class="md-body">
          <article ref="mdRoot" class="md-content" v-html="htmlBody" />
        </div>

        <section class="comments-placeholder" aria-label="评论">
          <h2 class="h3">评论</h2>
          <p class="muted">评论、回复与点赞需登录，将在 `/api/comments` 联调完成后接入；当前页面仅展示正文。</p>
        </section>

        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>

        <div class="foot">
          <router-link class="back-link" to="/blog">← 返回列表</router-link>
        </div>
      </template>

      <template v-else>
        <ErrorMessage :message="error ?? '加载失败'" @retry="loadDetail" />
      </template>
    </section>
  </main>
</template>

<style scoped>
.blog-detail-card {
  width: min(1080px, 100%);
  max-width: 100%;
}

.md-body {
  overflow-x: hidden;
  max-width: 100%;
}

.title {
  margin: 0 0 10px;
  font-weight: 900;
  font-size: 1.75rem;
  line-height: 1.25;
}

.lead {
  margin: 0 0 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.cover {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  border-radius: 14px;
  margin-bottom: 16px;
  border: 1px solid rgba(203, 216, 231, 0.75);
}

.meta {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.muted {
  font-size: 13px;
  color: #6a7f96;
}

.chip {
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(79, 116, 163, 0.06);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 900;
}

.chip.muted {
  color: var(--text-secondary);
}

.md-content {
  line-height: 1.65;
  color: var(--text-main);
}

.md-content :deep(h1),
.md-content :deep(h2),
.md-content :deep(h3) {
  margin: 1.2em 0 0.5em;
  font-weight: 800;
}

.md-content :deep(p) {
  margin: 0.6em 0;
}

.md-content :deep(a) {
  color: var(--accent, #4f74a3);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.md-content :deep(pre) {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(79, 116, 163, 0.08);
  border: 1px solid rgba(203, 216, 231, 0.85);
  overflow: auto;
  font-size: 13px;
}

.md-content :deep(code) {
  font-family: ui-monospace, monospace;
  font-size: 0.92em;
}

.md-content :deep(ul),
.md-content :deep(ol) {
  padding-left: 1.4em;
}

.md-content :deep(table) {
  border-collapse: collapse;
  border-spacing: 0;
  width: 100%;
  table-layout: fixed;
  font-size: 14px;
}

.md-content :deep(th),
.md-content :deep(td) {
  border: 1px solid var(--border, #cbd8e7);
  padding: 10px 14px;
  vertical-align: top;
  text-align: left;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.md-content :deep(th) {
  background: rgba(79, 116, 163, 0.1);
  font-weight: 800;
}

.md-content :deep(tr:nth-child(even) td) {
  background: rgba(255, 255, 255, 0.5);
}

.md-content :deep(.myweb-mermaid) {
  margin: 1.25em 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 2rem;
}

.md-content :deep(.myweb-mermaid svg) {
  max-width: 100%;
  height: auto;
}

.md-content :deep(.mermaid-error) {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid rgba(180, 71, 95, 0.35);
  background: rgba(180, 71, 95, 0.06);
  color: var(--error, #b4475f);
  font-size: 13px;
}

.h3 {
  margin: 1.5em 0 0.5em;
  font-size: 1.1rem;
  font-weight: 800;
}

.comments-placeholder {
  margin-top: 28px;
  padding: 16px;
  border-radius: 14px;
  border: 1px dashed rgba(203, 216, 231, 0.95);
  background: rgba(79, 116, 163, 0.04);
}

.foot {
  margin-top: 28px;
  padding-top: 16px;
  border-top: 1px solid rgba(203, 216, 231, 0.65);
}

.back-link {
  font-weight: 800;
  color: var(--accent, #4f74a3);
  text-decoration: none;
}

.back-link:hover {
  text-decoration: underline;
}
</style>
