import request from '@/utils/request'

// 获取大类列表
export function getCategoryBigList(params) {
  return request({
    url: '/config/category/big/list',
    method: 'get',
    params
  })
}

// 获取案件列表
export function getCaseList(params) {
  return request({
    url: '/case/list',
    method: 'get',
    params
  })
}

/** 综合查询（多条件分页） */
export function queryCases(data) {
  return request({
    url: '/case/query',
    method: 'post',
    data
  })
}

/** 考核统计（按处置部门） */
export function getCaseReportStatistics(data) {
  return request({
    url: '/case/report/statistics',
    method: 'post',
    data
  })
}

/** 考核统计数字反查 */
export function drillCaseReport(data) {
  return request({
    url: '/case/report/drill',
    method: 'post',
    data
  })
}

// 工作台案件统计（period: day | week | month | year）
export function getCaseDashboardStats(params) {
  return request({
    url: '/case/dashboard/stats',
    method: 'get',
    params
  })
}

// 工作台待办事项
export function getCaseDashboardTodos(params) {
  return request({
    url: '/case/dashboard/todos',
    method: 'get',
    params
  })
}

/** 工作台全部待办（分页，聚合各角色待办队列） */
export function getCaseDashboardTodosPage(params) {
  return request({
    url: '/case/dashboard/todos/page',
    method: 'get',
    params
  })
}

// 获取待处理案件列表
export function getPendingCaseList(params) {
  return request({
    url: '/case/pending',
    method: 'get',
    params
  })
}

// 获取案件详情
export function getCaseDetail(id) {
  return request({
    url: `/case/${id}`,
    method: 'get'
  })
}

/** 受理员人工登记（电话投诉等，入库为待立案） */
export function acceptorRegisterCase(data) {
  return request({
    url: '/case/acceptor-register',
    method: 'post',
    data
  })
}

// 立案
export function registerCase(data) {
  return request({
    url: '/case/register',
    method: 'post',
    data
  })
}

// 派遣案件至处置部门
export function dispatchCase(data) {
  return request({
    url: '/case/dispatch',
    method: 'post',
    data
  })
}

// 处置部门指派具体处置人员
export function assignHandlerCase(data) {
  return request({
    url: '/case/assign-handler',
    method: 'post',
    data
  })
}

// 处置案件
export function handleCase(data) {
  return request({
    url: '/case/handle',
    method: 'post',
    data
  })
}

// 处置部门确认并批转派遣员
export function deptConfirmCase(data) {
  return request({
    url: '/case/dept-confirm',
    method: 'post',
    data
  })
}

// 处置部门回退至派遣员
export function deptReturnCase(data) {
  return request({
    url: '/case/dept-return',
    method: 'post',
    data
  })
}

export function deptRevokeAssign(data) {
  return request({
    url: '/case/dept-revoke-assign',
    method: 'post',
    data
  })
}

export function deptReturnHandler(data) {
  return request({
    url: '/case/dept-return-handler',
    method: 'post',
    data
  })
}

export function handlerReturnDept(data) {
  return request({
    url: '/case/handler-return-dept',
    method: 'post',
    data
  })
}

export function dispatcherReturnAcceptor(data) {
  return request({
    url: '/case/dispatcher-return-acceptor',
    method: 'post',
    data
  })
}

export function dispatcherReturnDept(data) {
  return request({
    url: '/case/dispatcher-return-dept',
    method: 'post',
    data
  })
}

export function acceptorReturnDispatcher(data) {
  return request({
    url: '/case/acceptor-return-dispatcher',
    method: 'post',
    data
  })
}

// 派遣员把关通过，批转立案受理员
export function dispatcherForwardToAcceptor(data) {
  return request({
    url: '/case/dispatcher-forward-acceptor',
    method: 'post',
    data
  })
}

/** 下发核查/核实前：可选采集员（含距离推荐、最近上报位置） */
export function getCollectorCandidates(caseId) {
  return request({
    url: `/case/${caseId}/collector-candidates`,
    method: 'get'
  })
}

/** 可选：立案前下发核查（非必经） */
export function sendCheckTask(data) {
  return request({
    url: '/case/send-check',
    method: 'post',
    data
  })
}

/** 可选：结案前下发核实（非必经） */
export function sendVerifyTask(data) {
  return request({
    url: '/case/send-verify',
    method: 'post',
    data
  })
}

// 核查案件（已废弃）
export function verifyCase(data) {
  return request({
    url: '/case/verify',
    method: 'post',
    data
  })
}

// 核实案件
export function checkCase(data) {
  return request({
    url: '/case/check',
    method: 'post',
    data
  })
}

// 结案
export function closeCase(data) {
  return request({
    url: '/case/close',
    method: 'post',
    data
  })
}

// 不受理
export function rejectCase(data) {
  return request({
    url: '/case/reject',
    method: 'post',
    data
  })
}

// 管理员删除案件（支持批量，逻辑删除）
export function deleteCases(caseIds) {
  return request({
    url: '/case/delete',
    method: 'post',
    data: { caseIds }
  })
}

// 获取案件流程记录
export function getCaseFlowRecords(caseId) {
  return request({
    url: `/case/${caseId}/flow`,
    method: 'get'
  })
}

// 获取案件附件
export function getCaseAttachments(caseId) {
  return request({
    url: `/case/${caseId}/attachments`,
    method: 'get'
  })
}

// 核查任务记录（含现场照片）
export function getCheckTaskRecords(caseId) {
  return request({
    url: `/case/${caseId}/check-task-records`,
    method: 'get'
  })
}

// 核实任务记录（含现场照片）
export function getVerifyTaskRecords(caseId) {
  return request({
    url: `/case/${caseId}/verify-task-records`,
    method: 'get'
  })
}

// 延期/挂账申请
export function applyCaseAdjustment(data) {
  return request({
    url: '/case/adjustment/apply',
    method: 'post',
    data
  })
}

// 处置部门初审
export function deptReviewCaseAdjustment(data) {
  return request({
    url: '/case/adjustment/dept-review',
    method: 'post',
    data
  })
}

// 处置部门待审列表
export function getCaseAdjustmentPendingDept(params) {
  return request({
    url: '/case/adjustment/pending-dept',
    method: 'get',
    params
  })
}

// 延期/挂账：派遣员待审列表
export function getCaseAdjustmentPending(params) {
  return request({
    url: '/case/adjustment/pending',
    method: 'get',
    params
  })
}

// 延期/挂账：派遣员审批
export function reviewCaseAdjustment(data) {
  return request({
    url: '/case/adjustment/review',
    method: 'post',
    data
  })
}

// 延期/挂账：案件申请记录
export function getCaseAdjustmentList(caseId) {
  return request({
    url: `/case/adjustment/list/${caseId}`,
    method: 'get'
  })
}