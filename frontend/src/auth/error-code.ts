/**
 * 将后端 `error` 业务码映射为面向用户的文案；未知码统一降级为通用提示，避免暴露内部细节。
 */
import axios from 'axios'

const AUTH_ERROR_MESSAGE_MAP: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: '用户名或密码错误。',
  AUTH_INVALID_REFRESH_TOKEN: '登录状态已过期，请重新登录。',
  AUTH_CAPTCHA_INVALID: '验证码无效，请重试。',
  AUTH_WEAK_PASSWORD: '密码强度不足，需至少 8 位且包含字母和数字。',
  AUTH_USERNAME_EXISTS: '用户名已存在，请更换后重试。',
  AUTH_REGISTER_RATE_LIMITED: '注册请求过于频繁，请稍后再试。',
  AUTH_LOGIN_RATE_LIMITED: '登录请求过于频繁，请稍后再试。',
  UNAUTHORIZED: '未登录或登录已失效，请重新登录。',
  VALIDATION_ERROR: '输入格式不正确：用户名 3-50 位；密码 8-128 位，且需包含字母和数字。',
  FORBIDDEN: '权限不足，无法执行当前操作。',
  NOT_FOUND: '请求的资源不存在。',
  RATE_LIMITED: '请求过于频繁，请稍后重试。',
  INTERNAL_ERROR: '服务繁忙，请稍后再试。',
}

export function mapApiErrorCodeToMessage(codeOrMessage: string | null | undefined): string {
  if (!codeOrMessage) {
    return '请求失败，请稍后再试。'
  }
  return AUTH_ERROR_MESSAGE_MAP[codeOrMessage] ?? '请求失败，请稍后再试。'
}

/** 解析 Axios 或非 Error 异常为可读文案；认证与业务接口共用同一套 error 码映射。 */
export function toUserFriendlyApiError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const code = (error.response?.data as { error?: string } | undefined)?.error
    return mapApiErrorCodeToMessage(code)
  }
  if (error instanceof Error) {
    return error.message
  }
  return '请求失败，请稍后再试。'
}

export function toUserFriendlyAuthError(error: unknown): string {
  return toUserFriendlyApiError(error)
}
