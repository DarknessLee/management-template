import request from '@/utils/request'

export function getNoticesApi(params) {
  return request({
    url: '/notices',
    method: 'get',
    params
  })
}

export function getNoticeCenterApi(params) {
  return request({
    url: '/notices/center',
    method: 'get',
    params
  })
}

export function getNoticeUnreadCountApi() {
  return request({
    url: '/notices/unread-count',
    method: 'get'
  })
}

export function createNoticeApi(data) {
  return request({
    url: '/notices',
    method: 'post',
    data
  })
}

export function updateNoticeApi(id, data) {
  return request({
    url: `/notices/${id}`,
    method: 'put',
    data
  })
}

export function updateNoticeStatusApi(id, status) {
  return request({
    url: `/notices/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function updateNoticePinnedApi(id, pinned) {
  return request({
    url: `/notices/${id}/pin`,
    method: 'put',
    params: { pinned }
  })
}

export function getNoticeReadStatsApi(id) {
  return request({
    url: `/notices/${id}/read-stats`,
    method: 'get'
  })
}

export function getNoticeReadsApi(id, params) {
  return request({
    url: `/notices/${id}/reads`,
    method: 'get',
    params
  })
}

export function deleteNoticeApi(id) {
  return request({
    url: `/notices/${id}`,
    method: 'delete'
  })
}

export function getUnreadNoticesApi() {
  return request({
    url: '/notices/unread',
    method: 'get'
  })
}

export function markNoticeReadApi(id) {
  return request({
    url: `/notices/${id}/read`,
    method: 'post'
  })
}
