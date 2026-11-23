import request from '@/utils/request'

export function loginApi(username, password, captchaKey, captchaCode, rememberMe = false) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: {
      username,
      password,
      captchaKey,
      captchaCode,
      rememberMe
    }
  })
}

export function getCaptchaApi() {
  return request({
    url: '/auth/captcha',
    method: 'get'
  })
}

export function getUserInfoApi() {
  return request({
    url: '/auth/userinfo',
    method: 'get'
  })
}

export function logoutApi() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getUsersApi(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

export function getUserApi(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export function createUserApi(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

export function updateUserApi(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUserApi(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

export function downloadUserTemplateApi() {
  return request({
    url: '/users/import-template',
    method: 'get',
    responseType: 'blob'
  })
}

export function exportUsersApi(params) {
  return request({
    url: '/users/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function importUsersApi(file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/users/import',
    method: 'post',
    data
  })
}

export function kickoutUserApi(id) {
  return request({
    url: `/auth/kickout/${id}`,
    method: 'post'
  })
}