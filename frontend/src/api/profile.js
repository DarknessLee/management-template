import request from '@/utils/request'

export function getProfileApi() {
  return request({
    url: '/profile',
    method: 'get'
  })
}

export function updateProfileApi(data) {
  return request({
    url: '/profile',
    method: 'put',
    data
  })
}

export function changePasswordApi(data) {
  return request({
    url: '/profile/password',
    method: 'put',
    data
  })
}

export function uploadAvatarApi(formData) {
  return request({
    url: '/profile/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
