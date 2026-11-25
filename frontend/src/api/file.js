import request from '@/utils/request'

export function getFilesApi(params) {
  return request({
    url: '/files',
    method: 'get',
    params
  })
}

export function getFileStatsApi() {
  return request({
    url: '/files/stats',
    method: 'get'
  })
}

export function uploadFileApi(file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/files',
    method: 'post',
    data
  })
}

export function deleteFileApi(id) {
  return request({
    url: `/files/${id}`,
    method: 'delete'
  })
}

export function deleteFilesApi(ids) {
  return request({
    url: '/files/batch',
    method: 'delete',
    data: ids
  })
}

export function previewFileApi(id) {
  return request({
    url: `/files/${id}/preview`,
    method: 'get',
    responseType: 'blob'
  })
}

export function downloadFileApi(id) {
  return request({
    url: `/files/${id}/download`,
    method: 'get',
    responseType: 'blob'
  })
}
