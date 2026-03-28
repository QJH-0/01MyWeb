<script setup lang="ts">
/**
 * 管理端新建/编辑博客：字段与后端 Bean Validation 对齐；发布/下架单独调用接口，失败不误报成功。
 */
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../../stores/auth'
import {
  createAdminBlog,
  fetchAdminBlogDetail,
  publishAdminBlog,
  unpublishAdminBlog,
  updateAdminBlog,
  type Blog,
  type BlogUpsertPayload,
} from '../../../api/blogs'
import BackendOfflineBanner from '../../../components/common/BackendOfflineBanner.vue'
import ErrorMessage from '../../../components/common/ErrorMessage.vue'
import Loading from '../../../components/common/Loading.vue'
import { mapApiErrorCodeToMessage, toUserFriendlyApiError } from '../../../auth/error-code'
import { hasAccessToken } from '../../../auth/token'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const mode = computed(() => (route.name === 'admin-blog-new' ? 'create' : 'edit'))
const id = computed(() => Number(route.params.id))
const editIdValid = computed(() => mode.value !== 'edit' || (Number.isFinite(id.value) && id.value > 0))

const loading = ref(true)
const saving = ref(false)
const publishing = ref(false)
const offline = ref(false)
const traceId = ref('')
const error = ref<string | null>(null)
const success = ref<string | null>(null)

const hasAdminPermission = computed(() => authStore.profile?.permissions.includes('PERM_ADMIN_PANEL') ?? false)
const adminTokenConfigured = computed(() => Boolean(import.meta.env.VITE_ADMIN_TOKEN))

const form = ref<BlogUpsertPayload>({
  title: '',
  slug: '',
  summary: '',
  category: '',
  tags: [],
  content: '',
  coverUrl: '',
})

const tagsText = ref('')
const currentStatus = ref<string>('DRAFT')

const SLUG_PATTERN = /^[a-zA-Z0-9_-]+$/

function parseTags(input: string) {
  return input
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
}

const validationError = computed(() => {
  if (!form.value.title.trim()) return '标题不能为空'
  if (form.value.title.length > 160) return '标题过长（<= 160）'
  if (!form.value.slug.trim()) return 'slug 不能为空'
  if (form.value.slug.length > 200) return 'slug 过长（<= 200）'
  if (!SLUG_PATTERN.test(form.value.slug.trim())) return 'slug 仅允许字母、数字、下划线与连字符'
  if (!form.value.summary.trim()) return '摘要不能为空'
  if (form.value.summary.length > 500) return '摘要过长（<= 500）'
  if (form.value.category && form.value.category.length > 80) return '分类过长（<= 80）'

  const tags = parseTags(tagsText.value)
  if (tags.length < 1) return '至少需要 1 个标签（逗号分隔）'
  if (tags.length > 10) return '标签最多 10 个'
  if (tags.some((t) => t.length > 60)) return '单个标签最长 60'

  if (!form.value.content || !/\S/.test(form.value.content)) return '正文不能为空且不能为纯空白'
  if (form.value.coverUrl && form.value.coverUrl.length > 1000) return '封面 URL 过长（<= 1000）'

  return null
})

function setFromBlog(b: Blog) {
  form.value = {
    title: b.title ?? '',
    slug: b.slug ?? '',
    summary: b.summary ?? '',
    category: b.category ?? '',
    tags: b.tags ?? [],
    content: b.content ?? '',
    coverUrl: b.coverUrl ?? '',
  }
  tagsText.value = (b.tags ?? []).join(', ')
  currentStatus.value = b.status ?? 'DRAFT'
}

function buildPayload(): BlogUpsertPayload {
  return {
    title: form.value.title.trim(),
    slug: form.value.slug.trim(),
    summary: form.value.summary.trim(),
    category: form.value.category ? form.value.category.trim() : '',
    tags: parseTags(tagsText.value),
    content: form.value.content,
    coverUrl: form.value.coverUrl ? form.value.coverUrl.trim() : '',
  }
}

async function loadForEdit() {
  if (mode.value !== 'edit') return
  if (!Number.isFinite(id.value) || id.value <= 0) {
    error.value = '无效的博客 ID'
    loading.value = false
    return
  }
  const response = await fetchAdminBlogDetail(id.value)
  traceId.value = response.traceId
  if (!response.success || !response.data) {
    throw new Error(mapApiErrorCodeToMessage(response.error))
  }
  setFromBlog(response.data)
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
    if (mode.value === 'create') {
      form.value = {
        title: '',
        slug: '',
        summary: '',
        category: '',
        tags: [],
        content: '',
        coverUrl: '',
      }
      tagsText.value = ''
      currentStatus.value = 'DRAFT'
    } else {
      await loadForEdit()
    }
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
    error.value = '无效的博客 ID'
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
      mode.value === 'create' ? await createAdminBlog(payload) : await updateAdminBlog(id.value, payload)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    success.value = mode.value === 'create' ? '创建成功' : '保存成功'
    await router.replace('/admin/blogs')
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (mode.value === 'create' || !editIdValid.value) {
    error.value = '请先保存为草稿后再发布'
    return
  }
  if (!hasAdminPermission.value || !adminTokenConfigured.value) {
    error.value = '权限或 Admin Token 未就绪'
    return
  }
  publishing.value = true
  error.value = null
  success.value = null
  try {
    const response = await publishAdminBlog(id.value)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    currentStatus.value = 'PUBLISHED'
    success.value = '已发布'
    if (response.data) setFromBlog(response.data)
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    publishing.value = false
  }
}

async function unpublish() {
  if (mode.value === 'create' || !editIdValid.value) return
  if (!hasAdminPermission.value || !adminTokenConfigured.value) {
    error.value = '权限或 Admin Token 未就绪'
    return
  }
  publishing.value = true
  error.value = null
  success.value = null
  try {
    const response = await unpublishAdminBlog(id.value)
    traceId.value = response.traceId
    if (!response.success) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    currentStatus.value = 'DRAFT'
    success.value = '已下架为草稿'
    if (response.data) setFromBlog(response.data)
  } catch (e) {
    offline.value = axios.isAxiosError(e) ? !e.response : true
    error.value = toUserFriendlyApiError(e)
  } finally {
    publishing.value = false
  }
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
          <h1>{{ mode === 'create' ? '新建文章' : '编辑文章' }}</h1>
          <p class="desc">新建默认为草稿；发布后前台可见。</p>
        </div>
        <div class="head-actions">
          <button class="ghost-btn" type="button" @click="router.push('/admin/blogs')">返回列表</button>
          <template v-if="mode === 'edit' && currentStatus !== 'PUBLISHED'">
            <button class="submit-btn" type="button" :disabled="publishing || loading" @click="publish">
              {{ publishing ? '处理中…' : '发布' }}
            </button>
          </template>
          <template v-if="mode === 'edit' && currentStatus === 'PUBLISHED'">
            <button class="ghost-btn" type="button" :disabled="publishing || loading" @click="unpublish">
              {{ publishing ? '处理中…' : '下架' }}
            </button>
          </template>
          <button class="submit-btn" type="button" :disabled="saving || loading" @click="save">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>

      <template v-if="loading">
        <Loading title="正在加载…" hint="从 /api/admin/blogs 获取数据" />
      </template>

      <template v-else>
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="load" />
        <ErrorMessage v-if="error && !offline" :message="error" />
        <p v-if="success" class="state success">{{ success }}</p>

        <div v-if="!editIdValid" class="warn">
          <p class="warn-title">无效的博客 ID</p>
          <p class="warn-desc">请从列表重新选择。</p>
        </div>

        <div v-else-if="!hasAdminPermission" class="warn">
          <p class="warn-title">权限不足</p>
          <p class="warn-desc">缺少 `PERM_ADMIN_PANEL`。</p>
        </div>

        <div v-else-if="!adminTokenConfigured" class="warn">
          <p class="warn-title">未配置 Admin Token</p>
          <p class="warn-desc">请设置 `VITE_ADMIN_TOKEN`。</p>
        </div>

        <form v-else class="form" @submit.prevent="save">
          <p v-if="mode === 'edit'" class="status-line">
            当前状态：<strong>{{ currentStatus === 'PUBLISHED' ? '已发布' : '草稿' }}</strong>
          </p>

          <label class="field">
            <span>标题 *</span>
            <input v-model="form.title" maxlength="160" placeholder="文章标题（<= 160）" />
          </label>

          <label class="field">
            <span>slug *（URL 片段，唯一）</span>
            <input v-model="form.slug" maxlength="200" placeholder="例如 my-first-post" />
          </label>

          <label class="field">
            <span>摘要 *</span>
            <input v-model="form.summary" maxlength="500" placeholder="列表与 SEO 用摘要（<= 500）" />
          </label>

          <label class="field">
            <span>分类</span>
            <input v-model="form.category" maxlength="80" placeholder="可选，<= 80" />
          </label>

          <label class="field">
            <span>标签 *（逗号分隔，1~10 个）</span>
            <input v-model="tagsText" placeholder="Vue, 后端, 随笔" />
          </label>

          <label class="field">
            <span>封面 URL</span>
            <input v-model="form.coverUrl" maxlength="1000" placeholder="https://..." />
          </label>

          <label class="field">
            <span>正文 *（Markdown）</span>
            <textarea v-model="form.content" class="textarea" rows="16" placeholder="支持 Markdown，需包含非空白字符" />
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

.status-line {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
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
</style>
