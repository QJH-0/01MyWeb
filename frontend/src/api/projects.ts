/**
 * 作品/项目相关的 HTTP 客户端：与后端 `/api/projects`（访客）及 `/api/admin/projects`（管理）对齐。
 * 统一通过 {@link ApiResponse} 解析 envelope；分页结构需与接口契约一致，避免前端自行假设字段名。
 */
import http, { type ApiResponse } from './http'

/** 列表接口分页包装：与后端 list + total/page/limit 约定一致。 */
export interface PagedResult<T> {
  list: T[]
  total: number
  page: number
  limit: number
}

/** 详情/列表项实体：含软删时间等只读字段，创建与更新请用 {@link ProjectUpsertPayload}。 */
export interface Project {
  id: number
  title: string
  summary: string
  description?: string | null
  category?: string | null
  tags: string[]
  coverUrl?: string | null
  projectUrl?: string | null
  sourceUrl?: string | null
  visible: boolean
  sortOrder?: number | null
  createdAt: string
  updatedAt: string
  deletedAt?: string | null
}

/** 创建/更新请求体：仅包含可写业务字段；id 与时间戳由服务端生成或维护。 */
export interface ProjectUpsertPayload {
  title: string
  summary: string
  description?: string | null
  category?: string | null
  tags: string[]
  coverUrl?: string | null
  projectUrl?: string | null
  sourceUrl?: string | null
  sortOrder?: number | null
  visible: boolean
}

/**
 * 开发环境下为管理端请求附带 `X-Admin-Token`，便于本地联调；生产构建不包含此头，避免误把本地 token 打进产物。
 * Token 仅来自 `VITE_ADMIN_TOKEN`，禁止在代码中写死；若未配置则不带头（由后端返回 401 等，便于发现问题）。
 */
function adminHeaders() {
  if (!import.meta.env.DEV) return undefined
  const token = String(import.meta.env.VITE_ADMIN_TOKEN ?? '').trim()
  return token ? { 'X-Admin-Token': token } : undefined
}

/** 访客可见列表（公开接口）。 */
export async function fetchProjects(params?: { category?: string; page?: number; limit?: number }) {
  const response = await http.get<ApiResponse<PagedResult<Project>>>('/api/projects', {
    params,
  })
  return response.data
}

/** 访客可见详情（公开接口）。 */
export async function fetchProjectDetail(id: number) {
  const response = await http.get<ApiResponse<Project>>(`/api/projects/${id}`)
  return response.data
}

/** 管理端分页列表；需鉴权，开发环境通过 {@link adminHeaders} 注入。 */
export async function fetchAdminProjects(params?: {
  category?: string
  visible?: boolean
  page?: number
  limit?: number
}) {
  const response = await http.get<ApiResponse<PagedResult<Project>>>('/api/admin/projects', {
    params,
    headers: adminHeaders(),
  })
  return response.data
}

/** 管理端单条详情（含不可见/已删等，以后端策略为准）。 */
export async function fetchAdminProjectDetail(id: number) {
  const response = await http.get<ApiResponse<Project>>(`/api/admin/projects/${id}`, {
    headers: adminHeaders(),
  })
  return response.data
}

/** 管理端创建。 */
export async function createAdminProject(payload: ProjectUpsertPayload) {
  const response = await http.post<ApiResponse<Project>>('/api/admin/projects', payload, {
    headers: adminHeaders(),
  })
  return response.data
}

/** 管理端全量更新（payload 字段语义以后端 PUT 契约为准）。 */
export async function updateAdminProject(id: number, payload: ProjectUpsertPayload) {
  const response = await http.put<ApiResponse<Project>>(`/api/admin/projects/${id}`, payload, {
    headers: adminHeaders(),
  })
  return response.data
}

/** 管理端删除（软删或物理删由后端实现决定）。 */
export async function deleteAdminProject(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/api/admin/projects/${id}`, {
    headers: adminHeaders(),
  })
  return response.data
}

