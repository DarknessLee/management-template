import { computed, ref } from 'vue'
import { getDictItemsByCodeApi } from '@/api/dict'

// 同一编码只请求一次，页面切换后直接复用
const cache = new Map()

/**
 * 读取业务字典。
 * @param {string} code 字典编码
 * @param {Array<{label: string, value: *}>} fallback 接口返回前的兜底选项
 * @param {(value: string) => *} convert 把字典值转成业务需要的类型
 */
export function useDictOptions(code, fallback = [], convert = value => value) {
  const items = ref(cache.get(code) || fallback)
  if (!cache.has(code)) {
    getDictItemsByCodeApi(code)
      .then(res => {
        if (res.data && res.data.length) {
          cache.set(code, res.data)
          items.value = res.data
        }
      })
      .catch(() => {
        // 字典接口异常时继续使用兜底选项
      })
  }
  return computed(() => items.value.map(item => ({
    label: item.label,
    value: convert(item.value)
  })))
}

/** 按值取字典标签，未匹配时直接展示原值；兼容传入 ref 或已解包的数组 */
export function dictLabel(options, value) {
  const list = Array.isArray(options) ? options : (options?.value || [])
  const hit = list.find(item => String(item.value) === String(value))
  return hit ? hit.label : String(value)
}
