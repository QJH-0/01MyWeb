import http, { type ApiResponse } from './http'

export interface PagedResult<T> {
  list: T[]
  total: number
  page: number
  limit: number
}

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

function adminHeaders() {
  if (!import.meta.env.DEV) return undefined
  const token = String(import.meta.env.VITE_ADMIN_TOKEN ?? '').trim()
  return token ? { 'X-Admin-Token': token } : undefined
}

export async function fetchProjects(params?: { category?: string; page?: number; limit?: number }) {
  const response = await http.get<ApiResponse<PagedResult<Project>>>('/api/projects', {
    params,
  })
  return response.data
}

export async function fetchProjectDetail(id: number) {
  const response = await http.get<ApiResponse<Project>>(`/api/projects/${id}`)
  return response.data
}

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

export async function fetchAdminProjectDetail(id: number) {
  const response = await http.get<ApiResponse<Project>>(`/api/admin/projects/${id}`, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function createAdminProject(payload: ProjectUpsertPayload) {
  const response = await http.post<ApiResponse<Project>>('/api/admin/projects', payload, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function updateAdminProject(id: number, payload: ProjectUpsertPayload) {
  const response = await http.put<ApiResponse<Project>>(`/api/admin/projects/${id}`, payload, {
    headers: adminHeaders(),
  })
  return response.data
}

export async function deleteAdminProject(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/api/admin/projects/${id}`, {
    headers: adminHeaders(),
  })
  return response.data
}

