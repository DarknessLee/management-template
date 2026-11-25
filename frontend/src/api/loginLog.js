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

export function getLoginSecurityApi() {
  return request({
    url: '/login-logs/security',
    method: 'get'
  })
}

export function getLoginLogStatsApi() {
  return request({
    url: '/login-logs/stats',
    method: 'get'
  })
}

export function unlockLoginAccountApi(username) {
  return request({
    url: '/login-logs/unlock',
    method: 'post',
    params: { username }
  })
}
