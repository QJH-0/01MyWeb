/**
 * 认证状态：编排 login/register/me/logout，与 token 存取及业务错误码映射联动。
 * `isAuthenticated` 要求既有 token 又成功拉取过 profile，避免“有 token 无用户上下文”的中间态。
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login, logout, me, register, type MeResponse } from '../api/auth'
import { mapApiErrorCodeToMessage, toUserFriendlyAuthError } from '../auth/error-code'
import { clearTokens, getRefreshToken, hasAccessToken, setTokens } from '../auth/token'

/**
 * 认证状态管理 Store。
 * 使用 Pinia Composition API 风格，管理用户登录状态、个人信息和操作。
 */
export const useAuthStore = defineStore('auth', () => {
  /** 当前用户个人信息 */
  const profile = ref<MeResponse | null>(null)
  /** 登录操作加载状态 */
  const loading = ref(false)
  /** 认证初始化完成标记 */
  const initialized = ref(false)

  /**
   * 计算属性：判断是否已认证。
   * 要求既有 access token 又成功获取过用户信息。
   */
  const isAuthenticated = computed(() => hasAccessToken() && profile.value !== null)

  async function loginWithPassword(username: string, password: string) {
    loading.value = true
    try {
      const response = await login({ username, password })
      if (!response.success || !response.data) {
        throw new Error(mapApiErrorCodeToMessage(response.error))
      }
      setTokens(response.data)
      try {
        await fetchMe()
      } catch {
        // token 可能无效或与用户状态不一致：清 token，避免后续接口连环 401。
        clearTokens()
        profile.value = null
        throw new Error('登录状态初始化失败，请重新登录。')
      }
    } catch (error: unknown) {
      throw new Error(toUserFriendlyAuthError(error))
    } finally {
      loading.value = false
    }
  }

  async function registerAndLogin(username: string, password: string, captchaToken: string) {
    loading.value = true
    try {
      const registerResult = await register({ username, password, captchaToken })
      if (!registerResult.success) {
        throw new Error(mapApiErrorCodeToMessage(registerResult.error))
      }
      await loginWithPassword(username, password)
    } catch (error: unknown) {
      throw new Error(toUserFriendlyAuthError(error))
    } finally {
      loading.value = false
    }
  }

  async function fetchMe() {
    const response = await me()
    if (!response.success || !response.data) {
      throw new Error(mapApiErrorCodeToMessage(response.error))
    }
    profile.value = response.data
    initialized.value = true
  }

  async function initAuth() {
    if (!hasAccessToken()) {
      initialized.value = true
      profile.value = null
      return
    }
    try {
      await fetchMe()
    } catch {
      clearTokens()
      profile.value = null
      initialized.value = true
    }
  }

  async function logoutCurrentUser() {
    const refreshToken = getRefreshToken()
    try {
      if (refreshToken) {
        await logout(refreshToken)
      }
    } finally {
      clearTokens()
      profile.value = null
      initialized.value = true
    }
  }

  return {
    profile,
    loading,
    initialized,
    isAuthenticated,
    initAuth,
    fetchMe,
    loginWithPassword,
    registerAndLogin,
    logoutCurrentUser,
  }
})
