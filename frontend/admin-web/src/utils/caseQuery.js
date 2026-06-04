/**
 * 综合查询 / 考核统计：问题状态、问题来源（业务口径，非入库渠道 register/transfer）
 */

/** 查询条件下拉：不含重复作废、不含未使用的 forced_close 独立项 */
export const CASE_QUERY_STATUS_OPTIONS = [
  { value: 'reported', label: '已上报' },
  { value: 'pending_verify', label: '待核实' },
  { value: 'pending_register', label: '待立案' },
  { value: 'accepted', label: '已立案' },
  { value: 'pending_dispatch', label: '待派遣' },
  { value: 'dispatched', label: '已派遣' },
  { value: 'pending_handle', label: '待指派' },
  { value: 'handling', label: '处置中' },
  { value: 'handle_finish', label: '处置人员已处置' },
  { value: 'pending_check', label: '待核查' },
  { value: 'checking', label: '核查中' },
  { value: 'check_pass', label: '核查通过' },
  { value: 'check_not_pass', label: '核查不通过' },
  { value: 'pending_close', label: '待结案' },
  { value: 'returned', label: '部门回退' },
  { value: 'closed', label: '已结案' },
  { value: '__voided__', label: '作废' }
]

/**
 * 问题来源（业务）：受理登记填 source_desc；采集等为 source_type
 */
export const CASE_QUERY_ORIGIN_OPTIONS = [
  { value: 'collector', label: '采集员上报' },
  { value: 'citizen', label: '市民投诉' },
  { value: 'phone', label: '电话投诉' },
  { value: 'leader', label: '领导交办' },
  { value: 'public', label: '公众举报' },
  { value: 'video', label: '视频上报' }
]

const ORIGIN_LABEL_MAP = Object.fromEntries(
  CASE_QUERY_ORIGIN_OPTIONS.map((o) => [o.value, o.label])
)

/** 将 UI 状态多选展开为后端 case_status 列表 */
export function expandCaseStatusesForQuery(selected) {
  if (!selected?.length) return undefined
  const out = new Set()
  for (const s of selected) {
    if (s === '__voided__') {
      out.add('not_accepted')
      out.add('cancelled')
    } else if (s === 'closed') {
      out.add('closed')
      out.add('forced_close')
    } else {
      out.add(s)
    }
  }
  return [...out]
}

/** 列表/详情：来源展示（渠道 + 登记说明） */
export function formatCaseOriginLabel(sourceType, sourceDesc) {
  const t = (sourceType || '').trim()
  const d = (sourceDesc || '').trim()
  if (t === 'collector') return '采集员上报'
  if (t === 'public') return '公众举报'
  if (t === 'video') return '视频上报'
  if (t === 'leader') return d ? `领导交办（${d}）` : '领导交办'
  if (t === 'register') {
    if (!d) return '受理登记'
    if (d.includes('电话')) return d.includes('投诉') ? d : `电话投诉（${d}）`
    if (d.includes('市民') || d.includes('投诉')) return d
    if (d.includes('领导')) return `领导交办（${d}）`
    return d
  }
  if (t === 'transfer') return d ? `其他（${d}）` : '其他'
  return d || t || '--'
}

export function caseOriginLabelFromRow(row) {
  if (!row) return '--'
  return formatCaseOriginLabel(row.sourceType, row.sourceDesc)
}

export function categoryApiType(categoryType) {
  return categoryType === 'component' ? 1 : 2
}
