<script setup lang="ts">
/**
 * 联系页：当前仅前端校验（M1）；提交流程与防刷需后续对接后端与验证码策略。
 */
import { computed, ref } from 'vue'

const name = ref('')
const email = ref('')
const message = ref('')

const error = ref<string | null>(null)
const feedback = ref<string | null>(null)

const canSubmit = computed(() => !!name.value.trim() && !!email.value.trim() && !!message.value.trim())

/** 验证邮箱格式 */
function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

/** 提交联系表单 */
function onSubmit() {
  error.value = null
  feedback.value = null

  if (!name.value.trim() || !email.value.trim() || !message.value.trim()) {
    error.value = '请完整填写姓名、邮箱与内容。'
    return
  }

  if (!isValidEmail(email.value.trim())) {
    error.value = '邮箱格式不正确。'
    return
  }

  feedback.value = '已完成前端校验（M1）。后续将接入后端提交接口。'
  message.value = ''
}
</script>

<template>
  <main class="page">
    <section class="card">
      <h1>联系</h1>
      <p class="desc">展示邮箱、社交链接以及带基础校验的联系表单。</p>

      <div class="grid" style="margin-top: 14px">
        <p>
          邮箱：
          <strong><a href="mailto:hello@example.com">hello@example.com</a></strong>
        </p>
        <p>
          GitHub：
          <strong><a href="https://github.com/" target="_blank" rel="noreferrer">github.com</a></strong>
        </p>
      </div>

      <form class="auth-form" style="margin-top: 14px" @submit.prevent="onSubmit">
        <label class="field">
          <span>姓名</span>
          <input v-model="name" type="text" maxlength="50" autocomplete="name" />
        </label>
        <label class="field">
          <span>邮箱</span>
          <input v-model="email" type="email" maxlength="120" autocomplete="email" />
        </label>
        <label class="field">
          <span>内容</span>
          <input v-model="message" type="text" maxlength="500" autocomplete="off" />
        </label>

        <p v-if="error" class="state error">{{ error }}</p>
        <p v-if="feedback" class="state success">{{ feedback }}</p>

        <button class="submit-btn" type="submit" :disabled="!canSubmit">发送</button>
      </form>
    </section>
  </main>
</template>

