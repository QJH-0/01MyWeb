<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import Section from '../components/common/Section.vue'
import type { Project } from '../api/projects'
import {
  createAdminProject,
  deleteAdminProject,
  getAdminProjects,
  updateAdminProject,
  type ProjectFormPayload
} from '../api/adminProjects'

const loading = ref(false)
const projects = ref<Project[]>([])
const total = ref(0)
const categoryFilter = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const form = reactive<ProjectFormPayload>({
  title: '',
  summary: '',
  category: '',
  tags: [],
  coverUrl: '',
  githubUrl: '',
  demoUrl: '',
  visible: true
})
const tagsInput = ref('')

const isEditMode = computed(() => editingId.value !== null)

function resetForm() {
  form.title = ''
  form.summary = ''
  form.category = ''
  form.tags = []
  form.coverUrl = ''
  form.githubUrl = ''
  form.demoUrl = ''
  form.visible = true
  tagsInput.value = ''
  editingId.value = null
}

function fillForm(item: Project) {
  form.title = item.title
  form.summary = item.summary
  form.category = item.category || ''
  form.tags = [...item.tags]
  form.coverUrl = item.coverUrl || ''
  form.githubUrl = item.githubUrl || ''
  form.demoUrl = item.demoUrl || ''
  form.visible = item.visible
  tagsInput.value = item.tags.join(', ')
  editingId.value = item.id
}

function parseTags() {
  form.tags = tagsInput.value
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0)
}

async function loadProjects() {
  loading.value = true
  try {
    const data = await getAdminProjects({
      page: 0,
      limit: 50,
      category: categoryFilter.value.trim() || undefined
    })
    projects.value = data.items
    total.value = data.total
  } catch (error) {
    ElMessage.error('后台项目列表加载失败')
  } finally {
    loading.value = false
  }
}

function onCreate() {
  resetForm()
  dialogVisible.value = true
}

function onEdit(item: Project) {
  fillForm(item)
  dialogVisible.value = true
}

async function onDelete(item: Project) {
  await ElMessageBox.confirm(`确认下线项目「${item.title}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  })
  await deleteAdminProject(item.id)
  ElMessage.success('项目已下线')
  await loadProjects()
}

async function onSubmit() {
  if (!form.title.trim() || !form.summary.trim()) {
    ElMessage.warning('标题和简介为必填项')
    return
  }
  parseTags()
  saving.value = true
  try {
    if (editingId.value === null) {
      await createAdminProject({ ...form })
      ElMessage.success('项目创建成功')
    } else {
      await updateAdminProject(editingId.value, { ...form })
      ElMessage.success('项目更新成功')
    }
    dialogVisible.value = false
    await loadProjects()
  } catch (error) {
    ElMessage.error('保存失败，请检查输入')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadProjects()
})
</script>

<template>
  <Section>
    <div class="header">
      <h1>项目管理</h1>
      <div class="actions">
        <el-input v-model="categoryFilter" placeholder="按分类筛选" clearable class="filter-input" />
        <el-button @click="loadProjects">查询</el-button>
        <el-button type="primary" @click="onCreate">新建项目</el-button>
      </div>
    </div>

    <el-table :data="projects" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="可见" width="90">
        <template #default="{ row }">
          <el-tag :type="row.visible ? 'success' : 'info'">{{ row.visible ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="onDelete(row)">下线</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="meta">共 {{ total }} 条</div>

    <el-dialog v-model="dialogVisible" :title="isEditMode ? '编辑项目' : '新建项目'" width="760px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.summary" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <div class="row">
          <el-form-item label="分类" class="col">
            <el-input v-model="form.category" maxlength="80" />
          </el-form-item>
          <el-form-item label="标签（逗号分隔）" class="col">
            <el-input v-model="tagsInput" />
          </el-form-item>
        </div>
        <el-form-item label="封面 URL">
          <el-input v-model="form.coverUrl" maxlength="1000" />
        </el-form-item>
        <div class="row">
          <el-form-item label="GitHub URL" class="col">
            <el-input v-model="form.githubUrl" maxlength="1000" />
          </el-form-item>
          <el-form-item label="Demo URL" class="col">
            <el-input v-model="form.demoUrl" maxlength="1000" />
          </el-form-item>
        </div>
        <el-form-item>
          <el-switch v-model="form.visible" active-text="前台可见" inactive-text="前台隐藏" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </Section>
</template>

<style scoped lang="scss">
.header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-input {
  width: 180px;
}

.meta {
  margin-top: 12px;
  color: var(--text-secondary);
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.col {
  min-width: 0;
}

@media (max-width: 760px) {
  .row {
    grid-template-columns: 1fr;
  }
}
</style>

