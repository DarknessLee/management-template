import request from '@/utils/request'

export function getMonitorApi() {
  return request({
    url: '/monitor',
    method: 'get'
  })
}

export function clearMonitorCacheApi(type = 'all') {
  return request({
    url: '/monitor/cache/clear',
    method: 'post',
    params: { type }
  })
}
