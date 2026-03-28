/**
 * 管理端文件 API：`/api/admin/files` 与公开下载 URL（accessUrl）字段对齐后端 DTO。
 */
import http, { type ApiResponse } from './http'

export interface PagedResult<T> {
  list: T[]
  total: number
  page: number
  limit: number
}

export interface FileItem {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  storageKey: string
  accessUrl: string
  uploadedBy?: string | null
  createdAt: string
  deletedAt?: string | null
}

function adminHeaders(): Record<string, string> | undefined {
  if (!import.meta.env.DEV) return undefined
  const token = String(import.meta.env.VITE_ADMIN_TOKEN ?? '').trim()
  return token ? { 'X-Admin-Token': token } : undefined
}

export async function fetchAdminFiles(params: {
  fileType?: string
  page?: number
  limit?: number
}) {
  const response = await http.get<ApiResponse<PagedResult<FileItem>>>('/api/admin/files', {
    params,
    headers: adminHeaders(),
  })
  return response.data
}

export async function uploadAdminFile(file: File, folder?: string) {
  const body = new FormData()
  body.append('file', file)
  const f = folder?.trim()
  if (f) body.append('folder', f)
  const response = await http.post<ApiResponse<FileItem>>('/api/admin/files/upload', body, {
    headers: adminHeaders(),
    timeout: 60_000,
  })
  return response.data
}

export async function deleteAdminFile(id: number) {
  const response = await http.delete<ApiResponse<null>>(`/api/admin/files/${id}`, {
    headers: adminHeaders(),
  })
  return response.data
}
