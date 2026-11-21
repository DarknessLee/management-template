import request from '@/utils/request'

export function getFilesApi(params) {
  return request({
    url: '/files',
    method: 'get',
    params
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

export function downloadFileApi(id) {
  return request({
    url: `/files/${id}/download`,
    method: 'get',
    responseType: 'blob'
  })
}
