/**
 * 公开搜索：`GET /api/search` 与后端 SearchItemDTO 字段对齐。
 */
import http, { type ApiResponse } from './http'
import type { PagedResult } from './blogs'

export interface SearchItem {
  sourceType: string
  sourceId: number
  title: string
  url: string
  summary: string
  highlights: string[]
}

export async function fetchSearch(params: { q: string; type?: string; page?: number; limit?: number }) {
  const response = await http.get<ApiResponse<PagedResult<SearchItem>>>('/api/search', { params })
  return response.data
}
