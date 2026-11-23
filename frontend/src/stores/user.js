import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi, getUserInfoApi } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/token'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref(null)
  const permissions = ref([])

  const isLoggedIn = computed(() => !!token.value)

  async function login(username, password, captchaKey, captchaCode, rememberMe) {
    try {
      const res = await loginApi(username, password, captchaKey, captchaCode, rememberMe)
      token.value = res.data.token
      setToken(token.value, rememberMe)
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
      permissions.value = res.data.permissions || []
    } catch (error) {
      userInfo.value = null
      permissions.value = []
    }
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    removeToken()
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      // 后端不可用时也允许本地退出
    } finally {
      clearAuth()
    }
  }

  return {
    token,
    userInfo,
    permissions,
    isLoggedIn,
    login,
    getUserInfo,
    logout,
    clearAuth
  }
})
