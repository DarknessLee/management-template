import request from '@/utils/request'

export function getJobsApi(params) {
  return request({
    url: '/jobs',
    method: 'get',
    params
  })
}

export function getJobHandlersApi() {
  return request({
    url: '/jobs/handlers',
    method: 'get'
  })
}

export function createJobApi(data) {
  return request({
    url: '/jobs',
    method: 'post',
    data
  })
}

export function updateJobApi(id, data) {
  return request({
    url: `/jobs/${id}`,
    method: 'put',
    data
  })
}

export function deleteJobApi(id) {
  return request({
    url: `/jobs/${id}`,
    method: 'delete'
  })
}

export function runJobApi(id) {
  return request({
    url: `/jobs/${id}/run`,
    method: 'post'
  })
}
