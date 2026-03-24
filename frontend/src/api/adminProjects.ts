import axios from 'axios'
import type { Project } from './projects'

type PagedResult<T> = {
  items: T[]
  total: number
  page: number
  limit: number
}

type ApiResponse<T> = {
  success: boolean
  data: T
  error: string | null
  timestamp: string
  traceId: string
}

export type ProjectFormPayload = {
  title: string
  summary: string
  category: string
  tags: string[]
  coverUrl: string
  githubUrl: string
  demoUrl: string
  visible: boolean
}

export async function getAdminProjects(params: {
  page?: number
  limit?: number
  category?: string
} = {}): Promise<PagedResult<Project>> {
  const response = await axios.get<ApiResponse<PagedResult<Project>>>('/api/admin/projects', {
    params
  })
  if (!response.data.success) {
    throw new Error(response.data.error || '后台项目列表获取失败')
  }
  return response.data.data
}

export async function createAdminProject(payload: ProjectFormPayload): Promise<Project> {
  const response = await axios.post<ApiResponse<Project>>('/api/admin/projects', payload)
  if (!response.data.success) {
    throw new Error(response.data.error || '项目创建失败')
  }
  return response.data.data
}

export async function updateAdminProject(id: number, payload: ProjectFormPayload): Promise<Project> {
  const response = await axios.put<ApiResponse<Project>>(`/api/admin/projects/${id}`, payload)
  if (!response.data.success) {
    throw new Error(response.data.error || '项目更新失败')
  }
  return response.data.data
}

export async function deleteAdminProject(id: number): Promise<void> {
  const response = await axios.delete<ApiResponse<null>>(`/api/admin/projects/${id}`)
  if (!response.data.success) {
    throw new Error(response.data.error || '项目下线失败')
  }
}

