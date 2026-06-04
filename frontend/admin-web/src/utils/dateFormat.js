/** 格式化接口返回的日期时间（ISO 的 T 转为空格，显示到秒） */
export function formatDateTime(value) {
  if (value == null || value === '') {
    return '--'
  }
  let s = String(value).trim()
  if (s.includes('T')) {
    s = s.replace('T', ' ')
  }
  if (s.length >= 19) {
    return s.slice(0, 19)
  }
  if (s.length >= 16) {
    return s.slice(0, 16)
  }
  return s
}
