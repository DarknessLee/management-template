<template>
  <div class="dashboard">
    <section class="hero">
      <div class="hero-main">
        <p class="hero-date">{{ dateText }} · {{ weekdayText }}</p>
        <h1 class="hero-title">{{ greeting }}，{{ displayName }}</h1>
        <p class="hero-desc">这是系统今天的运行概况，祝你工作顺利。</p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-value">{{ data.todayLogin }}</span>
            <span class="hero-stat-label">今日登录</span>
          </div>
          <span class="hero-divider"></span>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ data.todayOperations }}</span>
            <span class="hero-stat-label">今日操作</span>
          </div>
          <span class="hero-divider"></span>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ data.unreadNotices }}</span>
            <span class="hero-stat-label">待读公告</span>
          </div>
        </div>
      </div>
      <div class="hero-actions">
        <button v-permission="'users:view'" class="hero-btn" @click="go('/users')">用户管理</button>
        <button v-permission="'notice:view'" class="hero-btn" @click="go('/notices')">通知公告</button>
        <button v-permission="'logs:view'" class="hero-btn" @click="go('/logs')">操作日志</button>
      </div>
      <span class="hero-circle circle-large"></span>
      <span class="hero-circle circle-small"></span>
    </section>

    <section class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon icon-blue">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">用户总数</p>
          <p class="stat-value">{{ data.totalUsers }}</p>
          <p class="stat-extra">启用 {{ data.enabledUsers }} · 禁用 {{ data.disabledUsers }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-green">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">部门数量</p>
          <p class="stat-value">{{ data.totalDepts }}</p>
          <p class="stat-extra">角色 {{ data.totalRoles }} 个</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-orange">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">文件总数</p>
          <p class="stat-value">{{ data.totalFiles }}</p>
          <p class="stat-extra">集中存储与在线预览</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-purple">
          <el-icon><Message /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">已发布公告</p>
          <p class="stat-value">{{ data.publishedNotices }}</p>
          <p class="stat-extra">待读 {{ data.unreadNotices }} 条</p>
        </div>
      </div>
    </section>

    <section class="main-grid">
      <div class="card trend-card">
        <div class="card-header">
          <div>
            <h3 class="card-title">登录趋势</h3>
            <p class="card-subtitle">近 7 天每日登录次数</p>
          </div>
          <span class="chart-legend"><i></i>登录次数</span>
        </div>
        <div class="trend-wrapper">
          <svg class="trend-svg" viewBox="0 0 640 220" role="img" aria-label="近7天登录趋势">
            <defs>
              <linearGradient id="trendArea" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#409EFF" stop-opacity="0.28" />
                <stop offset="100%" stop-color="#409EFF" stop-opacity="0.02" />
              </linearGradient>
            </defs>
            <line
              v-for="line in chart.grid"
              :key="`grid-${line.y}`"
              :x1="chart.padLeft"
              :x2="chart.width - 16"
              :y1="line.y"
              :y2="line.y"
              stroke="#EBEEF5"
              stroke-dasharray="4 4"
            />
            <text
              v-for="line in chart.grid"
              :key="`label-${line.y}`"
              :x="chart.padLeft - 8"
              :y="line.y + 4"
              text-anchor="end"
              class="axis-text"
            >{{ line.value }}</text>
            <path v-if="chart.areaPath" :d="chart.areaPath" fill="url(#trendArea)" />
            <path
              v-if="chart.linePath"
              :d="chart.linePath"
              fill="none"
              stroke="#409EFF"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <g v-for="point in chart.points" :key="point.label">
              <circle :cx="point.x" :cy="point.y" r="4" fill="#fff" stroke="#409EFF" stroke-width="2">
                <title>{{ point.label }}：{{ point.value }} 次</title>
              </circle>
              <text :x="point.x" :y="point.y - 11" text-anchor="middle" class="point-value">
                {{ point.value }}
              </text>
              <text :x="point.x" :y="chart.height - 8" text-anchor="middle" class="axis-text">
                {{ point.label }}
              </text>
            </g>
          </svg>
        </div>
      </div>

      <div class="card overview-card">
        <div class="card-header">
          <div>
            <h3 class="card-title">运行概览</h3>
            <p class="card-subtitle">关键指标一览</p>
          </div>
        </div>
        <ul class="overview-list">
          <li>
            <span class="overview-dot dot-blue"></span>
            <span class="overview-name">定时任务</span>
            <span class="overview-value">{{ data.enabledJobs }} / {{ data.totalJobs }} 启用</span>
          </li>
          <li>
            <span class="overview-dot dot-red"></span>
            <span class="overview-name">近 7 天失败操作</span>
            <span class="overview-value" :class="{ danger: data.failedOperations > 0 }">
              {{ data.failedOperations }} 次
            </span>
          </li>
          <li>
            <span class="overview-dot dot-green"></span>
            <span class="overview-name">平均在线时长</span>
            <span class="overview-value">{{ data.avgLoginTime }}</span>
          </li>
          <li>
            <span class="overview-dot dot-orange"></span>
            <span class="overview-name">今日操作</span>
            <span class="overview-value">{{ data.todayOperations }} 次</span>
          </li>
          <li>
            <span class="overview-dot dot-purple"></span>
            <span class="overview-name">公告待读</span>
            <span class="overview-value">{{ data.unreadNotices }} 条</span>
          </li>
        </ul>
      </div>
    </section>

    <section class="bottom-grid">
      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">操作模块分布</h3>
            <p class="card-subtitle">近 7 天各模块操作次数</p>
          </div>
        </div>
        <div v-if="data.operationModules.length" class="module-list">
          <div v-for="(item, index) in data.operationModules" :key="item.name" class="module-row">
            <span class="module-name">{{ item.name }}</span>
            <div class="module-track">
              <div
                class="module-fill"
                :style="{ width: modulePercent(item.value) + '%', background: moduleColors[index % moduleColors.length] }"
              ></div>
            </div>
            <span class="module-value">{{ item.value }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无操作数据" :image-size="72" />
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">最近操作</h3>
            <p class="card-subtitle">最新 5 条操作记录</p>
          </div>
        </div>
        <ul v-if="data.recentOperations.length" class="operation-list">
          <li v-for="(item, index) in data.recentOperations" :key="index" class="operation-item">
            <span class="operation-dot" :class="item.success === 1 ? 'is-success' : 'is-failed'"></span>
            <div class="operation-main">
              <p class="operation-title">{{ item.username }} · {{ item.action }}</p>
              <p class="operation-meta">
                {{ item.module }} · {{ formatTime(item.createTime) }} · {{ item.durationMs }}ms
              </p>
            </div>
            <el-tag :type="item.success === 1 ? 'success' : 'danger'" size="small">
              {{ item.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </li>
        </ul>
        <el-empty v-else description="暂无操作记录" :image-size="72" />
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">最新公告</h3>
            <p class="card-subtitle">已发布的公告</p>
          </div>
        </div>
        <ul v-if="data.latestNotices.length" class="notice-list">
          <li
            v-for="notice in data.latestNotices"
            :key="notice.id"
            class="notice-item"
            @click="openNotice(notice)"
          >
            <div class="notice-line">
              <el-tag v-if="notice.pinned === 1" type="danger" size="small" class="notice-pin">置顶</el-tag>
              <span class="notice-title">{{ notice.title }}</span>
            </div>
            <span class="notice-time">{{ formatNoticeTime(notice.publishTime) }}</span>
          </li>
        </ul>
        <el-empty v-else description="暂无公告" :image-size="72" />
      </div>
    </section>

    <el-dialog v-model="noticeVisible" :title="currentNotice.title" width="560px">
      <p class="notice-dialog-meta">{{ currentNotice.publishTime }}</p>
      <div class="notice-content">{{ currentNotice.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, OfficeBuilding, FolderOpened, Message } from '@element-plus/icons-vue'
import { getDashboardApi } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const data = ref({
  totalUsers: 0,
  enabledUsers: 0,
  disabledUsers: 0,
  totalRoles: 0,
  totalDepts: 0,
  totalFiles: 0,
  publishedNotices: 0,
  unreadNotices: 0,
  totalJobs: 0,
  enabledJobs: 0,
  todayLogin: 0,
  todayOperations: 0,
  failedOperations: 0,
  avgLoginTime: '0分钟',
  loginTrend: [],
  operationModules: [],
  recentOperations: [],
  latestNotices: []
})

const noticeVisible = ref(false)
const currentNotice = ref({ title: '', content: '', publishTime: '' })

const moduleColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9254DE', '#36CFC9']

const displayName = computed(() => {
  const info = userStore.userInfo || {}
  return info.realName || info.username || '管理员'
})

const now = new Date()
const dateText = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
const weekdayText = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.getDay()]

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const chart = computed(() => {
  const list = data.value.loginTrend || []
  const width = 640
  const height = 220
  const padLeft = 36
  const innerWidth = width - padLeft - 16
  const top = 24
  const innerHeight = 150
  const max = Math.max(...list.map(item => item.value), 4)
  const stepX = list.length > 1 ? innerWidth / (list.length - 1) : innerWidth

  const points = list.map((item, index) => ({
    label: item.label,
    value: item.value,
    x: padLeft + index * stepX,
    y: top + innerHeight - (item.value / max) * innerHeight
  }))
  const linePath = points
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)},${point.y.toFixed(1)}`)
    .join(' ')
  const areaPath = points.length
    ? `${linePath} L${points[points.length - 1].x.toFixed(1)},${top + innerHeight} L${points[0].x.toFixed(1)},${top + innerHeight} Z`
    : ''
  const grid = [0, 1, 2, 3].map(index => ({
    y: top + (innerHeight / 3) * index,
    value: Math.round(max - (max / 3) * index)
  }))

  return { width, height, padLeft, points, linePath, areaPath, grid }
})

const maxModuleValue = computed(() =>
  Math.max(...data.value.operationModules.map(item => item.value), 1)
)

function modulePercent(value) {
  return Math.max(6, Math.round((value / maxModuleValue.value) * 100))
}

function formatTime(value) {
  if (!value) return '-'
  const [date, time] = value.split(' ')
  if (!time) return value
  const today = new Date()
  const todayText = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  return date === todayText ? time.slice(0, 5) : value.slice(5, 16)
}

function formatNoticeTime(value) {
  return value ? value.slice(5, 16) : ''
}

function go(path) {
  router.push(path)
}

function openNotice(notice) {
  currentNotice.value = notice
  noticeVisible.value = true
}

onMounted(() => {
  loadDashboard()
})

async function loadDashboard() {
  try {
    const res = await getDashboardApi()
    data.value = { ...data.value, ...res.data }
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  padding: 28px 32px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(120deg, #2f7cf6 0%, #5b8ff9 55%, #6fb1fc 100%);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.25);
}

.hero-main {
  position: relative;
  z-index: 1;
}

.hero-date {
  font-size: 13px;
  opacity: 0.82;
  margin-bottom: 8px;
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 5px;
}

.hero-desc {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 18px;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hero-stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.hero-stat-label {
  font-size: 12px;
  opacity: 0.8;
}

.hero-divider {
  width: 1px;
  height: 28px;
  background: rgba(255, 255, 255, 0.35);
}

.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hero-btn:hover {
  background: rgba(255, 255, 255, 0.24);
}

.hero-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-large {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -90px;
}

.circle-small {
  width: 120px;
  height: 120px;
  right: 120px;
  bottom: -70px;
  background: rgba(255, 255, 255, 0.08);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 24px;
}

.icon-blue {
  background: #e8f4fd;
  color: #409eff;
}

.icon-green {
  background: #e8f8eb;
  color: #67c23a;
}

.icon-orange {
  background: #fff7e6;
  color: #e6a23c;
}

.icon-purple {
  background: #f5f0ff;
  color: #9254de;
}

.stat-body {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-extra {
  margin-top: 4px;
  font-size: 12px;
  color: #a8abb2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 16px;
}

.bottom-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.card {
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #a8abb2;
}

.chart-legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.chart-legend i {
  width: 12px;
  height: 3px;
  border-radius: 2px;
  background: #409eff;
}

.trend-wrapper {
  width: 100%;
}

.trend-svg {
  width: 100%;
  height: 230px;
  display: block;
}

.axis-text {
  font-size: 11px;
  fill: #a8abb2;
}

.point-value {
  font-size: 11px;
  fill: #409eff;
}

.overview-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overview-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.overview-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-blue {
  background: #409eff;
}

.dot-green {
  background: #67c23a;
}

.dot-orange {
  background: #e6a23c;
}

.dot-red {
  background: #f56c6c;
}

.dot-purple {
  background: #9254de;
}

.overview-name {
  color: #606266;
}

.overview-value {
  margin-left: auto;
  color: #303133;
  font-weight: 600;
}

.overview-value.danger {
  color: #f56c6c;
}

.module-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.module-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.module-name {
  width: 72px;
  flex-shrink: 0;
  color: #606266;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.module-track {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: #f0f2f5;
  overflow: hidden;
}

.module-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.module-value {
  width: 32px;
  text-align: right;
  color: #303133;
  font-weight: 600;
}

.operation-list,
.notice-list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.operation-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f2f3f5;
}

.operation-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.operation-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}

.operation-dot.is-success {
  background: #67c23a;
}

.operation-dot.is-failed {
  background: #f56c6c;
}

.operation-main {
  flex: 1;
  min-width: 0;
}

.operation-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.operation-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #a8abb2;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px solid #f2f3f5;
  cursor: pointer;
}

.notice-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.notice-item:hover .notice-title {
  color: #409eff;
}

.notice-line {
  display: flex;
  align-items: center;
  min-width: 0;
}

.notice-pin {
  margin-right: 6px;
  flex-shrink: 0;
}

.notice-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  transition: color 0.2s ease;
}

.notice-time {
  font-size: 12px;
  color: #a8abb2;
  flex-shrink: 0;
}

.notice-dialog-meta {
  font-size: 12px;
  color: #a8abb2;
  margin-bottom: 12px;
}

.notice-content {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bottom-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .main-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    padding: 22px;
  }

  .hero-title {
    font-size: 22px;
  }
}
</style>
