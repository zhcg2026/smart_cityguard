import request from '@/utils/request'

export function getUnreadMessageCount() {
  return request({
    url: '/message/unread/count',
    method: 'get'
  })
}

export function getUnreadMessages() {
  return request({
    url: '/message/unread',
    method: 'get'
  })
}

/** 当前用户可见的今日提示（与管理端工作台同源） */
export function getDailyTipList(params) {
  return request({
    url: '/message/dailytip/latest',
    method: 'get',
    params
  })
}

export function getDailyTipDetail(id) {
  return request({
    url: `/message/dailytip/${id}`,
    method: 'get'
  })
}

/** 当前用户可见的公文通告 */
export function getAnnouncementList(params) {
  return request({
    url: '/message/announcement/list',
    method: 'get',
    params
  })
}

export function getAnnouncementDetail(id) {
  return request({
    url: `/message/announcement/${id}`,
    method: 'get'
  })
}
