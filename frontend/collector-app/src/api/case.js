import request from '@/utils/request'

// 问题上报
export function reportCase(data) {
  return request({
    url: '/case/report',
    method: 'post',
    data
  })
}

// 获取我的上报记录
export function getMyCaseList(params) {
  return request({
    url: '/case/reporter/list',
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

// 待办案件（处置人员：status=handling）
export function getPendingCaseList(params) {
  return request({
    url: '/case/pending',
    method: 'get',
    params
  })
}

// 提交处置结果
export function handleCase(data) {
  return request({
    url: '/case/handle',
    method: 'post',
    data
  })
}

// 处置人员回退至部门（提交前）
export function handlerReturnDept(data) {
  return request({
    url: '/case/handler-return-dept',
    method: 'post',
    data
  })
}

// 案件附件
export function getCaseAttachments(caseId) {
  return request({
    url: `/case/${caseId}/attachments`,
    method: 'get'
  })
}

// 获取大类列表；type: 1=部件, 2=事件
export function getCategoryBigList(type) {
  return request({
    url: '/config/category/big/list',
    method: 'get',
    params: type != null ? { type } : undefined
  })
}

// 获取小类列表
export function getCategorySmallList(bigId) {
  return request({
    url: `/config/category/small/list/${bigId}`,
    method: 'get'
  })
}

// 获取立案条件
export function getConditions(smallId) {
  return request({
    url: `/config/standard/conditions/${smallId}`,
    method: 'get'
  })
}

// 文件上传
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    skipErrorToast: true
  })
}