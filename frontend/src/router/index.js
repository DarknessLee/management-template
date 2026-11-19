import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

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
        meta: { title: '仪表盘', icon: 'icon-dashboard' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/index.vue'),
        meta: { title: '用户管理', icon: 'icon-users', permission: 'users:view' }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/roles/index.vue'),
        meta: { title: '角色管理', icon: 'icon-roles', permission: 'roles:view' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/logs/index.vue'),
        meta: { title: '操作日志', icon: 'icon-logs', permission: 'logs:view' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: '系统设置', icon: 'icon-settings', permission: 'settings:view' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
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
  // 路由级权限：没有查看权限直接回仪表盘
  const permission = to.meta.permission
  if (permission && !userStore.permissions.includes(permission)) {
    next('/dashboard')
    return
  }
  next()
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 后台管理系统` : '后台管理系统'
})

export default router