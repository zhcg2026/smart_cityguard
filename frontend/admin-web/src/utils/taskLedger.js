/** 任务台账：与库表 check_task（核查）、verify_task（核实）及 TaskStatusConstant 对齐 */

export const TASK_STATUS_OPTIONS = [
  { label: '待执行', value: 'pending' },
  { label: '已完成', value: 'done' },
  { label: '已取消', value: 'cancelled' }
]

export function taskStatusLabel(status) {
  const map = {
    pending: '待执行',
    done: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status || '--'
}

export function taskStatusTagType(status) {
  if (status === 'pending') return 'warning'
  if (status === 'done') return 'success'
  if (status === 'cancelled') return 'info'
  return 'info'
}

export function checkResultLabel(code) {
  const map = {
    pass: '核查通过',
    not_pass: '核查不通过',
    unable: '无法核查'
  }
  return map[code] || code || '--'
}

export function verifyResultLabel(code) {
  const map = {
    exist: '问题存在',
    not_exist: '问题不存在',
    unable: '无法核实'
  }
  return map[code] || code || '--'
}
