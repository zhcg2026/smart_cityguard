/** 案件状态码 → 中文展示（列表/详情共用） */
export const CASE_STATUS_LABELS = {
  reported: '已上报',
  pending_verify: '待核实',
  pending_register: '待立案',
  accepted: '已立案',
  pending_dispatch: '待派遣',
  dispatched: '已派遣',
  pending_handle: '待指派',
  handling: '处置中',
  suspended: '挂账中',
  handle_finish: '处置人员已处置',
  pending_check: '待结案',
  checking: '核查中',
  check_pass: '核查通过',
  check_not_pass: '核查不通过',
  pending_close: '待结案',
  returned: '部门回退',
  closed: '已结案',
  /** 历史预留状态码，结案接口实际写入 closed；展示与已结案一致 */
  forced_close: '已结案',
  not_accepted: '作废',
  cancelled: '作废'
}

export const CASE_STATUS_TAG_TYPES = {
  reported: 'warning',
  pending_verify: 'warning',
  pending_register: 'warning',
  pending_dispatch: 'info',
  pending_handle: 'primary',
  handling: 'warning',
  suspended: 'info',
  handle_finish: 'success',
  pending_check: 'primary',
  checking: 'primary',
  returned: 'warning',
  closed: 'success',
  not_accepted: 'danger',
  cancelled: 'danger'
}

export function normalizeCaseStatus(status) {
  if (status == null || status === '') return ''
  return String(status).trim().toLowerCase()
}

/**
 * @param {string|object} statusOrRow 状态码或案件行（含 pendingVerifyTask / pendingCheckTask 等）
 * @param {{ detail?: boolean }} options detail 为 true 时支线任务带「（可选）」后缀
 */
export function formatCaseStatusLabel(statusOrRow, options = {}) {
  let row = null
  let status = statusOrRow
  if (statusOrRow && typeof statusOrRow === 'object') {
    row = statusOrRow
    status = row.caseStatus
  }
  if (row?.awaitingDeptConfirm) return '处置人员已处置'
  if (row?.awaitingDispatcherForward) return '待派遣员把关'
  const code = normalizeCaseStatus(status)
  if (!code) return '--'

  if (row?.pendingVerifyTask) {
    return options.detail ? '核实中（可选）' : '核实中'
  }
  if (row?.pendingCheckTask || code === 'checking') {
    return options.detail ? '核查中（可选）' : '核查中'
  }

  if (code === 'pending_check') {
    if (row?.currentHandlerId != null) {
      return '待结案'
    }
    return '待批转受理员'
  }

  return CASE_STATUS_LABELS[code] || status || '--'
}

export function getCaseStatusTagType(status) {
  const code = normalizeCaseStatus(status)
  return CASE_STATUS_TAG_TYPES[code] || 'info'
}
