/** 案件各阶段计时展示（与 admin-web 对齐） */

const STAGE_DEADLINE_LABEL = {
  accept: '受理截止',
  dispatch: '派遣截止',
  handle: '处置截止'
}

export function stageDeadlineLabel(stage) {
  return STAGE_DEADLINE_LABEL[stage] || '截止时间'
}

export function rowStageDeadline(row) {
  if (row?.stageDeadlineTime) return row.stageDeadlineTime
  if (row?.timerStage === 'handle' && row?.deadlineTime) return row.deadlineTime
  return row?.stageDeadlineTime || row?.deadlineTime || ''
}

export function isRowStageOverdue(row) {
  if (row?.stageTimeout != null) return row.stageTimeout
  if (row?.handleTimeout) return true
  const t = rowStageDeadline(row)
  if (!t) return false
  return new Date(t).getTime() < Date.now()
}

export function formatDateTimeShort(value) {
  if (!value) return ''
  const s = String(value)
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

/** 列表/详情：优先接口返回的 timeRemaining，否则按截止时间推算 */
export function formatRemainingText(row) {
  if (row?.timeRemaining) return row.timeRemaining
  const deadline = rowStageDeadline(row)
  if (!deadline) return ''
  const remainSec = Math.floor((new Date(deadline).getTime() - Date.now()) / 1000)
  if (remainSec < 0) {
    const over = Math.abs(remainSec)
    const h = Math.floor(over / 3600)
    const m = Math.floor((over % 3600) / 60)
    if (h > 0 && m > 0) return `已超时${h}小时${m}分`
    if (h > 0) return `已超时${h}小时`
    return `已超时${Math.max(1, m)}分钟`
  }
  const h = Math.floor(remainSec / 3600)
  const m = Math.floor((remainSec % 3600) / 60)
  if (h > 0 && m > 0) return `剩余${h}小时${m}分`
  if (h > 0) return `剩余${h}小时`
  return `剩余${Math.max(1, m)}分钟`
}

export function findHandleTimerStage(caseInfo) {
  const stages = caseInfo?.timerStages
  if (Array.isArray(stages) && stages.length) {
    return stages.find((s) => s.timerStage === 'handle') || null
  }
  return null
}

/** 待指派/处置中：处置阶段是否已超时（不可申请延期/挂账） */
export function isHandleStageOverdue(caseInfo) {
  if (!caseInfo) return false
  const st = caseInfo.caseStatus
  if (st !== 'pending_handle' && st !== 'handling') return false
  const handleStage = findHandleTimerStage(caseInfo)
  if (handleStage?.active && handleStage.timedOut) return true
  return buildHandleTimerDisplay(caseInfo).overdue
}

export function buildHandleTimerDisplay(caseInfo) {
  const stage = findHandleTimerStage(caseInfo)
  const deadline = stage?.deadlineTime || rowStageDeadline(caseInfo)
  if (!deadline && !caseInfo?.timeRemaining) {
    return { show: false }
  }
  const row = {
    ...caseInfo,
    stageDeadlineTime: stage?.deadlineTime || caseInfo?.stageDeadlineTime,
    deadlineTime: caseInfo?.deadlineTime,
    timeRemaining: stage?.timeRemaining || caseInfo?.timeRemaining,
    timerStage: 'handle',
    stageTimeout: stage?.timedOut ?? caseInfo?.stageTimeout,
    handleTimeout: caseInfo?.handleTimeout
  }
  const overdue = stage?.timedOut ?? isRowStageOverdue(row)
  return {
    show: true,
    limitLabel: stage?.timeLimitLabel
      ? `${stage.timeLimitLabel}${stage.continuous ? '（连续）' : ''}`
      : '',
    deadlineText: formatDateTimeShort(deadline),
    remainingText: formatRemainingText(row),
    overdue
  }
}
