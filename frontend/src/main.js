import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import permission from './directives/permission'
import { useSiteStore } from './stores/site'
import './styles/index.scss'

const app = createApp(App)

app.use(createPinia())
// 先读取品牌配置，登录页和侧边栏可直接展示
useSiteStore().load()
app.use(router)
app.use(ElementPlus)
app.directive('permission', permission)

app.mount('#app')