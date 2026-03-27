import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: string | null
  timestamp: string
  traceId: string
}

export interface HealthStatus {
  mysql: boolean
  redis: boolean
  elasticsearch: boolean
  minio: boolean
  elasticsearchStatus: string
}

export async function fetchHealth() {
  const response = await axios.get<ApiResponse<HealthStatus>>('/api/health', {
    timeout: 5000,
  })
  return response.data
}
