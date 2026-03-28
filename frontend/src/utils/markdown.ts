/**
 * 前台博客正文渲染：Markdown -> HTML 后经 DOMPurify 白名单过滤，降低反射型 XSS 面。
 * 不替代后端校验与入库策略；仅作为浏览器侧最后一道展示层收敛。
 */
import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({ gfm: true })

export function renderSafeMarkdown(source: string): string {
  const md = source ?? ''
  const rawHtml = marked(md, { async: false }) as string
  return DOMPurify.sanitize(rawHtml)
}
