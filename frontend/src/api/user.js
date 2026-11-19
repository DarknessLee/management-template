import request from '@/utils/request'

export function loginApi(username, password, captchaKey, captchaCode) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: {
      username,
      password,
      captchaKey,
      captchaCode
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

export function kickoutUserApi(id) {
  return request({
    url: `/auth/kickout/${id}`,
    method: 'post'
  })
}