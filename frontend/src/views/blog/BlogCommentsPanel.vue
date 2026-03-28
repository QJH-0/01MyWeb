<script setup lang="ts">
/**
 * 评论面板：博客/项目详情复用；列表公开；写操作依赖登录，失败不误报成功（含网络/401/403）。
 */
import axios from 'axios'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  createComment,
  fetchComments,
  replyToComment,
  toggleCommentLike,
  type CommentItem,
} from '../../api/comments'
import { mapApiErrorCodeToMessage } from '../../auth/error-code'
import { hasAccessToken } from '../../auth/token'
import { buildCommentTree } from '../../utils/commentTree'
import CommentThreadNode from './CommentThreadNode.vue'

const props = withDefaults(
  defineProps<{
    targetType?: 'blog' | 'project'
    targetId: number
    /** 详情页处于离线降级时禁止拉取评论，避免对示例 ID 误请求 */
    offline: boolean
  }>(),
  { targetType: 'blog' },
)

const route = useRoute()
const loginRedirect = computed(() => ({ name: 'login', query: { redirect: route.fullPath } }))

const loading = ref(false)
const items = ref<CommentItem[]>([])
const total = ref(0)
const page = ref(0)
const limit = 30
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)

const newContent = ref('')
const replyTo = ref<{ id: number; preview: string } | null>(null)
const replyBody = ref('')
const submitting = ref(false)
const likingId = ref<number | null>(null)

async function loadFirstPage() {
  if (props.offline || !Number.isFinite(props.targetId) || props.targetId <= 0) {
    items.value = []
    total.value = 0
    loadError.value = props.offline ? null : null
    return
  }
  loading.value = true
  loadError.value = null
  page.value = 0
  try {
    const res = await fetchComments({ targetType: props.targetType, targetId: props.targetId, page: 0, limit })
    if (!res.success || !res.data) {
      loadError.value = mapApiErrorCodeToMessage(res.error)
      items.value = []
      total.value = 0
      return
    }
    items.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    if (axios.isAxiosError(e) && !e.response) {
      loadError.value = '无法连接后端，评论列表暂时不可用。'
    } else {
      const code = axios.isAxiosError(e) ? (e.response?.data as { error?: string } | undefined)?.error : undefined
      loadError.value = mapApiErrorCodeToMessage(code) ?? '评论加载失败。'
    }
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (props.offline || items.value.length >= total.value) return
  const nextPage = page.value + 1
  try {
    const res = await fetchComments({ targetType: props.targetType, targetId: props.targetId, page: nextPage, limit })
    if (res.success && res.data) {
      page.value = nextPage
      items.value = items.value.concat(res.data.list)
    }
  } catch {
    actionError.value = '加载更多失败，请稍后重试。'
  }
}

async function submitNew() {
  actionError.value = null
  const text = newContent.value.trim()
  if (!text) {
    actionError.value = '请输入评论内容。'
    return
  }
  if (!hasAccessToken()) {
    actionError.value = '请先登录后再发表评论。'
    return
  }
  submitting.value = true
  try {
    const res = await createComment({ targetType: props.targetType, targetId: props.targetId, content: text })
    if (!res.success || !res.data) {
      actionError.value = mapCommentWriteError(res.error)
      return
    }
    newContent.value = ''
    await loadFirstPage()
  } catch (e) {
    actionError.value = commentWriteErrorFromAxios(e)
  } finally {
    submitting.value = false
  }
}

async function submitReply() {
  actionError.value = null
  if (!replyTo.value) return
  const text = replyBody.value.trim()
  if (!text) {
    actionError.value = '请输入回复内容。'
    return
  }
  if (!hasAccessToken()) {
    actionError.value = '请先登录后再回复。'
    return
  }
  submitting.value = true
  try {
    const res = await replyToComment(replyTo.value.id, { content: text })
    if (!res.success || !res.data) {
      actionError.value = mapCommentWriteError(res.error)
      return
    }
    replyTo.value = null
    replyBody.value = ''
    await loadFirstPage()
  } catch (e) {
    actionError.value = commentWriteErrorFromAxios(e)
  } finally {
    submitting.value = false
  }
}

async function onLike(id: number) {
  actionError.value = null
  if (!hasAccessToken()) {
    actionError.value = '请先登录后再点赞。'
    return
  }
  likingId.value = id
  try {
    const res = await toggleCommentLike(id)
    if (!res.success || res.data == null) {
      actionError.value = mapCommentWriteError(res.error)
      return
    }
    const { likeCount } = res.data
    items.value = items.value.map((c) => (c.id === id ? { ...c, likeCount } : c))
  } catch (e) {
    actionError.value = commentWriteErrorFromAxios(e)
  } finally {
    likingId.value = null
  }
}

function mapCommentWriteError(code: string | null | undefined) {
  if (code === 'UNAUTHORIZED' || code === 'FORBIDDEN') {
    return '请先登录或确认账号具备评论权限后再试。'
  }
  return mapApiErrorCodeToMessage(code)
}

function commentWriteErrorFromAxios(e: unknown) {
  if (axios.isAxiosError(e)) {
    const code = (e.response?.data as { error?: string } | undefined)?.error
    return mapCommentWriteError(code)
  }
  return '操作失败，请稍后再试。'
}

function startReply(c: CommentItem) {
  actionError.value = null
  const who = c.authorUsername ? `${c.authorUsername}：` : ''
  replyTo.value = { id: c.id, preview: `${who}${c.content}`.slice(0, 80) }
  replyBody.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyBody.value = ''
}

const hasMore = computed(() => !props.offline && items.value.length < total.value)

const threadRoots = computed(() => buildCommentTree(items.value))

watch(
  () => [props.targetId, props.targetType, props.offline] as const,
  () => {
    void loadFirstPage()
  },
  { immediate: true },
)
</script>

<template>
  <section class="comments-panel" aria-label="评论">
    <h2 class="h3">评论</h2>

    <p v-if="offline" class="muted">后端不可用时暂不加载评论；连接恢复后刷新页面即可。</p>

    <template v-else>
      <p v-if="loading" class="muted">正在加载评论…</p>
      <p v-else-if="loadError" class="err">{{ loadError }}</p>

      <p v-if="actionError" class="err">{{ actionError }}</p>

      <div v-if="!loading && !loadError && threadRoots.length === 0" class="muted">还没有评论，欢迎抢沙发。</div>

      <ul v-else class="list">
        <CommentThreadNode
          v-for="root in threadRoots"
          :key="root.id"
          :node="root"
          :liking-id="likingId"
          :submitting="submitting"
          @like="onLike"
          @reply="startReply"
        />
      </ul>

      <button
        v-if="hasMore"
        type="button"
        class="more"
        :disabled="loading"
        @click="loadMore"
      >
        加载更多（{{ items.length }} / {{ total }}）
      </button>

      <div v-if="replyTo" class="composer reply-composer">
        <p class="muted">回复：{{ replyTo.preview }}{{ replyTo.preview.length >= 80 ? '…' : '' }}</p>
        <textarea v-model="replyBody" class="area" rows="3" maxlength="8000" placeholder="回复内容" />
        <div class="actions">
          <button type="button" class="btn secondary" :disabled="submitting" @click="cancelReply">取消</button>
          <button type="button" class="btn primary" :disabled="submitting" @click="submitReply">发表回复</button>
        </div>
      </div>

      <div class="composer">
        <template v-if="hasAccessToken()">
          <textarea v-model="newContent" class="area" rows="4" maxlength="8000" placeholder="发表评论" />
          <button type="button" class="btn primary" :disabled="submitting" @click="submitNew">发表评论</button>
        </template>
        <template v-else>
          <p class="muted">登录后可发表评论、回复与点赞。</p>
          <router-link class="login-link" :to="loginRedirect">去登录</router-link>
        </template>
      </div>
    </template>
  </section>
</template>

<style scoped>
.comments-panel {
  margin-top: 28px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid rgba(203, 216, 231, 0.95);
  background: rgba(79, 116, 163, 0.04);
}

.h3 {
  margin: 0 0 12px;
  font-size: 1.1rem;
  font-weight: 800;
}

.muted {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6a7f96;
}

.err {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--error, #b4475f);
}

.list {
  list-style: none;
  margin: 0 0 16px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.more {
  margin-bottom: 14px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid rgba(203, 216, 231, 0.95);
  background: rgba(255, 255, 255, 0.85);
  font-weight: 800;
  cursor: pointer;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-composer {
  margin-bottom: 14px;
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed rgba(203, 216, 231, 0.9);
  background: rgba(255, 255, 255, 0.55);
}

.area {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(203, 216, 231, 0.95);
  font: inherit;
  resize: vertical;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 14px;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
  border: 1px solid transparent;
}

.btn.primary {
  background: var(--accent, #4f74a3);
  color: #fff;
}

.btn.secondary {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(203, 216, 231, 0.95);
  color: var(--text-main);
}

.login-link {
  font-weight: 800;
  color: var(--accent, #4f74a3);
  text-decoration: none;
  width: fit-content;
}

.login-link:hover {
  text-decoration: underline;
}
</style>
