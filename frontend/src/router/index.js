import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'
import { useSiteStore } from '@/stores/site'
import i18n from '@/locales'
import { localizedTitle } from '@/utils/title'

// 菜单表里的 component 形如 views/users/index.vue，这里建立懒加载映射
const viewModules = import.meta.glob('../views/**/*.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', titleEn: 'Dashboard' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', titleEn: 'Profile' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 把后端菜单注册成 Layout 的子路由。
 * 目录只是分组没有 component，只有叶子菜单需要注册路由；
 * 仪表盘已有静态路由，跳过避免重复注册。
 */
function registerMenuRoutes(menus) {
  collectLeaves(menus).forEach((menu) => {
    if (menu.path === '/dashboard' || router.hasRoute(menu.path)) {
      return
    }
    const component = viewModules[`../${menu.component}`]
    if (!component) {
      console.warn(`未找到菜单组件：${menu.component}`)
      return
    }
    router.addRoute('Layout', {
      path: menu.path,
      name: menu.path,
      component,
      meta: {
        title: menu.title,
        titleEn: menu.titleEn,
        icon: menu.icon,
        permission: menu.permission
      }
    })
  })
}

function collectLeaves(menus, leaves = []) {
  menus.forEach((menu) => {
    if (menu.children && menu.children.length) {
      collectLeaves(menu.children, leaves)
    } else {
      leaves.push(menu)
    }
  })
  return leaves
}

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const menuStore = useMenuStore()

  if (to.path === '/login') {
    next()
    return
  }
  if (!userStore.token) {
    next('/login')
    return
  }
  // 刷新页面后 pinia 为空，必须先恢复用户信息和权限，
  // 否则 v-permission 会认为"没有任何权限"而把按钮全部移除
  if (!userStore.userInfo) {
    await userStore.getUserInfo()
  }
  if (!userStore.userInfo) {
    next('/login')
    return
  }

  // 首次进入时拉取菜单并注册路由，然后按路径重新匹配当前地址
  if (!menuStore.loaded) {
    try {
      await menuStore.loadMenus()
      registerMenuRoutes(menuStore.menus)
    } catch (error) {
      // 菜单加载失败时避免重定向死循环，仪表盘仍可访问
      menuStore.loaded = true
    }
    if (router.resolve(to.fullPath).matched.length === 0) {
      next('/dashboard')
    } else {
      next({ path: to.path, query: to.query, hash: to.hash, replace: true })
    }
    return
  }

  // 动态路由注册完后仍匹配不到，说明地址不存在
  if (to.matched.length === 0) {
    next('/dashboard')
    return
  }

  // 路由级权限：没有查看权限直接回仪表盘
  const permission = to.meta.permission
  if (permission && !userStore.permissions.includes(permission)) {
    next('/dashboard')
    return
  }
  next()
})

router.afterEach((to) => {
  const siteTitle = useSiteStore().config.siteTitle || '后台管理系统'
  const pageTitle = localizedTitle(to.meta, i18n.global.locale.value)
  document.title = pageTitle ? `${pageTitle} - ${siteTitle}` : siteTitle
})

export default router
