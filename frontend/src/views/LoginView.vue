<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const captchaToken = ref('')
const feedback = ref<string | null>(null)
const error = ref<string | null>(null)

const submitLabel = computed(() => (mode.value === 'login' ? '登录' : '注册并登录'))
const title = computed(() => (mode.value === 'login' ? '账号登录' : '新用户注册'))

async function onSubmit() {
  error.value = null
  feedback.value = null

  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码。'
    return
  }

  if (mode.value === 'register') {
    if (password.value !== confirmPassword.value) {
      error.value = '两次输入的密码不一致。'
      return
    }
    if (password.value.length < 8) {
      error.value = '密码至少 8 位。'
      return
    }
    if (!captchaToken.value.trim() || captchaToken.value.trim().length < 6) {
      error.value = '请输入至少 6 位验证码。'
      return
    }
  }

  try {
    if (mode.value === 'login') {
      await authStore.loginWithPassword(username.value.trim(), password.value)
      feedback.value = '登录成功，正在跳转...'
    } else {
      await authStore.registerAndLogin(
        username.value.trim(),
        password.value,
        captchaToken.value.trim(),
      )
      feedback.value = '注册成功，已自动登录，正在跳转...'
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/profile'
    await router.replace(redirect)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '请求失败，请稍后再试。'
  }
}

function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode
  error.value = null
  feedback.value = null
  captchaToken.value = ''
}
</script>

<template>
  <main class="page auth-page">
    <section class="card auth-card">
      <div class="auth-head">
        <h1>{{ title }}</h1>
        <p class="desc">M0 登录联调页：已接入 register/login/me/refresh/logout。</p>
      </div>

      <div class="auth-switch">
        <button
          type="button"
          class="switch-btn"
          :class="{ active: mode === 'login' }"
          @click="switchMode('login')"
        >
          登录
        </button>
        <button
          type="button"
          class="switch-btn"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>

      <form class="auth-form" @submit.prevent="onSubmit">
        <label class="field">
          <span>用户名</span>
          <input v-model="username" type="text" maxlength="50" autocomplete="username" />
        </label>

        <label class="field">
          <span>密码</span>
          <input v-model="password" type="password" maxlength="128" autocomplete="current-password" />
        </label>

        <label v-if="mode === 'register'" class="field">
          <span>确认密码</span>
          <input
            v-model="confirmPassword"
            type="password"
            maxlength="128"
            autocomplete="new-password"
          />
        </label>

        <label v-if="mode === 'register'" class="field">
          <span>验证码</span>
          <input
            v-model="captchaToken"
            type="text"
            maxlength="64"
            autocomplete="one-time-code"
            placeholder="联调阶段：任意输入 6 位以上字符"
          />
        </label>
        <p v-if="mode === 'register'" class="captcha-tip">
          当前为联调模式，后端仅校验长度；请输入任意 6 位以上字符即可。
        </p>

        <p v-if="error" class="state error">{{ error }}</p>
        <p v-if="feedback" class="state success">{{ feedback }}</p>

        <button class="submit-btn" type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? '处理中...' : submitLabel }}
        </button>
      </form>
    </section>
  </main>
</template>
