<template>
  <div class="card">
    <slot />
  </div>
</template>

<style scoped lang="scss">
.card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow, rgba(0, 0, 0, 0.05) 0 10px 15px -3px);
  padding: 18px;
}
</style>

