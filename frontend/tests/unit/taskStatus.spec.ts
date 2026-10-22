import { describe, expect, it } from 'vitest'
import { STATUS_LABELS, STATUS_ORDER, adjacentStatuses } from '@/services/taskStatus'

describe('taskStatus 看板列与相邻关系', () => {
  it('四列顺序固定', () => {
    expect(STATUS_ORDER).toEqual(['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'])
  })

  it('相邻列仅前进或后退一步', () => {
    expect(adjacentStatuses('TODO')).toEqual(['IN_PROGRESS'])
    expect(adjacentStatuses('IN_PROGRESS')).toEqual(['TODO', 'IN_REVIEW'])
    expect(adjacentStatuses('IN_REVIEW')).toEqual(['IN_PROGRESS', 'DONE'])
    expect(adjacentStatuses('DONE')).toEqual(['IN_REVIEW'])
  })

  it('每列都有中文标签', () => {
    expect(STATUS_LABELS.TODO).toBe('待办事项')
    expect(STATUS_LABELS.IN_PROGRESS).toBe('进行中')
    expect(STATUS_LABELS.IN_REVIEW).toBe('审查中')
    expect(STATUS_LABELS.DONE).toBe('已完成')
  })
})
