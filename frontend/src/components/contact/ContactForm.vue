<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

type ContactFormModel = {
  name: string
  email: string
  message: string
  // 反爬占位：后端接入前，这里只做前端校验占位
  honeypot: string
}

const model = reactive<ContactFormModel>({
  name: '',
  email: '',
  message: '',
  honeypot: ''
})

const formRef = ref<FormInstance>()
const submitting = ref(false)

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const rules = reactive<FormRules<ContactFormModel>>({
  name: [{ required: true, message: '姓名为必填项', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱为必填项', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || emailRegex.test(value)) return callback()
        callback(new Error('邮箱格式不正确'))
      },
      trigger: 'blur'
    }
  ],
  message: [
    { required: true, message: '留言为必填项', trigger: 'blur' },
    { min: 5, message: '留言长度至少 5 个字符', trigger: 'blur' }
  ],
  honeypot: [
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback()
        callback(new Error('疑似机器人提交（honeypot 不应填写）'))
      },
      trigger: 'change'
    }
  ]
})

async function onSubmit() {
  submitting.value = true
  try {
    const form = formRef.value
    if (!form) return

    await form.validate()
    ElMessage.success('表单已校验通过（M1 阶段未接入后端发送链路）')

    model.name = ''
    model.email = ''
    model.message = ''
    model.honeypot = ''
  } catch (e) {
    // ElementPlus 会自动展示校验错误，这里仅兜底
    ElMessage.error('提交失败，请检查表单内容')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-form
    ref="formRef"
    :model="model"
    :rules="rules"
    label-position="top"
    class="form"
  >
    <div class="honeypot" aria-hidden="true">
      <!-- honeypot：机器人填写后会触发校验 -->
      <input v-model="model.honeypot" name="company" tabindex="-1" />
    </div>

    <el-form-item label="姓名" prop="name">
      <el-input v-model="model.name" placeholder="请输入你的姓名" />
    </el-form-item>

    <el-form-item label="邮箱" prop="email">
      <el-input v-model="model.email" placeholder="name@example.com" />
    </el-form-item>

    <el-form-item label="留言" prop="message">
      <el-input
        v-model="model.message"
        placeholder="想对我说点什么？"
        type="textarea"
        :rows="4"
      />
    </el-form-item>

    <el-button type="primary" :loading="submitting" @click="onSubmit">
      提交
    </el-button>
  </el-form>
</template>

<style scoped lang="scss">
.form {
  max-width: 720px;
}

.honeypot {
  position: absolute;
  left: -9999px;
  width: 1px;
  height: 1px;
  overflow: hidden;
}
</style>

