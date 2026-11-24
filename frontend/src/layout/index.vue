<template>
  <div class="layout-container">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-logo">
        <img
          v-if="siteStore.config.siteLogo"
          :src="siteStore.config.siteLogo"
          class="logo-image"
          alt="logo"
        />
        <h2 v-show="!sidebarCollapsed">{{ siteStore.config.siteTitle }}</h2>
        <span v-show="sidebarCollapsed" class="logo-icon">{{ logoLetter }}</span>
      </div>
      <nav class="sidebar-nav">
        <el-menu 
          :default-active="activeMenu" 
          :collapse="sidebarCollapsed"
          background-color="var(--app-sidebar-bg)"
          text-color="var(--app-sidebar-text)"
          active-text-color="#409EFF"
          router
        >
          <template v-for="menu in menuStore.menus" :key="menu.path">
            <el-sub-menu
              v-if="menu.children && menu.children.length"
              :index="menu.path"
            >
              <template #title>
                <el-icon><component :is="icons[menu.icon] || icons.Menu" /></el-icon>
                <span>{{ localizedTitle(menu, siteStore.locale) }}</span>
              </template>
              <el-menu-item
                v-for="child in menu.children"
                :key="child.path"
                :index="child.path"
              >
                <el-icon><component :is="icons[child.icon] || icons.Menu" /></el-icon>
                <span>{{ localizedTitle(child, siteStore.locale) }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="menu.path">
              <el-icon><component :is="icons[menu.icon] || icons.Menu" /></el-icon>
              <span>{{ localizedTitle(menu, siteStore.locale) }}</span>
            </el-menu-item>
          </template>
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
          <el-button
            class="notice-btn"
            circle
            :title="t('header.toggleTheme')"
            @click="siteStore.toggleTheme()"
          >
            <el-icon><component :is="icons[siteStore.theme === 'dark' ? 'Sunny' : 'Moon']" /></el-icon>
          </el-button>
          <el-dropdown trigger="click" @command="handleLocaleCommand">
            <el-button class="notice-btn locale-btn" :title="t('header.toggleLanguage')">
              {{ siteStore.locale === 'en' ? 'EN' : '中' }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh-CN" :disabled="siteStore.locale === 'zh-CN'">简体中文</el-dropdown-item>
                <el-dropdown-item command="en" :disabled="siteStore.locale === 'en'">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-popover
            v-model:visible="messagePopoverVisible"
            placement="bottom-end"
            :width="380"
            trigger="click"
          >
            <template #reference>
              <el-badge
                :value="messageStore.unreadCount"
                :max="99"
                :hidden="messageStore.unreadCount === 0"
                class="notice-badge"
              >
                <el-button class="notice-btn" circle :title="t('header.messageCenter')">
                  <el-icon><component :is="icons.Bell" /></el-icon>
                </el-button>
              </el-badge>
            </template>
            <div class="notice-panel">
              <div class="notice-panel-header">
                <span>{{ t('header.messageCenter') }}</span>
                <el-button
                  link
                  type="primary"
                  size="small"
                  :disabled="messageStore.unreadCount === 0"
                  @click="handleMarkAllRead"
                >
                  {{ t('header.allRead') }}
                </el-button>
              </div>
              <div v-if="messageStore.loading && messageStore.messages.length === 0" class="notice-empty">
                {{ t('common.loading') }}
              </div>
              <div v-else-if="messageStore.messages.length === 0" class="notice-empty">
                {{ t('header.noMessages') }}
              </div>
              <div v-else class="notice-list">
                <div
                  v-for="message in messageStore.messages"
                  :key="message.id"
                  class="notice-item"
                  :class="{ unread: message.readFlag === 0 }"
                  @click="openMessage(message)"
                >
                  <div class="message-head">
                    <el-tag :type="messageTagType(message.type)" size="small">{{ typeLabel(message.type) }}</el-tag>
                    <p class="notice-title">{{ message.title }}</p>
                    <el-button class="message-delete" link @click.stop="handleRemoveMessage(message)">
                      <el-icon><component :is="icons.Close" /></el-icon>
                    </el-button>
                  </div>
                  <p v-if="message.content" class="message-content">{{ message.content }}</p>
                  <p class="notice-time">{{ message.createTime }}</p>
                </div>
              </div>
            </div>
          </el-popover>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="28" :src="assetUrl(userStore.userInfo?.avatar)">
                <el-icon><component :is="icons.User" /></el-icon>
              </el-avatar>
              <span>{{ userStore.userInfo?.realName || t('header.user') }}</span>
              <el-icon><component :is="icons.ArrowDown" /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">{{ t('header.profile') }}</el-dropdown-item>
                <el-dropdown-item command="logout">{{ t('header.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <TagsView @refresh="refreshView" />
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="viewKey" />
        </router-view>
      </div>
    </main>

    <el-dialog v-model="messageDialogVisible" :title="currentMessage.title" width="600px">
      <div class="notice-detail">
        <p class="notice-detail-time">{{ currentMessage.createTime || '—' }}</p>
        <p class="notice-detail-content">{{ currentMessage.content || t('header.noContent') }}</p>
      </div>
      <template #footer>
        <el-button @click="messageDialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button v-if="currentMessage.link" type="primary" @click="goToMessageLink">
          {{ t('header.goTo') }}
        </el-button>
      </template>
    </el-dialog>

    <Watermark v-if="siteStore.config.watermark" :text="watermarkText" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMenuStore } from '@/stores/menu'
import { useMessageStore } from '@/stores/message'
import { assetUrl } from '@/utils/asset'
import { localizedTitle } from '@/utils/title'
import TagsView from './components/TagsView.vue'
import Watermark from './components/Watermark.vue'
import { useSiteStore } from '@/stores/site'
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
  Timer,
  FolderOpened,
  Setting,
  Tickets,
  DataLine,
  ChatDotRound,
  Fold, 
  Expand, 
  ArrowDown,
  Moon,
  Sunny,
  Close
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const menuStore = useMenuStore()
const siteStore = useSiteStore()
const messageStore = useMessageStore()
const { t } = useI18n()

// 折叠时用品牌名首字作为图标
const logoLetter = computed(() => (siteStore.config.siteTitle || '后台').slice(0, 1))

// 水印内容带上登录人和日期，便于截图追溯
const watermarkText = computed(() => {
  const info = userStore.userInfo || {}
  const name = [info.username, info.realName].filter(Boolean).join(' ')
  const now = new Date()
  const day = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
  return name ? `${name} ${day}` : day
})

const sidebarCollapsed = ref(false)

// 标签页“刷新当前”通过更换 key 强制重建页面组件
const viewKey = ref(0)

function refreshView() {
  viewKey.value += 1
}

const messagePopoverVisible = ref(false)
const messageDialogVisible = ref(false)
const currentMessage = ref({})

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
  Timer,
  FolderOpened,
  Setting,
  Tickets,
  DataLine,
  ChatDotRound,
  Fold,
  Expand,
  ArrowDown,
  Moon,
  Sunny,
  Close
}

const activeMenu = computed(() => route.path)

const currentPageTitle = computed(() => localizedTitle(route.meta, siteStore.locale))

// 语言切换后同步浏览器标题（afterEach 只在路由变化时触发）
watch(() => siteStore.locale, () => {
  const siteTitle = siteStore.config.siteTitle || '后台管理系统'
  const pageTitle = currentPageTitle.value
  document.title = pageTitle ? `${pageTitle} - ${siteTitle}` : siteTitle
})

// 刷新页面后 pinia 状态丢失，用 token 重新拉一次用户信息
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.getUserInfo()
  }
  if (userStore.token) {
    messageStore.setKickoutHandler(handleKickout)
    messageStore.start()
  }
})

onBeforeUnmount(() => {
  messageStore.stop()
})

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    messageStore.stop()
    await userStore.logout()
    resetDynamicRoutes()
    ElMessage.success(t('header.logoutSuccess'))
    router.push('/login')
  }
}

// 点击消息即视为已读，并弹出内容详情
async function openMessage(message) {
  currentMessage.value = message
  messageDialogVisible.value = true
  messagePopoverVisible.value = false
  await messageStore.markRead(message)
}

async function handleMarkAllRead() {
  await messageStore.markAllRead()
  ElMessage.success(t('header.allReadSuccess'))
}

async function handleRemoveMessage(message) {
  await messageStore.remove(message)
}

function goToMessageLink() {
  const link = currentMessage.value.link
  messageDialogVisible.value = false
  if (link) {
    router.push(link)
  }
}

const MESSAGE_TYPE_LABELS = {
  notice: 'header.messageType.notice',
  job: 'header.messageType.job',
  kick: 'header.messageType.kick',
  system: 'header.messageType.system'
}

const MESSAGE_TAG_TYPES = {
  notice: 'primary',
  job: 'warning',
  kick: 'danger',
  system: 'info'
}

function typeLabel(type) {
  return t(MESSAGE_TYPE_LABELS[type] || 'header.messageType.system')
}

function messageTagType(type) {
  return MESSAGE_TAG_TYPES[type] || 'info'
}

// 被强制下线：消息通道已由 store 关闭，这里清理本地登录态并提示
function handleKickout() {
  resetDynamicRoutes()
  userStore.clearAuth()
  ElMessageBox.alert(t('header.kicked'), t('common.confirmTitle'), { type: 'warning' })
    .finally(() => router.push('/login'))
}

// 顶栏语言切换：与设置页共用 siteStore，即时生效并写入本地偏好
function handleLocaleCommand(locale) {
  siteStore.setLocale(locale)
}

// 退出时移除动态注册的路由，避免下一个登录用户还能访问上一个用户的页面
function resetDynamicRoutes() {
  collectLeafPaths(menuStore.menus).forEach((path) => {
    if (router.hasRoute(path)) {
      router.removeRoute(path)
    }
  })
  menuStore.reset()
}

// 只有叶子菜单注册了路由，目录只是侧边栏分组
function collectLeafPaths(menus, paths = []) {
  menus.forEach((menu) => {
    if (menu.children && menu.children.length) {
      collectLeafPaths(menu.children, paths)
    } else {
      paths.push(menu.path)
    }
  })
  return paths
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
  background: var(--app-sidebar-bg);
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
  background: var(--app-sidebar-logo-bg);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.logo-image {
  width: 28px;
  height: 28px;
  margin-right: 8px;
  object-fit: contain;
  border-radius: 4px;
}

.sidebar-logo h2 {
  max-width: 150px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
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
  background: var(--app-bg-container);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--app-border);
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
  background: var(--app-hover-bg);
  cursor: pointer;
}

.toggle-btn:hover {
  background: var(--app-hover-bg-strong);
}

.breadcrumb {
  font-size: 14px;
  color: var(--app-text-regular);
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
  background: var(--app-hover-bg);
}

.notice-btn:hover {
  background: var(--app-hover-bg-strong);
}

.locale-btn {
  width: auto;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 600;
}

.notice-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--app-border);
  font-weight: 600;
  color: var(--app-text-primary);
}

.notice-count {
  font-size: 12px;
  font-weight: 400;
  color: var(--app-text-secondary);
}

.notice-empty {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.notice-list {
  max-height: 320px;
  overflow-y: auto;
}

.notice-item {
  padding: 10px 4px;
  border-bottom: 1px solid var(--app-border-light);
  cursor: pointer;
}

.notice-item:hover {
  background: var(--app-hover-bg);
}

.notice-title {
  font-size: 14px;
  color: var(--app-text-primary);
  margin-bottom: 4px;
}

.message-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-head .notice-title {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-item.unread .notice-title {
  font-weight: 600;
}

.message-content {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-delete {
  visibility: hidden;
  padding: 0;
  height: auto;
  color: var(--app-text-secondary);
}

.notice-item:hover .message-delete {
  visibility: visible;
}

.notice-time {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.notice-detail-time {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 12px;
}

.notice-detail-content {
  font-size: 14px;
  color: var(--app-text-primary);
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
  background: var(--app-hover-bg);
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  background: var(--app-bg-page);
}
</style>