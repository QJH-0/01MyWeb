import { describe, expect, it } from 'vitest'
import { buildCommentTree, threadNodeToItem, type CommentTreeNode } from './commentTree'
import type { CommentItem } from '../api/comments'

function item(p: Partial<CommentItem> & Pick<CommentItem, 'id' | 'content' | 'createdAt'>): CommentItem {
  return {
    targetType: 'blog',
    targetId: 1,
    parentId: null,
    authorUserId: 1,
    authorUsername: 'u',
    likeCount: 0,
    ...p,
  }
}

describe('buildCommentTree', () => {
  it('nests replies under parent and sorts by time', () => {
    const flat: CommentItem[] = [
      item({ id: 2, parentId: 1, content: 'b', createdAt: '2026-01-01T00:00:02Z' }),
      item({ id: 1, parentId: null, content: 'a', createdAt: '2026-01-01T00:00:01Z' }),
      item({ id: 3, parentId: 1, content: 'c', createdAt: '2026-01-01T00:00:03Z' }),
    ]
    const roots = buildCommentTree(flat)
    expect(roots).toHaveLength(1)
    expect(roots[0].id).toBe(1)
    expect(roots[0].replies.map((r) => r.id)).toEqual([2, 3])
  })

  it('promotes orphan replies to roots when parent missing', () => {
    const flat: CommentItem[] = [item({ id: 9, parentId: 999, content: 'orphan', createdAt: '2026-01-01T00:00:01Z' })]
    const roots = buildCommentTree(flat)
    expect(roots).toHaveLength(1)
    expect(roots[0].id).toBe(9)
  })
})

describe('threadNodeToItem', () => {
  it('strips replies field', () => {
    const n: CommentTreeNode = {
      ...item({ id: 1, content: 'x', createdAt: '2026-01-01T00:00:00Z' }),
      replies: [],
    }
    const flat = threadNodeToItem(n)
    expect(flat).not.toHaveProperty('replies')
    expect(flat.id).toBe(1)
  })
})
