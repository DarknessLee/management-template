import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/** 401 时只清理本地状态并跳登录页，避免再调 logout 接口造成递归 */
function forceLogout() {
  localStorage.removeItem('token')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

service.interceptors.request.use(
  (config) => {
    // 直接从 localStorage 取 token，避免 pinia store 与 request 循环依赖
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    // 文件下载直接返回完整响应，交给调用方处理 Blob 与文件名
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        forceLogout()
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 直接返回响应体 { code, message, data }，页面里少一层 .data
    return res
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        forceLogout()
      } else if (status === 403) {
        ElMessage.error('没有权限访问')
      } else {
        ElMessage.error('网络错误，请稍后重试')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default service
