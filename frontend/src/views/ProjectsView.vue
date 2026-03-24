<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import Card from '../components/common/Card.vue'
import Section from '../components/common/Section.vue'
import { getProjectById, getProjects, type Project } from '../api/projects'

const loading = ref(false)
const projects = ref<Project[]>([])
const total = ref(0)
const activeCategory = ref('全部')
const errorMessage = ref<string | null>(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailProject = ref<Project | null>(null)

const categoryOptions = computed(() => {
  const values = new Set<string>()
  for (const item of projects.value) {
    if (item.category && item.category.trim()) {
      values.add(item.category.trim())
    }
  }
  return ['全部', ...Array.from(values)]
})

const filteredProjects = computed(() => {
  if (activeCategory.value === '全部') {
    return projects.value
  }
  return projects.value.filter((item) => item.category === activeCategory.value)
})

async function loadProjects() {
  loading.value = true
  errorMessage.value = null
  try {
    const result = await getProjects({ page: 0, limit: 50 })
    projects.value = result.items
    total.value = result.total
  } catch (error) {
    errorMessage.value = '项目列表加载失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

function selectCategory(category: string) {
  activeCategory.value = category
}

async function openDetail(id: number) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    detailProject.value = await getProjectById(id)
  } catch (error) {
    detailProject.value = null
    ElMessage.error('项目详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function openExternal(url: string | null, label: string) {
  if (!url) {
    ElMessage.warning(`${label} 链接暂未配置`)
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  void loadProjects()
})
</script>

<template>
  <Section>
    <div class="page-header">
      <h1 class="title">项目作品</h1>
      <p class="subtitle">按分类浏览项目，并查看详情与外部链接。</p>
    </div>

    <div class="toolbar">
      <button
        v-for="category in categoryOptions"
        :key="category"
        class="chip"
        :class="{ active: category === activeCategory }"
        @click="selectCategory(category)"
      >
        {{ category }}
      </button>
      <span class="meta">共 {{ total }} 个项目</span>
    </div>

    <div v-if="loading" class="state">项目加载中...</div>
    <div v-else-if="errorMessage" class="state error">{{ errorMessage }}</div>
    <div v-else-if="filteredProjects.length === 0" class="state">暂无可展示项目</div>

    <div v-else class="grid">
      <Card v-for="item in filteredProjects" :key="item.id" class="project-card">
        <div class="cover" :style="{ backgroundImage: item.coverUrl ? `url(${item.coverUrl})` : 'none' }">
          <div v-if="!item.coverUrl" class="cover-placeholder">无封面</div>
        </div>
        <div class="content">
          <div class="project-title">{{ item.title }}</div>
          <div class="project-summary">{{ item.summary }}</div>
          <div class="tags">
            <span v-for="tag in item.tags" :key="`${item.id}-${tag}`" class="tag">{{ tag }}</span>
          </div>
          <div class="actions">
            <button class="btn" @click="openDetail(item.id)">查看详情</button>
            <button class="btn ghost" @click="openExternal(item.demoUrl, 'Demo')">Demo</button>
            <button class="btn ghost" @click="openExternal(item.githubUrl, 'GitHub')">GitHub</button>
          </div>
        </div>
      </Card>
    </div>

    <el-dialog v-model="detailVisible" title="项目详情" width="680px">
      <div v-if="detailLoading" class="state">详情加载中...</div>
      <div v-else-if="detailProject" class="detail">
        <h3>{{ detailProject.title }}</h3>
        <p>{{ detailProject.summary }}</p>
        <p><strong>分类：</strong>{{ detailProject.category || '未分类' }}</p>
        <p><strong>标签：</strong>{{ detailProject.tags.join(' / ') || '无' }}</p>
        <div class="actions">
          <button class="btn ghost" @click="openExternal(detailProject.demoUrl, 'Demo')">打开 Demo</button>
          <button class="btn ghost" @click="openExternal(detailProject.githubUrl, 'GitHub')">
            打开 GitHub
          </button>
        </div>
      </div>
      <div v-else class="state error">详情不可用</div>
    </el-dialog>
  </Section>
</template>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
}

.title {
  margin: 0;
  color: var(--text-primary);
}

.subtitle {
  margin: 8px 0 0;
  color: var(--text-secondary);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}

.chip {
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 6px 12px;
  background: var(--bg-card);
  color: var(--text-primary);
  cursor: pointer;
}

.chip.active {
  background: rgba(125, 157, 156, 0.15);
}

.meta {
  margin-left: auto;
  color: var(--text-secondary);
  font-size: 14px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.project-card {
  padding: 0;
  overflow: hidden;
}

.cover {
  height: 180px;
  background-color: var(--bg-secondary);
  background-size: cover;
  background-position: center;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-placeholder {
  color: var(--text-muted);
}

.content {
  padding: 14px;
}

.project-title {
  font-weight: 700;
  margin-bottom: 6px;
}

.project-summary {
  color: var(--text-secondary);
  line-height: 1.6;
}

.tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  color: var(--text-secondary);
}

.actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  border: 1px solid var(--border);
  background: rgba(125, 157, 156, 0.12);
  color: var(--text-primary);
  border-radius: 10px;
  padding: 7px 10px;
  cursor: pointer;
}

.btn.ghost {
  background: transparent;
}

.state {
  padding: 16px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  color: var(--text-secondary);
}

.state.error {
  color: var(--error);
}

.detail p {
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (min-width: 860px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

