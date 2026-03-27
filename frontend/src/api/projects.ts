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

