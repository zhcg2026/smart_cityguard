/** 格式化接口返回的日期时间（LocalDateTime 序列化多为 ISO 或 yyyy-MM-dd HH:mm:ss） */
export function formatDateTime(value) {
  if (value == null || value === '') {
    return '--'
  }
  const s = String(value)
  if (s.includes('T')) {
    return s.slice(0, 16).replace('T', ' ')
  }
  return s.length >= 16 ? s.slice(0, 16) : s
}
