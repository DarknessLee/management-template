import axios from 'axios'
import { ElMessage } from 'element-plus'
import i18n from '@/locales'
import { getToken, refreshToken, removeToken } from '@/utils/token'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/** 401 时只清理本地状态并跳登录页，避免再调 logout 接口造成递归 */
function forceLogout() {
  removeToken()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

service.interceptors.request.use(
  (config) => {
    // 直接读本地存储取 token，避免 pinia store 与 request 循环依赖
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 让后端按当前界面语言返回提示信息（错误信息、校验信息、401/403）
    config.headers['Accept-Language'] = i18n.global.locale.value === 'en' ? 'en' : 'zh-CN'
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    // 后端在会话临期时通过响应头下发新 token，写回原存储位置即可无感续期
    const renewed = response.headers['x-new-token']
    if (renewed) {
      refreshToken(renewed)
    }
    // 文件下载直接返回完整响应，交给调用方处理 Blob 与文件名
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || i18n.global.t('common.requestFailed'))
      if (res.code === 401) {
        forceLogout()
      }
      return Promise.reject(new Error(res.message || i18n.global.t('common.requestFailed')))
    }
    // 直接返回响应体 { code, message, data }，页面里少一层 .data
    return res
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error(i18n.global.t('common.sessionExpired'))
        forceLogout()
      } else if (status === 403) {
        ElMessage.error(i18n.global.t('common.noPermission'))
      } else {
        ElMessage.error(i18n.global.t('common.networkError'))
      }
    } else {
      ElMessage.error(i18n.global.t('common.networkFailed'))
    }
    return Promise.reject(error)
  }
)

export default service
