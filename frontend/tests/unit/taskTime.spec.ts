import { describe, expect, it } from 'vitest'
import { formatTaskTime, toDatetimeLocal, fromDatetimeLocal } from '@/services/taskTime'

describe('taskTime', () => {
  it('空值返回空串', () => {
    expect(formatTaskTime(null, false)).toBe('')
    expect(formatTaskTime(undefined, true)).toBe('')
  })

  it('全天只显示日期（不含时刻）', () => {
    const text = formatTaskTime('2026-10-25T09:30:00Z', true)
    expect(text).toContain('2026')
    expect(text).not.toContain(':')
  })

  it('datetime-local 往返可还原日期部分', () => {
    const iso = fromDatetimeLocal('2026-10-25T09:00')
    expect(iso).not.toBeNull()
    expect(toDatetimeLocal(iso!)).toContain('2026-10-25')
  })

  it('非法输入安全降级', () => {
    expect(formatTaskTime('not-a-date', false)).toBe('not-a-date')
    expect(fromDatetimeLocal('')).toBeNull()
  })
})
