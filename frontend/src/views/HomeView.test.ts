import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import HomeView from './HomeView.vue'

vi.mock('../api/health', () => ({
  fetchHealth: vi.fn(),
}))

import { fetchHealth } from '../api/health'

describe('HomeView', () => {
  it('shows health values when API succeeds', async () => {
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
