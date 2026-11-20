import request from '@/utils/request'

export function getDeptTreeApi() {
  return request({
    url: '/depts/tree',
    method: 'get'
  })
}

export function getDeptOptionsApi() {
  return request({
    url: '/depts/options',
    method: 'get'
  })
}

export function createDeptApi(data) {
  return request({
    url: '/depts',
    method: 'post',
    data
  })
}

export function updateDeptApi(id, data) {
  return request({
    url: `/depts/${id}`,
    method: 'put',
    data
  })
}

export function deleteDeptApi(id) {
  return request({
    url: `/depts/${id}`,
    method: 'delete'
  })
}
