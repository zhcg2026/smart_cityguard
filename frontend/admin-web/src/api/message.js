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

export function getMessageList() {
  return request({
    url: '/message/list',
    method: 'get'
  })
}

export function markMessageRead(id) {
  return request({
    url: `/message/read/${id}`,
    method: 'post'
  })
}

export function markAllMessagesRead() {
  return request({
    url: '/message/read/all',
    method: 'post'
  })
}
