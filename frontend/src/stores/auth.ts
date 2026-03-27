import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login, logout, me, register, type MeResponse } from '../api/auth'
import { mapApiErrorCodeToMessage, toUserFriendlyAuthError } from '../auth/error-code'
import { clearTokens, getRefreshToken, hasAccessToken, setTokens } from '../auth/token'

export const useAuthStore = defineStore('auth', () => {
  const profile = ref<MeResponse | null>(null)
  const loading = ref(false)
  const initialized = ref(false)

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
