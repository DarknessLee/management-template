import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import en from './en'

// 与后端设置页 language 字段保持一致：zh-CN / en
export const DEFAULT_LOCALE = 'zh-CN'
export const SUPPORTED_LOCALES = ['zh-CN', 'en']
export const LOCALE_KEY = 'mgmt-locale'

/**
 * 后端可能存 en / en-US / zh / zh-CN，统一归一化为前端支持的两种语言。
 */
export function normalizeLocale(value) {
  if (!value) {
    return DEFAULT_LOCALE
  }
  return String(value).toLowerCase().startsWith('en') ? 'en' : DEFAULT_LOCALE
}

export function getCachedLocale() {
  return normalizeLocale(localStorage.getItem(LOCALE_KEY))
}

export function cacheLocale(locale) {
  localStorage.setItem(LOCALE_KEY, normalizeLocale(locale))
}

const i18n = createI18n({
  legacy: false,
  locale: getCachedLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: {
    'zh-CN': zhCN,
    en
  }
})

/**
 * 切换语言并同步 html lang 属性，供屏幕阅读器和浏览器判断语种。
 */
export function setI18nLanguage(locale) {
  const next = normalizeLocale(locale)
  i18n.global.locale.value = next
  document.documentElement.setAttribute('lang', next)
  return next
}

export default i18n
