const APPEAL_STATUS_MAP = {
  pending_dept: { label: '待部门审核', type: 'warning' },
  pending_dispatcher: { label: '待派遣员初审', type: 'warning' },
  pending_acceptor: { label: '待受理员二审', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已打回', type: 'danger' }
}

export function formatAppealStatusLabel(status) {
  return APPEAL_STATUS_MAP[status]?.label || status || '--'
}

export function getAppealStatusTagType(status) {
  return APPEAL_STATUS_MAP[status]?.type || 'info'
}
