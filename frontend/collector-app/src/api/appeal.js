import request from '@/utils/request'

export function submitTimeoutAppeal(data) {
  return request({
    url: '/appeal/timeout/submit',
    method: 'post',
    data
  })
}

export function deptReviewTimeoutAppeal(data) {
  return request({
    url: '/appeal/timeout/dept-review',
    method: 'post',
    data
  })
}

export function dispatcherReviewTimeoutAppeal(data) {
  return request({
    url: '/appeal/timeout/dispatcher-review',
    method: 'post',
    data
  })
}

export function acceptorReviewTimeoutAppeal(data) {
  return request({
    url: '/appeal/timeout/acceptor-review',
    method: 'post',
    data
  })
}

export function getTimeoutAppealDetail(id) {
  return request({
    url: `/appeal/timeout/detail/${id}`,
    method: 'get'
  })
}

export function getTimeoutAppealByCase(caseId) {
  return request({
    url: `/appeal/timeout/by-case/${caseId}`,
    method: 'get'
  })
}

export function getTimeoutAppealList(params) {
  return request({
    url: '/appeal/timeout/list',
    method: 'get',
    params
  })
}

export function getAppealableCases(params) {
  return request({
    url: '/appeal/timeout/appealable',
    method: 'get',
    params
  })
}
