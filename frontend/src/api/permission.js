import request from '@/utils/request'

export function getPermissionsApi() {
  return request({
    url: '/permissions',
    method: 'get'
  })
}

export function createPermissionApi(data) {
  return request({
    url: '/permissions',
    method: 'post',
    data
  })
}

export function updatePermissionApi(id, data) {
  return request({
    url: `/permissions/${id}`,
    method: 'put',
    data
  })
}

export function deletePermissionApi(id) {
  return request({
    url: `/permissions/${id}`,
    method: 'delete'
  })
}
