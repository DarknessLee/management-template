import request from '@/utils/request'

export function getDictTypesApi(params) {
  return request({
    url: '/dicts/types',
    method: 'get',
    params
  })
}

export function createDictTypeApi(data) {
  return request({
    url: '/dicts/types',
    method: 'post',
    data
  })
}

export function updateDictTypeApi(id, data) {
  return request({
    url: `/dicts/types/${id}`,
    method: 'put',
    data
  })
}

export function deleteDictTypeApi(id) {
  return request({
    url: `/dicts/types/${id}`,
    method: 'delete'
  })
}

export function getDictItemsApi(typeId) {
  return request({
    url: `/dicts/types/${typeId}/items`,
    method: 'get'
  })
}

export function createDictItemApi(data) {
  return request({
    url: '/dicts/items',
    method: 'post',
    data
  })
}

export function updateDictItemApi(id, data) {
  return request({
    url: `/dicts/items/${id}`,
    method: 'put',
    data
  })
}

export function deleteDictItemApi(id) {
  return request({
    url: `/dicts/items/${id}`,
    method: 'delete'
  })
}

/** 业务页面按编码取字典项，用于渲染下拉框 */
export function getDictItemsByCodeApi(code) {
  return request({
    url: `/dicts/code/${code}/items`,
    method: 'get'
  })
}
