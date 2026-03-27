import axios from 'axios'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '../auth/token'

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: string | null
  timestamp: string
  traceId: string
}

interface RefreshPayload {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

const http = axios.create({
  timeout: 8000,
})

let refreshPromise: Promise<string | null> | null = null

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

async function refreshAccessToken(): Promise<string | null> {
  if (!getRefreshToken()) {
    return null
  }

  if (!refreshPromise) {
    refreshPromise = axios
      .post<ApiResponse<RefreshPayload>>('/api/auth/refresh', {
        refreshToken: getRefreshToken(),
      })
      .then((response) => {
        const payload = response.data.data
        if (!response.data.success || !payload) {
          clearTokens()
          return null
        }
        setTokens({
          accessToken: payload.accessToken,
          refreshToken: payload.refreshToken,
        })
        return payload.accessToken
      })
      .catch(() => {
        clearTokens()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status
    const originalRequest = error?.config as
      | (typeof error.config & { __retry?: boolean; headers?: Record<string, string> })
      | undefined
    if (!originalRequest || status !== 401 || originalRequest.__retry) {
      return Promise.reject(error)
    }

    if (String(originalRequest.url).includes('/api/auth/refresh')) {
      return Promise.reject(error)
    }

    originalRequest.__retry = true
    const nextToken = await refreshAccessToken()
    if (!nextToken) {
      return Promise.reject(error)
    }

    originalRequest.headers = originalRequest.headers ?? {}
    originalRequest.headers.Authorization = `Bearer ${nextToken}`
    return http.request(originalRequest)
  },
)

export default http
