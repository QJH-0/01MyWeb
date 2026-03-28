import { describe, expect, it } from 'vitest'
import { renderSafeMarkdown } from './markdown'

describe('renderSafeMarkdown', () => {
  it('renders headings from Markdown', () => {
    const html = renderSafeMarkdown('# Hello')
    expect(html).toContain('Hello')
    expect(html.toLowerCase()).toContain('h1')
  })

  it('does not retain script injection in raw HTML blocks', () => {
    const html = renderSafeMarkdown('<script>alert(1)</script>\n\nPlain.')
    expect(html.toLowerCase()).not.toContain('<script')
  })
})
