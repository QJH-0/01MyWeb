import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import HomeView from './HomeView.vue'

vi.mock('../api/health', () => ({
  fetchHealth: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

import { fetchHealth } from '../api/health'

describe('HomeView', () => {
  it('shows health values when API succeeds', async () => {
    setActivePinia(createPinia())
    vi.mocked(fetchHealth).mockResolvedValueOnce({
      success: true,
      data: {
        mysql: true,
        redis: true,
        elasticsearch: false,
        minio: true,
        elasticsearchStatus: 'UNAVAILABLE',
      },
      error: null,
      timestamp: new Date().toISOString(),
      traceId: 'trace-001',
    })

    const wrapper = mount(HomeView)
    await Promise.resolve()
    await nextTick()

    expect(wrapper.text()).toContain('MySQL: UP')
    expect(wrapper.text()).toContain('traceId: trace-001')
  })
})
