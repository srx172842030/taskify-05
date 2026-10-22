function pad(n: number): string {
  return String(n).padStart(2, '0')
}

/** 将时间戳格式化为展示文本；全天只显示日期。 */
export function formatTaskTime(value: string | null | undefined, allDay: boolean): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  if (allDay) return date
  return `${date} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** ISO 时间戳 → datetime-local 输入值（本地时区）。 */
export function toDatetimeLocal(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** datetime-local 输入值 → ISO 时间戳。 */
export function fromDatetimeLocal(v: string): string | null {
  if (!v) return null
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return null
  return d.toISOString()
}
