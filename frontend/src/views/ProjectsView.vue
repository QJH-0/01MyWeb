<script setup lang="ts">
/**
 * 公开项目列表：支持分类筛选与分页状态；接口失败时用内置示例数据降级（仅前台展示）。
 */
import { computed, onMounted, ref } from 'vue'
import { fetchProjects, type Project } from '../api/projects'
import BackendOfflineBanner from '../components/common/BackendOfflineBanner.vue'
import EmptyState from '../components/common/EmptyState.vue'
import ErrorMessage from '../components/common/ErrorMessage.vue'
import Loading from '../components/common/Loading.vue'

const loading = ref(true)
const error = ref<string | null>(null)
const offline = ref(false)
const traceId = ref('')
const page = ref(0)
const limit = ref(10)
const category = ref<string>('')

type PageData = { list: Project[]; total: number; page: number; limit: number }
const data = ref<PageData | null>(null)

const fallbackProjects: Project[] = [
  {
    id: 1,
    title: 'MyWeb',
    summary: '一个从 M0→M8 按阶段闭环推进的个人站点工程。',
    category: 'web',
    tags: ['Vue 3', 'Spring Boot', 'MySQL'],
    visible: true,
    sortOrder: 0,
    createdAt: new Date(0).toISOString(),
    updatedAt: new Date(0).toISOString(),
  },
]

async function loadProjects() {
  loading.value = true
  error.value = null
  offline.value = false

  try {
    const response = await fetchProjects({
      category: category.value ? category.value : undefined,
      page: page.value,
      limit: limit.value,
    })
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Projects load failed')
    }
    data.value = response.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
    offline.value = true
    data.value = { list: fallbackProjects, total: fallbackProjects.length, page: 0, limit: fallbackProjects.length }
  } finally {
    loading.value = false
  }
}

const projects = computed(() => data.value?.list ?? [])
const isEmpty = computed(() => !projects.value.length)

onMounted(loadProjects)
</script>

<template>
  <main class="page">
    <section class="card">
      <h1>项目列表</h1>
      <p class="desc">展示项目列表与分类筛选（M2）。后端不可用时将使用默认数据降级渲染。</p>

      <template v-if="loading">
        <Loading title="正在加载项目列表…" hint="从 /api/projects 获取数据" />
      </template>

      <template v-else>
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="loadProjects" />

        <div class="toolbar">
          <label class="field">
            <span class="label">分类</span>
            <select v-model="category" class="select" @change="loadProjects">
              <option value="">全部</option>
              <option value="web">web</option>
              <option value="backend">backend</option>
              <option value="frontend">frontend</option>
            </select>
          </label>
        </div>

        <template v-if="error && !offline">
          <ErrorMessage :message="error" @retry="loadProjects" />
        </template>

        <template v-else-if="isEmpty">
          <EmptyState title="暂无项目" hint="后续可在后台管理端创建项目。" />
        </template>

        <div v-else class="grid" aria-label="项目列表">
          <router-link v-for="p in projects" :key="p.id" class="item" :to="`/projects/${p.id}`">
            <p class="title">{{ p.title }}</p>
            <p class="summary">{{ p.summary }}</p>
            <div class="meta">
              <span v-if="p.category" class="chip">{{ p.category }}</span>
              <span v-for="t in p.tags.slice(0, 3)" :key="t" class="chip muted">{{ t }}</span>
            </div>
          </router-link>
        </div>

        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>
      </template>
    </section>
  </main>
</template>

<style scoped>
.toolbar {
  margin: 14px 0 10px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.field {
  display: grid;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 800;
}

.select {
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(203, 216, 231, 0.85);
  padding: 0 10px;
  background: rgba(255, 255, 255, 0.88);
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 10px;
}

.item {
  display: block;
  padding: 14px;
  border: 1px solid rgba(203, 216, 231, 0.85);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  text-decoration: none;
  color: inherit;
}

.item:hover {
  border-color: rgba(79, 116, 163, 0.4);
}

.title {
  margin: 0 0 6px;
  font-weight: 900;
}

.summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

.meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(79, 116, 163, 0.06);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 900;
  color: var(--text-main);
}

.chip.muted {
  color: var(--text-secondary);
}

@media (max-width: 760px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

