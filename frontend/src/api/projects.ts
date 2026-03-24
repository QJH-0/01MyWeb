import axios from 'axios'

export type Project = {
  id: number
  title: string
  summary: string
  category: string | null
  tags: string[]
  coverUrl: string | null
  githubUrl: string | null
  demoUrl: string | null
  visible: boolean
  createdAt: string
  updatedAt: string
}

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

export async function getProjects(params: {
  page?: number
  limit?: number
  category?: string
} = {}): Promise<PagedResult<Project>> {
  const { page = 0, limit = 12, category } = params
  const response = await axios.get<ApiResponse<PagedResult<Project>>>('/api/projects', {
    params: {
      page,
      limit,
      category: category && category.trim() ? category.trim() : undefined
    }
  })

  if (!response.data.success) {
    throw new Error(response.data.error || '项目列表获取失败')
  }
  return response.data.data
}

export async function getProjectById(id: number): Promise<Project> {
  const response = await axios.get<ApiResponse<Project>>(`/api/projects/${id}`)
  if (!response.data.success) {
    throw new Error(response.data.error || '项目详情获取失败')
  }
  return response.data.data
}

