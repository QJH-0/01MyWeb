<script setup lang="ts">
/**
 * 管理端项目列表：依赖 JWT +（开发环境）`X-Admin-Token`；403/仅 JWT 时提示配置 admin token。
 * 删除为软删，具体以后端为准。
 */
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../../stores/auth'
import { deleteAdminProject, fetchAdminProjects, type Project } from '../../../api/projects'
import BackendOfflineBanner from '../../../components/common/BackendOfflineBanner.vue'
import EmptyState from '../../../components/common/EmptyState.vue'
import ErrorMessage from '../../../components/common/ErrorMessage.vue'
import Loading from '../../../components/common/Loading.vue'
import { hasAccessToken } from '../../../auth/token'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref<string | null>(null)
const traceId = ref('')
const offline = ref(false)

const page = ref(0)
const limit = ref(10)
const category = ref('')
const visible = ref<string>('all')

type PageData = { list: Project[]; total: number; page: number; limit: number }
const data = ref<PageData | null>(null)

const hasAdminPermission = computed(() => authStore.profile?.permissions.includes('PERM_ADMIN_PANEL') ?? false)
const adminTokenConfigured = computed(() => Boolean(import.meta.env.VITE_ADMIN_TOKEN))

const projects = computed(() => data.value?.list ?? [])
const total = computed(() => data.value?.total ?? 0)
const totalPages = computed(() => (limit.value > 0 ? Math.max(1, Math.ceil(total.value / limit.value)) : 1))

function visibleParam() {
  if (visible.value === 'true') return true
  if (visible.value === 'false') return false
  return undefined
}

async function load() {
  if (!hasAccessToken()) {
    await router.replace({ name: 'login', query: { redirect: '/admin/projects' } })
    return
  }

  loading.value = true
  error.value = null
  traceId.value = ''
  offline.value = false

  try {
    const response = await fetchAdminProjects({
      category: category.value ? category.value : undefined,
      visible: visibleParam(),
      page: page.value,
      limit: limit.value,
    })
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Admin projects load failed')
    }
    data.value = response.data
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = e instanceof Error ? e.message : 'Unknown error'
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

async function removeProject(p: Project) {
  const ok = window.confirm(`确认删除「${p.title}」？此操作为软删除，前台将不可见。`)
  if (!ok) return

  error.value = null
  successToast.value = null
  offline.value = false
  traceId.value = ''

  try {
    const response = await deleteAdminProject(p.id)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(response.error ?? 'Delete failed')
    }
    successToast.value = '删除成功'
    await load()
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = e instanceof Error ? e.message : 'Unknown error'
  }
}

const successToast = ref<string | null>(null)

onMounted(async () => {
  if (hasAccessToken() && !authStore.profile) {
    try {
      await authStore.fetchMe()
    } catch {
      // ignore; load() will redirect if needed
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
          <h1>项目管理</h1>
          <p class="desc">管理端项目列表（M2）。需要管理员权限与 `VITE_ADMIN_TOKEN`。</p>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" @click="router.push('/projects')">前台项目页</button>
          <button class="submit-btn" type="button" @click="router.push('/admin/projects/new')">新建项目</button>
        </div>
      </div>

      <template v-if="loading">
        <Loading title="正在加载管理端项目…" hint="从 /api/admin/projects 获取数据" />
      </template>

      <template v-else>
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="load" />
        <p v-if="successToast" class="state success">{{ successToast }}</p>

        <div v-if="!hasAdminPermission" class="warn">
          <p class="warn-title">权限不足</p>
          <p class="warn-desc">当前账号缺少 `PERM_ADMIN_PANEL`，无法访问管理端能力。</p>
        </div>

        <div v-else-if="!adminTokenConfigured" class="warn">
          <p class="warn-title">未配置 Admin Token</p>
          <p class="warn-desc">请在前端环境变量中设置 `VITE_ADMIN_TOKEN`（仅用于开发环境联调）。</p>
        </div>

        <template v-else>
          <div class="toolbar">
            <label class="field">
              <span class="label">分类</span>
              <input
                v-model.trim="category"
                class="input"
                placeholder="例如 web / backend / frontend"
                @keydown.enter.prevent="resetAndLoad"
              />
            </label>

            <label class="field">
              <span class="label">可见性</span>
              <select v-model="visible" class="select" @change="resetAndLoad">
                <option value="all">全部</option>
                <option value="true">可见</option>
                <option value="false">不可见</option>
              </select>
            </label>

            <label class="field">
              <span class="label">每页</span>
              <select v-model.number="limit" class="select" @change="resetAndLoad">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
              </select>
            </label>

            <div class="tool-actions">
              <button class="ghost-btn" type="button" @click="resetAndLoad">刷新</button>
            </div>
          </div>

          <template v-if="error && !offline">
            <ErrorMessage :message="error" @retry="load" />
          </template>

          <template v-else-if="projects.length === 0">
            <EmptyState title="暂无项目" hint="点击右上角“新建项目”创建第一条记录。" />
          </template>

          <div v-else class="list" aria-label="管理端项目列表">
            <article v-for="p in projects" :key="p.id" class="row">
              <div class="row-main">
                <p class="title">
                  <span class="badge" :class="{ off: !p.visible }">{{ p.visible ? 'VISIBLE' : 'HIDDEN' }}</span>
                  {{ p.title }}
                </p>
                <p class="summary">{{ p.summary }}</p>
                <p class="meta">
                  <span v-if="p.category" class="chip">{{ p.category }}</span>
                  <span v-for="t in p.tags.slice(0, 4)" :key="t" class="chip muted">{{ t }}</span>
                  <span class="sep">·</span>
                  <span>ID: {{ p.id }}</span>
                </p>
              </div>
              <div class="row-actions">
                <button class="ghost-btn" type="button" @click="router.push(`/admin/projects/${p.id}/edit`)">编辑</button>
                <button class="danger-btn" type="button" @click="removeProject(p)">删除</button>
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
  grid-template-columns: 1.4fr 0.8fr 0.6fr auto;
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

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.6px;
  font-weight: 900;
  color: var(--success);
  background: rgba(47, 126, 85, 0.12);
  border: 1px solid rgba(47, 126, 85, 0.25);
}

.badge.off {
  color: var(--error);
  background: rgba(180, 71, 95, 0.1);
  border-color: rgba(180, 71, 95, 0.25);
}

.row-actions {
  display: flex;
  gap: 10px;
  align-items: flex-start;
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
    justify-content: flex-end;
  }
}
</style>

