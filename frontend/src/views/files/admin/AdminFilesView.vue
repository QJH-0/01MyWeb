<script setup lang="ts">
/**
 * 管理端文件：上传（类型/大小前端预检）、分页列表、复制访问链接、软删；后端不可用时展示占位示例并入离线横幅。
 */
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../../stores/auth'
import {
  deleteAdminFile,
  fetchAdminFiles,
  uploadAdminFile,
  type FileItem,
} from '../../../api/adminFiles'
import BackendOfflineBanner from '../../../components/common/BackendOfflineBanner.vue'
import EmptyState from '../../../components/common/EmptyState.vue'
import ErrorMessage from '../../../components/common/ErrorMessage.vue'
import Loading from '../../../components/common/Loading.vue'
import { mapApiErrorCodeToMessage, toUserFriendlyApiError } from '../../../auth/error-code'
import { hasAccessToken } from '../../../auth/token'

const router = useRouter()
const authStore = useAuthStore()

const MAX_BYTES = 10 * 1024 * 1024

const OFFLINE_DEMO: FileItem[] = [
  {
    id: -1,
    fileName: '示例-首页配图.png',
    fileType: 'image/png',
    fileSize: 36864,
    storageKey: '—',
    accessUrl: '/api/files/0/download',
    uploadedBy: 'demo',
    createdAt: new Date().toISOString(),
  },
  {
    id: -2,
    fileName: '示例-说明.md',
    fileType: 'text/markdown',
    fileSize: 512,
    storageKey: '—',
    accessUrl: '/api/files/0/download',
    uploadedBy: 'demo',
    createdAt: new Date().toISOString(),
  },
]

const loading = ref(true)
const uploading = ref(false)
const error = ref<string | null>(null)
const traceId = ref('')
const offline = ref(false)
const busyId = ref<number | null>(null)
const successToast = ref<string | null>(null)

const page = ref(0)
const limit = ref(10)
const fileTypeFilter = ref('')

type PageData = { list: FileItem[]; total: number; page: number; limit: number }
const data = ref<PageData | null>(null)

const hasAdminPermission = computed(() => authStore.profile?.permissions.includes('PERM_ADMIN_PANEL') ?? false)
const adminTokenConfigured = computed(() => Boolean(import.meta.env.VITE_ADMIN_TOKEN))

const files = computed(() => data.value?.list ?? [])
const total = computed(() => data.value?.total ?? 0)
const totalPages = computed(() => (limit.value > 0 ? Math.max(1, Math.ceil(total.value / limit.value)) : 1))

const displayFiles = computed(() => {
  if (offline.value && files.value.length === 0) {
    return OFFLINE_DEMO
  }
  return files.value
})

const isDemoRow = (id: number) => id < 0

const uploadFolder = ref('')

async function load() {
  if (!hasAccessToken()) {
    await router.replace({ name: 'login', query: { redirect: '/admin/files' } })
    return
  }

  loading.value = true
  error.value = null
  traceId.value = ''
  offline.value = false

  try {
    const response = await fetchAdminFiles({
      fileType: fileTypeFilter.value.trim() || undefined,
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

async function onFileSelected(ev: Event) {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (file.size > MAX_BYTES) {
    error.value = '文件超过 10MB 上限'
    return
  }

  error.value = null
  successToast.value = null
  uploading.value = true
  offline.value = false
  try {
    const response = await uploadAdminFile(file, uploadFolder.value)
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    successToast.value = '上传成功'
    await load()
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    uploading.value = false
  }
}

async function copyAccessUrl(url: string) {
  successToast.value = null
  try {
    await navigator.clipboard.writeText(url)
    successToast.value = '已复制访问链接'
  } catch {
    error.value = '复制失败，请手动选择链接'
  }
}

async function removeFile(f: FileItem) {
  if (isDemoRow(f.id)) return
  const ok = window.confirm(`确认删除「${f.fileName}」？后端将软删记录。`)
  if (!ok) return

  error.value = null
  successToast.value = null
  busyId.value = f.id
  try {
    const response = await deleteAdminFile(f.id)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    successToast.value = '已删除'
    await load()
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    busyId.value = null
  }
}

function formatBytes(n: number) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

function formatTime(iso: string) {
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
          <h1>文件管理</h1>
          <p class="desc">
            上传图片 / PDF / Markdown / 纯文本（≤10MB）。访问链接可粘贴到博客或项目封面字段。需 `PERM_ADMIN_PANEL` 与开发环境
            `VITE_ADMIN_TOKEN`。
          </p>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" @click="router.push('/admin/blogs')">博客后台</button>
        </div>
      </div>

      <template v-if="loading">
        <Loading title="正在加载文件列表…" hint="从 /api/admin/files 获取数据" />
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
          <div class="upload-panel">
            <label class="field">
              <span class="label">子目录（可选）</span>
              <input v-model.trim="uploadFolder" class="input" placeholder="如 docs / covers，勿含 .." />
            </label>
            <div class="upload-actions">
              <label class="submit-btn slim upload-btn">
                <input
                  type="file"
                  class="sr-only"
                  accept="image/*,.pdf,.md,.txt,text/markdown"
                  :disabled="uploading"
                  @change="onFileSelected"
                />
                {{ uploading ? '上传中…' : '选择文件上传' }}
              </label>
            </div>
          </div>

          <div class="toolbar">
            <label class="field">
              <span class="label">MIME 筛选</span>
              <input
                v-model.trim="fileTypeFilter"
                class="input"
                placeholder="如 image/png"
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
            <ErrorMessage :message="error" @retry="load" />
          </template>

          <p v-if="offline && files.length === 0" class="demo-note">后端连接失败：以下为占位示例数据，操作按钮已禁用。</p>

          <template v-if="!offline && displayFiles.length === 0">
            <EmptyState title="暂无文件" hint="点击上传或调整筛选条件。" />
          </template>

          <div v-else class="list" aria-label="管理端文件列表">
            <article v-for="f in displayFiles" :key="f.id" class="row">
              <div class="row-main">
                <p class="title">
                  <span v-if="isDemoRow(f.id)" class="badge muted">示例</span>
                  {{ f.fileName }}
                </p>
                <p class="meta">
                  <span class="chip mono">{{ f.fileType }}</span>
                  <span class="chip">{{ formatBytes(f.fileSize) }}</span>
                  <span v-if="f.uploadedBy" class="chip muted">{{ f.uploadedBy }}</span>
                  <span class="sep">·</span>
                  <span>ID {{ f.id > 0 ? f.id : '—' }}</span>
                  <span class="sep">·</span>
                  <span>{{ formatTime(f.createdAt) }}</span>
                </p>
                <p v-if="!isDemoRow(f.id)" class="url-line">
                  <code class="mono">{{ f.accessUrl }}</code>
                </p>
              </div>
              <div class="row-actions">
                <a
                  v-if="!isDemoRow(f.id)"
                  class="ghost-btn"
                  :href="f.accessUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  打开
                </a>
                <button class="ghost-btn" type="button" :disabled="isDemoRow(f.id)" @click="copyAccessUrl(f.accessUrl)">
                  复制链接
                </button>
                <button
                  class="danger-btn"
                  type="button"
                  :disabled="busyId === f.id || isDemoRow(f.id)"
                  @click="removeFile(f)"
                >
                  删除
                </button>
              </div>
            </article>
          </div>

          <div v-if="!offline && total > 0" class="pager" aria-label="分页">
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

.upload-panel {
  margin: 14px 0 8px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(203, 216, 231, 0.85);
  background: rgba(255, 255, 255, 0.65);
  display: grid;
  gap: 12px;
}

.upload-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.toolbar {
  margin: 14px 0 8px;
  display: grid;
  grid-template-columns: 1.3fr 0.6fr auto;
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

.demo-note {
  margin: 8px 0 4px;
  font-size: 13px;
  color: var(--text-secondary);
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
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.row {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(255, 255, 255, 0.72);
}

.title {
  margin: 0 0 6px;
  font-weight: 800;
  color: var(--text-main);
}

.badge {
  display: inline-block;
 margin-right: 6px;
 padding: 2px 8px;
 border-radius: 999px;
 font-size: 11px;
 font-weight: 800;
 background: rgba(79, 116, 163, 0.15);
 color: var(--accent);
}

.badge.muted {
  background: rgba(120, 120, 120, 0.12);
  color: var(--text-secondary);
}

.meta {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.url-line {
  margin: 8px 0 0;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  word-break: break-all;
}

.chip {
  display: inline-block;
  margin-right: 6px;
  padding: 2px 8px;
  border-radius: 8px;
  background: rgba(79, 116, 163, 0.12);
  font-size: 12px;
}

.chip.muted {
  background: rgba(120, 120, 120, 0.1);
}

.sep {
  margin: 0 4px;
  color: rgba(120, 120, 120, 0.55);
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.pager {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}

.pager-meta {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.trace {
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.state.success {
  margin: 10px 0 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(46, 139, 87, 0.12);
  color: #1f6b45;
  font-weight: 700;
}
</style>
