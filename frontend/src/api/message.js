import request from '@/utils/request'

export function getMessagesApi(params) {
  return request({
    url: '/messages',
    method: 'get',
    params
  })
}

export function getUnreadMessageCountApi() {
  return request({
    url: '/messages/unread-count',
    method: 'get'
  })
}

export function markMessageReadApi(id) {
  return request({
    url: `/messages/${id}/read`,
    method: 'put'
  })
}

export function markAllMessagesReadApi() {
  return request({
    url: '/messages/read-all',
    method: 'put'
  })
}

export function deleteMessageApi(id) {
  return request({
    url: `/messages/${id}`,
    method: 'delete'
  })
}
