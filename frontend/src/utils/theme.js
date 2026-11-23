const THEME_KEY = 'mgmt-theme'

/**
 * 切换 html.dark 类：Element Plus 暗色变量和自定义 CSS 变量都挂在这个类下面。
 */
export function applyTheme(theme) {
  const dark = theme === 'dark'
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
}

/**
 * 缓存主题只为避免刷新瞬间闪白，最终主题仍以接口配置为准。
 */
export function cacheTheme(theme) {
  localStorage.setItem(THEME_KEY, theme)
}

export function getCachedTheme() {
  return localStorage.getItem(THEME_KEY) || 'light'
}
