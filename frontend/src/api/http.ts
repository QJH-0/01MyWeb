/**
 * Axios 实例与统一响应信封：请求自动带 Bearer，401 时串行刷新 access token 后重试一次，避免并发风暴。
 * 刷新失败会清本地 token，交由路由/页面引导重新登录。
 */
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
  /** 与后端默认超时大致对齐；过长会拖慢错误反馈，过短易误杀慢查询。 */
  timeout: 8000,
})

/** 同一时刻只进行一次 refresh，其余 401 重试共用这个 Promise。 */
let refreshPromise: Promise<string | null> | null = null

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 请求刷新 access token，使用单例模式避免并发风暴。 */
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

    // refresh 自身 401 再重试会形成递归，直接失败并走 clearTokens（在 refreshAccessToken 内）。
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
