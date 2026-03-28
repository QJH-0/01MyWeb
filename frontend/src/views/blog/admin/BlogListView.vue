<script setup lang="ts">
/**
 * 管理端博客列表：筛选、分页、发布/下架/删除；错误码映射为用户可读文案。
 */
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../../stores/auth'
import {
  deleteAdminBlog,
  fetchAdminBlogs,
  publishAdminBlog,
  unpublishAdminBlog,
  type Blog,
} from '../../../api/blogs'
import BackendOfflineBanner from '../../../components/common/BackendOfflineBanner.vue'
import EmptyState from '../../../components/common/EmptyState.vue'
import ErrorMessage from '../../../components/common/ErrorMessage.vue'
import Loading from '../../../components/common/Loading.vue'
import { mapApiErrorCodeToMessage, toUserFriendlyApiError } from '../../../auth/error-code'
import { hasAccessToken } from '../../../auth/token'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref<string | null>(null)
const traceId = ref('')
const offline = ref(false)
const busyId = ref<number | null>(null)

const page = ref(0)
const limit = ref(10)
const category = ref('')
const status = ref<string>('all')

type PageData = { list: Blog[]; total: number; page: number; limit: number }
const data = ref<PageData | null>(null)

const hasAdminPermission = computed(() => authStore.profile?.permissions.includes('PERM_ADMIN_PANEL') ?? false)
const adminTokenConfigured = computed(() => Boolean(import.meta.env.VITE_ADMIN_TOKEN))

const blogs = computed(() => data.value?.list ?? [])
const total = computed(() => data.value?.total ?? 0)
const totalPages = computed(() => (limit.value > 0 ? Math.max(1, Math.ceil(total.value / limit.value)) : 1))

function statusParam() {
  if (status.value === 'DRAFT' || status.value === 'PUBLISHED') return status.value
  return undefined
}

async function load() {
  if (!hasAccessToken()) {
    await router.replace({ name: 'login', query: { redirect: '/admin/blogs' } })
    return
  }

  loading.value = true
  error.value = null
  traceId.value = ''
  offline.value = false

  try {
    const response = await fetchAdminBlogs({
      status: statusParam(),
      category: category.value ? category.value : undefined,
      page: page.value,
      limit: limit.value,
    })
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    data.value = response.data
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
    data.value = { list: [], total: 0, page: 0, limit: limit.value }
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  page.value = 0
  void load()
}

function goPrev() {
  page.value = Math.max(0, page.value - 1)
  void load()
}

function goNext() {
  page.value = Math.min(totalPages.value - 1, page.value + 1)
  void load()
}

const successToast = ref<string | null>(null)

async function removeBlog(b: Blog) {
  const ok = window.confirm(`确认删除「${b.title}」？此操作为软删除。`)
  if (!ok) return

  error.value = null
  successToast.value = null
  busyId.value = b.id
  try {
    const response = await deleteAdminBlog(b.id)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    successToast.value = '删除成功'
    await load()
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    busyId.value = null
  }
}

async function publishBlog(b: Blog) {
  error.value = null
  successToast.value = null
  busyId.value = b.id
  try {
    const response = await publishAdminBlog(b.id)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    successToast.value = '已发布'
    await load()
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    busyId.value = null
  }
}

async function unpublishBlog(b: Blog) {
  error.value = null
  successToast.value = null
  busyId.value = b.id
  try {
    const response = await unpublishAdminBlog(b.id)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    successToast.value = '已下架为草稿'
    await load()
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    busyId.value = null
  }
}

function formatPublished(iso: string | null | undefined) {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('zh-CN')
}

onMounted(async () => {
  if (hasAccessToken() && !authStore.profile) {
    try {
      await authStore.fetchMe()
    } catch {
      // ignore
    }
  }
  await load()
})
</script>

<template>
  <main class="page">
    <section class="card admin-card">
      <div class="head">
        <div>
          <h1>博客管理</h1>
          <p class="desc">草稿仅管理端可见；发布后前台 `/blog` 可读。需 `PERM_ADMIN_PANEL` 与 `VITE_ADMIN_TOKEN`。</p>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" @click="router.push('/blog')">前台博客</button>
          <button class="submit-btn" type="button" @click="router.push('/admin/blogs/new')">新建文章</button>
        </div>
      </div>

      <template v-if="loading">
        <Loading title="正在加载博客列表…" hint="从 /api/admin/blogs 获取数据" />
      </template>

      <template v-else>
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="load" />
        <p v-if="successToast" class="state success">{{ successToast }}</p>

        <div v-if="!hasAdminPermission" class="warn">
          <p class="warn-title">权限不足</p>
          <p class="warn-desc">当前账号缺少 `PERM_ADMIN_PANEL`。</p>
        </div>

        <div v-else-if="!adminTokenConfigured" class="warn">
          <p class="warn-title">未配置 Admin Token</p>
          <p class="warn-desc">请设置 `VITE_ADMIN_TOKEN`（开发联调）。</p>
        </div>

        <template v-else>
          <div class="toolbar">
            <label class="field">
              <span class="label">分类</span>
              <input
                v-model.trim="category"
                class="input"
                placeholder="筛选分类"
                @keydown.enter.prevent="resetAndLoad"
              />
            </label>
            <label class="field">
              <span class="label">状态</span>
              <select v-model="status" class="select" @change="resetAndLoad">
                <option value="all">全部</option>
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">已发布</option>
              </select>
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
            <ErrorMessage :message="error" @retry="load" />
          </template>

          <template v-else-if="blogs.length === 0">
            <EmptyState title="暂无博客" hint="点击「新建文章」创建草稿。" />
          </template>

          <div v-else class="list" aria-label="管理端博客列表">
            <article v-for="b in blogs" :key="b.id" class="row">
              <div class="row-main">
                <p class="title">
                  <span class="badge" :class="{ pub: b.status === 'PUBLISHED' }">{{
                    b.status === 'PUBLISHED' ? '已发布' : '草稿'
                  }}</span>
                  {{ b.title }}
                </p>
                <p class="summary">{{ b.summary }}</p>
                <p class="meta">
                  <span class="chip mono">/{{ b.slug }}</span>
                  <span v-if="b.category" class="chip">{{ b.category }}</span>
                  <span v-for="t in b.tags.slice(0, 4)" :key="t" class="chip muted">{{ t }}</span>
                  <span class="sep">·</span>
                  <span>ID {{ b.id }}</span>
                  <span class="sep">·</span>
                  <span>发布 {{ formatPublished(b.publishedAt) }}</span>
                </p>
              </div>
              <div class="row-actions">
                <button
                  class="ghost-btn"
                  type="button"
                  :disabled="busyId === b.id"
                  @click="router.push(`/admin/blogs/${b.id}/edit`)"
                >
                  编辑
                </button>
                <button
                  v-if="b.status !== 'PUBLISHED'"
                  class="submit-btn slim"
                  type="button"
                  :disabled="busyId === b.id"
                  @click="publishBlog(b)"
                >
                  发布
                </button>
                <button
                  v-else
                  class="ghost-btn"
                  type="button"
                  :disabled="busyId === b.id"
                  @click="unpublishBlog(b)"
                >
                  下架
                </button>
                <button class="danger-btn" type="button" :disabled="busyId === b.id" @click="removeBlog(b)">
                  删除
                </button>
              </div>
            </article>
          </div>

          <div class="pager" aria-label="分页">
            <button class="ghost-btn" type="button" :disabled="page <= 0" @click="goPrev">上一页</button>
            <p class="pager-meta">
              第 <strong>{{ page + 1 }}</strong> / {{ totalPages }} 页（共 {{ total }} 条）
            </p>
            <button class="ghost-btn" type="button" :disabled="page >= totalPages - 1" @click="goNext">下一页</button>
          </div>

          <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
        </template>
      </template>
    </section>
  </main>
</template>

<style scoped>
.admin-card {
  width: min(980px, 100%);
}

.head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar {
  margin: 14px 0 8px;
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 0.6fr auto;
  gap: 12px;
  align-items: end;
}

.label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 800;
}

.input,
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

.warn {
  border: 1px solid rgba(180, 71, 95, 0.35);
  background: rgba(180, 71, 95, 0.06);
  border-radius: 14px;
  padding: 14px;
}

.warn-title {
  margin: 0 0 6px;
  font-weight: 900;
  color: var(--error);
}

.warn-desc {
  margin: 0;
  color: var(--text-secondary);
}

.list {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.row {
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(255, 255, 255, 0.88);
  border-radius: 14px;
  padding: 14px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.title {
  margin: 0 0 6px;
  font-weight: 900;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.55;
}

.meta {
  margin: 10px 0 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: #6a7f96;
}

.sep {
  opacity: 0.7;
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

.chip.mono {
  font-family: ui-monospace, monospace;
  font-weight: 700;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  color: var(--text-secondary);
  background: rgba(106, 127, 150, 0.12);
  border: 1px solid rgba(106, 127, 150, 0.28);
}

.badge.pub {
  color: var(--success);
  background: rgba(47, 126, 85, 0.12);
  border-color: rgba(47, 126, 85, 0.25);
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-start;
  justify-content: flex-end;
}

.slim {
  padding: 8px 12px;
  font-size: 13px;
}

.danger-btn {
  border-radius: 10px;
  font-size: 14px;
  padding: 10px 14px;
  border: 1px solid rgba(180, 71, 95, 0.35);
  background: rgba(180, 71, 95, 0.08);
  color: var(--error);
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 800;
}

.danger-btn:hover {
  background: rgba(180, 71, 95, 0.12);
  border-color: rgba(180, 71, 95, 0.45);
}

.danger-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
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

@media (max-width: 860px) {
  .toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .row {
    grid-template-columns: 1fr;
  }

  .row-actions {
    justify-content: flex-start;
  }
}
</style>
