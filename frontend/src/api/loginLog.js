import request from '@/utils/request'

export function getLoginLogsApi(params) {
  return request({
    url: '/login-logs',
    method: 'get',
    params
  })
}
