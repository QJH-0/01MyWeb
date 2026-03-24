<script setup lang="ts">
import Card from '../components/common/Card.vue'
import Section from '../components/common/Section.vue'
import ContactForm from '../components/contact/ContactForm.vue'

const contacts = [
  { k: '邮箱', v: 'your-email@example.com' },
  { k: '社交', v: 'GitHub / LinkedIn（占位）' }
]
</script>

<template>
  <Section>
    <div class="layout">
      <Card class="info">
        <div class="title">联系方式</div>
        <div class="rows">
          <div v-for="c in contacts" :key="c.k" class="row">
            <div class="k">{{ c.k }}</div>
            <div class="v">{{ c.v }}</div>
          </div>
        </div>
        <div class="hint">
          M1 阶段仅完成基础校验与前端体验；后续将接入防刷策略与发送链路。
        </div>
      </Card>

      <Card class="form-card">
        <div class="title">给我留言</div>
        <ContactForm />
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

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 12px;
}

.k {
  color: var(--text-secondary);
}

.v {
  color: var(--text-primary);
}

.hint {
  margin-top: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

@media (min-width: 900px) {
  .layout {
    grid-template-columns: 0.8fr 1.2fr;
  }
}
</style>

