import { describe, it, expect, beforeEach } from 'vitest'
import { ref } from 'vue'
import { useViewConfig } from '@/composables/useViewConfig'

describe('useViewConfig', () => {
  beforeEach(() => localStorage.clear())

  it('保存后能恢复，clear 后清除', () => {
    const pid = ref('p1')
    const { load, save, clear } = useViewConfig(pid, 'table')

    expect(load()).toBeNull()

    const cfg = { filters: { mineOnly: true }, groupBy: 'assignee', sortBy: 'dueAt', sortDir: 'asc' }
    save(cfg)
    expect(load()).toEqual(cfg)

    clear()
    expect(load()).toBeNull()
  })

  it('不同视图相互隔离', () => {
    const pid = ref('p1')
    const table = useViewConfig(pid, 'table')
    const board = useViewConfig(pid, 'board')

    table.save({ a: 1 })
    expect(board.load()).toBeNull()
    expect(table.load()).toEqual({ a: 1 })
  })

  it('不同项目相互隔离', () => {
    const tableP1 = useViewConfig(ref('p1'), 'table')
    const tableP2 = useViewConfig(ref('p2'), 'table')

    tableP1.save({ a: 1 })
    expect(tableP2.load()).toBeNull()
  })
})
