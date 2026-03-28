<script setup lang="ts">
/** 公开项目详情：校验路由 id；失败时保留路由中的 id 展示降级卡片，便于联调期辨认。 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchProjectDetail, type Project } from '../api/projects'
import { toAbsoluteHttpUrl } from '../utils/url'
import BackendOfflineBanner from '../components/common/BackendOfflineBanner.vue'
import EmptyState from '../components/common/EmptyState.vue'
import ErrorMessage from '../components/common/ErrorMessage.vue'
import Loading from '../components/common/Loading.vue'
import BlogCommentsPanel from './blog/BlogCommentsPanel.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)
const offline = ref(false)
const traceId = ref('')
const project = ref<Project | null>(null)

const fallbackProject: Project = {
  id: 0,
  title: '示例项目（离线）',
  summary: '后端不可用时的默认项目数据。',
  description: '当前为 M2 阶段降级视图。待后端恢复后可重试加载真实项目详情。',
  category: 'web',
  tags: ['Fallback'],
  visible: true,
  sortOrder: 0,
  createdAt: new Date(0).toISOString(),
  updatedAt: new Date(0).toISOString(),
}

const id = computed(() => Number(route.params.id))

async function loadDetail() {
  if (!Number.isFinite(id.value) || id.value <= 0) {
    error.value = '无效的项目 ID'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  offline.value = false

  try {
    const response = await fetchProjectDetail(id.value)
    traceId.value = response.traceId
    if (!response.success || !response.data) {
      throw new Error(response.error ?? 'Project load failed')
    }
    project.value = response.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
    offline.value = true
    project.value = { ...fallbackProject, id: id.value }
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)

const coverHref = computed(() => toAbsoluteHttpUrl(project.value?.coverUrl))
const projectHref = computed(() => toAbsoluteHttpUrl(project.value?.projectUrl))
const sourceHref = computed(() => toAbsoluteHttpUrl(project.value?.sourceUrl))
</script>

<template>
  <main class="page">
    <section class="card">
      <h1>项目详情</h1>
      <template v-if="loading">
        <Loading title="正在加载项目详情…" hint="从 /api/projects/:id 获取数据" />
      </template>

      <template v-else-if="project">
        <BackendOfflineBanner v-if="offline" :details="error ?? undefined" @retry="loadDetail" />
        <img
          v-if="coverHref"
          class="cover"
          :src="coverHref"
          :alt="`「${project.title}」封面`"
          loading="lazy"
        />

        <h2 class="title">{{ project.title }}</h2>
        <p class="desc">{{ project.summary }}</p>

        <div class="meta">
          <span v-if="project.category" class="chip">{{ project.category }}</span>
          <span v-for="t in project.tags ?? []" :key="t" class="chip muted">{{ t }}</span>
        </div>

        <div v-if="project.description" class="section">
          <h3 class="h3">说明</h3>
          <p class="desc">{{ project.description }}</p>
        </div>

        <div class="links" v-if="projectHref || sourceHref">
          <a
            v-if="projectHref"
            class="link"
            :href="projectHref"
            target="_blank"
            rel="noopener noreferrer"
          >
            访问项目
          </a>
          <a
            v-if="sourceHref"
            class="link"
            :href="sourceHref"
            target="_blank"
            rel="noopener noreferrer"
          >
            源码
          </a>
        </div>

        <BlogCommentsPanel target-type="project" :target-id="project.id" :offline="offline" />

        <p v-if="traceId" class="trace">traceId: {{ traceId }}</p>

        <button class="back" type="button" @click="router.push('/projects')">返回列表</button>
      </template>

      <template v-else-if="error">
        <ErrorMessage :message="error" @retry="loadDetail" />
        <button class="back" type="button" @click="router.push('/projects')">返回列表</button>
      </template>

      <template v-else>
        <EmptyState title="项目不存在" hint="请返回列表重新选择。" />
        <button class="back" type="button" @click="router.push('/projects')">返回列表</button>
      </template>
    </section>
  </main>
</template>

<style scoped>
.cover {
  display: block;
  width: 100%;
  max-height: 280px;
  margin: 0 0 12px;
  object-fit: cover;
  border-radius: 14px;
  border: 1px solid rgba(203, 216, 231, 0.85);
  background: rgba(79, 116, 163, 0.06);
}

.title {
  margin: 12px 0 6px;
}

.meta {
  margin: 10px 0 0;
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

.section {
  margin-top: 14px;
}

.h3 {
  margin: 0 0 8px;
  font-size: 14px;
}

.links {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid rgba(79, 116, 163, 0.35);
  background: rgba(79, 116, 163, 0.08);
  text-decoration: none;
  color: var(--text-main);
  font-weight: 900;
}

.back {
  margin-top: 16px;
  height: 38px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid rgba(203, 216, 231, 0.9);
  background: rgba(255, 255, 255, 0.9);
  font-weight: 900;
  color: var(--text-main);
  cursor: pointer;
}
</style>

