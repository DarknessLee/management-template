import { normalizeLocale } from '@/locales'

/**
 * 菜单和路由 meta 共用：英文环境优先取 titleEn，缺失时回退中文标题。
 */
export function localizedTitle(item, locale) {
  if (!item) {
    return ''
  }
  if (normalizeLocale(locale) === 'en') {
    return item.titleEn || item.title || ''
  }
  return item.title || ''
}
