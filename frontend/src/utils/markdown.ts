/**
 * 前台博客正文渲染：Markdown -> HTML 后经 DOMPurify 过滤；```mermaid 围栏转为占位 div，由 {@link renderMermaidInElement} 再绘制成 SVG。
 */
import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({ gfm: true })

/** UTF-8 安全 Base64，供放入 HTML data 属性（避免 `btoa` 对非 Latin1 抛错）。 */
function utf8ToBase64(str: string): string {
  const bytes = new TextEncoder().encode(str)
  let binary = ''
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]!)
  }
  return btoa(binary)
}

const MERMAID_FENCE = /^```mermaid\s*\r?\n([\s\S]*?)\r?\n```/gm

function extractMermaidBlocks(markdown: string): string {
  return markdown.replace(MERMAID_FENCE, (_full, code: string) => {
    const payload = utf8ToBase64(code)
    return `\n\n<div class="myweb-mermaid" data-mermaid="${payload}"></div>\n\n`
  })
}

const PURIFY_MERMAID = {
  ADD_TAGS: ['div'],
  ADD_ATTR: ['data-mermaid'],
}

export function renderSafeMarkdown(source: string): string {
  const md = extractMermaidBlocks(source ?? '')
  const rawHtml = marked(md, { async: false }) as string
  return DOMPurify.sanitize(rawHtml, PURIFY_MERMAID)
}
