<script setup lang="ts">
import { ElMessage } from 'element-plus'

import Card from '../components/common/Card.vue'
import Section from '../components/common/Section.vue'

const featuredProjects = [
  { id: 'p1', title: '项目一', desc: '一个代表性项目（占位）' },
  { id: 'p2', title: '项目二', desc: '一个代表性项目（占位）' },
  { id: 'p3', title: '项目三', desc: '一个代表性项目（占位）' }
]

const latestBlogs = [
  { id: 'b1', title: '博客一', desc: '最新文章摘要（占位）' },
  { id: 'b2', title: '博客二', desc: '最新文章摘要（占位）' }
]

function openAi() {
  ElMessage.info('AI 智能助手将在 M6 实现（当前占位）')
}
</script>

<template>
  <Section>
    <section class="hero">
      <div class="intro">
        <h1 class="name">你的名字</h1>
        <div class="title">技术专注 · 全栈工程（占位）</div>
        <p class="summary">用最简单的方式展示你的价值：项目、博客与可追溯的 AI 助手。</p>
        <div class="cta">
          <router-link class="link" to="/projects">查看项目</router-link>
          <router-link class="link" to="/blog">阅读博客</router-link>
        </div>
      </div>
    </section>

    <section class="grid">
      <div class="block">
        <div class="block-title">精选项目</div>
        <div class="cards">
          <router-link
            v-for="p in featuredProjects"
            :key="p.id"
            class="card-link"
            to="/projects"
          >
            <Card>
              <div class="card-title">{{ p.title }}</div>
              <div class="card-desc">{{ p.desc }}</div>
            </Card>
          </router-link>
        </div>
      </div>

      <div class="block">
        <div class="block-title">最新博客</div>
        <div class="cards">
          <router-link
            v-for="b in latestBlogs"
            :key="b.id"
            class="card-link"
            :to="`/blog/${b.id}`"
          >
            <Card>
              <div class="card-title">{{ b.title }}</div>
              <div class="card-desc">{{ b.desc }}</div>
            </Card>
          </router-link>
        </div>
      </div>
    </section>
  </Section>

  <button class="ai-fab" @click="openAi" aria-label="AI 智能助手入口">
    AI
  </button>
</template>

<style scoped lang="scss">
.hero {
  padding: 28px 0 18px;
}

.intro {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 22px;
  background: var(--bg-card);
  box-shadow: var(--shadow, rgba(0, 0, 0, 0.05) 0 10px 15px -3px);
}

.name {
  margin: 0;
  font-size: 40px;
  letter-spacing: -0.8px;
  color: var(--text-primary);
}

.title {
  margin-top: 10px;
  font-size: 16px;
  color: var(--text-secondary);
}

.summary {
  margin: 12px 0 0;
  line-height: 1.7;
  color: var(--text-secondary);
}

.cta {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.link {
  text-decoration: none;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(125, 157, 156, 0.08);
  color: var(--text-primary);
}

.grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
}

.block {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 18px;
  background: var(--bg-card);
}

.block-title {
  font-size: 18px;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.cards {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 12px;
}

.card-link {
  text-decoration: none;
}

.card-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.card-desc {
  color: var(--text-secondary);
  line-height: 1.6;
}

.ai-fab {
  position: fixed;
  right: 22px;
  bottom: 22px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background: rgba(230, 184, 162, 0.18);
  color: var(--text-primary);
  font-weight: 700;
  cursor: pointer;
}

@media (min-width: 900px) {
  .grid {
    grid-template-columns: 1fr 1fr;
  }

  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

