/**
 * 将用户输入的 URL 规范为可点击的绝对地址（外链用）。
 * 后端保存时要求 http(s)，历史数据或联调例外时在此兜底，避免 href 变成相对站内路径。
 */
export function toAbsoluteHttpUrl(url: string | null | undefined): string | null {
  if (url == null) return null
  const t = String(url).trim()
  if (!t) return null
  if (/^https?:\/\//i.test(t)) return t
  if (t.startsWith('//')) return `https:${t}`
  return `https://${t}`
}
