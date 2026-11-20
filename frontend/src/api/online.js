import request from '@/utils/request'

export function getOnlineUsersApi() {
  return request({
    url: '/online-users',
    method: 'get'
  })
}
