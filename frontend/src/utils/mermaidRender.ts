/**
 * 博客正文内 Mermaid：在客户端将 `.myweb-mermaid` 解码并交给 mermaid 生成 SVG（startOnLoad: false）。
 * 图表定义经 Base64 放入 data 属性，避免在 HTML 中直接嵌入未转义的箭头/尖括号。
 */
let mermaidLibPromise: Promise<typeof import('mermaid').default> | null = null

function loadMermaidLib() {
  if (!mermaidLibPromise) {
    mermaidLibPromise = import('mermaid').then((mod) => {
      const mermaid = mod.default
      mermaid.initialize({
        startOnLoad: false,
        securityLevel: 'strict',
        theme: 'neutral',
        fontFamily: 'inherit',
      })
      return mermaid
    })
  }
  return mermaidLibPromise
}

function utf8FromBase64(b64: string): string {
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new TextDecoder('utf-8').decode(bytes)
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/** 在已插入 DOM 的 Markdown 容器内渲染全部 Mermaid 占位节点。 */
export async function renderMermaidInElement(root: HTMLElement | null): Promise<void> {
  if (!root) return
  const mermaid = await loadMermaidLib()
  const hosts = root.querySelectorAll<HTMLElement>('.myweb-mermaid')
  for (let i = 0; i < hosts.length; i++) {
    const el = hosts[i]
    const b64 = el.getAttribute('data-mermaid')
    if (!b64) continue
    let src: string
    try {
      src = utf8FromBase64(b64)
    } catch {
      el.replaceChildren()
      el.appendChild(document.createTextNode('（图表源码解码失败）'))
      continue
    }
    el.replaceChildren()
    const graphId = `myweb-mmd-${Date.now()}-${i}`
    try {
      const { svg } = await mermaid.render(graphId, src)
      el.innerHTML = svg
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e)
      el.innerHTML = `<p class="mermaid-error">${escapeHtml(msg)}</p>`
    }
  }
}
