<script setup lang="ts">
/**
 * 管理端新建/编辑：表单校验与后端 Bean Validation 大致对齐；标签以逗号分隔再解析为数组提交。
 */
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../../stores/auth'
import {
  createAdminProject,
  fetchAdminProjectDetail,
  type Project,
  type ProjectUpsertPayload,
  updateAdminProject,
} from '../../../api/projects'
import BackendOfflineBanner from '../../../components/common/BackendOfflineBanner.vue'
import ErrorMessage from '../../../components/common/ErrorMessage.vue'
import Loading from '../../../components/common/Loading.vue'
import { mapApiErrorCodeToMessage, toUserFriendlyApiError } from '../../../auth/error-code'
import { hasAccessToken } from '../../../auth/token'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const mode = computed(() => (route.name === 'admin-project-new' ? 'create' : 'edit'))
const id = computed(() => Number(route.params.id))
const editIdValid = computed(() => mode.value !== 'edit' || (Number.isFinite(id.value) && id.value > 0))

const loading = ref(true)
const saving = ref(false)
const offline = ref(false)
const traceId = ref('')
const error = ref<string | null>(null)
const success = ref<string | null>(null)

const hasAdminPermission = computed(() => authStore.profile?.permissions.includes('PERM_ADMIN_PANEL') ?? false)
const adminTokenConfigured = computed(() => Boolean(import.meta.env.VITE_ADMIN_TOKEN))

const form = ref<ProjectUpsertPayload>({
  title: '',
  summary: '',
  description: '',
  category: '',
  tags: [],
  coverUrl: '',
  projectUrl: '',
  sourceUrl: '',
  sortOrder: 0,
  visible: true,
})

const tagsText = ref('')

function parseTags(input: string) {
  return input
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
}

const validationError = computed(() => {
  if (!form.value.title.trim()) return '标题不能为空'
  if (form.value.title.length > 200) return '标题过长（<= 200）'
  if (!form.value.summary.trim()) return '摘要不能为空'
  if (form.value.summary.length > 500) return '摘要过长（<= 500）'

  const tags = parseTags(tagsText.value)
  if (tags.length < 1) return '至少需要 1 个标签（逗号分隔）'
  if (tags.length > 10) return '标签最多 10 个'
  if (tags.some((t) => t.length > 60)) return '单个标签最长 60'

  if (form.value.category && form.value.category.length > 80) return '分类过长（<= 80）'
  const urls = [form.value.coverUrl, form.value.projectUrl, form.value.sourceUrl].filter(Boolean) as string[]
  if (urls.some((u) => u.length > 1000)) return 'URL 过长（<= 1000）'

  return null
})

function setFromProject(p: Project) {
  form.value = {
    title: p.title ?? '',
    summary: p.summary ?? '',
    description: p.description ?? '',
    category: p.category ?? '',
    tags: p.tags ?? [],
    coverUrl: p.coverUrl ?? '',
    projectUrl: p.projectUrl ?? '',
    sourceUrl: p.sourceUrl ?? '',
    sortOrder: p.sortOrder ?? 0,
    visible: p.visible ?? true,
  }
  tagsText.value = (p.tags ?? []).join(', ')
}

function buildPayload(): ProjectUpsertPayload {
  const payload: ProjectUpsertPayload = {
    ...form.value,
    title: form.value.title.trim(),
    summary: form.value.summary.trim(),
    description: form.value.description ? form.value.description.trim() : '',
    category: form.value.category ? form.value.category.trim() : '',
    coverUrl: form.value.coverUrl ? form.value.coverUrl.trim() : '',
    projectUrl: form.value.projectUrl ? form.value.projectUrl.trim() : '',
    sourceUrl: form.value.sourceUrl ? form.value.sourceUrl.trim() : '',
    sortOrder: form.value.sortOrder ?? 0,
    tags: parseTags(tagsText.value),
    visible: form.value.visible,
  }
  return payload
}

async function loadForEdit() {
  if (mode.value !== 'edit') return
  if (!Number.isFinite(id.value) || id.value <= 0) {
    error.value = '无效的项目 ID'
    loading.value = false
    return
  }
  const response = await fetchAdminProjectDetail(id.value)
  traceId.value = response.traceId
  if (!response.success || !response.data) {
    throw new Error(mapApiErrorCodeToMessage(response.error))
  }
  setFromProject(response.data)
}

async function load() {
  if (!hasAccessToken()) {
    await router.replace({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  loading.value = true
  offline.value = false
  error.value = null
  success.value = null
  traceId.value = ''

  try {
    await loadForEdit()
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    loading.value = false
  }
}

async function save() {
  success.value = null
  error.value = null

  if (!editIdValid.value) {
    error.value = '无效的项目 ID'
    return
  }

  if (!hasAdminPermission.value) {
    error.value = '权限不足：缺少 PERM_ADMIN_PANEL'
    return
  }
  if (!adminTokenConfigured.value) {
    error.value = '未配置 VITE_ADMIN_TOKEN'
    return
  }
  if (validationError.value) {
    error.value = validationError.value
    return
  }

  saving.value = true
  offline.value = false
  traceId.value = ''
  try {
    const payload = buildPayload()
    const response =
      mode.value === 'create' ? await createAdminProject(payload) : await updateAdminProject(id.value, payload)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    success.value = mode.value === 'create' ? '创建成功' : '保存成功'
    await router.replace('/admin/projects')
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    saving.value = false
  }
}

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
          <h1>{{ mode === 'create' ? '新建项目' : '编辑项目' }}</h1>
          <p class="desc">管理端写操作需要管理员权限与 `X-Admin-Token`。</p>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" @click="router.push('/admin/projects')">返回列表</button>
          <button class="submit-btn" type="button" :disabled="saving || loading" @click="save">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>

      <template v-if="loading">
        <Loading title="正在加载…" hint="从 /api/admin/projects 获取数据" />
      </template>

      <template v-else>
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="load" />
        <ErrorMessage v-if="error && !offline" :message="error" />
        <p v-if="success" class="state success">{{ success }}</p>

        <div v-if="!editIdValid" class="warn">
          <p class="warn-title">无效的项目 ID</p>
          <p class="warn-desc">请返回列表重新选择要编辑的项目。</p>
        </div>

        <div v-else-if="!hasAdminPermission" class="warn">
          <p class="warn-title">权限不足</p>
          <p class="warn-desc">当前账号缺少 `PERM_ADMIN_PANEL`，无法进行管理端写操作。</p>
        </div>

        <div v-else-if="!adminTokenConfigured" class="warn">
          <p class="warn-title">未配置 Admin Token</p>
          <p class="warn-desc">请在前端环境变量中设置 `VITE_ADMIN_TOKEN`（仅用于开发环境联调）。</p>
        </div>

        <form v-else class="form" @submit.prevent="save">
          <label class="field">
            <span>标题 *</span>
            <input v-model="form.title" maxlength="200" placeholder="项目标题（<= 200）" />
          </label>

          <label class="field">
            <span>摘要 *</span>
            <input v-model="form.summary" maxlength="500" placeholder="一句话说明（<= 500）" />
          </label>

          <label class="field">
            <span>分类</span>
            <input v-model="form.category" maxlength="80" placeholder="例如 web / backend / frontend" />
          </label>

          <label class="field">
            <span>标签 *（逗号分隔，1~10 个）</span>
            <input v-model="tagsText" placeholder="Vue 3, Spring Boot, MySQL" />
          </label>

          <label class="field">
            <span>描述（可选）</span>
            <textarea v-model="form.description" class="textarea" rows="6" placeholder="支持 Markdown（后端按字符串存储）" />
          </label>

          <div class="grid-2">
            <label class="field">
              <span>封面 URL</span>
              <input v-model="form.coverUrl" maxlength="1000" placeholder="https://..." />
              <p class="field-hint">
                可从 <router-link to="/admin/files">文件管理</router-link> 复制直链作为封面。
              </p>
            </label>
            <label class="field">
              <span>项目 URL</span>
              <input v-model="form.projectUrl" maxlength="1000" placeholder="https://..." />
            </label>
            <label class="field">
              <span>源码 URL</span>
              <input v-model="form.sourceUrl" maxlength="1000" placeholder="https://..." />
            </label>
            <label class="field">
              <span>排序权重</span>
              <input v-model.number="form.sortOrder" type="number" placeholder="0" />
            </label>
          </div>

          <label class="check">
            <input v-model="form.visible" type="checkbox" />
            <span>前台可见（visible）</span>
          </label>

          <p v-if="validationError" class="hint error">校验：{{ validationError }}</p>
          <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
        </form>
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

.form {
  margin-top: 12px;
  display: grid;
  gap: 12px;
}

.textarea {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  color: var(--text-main);
  background: #ffffff;
  resize: vertical;
}

.textarea:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(79, 116, 163, 0.18);
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.field-hint a {
  color: var(--accent);
  font-weight: 700;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.check {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
  font-weight: 700;
}

.hint {
  margin: 0;
  font-size: 13px;
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

@media (max-width: 860px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>

