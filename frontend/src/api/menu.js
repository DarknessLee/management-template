import request from '@/utils/request'

/** 当前用户可见的菜单，后端已按权限过滤 */
export function getCurrentMenusApi() {
  return request({
    url: '/menus/current',
    method: 'get'
  })
}

export function getMenusApi() {
  return request({
    url: '/menus',
    method: 'get'
  })
}

export function createMenuApi(data) {
  return request({
    url: '/menus',
    method: 'post',
    data
  })
}

export function updateMenuApi(id, data) {
  return request({
    url: `/menus/${id}`,
    method: 'put',
    data
  })
}

export function deleteMenuApi(id) {
  return request({
    url: `/menus/${id}`,
    method: 'delete'
  })
}
