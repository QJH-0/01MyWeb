/** 聚合健康探针：用于前端展示依赖就绪情况或 DevOps 面板（字段与后端探针实现一致）。 */
import http, { type ApiResponse } from './http'

export interface HealthStatus {
  mysql: boolean
  redis: boolean
  elasticsearch: boolean
  minio: boolean
  elasticsearchStatus: string
}

export async function fetchHealth() {
  const response = await http.get<ApiResponse<HealthStatus>>('/api/health')
  return response.data
}
