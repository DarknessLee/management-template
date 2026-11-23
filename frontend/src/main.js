import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// Element Plus 暗色变量，配合 html.dark 使用
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import permission from './directives/permission'
import i18n, { getCachedLocale, setI18nLanguage } from './locales'
import { useSiteStore } from './stores/site'
import { applyTheme, getCachedTheme } from './utils/theme'
import './styles/index.scss'

// 先用上次的主题和语言渲染，接口返回后再校正，避免刷新时闪动
applyTheme(getCachedTheme())
setI18nLanguage(getCachedLocale())

const app = createApp(App)

app.use(createPinia())
// 先读取品牌配置（含语言），登录页和侧边栏可直接展示
useSiteStore().load()
app.use(router)
app.use(i18n)
app.use(ElementPlus)
app.directive('permission', permission)

app.mount('#app')