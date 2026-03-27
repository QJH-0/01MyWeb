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
