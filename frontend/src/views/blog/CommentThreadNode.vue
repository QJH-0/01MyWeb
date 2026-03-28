<script setup lang="ts">
/**
 * 单层评论节点 + 递归子回复；事件向上透传，便于根面板统一处理点赞/回复。
 */
import { hasAccessToken } from '../../auth/token'
import type { CommentItem } from '../../api/comments'
import type { CommentTreeNode } from '../../utils/commentTree'
import { threadNodeToItem } from '../../utils/commentTree'
import CommentThreadNode from './CommentThreadNode.vue'

defineProps<{
  node: CommentTreeNode
  likingId: number | null
  submitting: boolean
}>()

const emit = defineEmits<{
  like: [id: number]
  reply: [item: CommentItem]
}>()

function formatTime(iso: string) {
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('zh-CN')
}

function onReply(n: CommentTreeNode) {
  emit('reply', threadNodeToItem(n))
}
</script>

<template>
  <li class="thread-item">
    <div class="head">
      <span class="author">{{ node.authorUsername || '用户' }}</span>
    </div>
    <p class="body">{{ node.content }}</p>
    <div class="row">
      <span class="muted">{{ formatTime(node.createdAt) }}</span>
      <span class="muted">赞 {{ node.likeCount }}</span>
      <button
        type="button"
        class="linkish"
        :disabled="likingId === node.id || submitting"
        @click="emit('like', node.id)"
      >
        点赞
      </button>
      <button
        type="button"
        class="linkish"
        :disabled="submitting || !hasAccessToken()"
        @click="onReply(node)"
      >
        回复
      </button>
    </div>
    <ul v-if="node.replies.length > 0" class="nested">
      <CommentThreadNode
        v-for="ch in node.replies"
        :key="ch.id"
        :node="ch"
        :liking-id="likingId"
        :submitting="submitting"
        @like="emit('like', $event)"
        @reply="emit('reply', $event)"
      />
    </ul>
  </li>
</template>

<style scoped>
.thread-item {
  list-style: none;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(203, 216, 231, 0.75);
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}

.author {
  font-weight: 800;
  font-size: 14px;
  color: var(--text-main);
}

.muted {
  font-size: 13px;
  color: #6a7f96;
}

.body {
  margin: 0 0 8px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.55;
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.linkish {
  border: none;
  background: none;
  padding: 0;
  font-size: 13px;
  font-weight: 800;
  color: var(--accent, #4f74a3);
  cursor: pointer;
}

.linkish:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.nested {
  list-style: none;
  margin: 12px 0 0;
  padding: 0 0 0 18px;
  border-left: 3px solid rgba(79, 116, 163, 0.28);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
