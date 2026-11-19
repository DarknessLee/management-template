import request from '@/utils/request'

export function getLogsApi(params) {
  return request({
    url: '/logs',
    method: 'get',
    params
  })
}
