/** 核查/核实任务时限展示 */

export function formatDateTimeShort(value) {
  if (!value) return ''
  const s = String(value)
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s.replace('T', ' ')
}

export function formatTaskRemaining(task) {
  if (!task || task.taskStatus !== 'pending') return ''
  if (task.timeRemaining) return task.timeRemaining
  const deadline = task.deadlineTime
  if (!deadline) return ''
  const remainSec = Math.floor((new Date(deadline).getTime() - Date.now()) / 1000)
  if (remainSec < 0) {
    const over = Math.abs(remainSec)
    const m = Math.floor((over % 3600) / 60)
    const h = Math.floor(over / 3600)
    if (h > 0 && m > 0) return `超时${h}小时${m}分`
    if (h > 0) return `超时${h}小时`
    return `超时${Math.max(1, m)}分钟`
  }
  const m = Math.floor((remainSec % 3600) / 60)
  const h = Math.floor(remainSec / 3600)
  if (h > 0 && m > 0) return `剩余${h}小时${m}分`
  if (h > 0) return `剩余${h}小时`
  return `剩余${Math.max(1, m)}分钟`
}

export function isTaskOverdue(task) {
  if (!task || task.taskStatus !== 'pending') return false
  if (task.timedOut != null) return task.timedOut
  const deadline = task.deadlineTime
  if (!deadline) return false
  return new Date(deadline).getTime() < Date.now()
}

export function buildTaskTimerBanner(task) {
  if (!task || task.taskStatus !== 'pending') {
    return { show: false }
  }
  const remainingText = formatTaskRemaining(task)
  const deadlineText = formatDateTimeShort(task.deadlineTime)
  if (!remainingText && !deadlineText) {
    return { show: false }
  }
  return {
    show: true,
    remainingText,
    deadlineText,
    overdue: isTaskOverdue(task),
    text: [remainingText, deadlineText ? `截止 ${deadlineText}` : ''].filter(Boolean).join(' · ')
  }
}

export function buildTaskListLabel(task) {
  const parts = []
  if (task?.address) parts.push(task.address)
  const remain = formatTaskRemaining(task)
  if (remain) parts.push(remain)
  return parts.join(' · ') || '—'
}
