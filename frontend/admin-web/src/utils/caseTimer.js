/** 案件各阶段计时展示文案 */

const STAGE_DEADLINE_LABEL = {
  accept: '受理截止时间',
  dispatch: '派遣截止时间',
  handle: '处置截止时间'
}

const STAGE_REMAINING_LABEL = {
  accept: '受理时限',
  dispatch: '派遣时限',
  handle: '处置时限'
}

export function stageDeadlineLabel(stage) {
  return STAGE_DEADLINE_LABEL[stage] || '截止时间'
}

export function stageRemainingLabel(stage) {
  return STAGE_REMAINING_LABEL[stage] || '剩余时限'
}

export function formatStageDeadlineTime(stage) {
  return stageDeadlineLabel(stage)
}

/** 列表行：优先 stageDeadlineTime，处置阶段可回退 case_info.deadlineTime */
export function rowStageDeadline(row) {
  if (row?.stageDeadlineTime) return row.stageDeadlineTime
  if (row?.timerStage === 'handle' && row?.deadlineTime) return row.deadlineTime
  return row?.stageDeadlineTime || row?.deadlineTime || ''
}

export function isRowStageOverdue(row) {
  if (row?.stageTimeout != null) return row.stageTimeout
  if (row?.timerStage === 'handle') return row?.handleTimeout
  return false
}
