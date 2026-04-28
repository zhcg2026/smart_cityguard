import request from '@/utils/request'

// 获取案件列表
export function getCaseList(params) {
  return request({
    url: '/case/list',
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

// 立案
export function registerCase(data) {
  return request({
    url: '/case/register',
    method: 'post',
    data
  })
}

// 派遣案件
export function dispatchCase(data) {
  return request({
    url: '/case/dispatch',
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

// 核查案件
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