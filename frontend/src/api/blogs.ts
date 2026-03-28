/**
 * 博客 HTTP 客户端：公开 `/api/blogs` 与管理端 `/api/admin/blogs` 契约与后端 DTO 字段名（camelCase）对齐。
 */
import http, { type ApiResponse } from './http'

export interface PagedResult<T> {
  list: T[]
  total: number
  page: number
  limit: number
}

export interface Blog {
  id: number
  title: string
  slug: string
  summary: string | null
  content?: string | null
  category?: string | null
  tags: string[]
  status: string
  coverUrl?: string | null
  viewCount?: number | null
  publishedAt?: string | null
  createdAt: string
  updatedAt: string
  deletedAt?: string | null
}

/** 与 {@link BlogCreateRequest} / {@link BlogUpdateRequest} 一致的管理端写入体。 */
export interface BlogUpsertPayload {
  title: string
  slug: string
  summary: string
  category?: string | null
  tags: string[]
  content: string
  coverUrl?: string | null
}

function adminHeaders() {
  if (!import.meta.env.DEV) return undefined
  const token = String(import.meta.env.VITE_ADMIN_TOKEN ?? '').trim()
  return token ? { 'X-Admin-Token': token } : undefined
}

export async function fetchBlogs(params?: { category?: string; tag?: string; page?: number; limit?: number }) {
  const response = await http.get<ApiResponse<PagedResult<Blog>>>('/api/blogs', { params })
  return response.data
}

export async function fetchBlogDetail(id: number) {
  const response = await http.get<ApiResponse<Blog>>(`/api/blogs/${id}`)
  return response.data
}

export async function fetchAdminBlogs(params?: {
  status?: string
  category?: string
  page?: number
  limit?: number
}) {
  const response = await http.get<ApiResponse<PagedResult<Blog>>>('/api/admin/blogs', {
    params,
    headers: adminHeaders(),
  })
  return response.data
}

export async function fetchAdminBlogDetail(id: number) {
  const response = await http.get<ApiResponse<Blog>>(`/api/admin/blogs/${id}`, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function createAdminBlog(payload: BlogUpsertPayload) {
  const response = await http.post<ApiResponse<Blog>>('/api/admin/blogs', payload, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function updateAdminBlog(id: number, payload: BlogUpsertPayload) {
  const response = await http.put<ApiResponse<Blog>>(`/api/admin/blogs/${id}`, payload, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function deleteAdminBlog(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/api/admin/blogs/${id}`, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function publishAdminBlog(id: number) {
  const response = await http.post<ApiResponse<Blog>>(`/api/admin/blogs/${id}/publish`, {}, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function unpublishAdminBlog(id: number) {
  const response = await http.post<ApiResponse<Blog>>(`/api/admin/blogs/${id}/unpublish`, {}, {
    headers: adminHeaders(),
  })
  return response.data
}
