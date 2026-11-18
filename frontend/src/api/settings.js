import request from '@/utils/request'

export function getSettingsApi() {
  return request({
    url: '/settings',
    method: 'get'
  })
}

export function updateSettingsApi(data) {
  return request({
    url: '/settings',
    method: 'put',
    data
  })
}
