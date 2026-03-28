/**
 * 评论 API：与 `/api/comments` 契约对齐；写操作依赖 http 拦截器注入的 Bearer。
 */
import http, { type ApiResponse } from './http'
import type { PagedResult } from './blogs'

export interface CommentItem {
  id: number
  targetType: string
  targetId: number
  content: string
  parentId: number | null
  authorUserId: number
  authorUsername: string
  likeCount: number
  createdAt: string
}

export async function fetchComments(params: {
  targetType: string
  targetId: number
  page?: number
  limit?: number
}) {
  const response = await http.get<ApiResponse<PagedResult<CommentItem>>>('/api/comments', {
    params: {
      targetType: params.targetType,
      targetId: params.targetId,
      page: params.page ?? 0,
      limit: params.limit ?? 30,
    },
  })
  return response.data
}

export async function createComment(payload: { targetType: string; targetId: number; content: string }) {
  const response = await http.post<ApiResponse<CommentItem>>('/api/comments', payload)
  return response.data
}

export async function replyToComment(commentId: number, payload: { content: string }) {
  const response = await http.post<ApiResponse<CommentItem>>(`/api/comments/${commentId}/reply`, payload)
  return response.data
}

export async function toggleCommentLike(commentId: number) {
  const response = await http.post<ApiResponse<{ liked: boolean; likeCount: number }>>(
    `/api/comments/${commentId}/like`,
    {},
  )
  return response.data
}
