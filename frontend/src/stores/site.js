import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getPublicSettingsApi } from '@/api/settings'
import { applyTheme, cacheTheme, getCachedTheme } from '@/utils/theme'

const THEME_KEY = 'mgmt-theme'

const DEFAULT_SITE = {
  siteTitle: '后台管理系统',
  siteLogo: '',
  defaultTheme: 'light',
  language: 'zh-CN',
  watermark: false
}

export const useSiteStore = defineStore('site', () => {
  const config = ref({ ...DEFAULT_SITE })
  // 当前实际生效的主题：用户手动切换的本地偏好优先于站点默认配置
  const theme = ref(getCachedTheme())
  let loaded = false

  /**
   * persist=false 只做即时预览（设置页），不写入本地缓存。
   */
  function setTheme(next, persist = true) {
    theme.value = next === 'dark' ? 'dark' : 'light'
    applyTheme(theme.value)
    if (persist) {
      cacheTheme(theme.value)
    }
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  /**
   * 品牌配置只在启动和保存设置后拉取；接口异常时保持默认值，不阻塞页面。
   */
  async function load(force = false) {
    if (loaded && !force) {
      return config.value
    }
    try {
      const res = await getPublicSettingsApi()
      config.value = { ...DEFAULT_SITE, ...(res.data || {}) }
      loaded = true
      // 首次访问用站点默认主题；保存设置时强制应用；其余情况保留用户本地切换结果
      if (force || !localStorage.getItem(THEME_KEY)) {
        setTheme(config.value.defaultTheme)
      }
    } catch (error) {
      // 读取失败时使用默认品牌
    }
    return config.value
  }

  return { config, theme, load, setTheme, toggleTheme }
})
