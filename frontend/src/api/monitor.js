import request from '@/utils/request'

export function getMonitorApi() {
  return request({
    url: '/monitor',
    method: 'get'
  })
}
