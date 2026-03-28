import type { CommentItem } from '../api/comments'

export interface CommentTreeNode extends CommentItem {
  replies: CommentTreeNode[]
}

function compareByTime(a: CommentItem, b: CommentItem): number {
  const t = a.createdAt.localeCompare(b.createdAt)
  if (t !== 0) return t
  return a.id - b.id
}

/** 将接口返回的扁平列表重建为树：回复挂在 `parentId` 对应节点下；父节点不在列表中时视为顶层（避免丢评）。 */
export function buildCommentTree(flat: CommentItem[]): CommentTreeNode[] {
  const byId = new Map<number, CommentTreeNode>()
  for (const c of flat) {
    byId.set(c.id, { ...c, replies: [] })
  }
  const roots: CommentTreeNode[] = []
  for (const c of flat) {
    const node = byId.get(c.id)!
    const pid = c.parentId
    if (pid == null) {
      roots.push(node)
      continue
    }
    const parent = byId.get(pid)
    if (parent) {
      parent.replies.push(node)
    } else {
      roots.push(node)
    }
  }
  roots.sort(compareByTime)
  const sortNested = (n: CommentTreeNode) => {
    n.replies.sort(compareByTime)
    n.replies.forEach(sortNested)
  }
  roots.forEach(sortNested)
  return roots
}

/** 供回复入口使用：去掉 `replies` 以符合 `CommentItem` 形态。 */
export function threadNodeToItem(node: CommentTreeNode): CommentItem {
  const { replies: _r, ...rest } = node
  return rest
}
