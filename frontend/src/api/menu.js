import request from '@/utils/request'

/** 当前用户可见的菜单，后端已按权限过滤 */
export function getCurrentMenusApi() {
  return request({
    url: '/menus/current',
    method: 'get'
  })
}
