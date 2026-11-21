import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getPublicSettingsApi } from '@/api/settings'

const DEFAULT_SITE = {
  siteTitle: '后台管理系统',
  siteLogo: '',
  defaultTheme: 'light',
  language: 'zh-CN'
}

export const useSiteStore = defineStore('site', () => {
  const config = ref({ ...DEFAULT_SITE })
  let loaded = false

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
    } catch (error) {
      // 读取失败时使用默认品牌
    }
    return config.value
  }

  return { config, load }
})
