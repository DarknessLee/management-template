<template>
  <div class="layout-container">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-logo">
        <h2 v-show="!sidebarCollapsed">后台管理</h2>
        <span v-show="sidebarCollapsed" class="logo-icon">MG</span>
      </div>
      <nav class="sidebar-nav">
        <el-menu 
          :default-active="activeMenu" 
          :collapse="sidebarCollapsed"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
        >
          <el-menu-item
            v-for="menu in menuStore.menus"
            :key="menu.path"
            :index="menu.path"
          >
            <el-icon><component :is="icons[menu.icon] || icons.Menu" /></el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
        </el-menu>
      </nav>
    </aside>
    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <el-button @click="sidebarCollapsed = !sidebarCollapsed" class="toggle-btn">
            <el-icon><component :is="sidebarCollapsed ? icons.Expand : icons.Fold" /></el-icon>
          </el-button>
          <span class="breadcrumb">
            {{ currentPageTitle }}
          </span>
        </div>
        <div class="header-right">
          <el-popover
            v-model:visible="noticePopoverVisible"
            placement="bottom-end"
            :width="360"
            trigger="click"
          >
            <template #reference>
              <el-badge
                :value="unreadNotices.length"
                :max="99"
                :hidden="unreadNotices.length === 0"
                class="notice-badge"
              >
                <el-button class="notice-btn" circle>
                  <el-icon><component :is="icons.Bell" /></el-icon>
                </el-button>
              </el-badge>
            </template>
            <div class="notice-panel">
              <div class="notice-panel-header">
                <span>未读公告</span>
                <span class="notice-count">{{ unreadNotices.length }} 条</span>
              </div>
              <div v-if="unreadNotices.length === 0" class="notice-empty">暂无未读公告</div>
              <div v-else class="notice-list">
                <div
                  v-for="notice in unreadNotices"
                  :key="notice.id"
                  class="notice-item"
                  @click="openNotice(notice)"
                >
                  <p class="notice-title">{{ notice.title }}</p>
                  <p class="notice-time">{{ notice.publishTime || notice.createTime }}</p>
                </div>
              </div>
            </div>
          </el-popover>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="28" :src="assetUrl(userStore.userInfo?.avatar)">
                <el-icon><component :is="icons.User" /></el-icon>
              </el-avatar>
              <span>{{ userStore.userInfo?.realName || '用户' }}</span>
              <el-icon><component :is="icons.ArrowDown" /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <div class="content-wrapper">
        <router-view />
      </div>
    </main>

    <el-dialog v-model="noticeDialogVisible" :title="currentNotice.title" width="600px">
      <div class="notice-detail">
        <p class="notice-detail-time">发布时间：{{ currentNotice.publishTime || '—' }}</p>
        <p class="notice-detail-content">{{ currentNotice.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'
import { assetUrl } from '@/utils/asset'
import { getUnreadNoticesApi, markNoticeReadApi } from '@/api/notice'
import { 
  Odometer, 
  User, 
  UserFilled, 
  Operation,
  Key,
  Document,
  Clock,
  Bell,
  Message,
  Collection,
  Menu,
  Monitor,
  Connection,
  OfficeBuilding,
  Fold, 
  Expand, 
  ArrowDown 
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const menuStore = useMenuStore()

const sidebarCollapsed = ref(false)

const unreadNotices = ref([])
const noticePopoverVisible = ref(false)
const noticeDialogVisible = ref(false)
const currentNotice = ref({})

const icons = {
  Odometer,
  User,
  UserFilled,
  Operation,
  Key,
  Document,
  Clock,
  Bell,
  Message,
  Collection,
  Menu,
  Monitor,
  Connection,
  OfficeBuilding,
  Fold,
  Expand,
  ArrowDown
}

const activeMenu = computed(() => route.path)

const currentPageTitle = computed(() => route.meta.title || '')

// 刷新页面后 pinia 状态丢失，用 token 重新拉一次用户信息
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.getUserInfo()
  }
  if (userStore.token) {
    loadUnreadNotices()
  }
})

async function loadUnreadNotices() {
  try {
    const res = await getUnreadNoticesApi()
    unreadNotices.value = res.data
  } catch (error) {
    // 拦截器已提示
  }
}

// 点击即视为已读：先展示内容，再调用接口并从列表移除
async function openNotice(notice) {
  currentNotice.value = notice
  noticeDialogVisible.value = true
  noticePopoverVisible.value = false
  try {
    await markNoticeReadApi(notice.id)
    unreadNotices.value = unreadNotices.value.filter(item => item.id !== notice.id)
  } catch (error) {
    // 标记失败不影响查看内容
  }
}

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    await userStore.logout()
    resetDynamicRoutes()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

// 退出时移除动态注册的路由，避免下一个登录用户还能访问上一个用户的页面
function resetDynamicRoutes() {
  menuStore.menus.forEach((menu) => {
    if (router.hasRoute(menu.path)) {
      router.removeRoute(menu.path)
    }
  })
  menuStore.reset()
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background: #304156;
  transition: width 0.3s ease;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2b3a4a;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: #409EFF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.sidebar-nav {
  padding-top: 20px;
}

.sidebar-nav :deep(.el-menu) {
  border-right: none;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 60px;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #EBEEF5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle-btn {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #F5F7FA;
  cursor: pointer;
}

.toggle-btn:hover {
  background: #E4E7ED;
}

.breadcrumb {
  font-size: 14px;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #F5F7FA;
}

.notice-btn:hover {
  background: #E4E7ED;
}

.notice-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #EBEEF5;
  font-weight: 600;
  color: #303133;
}

.notice-count {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.notice-empty {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.notice-list {
  max-height: 320px;
  overflow-y: auto;
}

.notice-item {
  padding: 10px 4px;
  border-bottom: 1px solid #F2F6FC;
  cursor: pointer;
}

.notice-item:hover {
  background: #F5F7FA;
}

.notice-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.notice-time {
  font-size: 12px;
  color: #909399;
}

.notice-detail-time {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.notice-detail-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 6px;
}

.user-info:hover {
  background: #F5F7FA;
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  background: #F5F7FA;
}
</style>