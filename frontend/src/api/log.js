import request from '@/utils/request'

export function getLogsApi(params) {
  return request({
    url: '/logs',
    method: 'get',
    params
  })
}

export function getLogStatsApi() {
  return request({
    url: '/logs/stats',
    method: 'get'
  })
}

export function exportLogsApi(params) {
  return request({
    url: '/logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function cleanLogsApi(before) {
  return request({
    url: '/logs/clean',
    method: 'delete',
    params: { before }
  })
}
