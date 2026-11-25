import request from '@/utils/request'

export function getLoginLogsApi(params) {
  return request({
    url: '/login-logs',
    method: 'get',
    params
  })
}

export function exportLoginLogsApi(params) {
  return request({
    url: '/login-logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
