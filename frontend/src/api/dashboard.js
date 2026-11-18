import request from '@/utils/request'

export function getDashboardApi() {
  return request({
    url: '/dashboard',
    method: 'get'
  })
}
