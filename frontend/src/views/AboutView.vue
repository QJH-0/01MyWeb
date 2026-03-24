<script setup lang="ts">
import Card from '../components/common/Card.vue'
import Section from '../components/common/Section.vue'

const skills = ['Java', 'Spring Boot', 'Elasticsearch', 'Vue 3', 'Redis', 'MinIO']
const stats = [
  { k: '3+', v: '代表项目' },
  { k: '20+', v: '技术积累' }
]
</script>

<template>
  <Section>
    <div class="layout">
      <Card class="photo-card">
        <div class="avatar" aria-label="个人照片占位" />
        <div class="bio">
          <div class="name">你的名字</div>
          <div class="role">全栈工程师（占位）</div>
          <p class="summary">
            我喜欢把复杂问题拆成可验证的步骤，并用工程化手段让结果可持续迭代。
          </p>
        </div>
      </Card>

      <Card class="skills-card">
        <div class="section-title">技能标签</div>
        <div class="tags">
          <span v-for="s in skills" :key="s" class="tag">{{ s }}</span>
        </div>

        <div class="section-title" style="margin-top: 18px">数据统计（占位）</div>
        <div class="stats">
          <div v-for="item in stats" :key="item.v" class="stat">
            <div class="stat-k">{{ item.k }}</div>
            <div class="stat-v">{{ item.v }}</div>
          </div>
        </div>
      </Card>
    </div>
  </Section>
</template>

<style scoped lang="scss">
.layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
}

.photo-card,
.skills-card {
  background: var(--bg-card);
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 24px;
  background: rgba(125, 157, 156, 0.15);
  border: 1px solid var(--border);
  margin-bottom: 14px;
}

.bio {
  display: flex;
  flex-direction: column;
}

.name {
  font-weight: 700;
  font-size: 26px;
  color: var(--text-primary);
}

.role {
  margin-top: 6px;
  color: var(--text-secondary);
}

.summary {
  margin-top: 12px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag {
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: rgba(125, 157, 156, 0.08);
  color: var(--text-primary);
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-k {
  font-size: 20px;
  font-weight: 700;
  color: var(--accent);
}

.stat-v {
  margin-top: 6px;
  color: var(--text-secondary);
}

@media (min-width: 900px) {
  .layout {
    grid-template-columns: 1fr 1fr;
  }
}
</style>

