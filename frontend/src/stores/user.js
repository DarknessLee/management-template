import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi, getUserInfoApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  async function login(username, password) {
    try {
      const res = await loginApi(username, password)
      token.value = res.data.token
      localStorage.setItem('token', token.value)
      await getUserInfo()
      return true
    } catch (error) {
      // 错误提示已在 axios 拦截器里统一弹出
      return false
    }
  }

  async function getUserInfo() {
    try {
      const res = await getUserInfoApi()
      userInfo.value = res.data
    } catch (error) {
      userInfo.value = null
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      // 后端不可用时也允许本地退出
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    getUserInfo,
    logout
  }
})
