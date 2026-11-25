<template>
  <div class="dashboard">
    <section class="hero">
      <div class="hero-main">
        <p class="hero-date">{{ dateText }} · {{ weekdayText }}</p>
        <h1 class="hero-title">{{ greetingText }}</h1>
        <p class="hero-desc">{{ t('dashboard.heroDesc') }}</p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-value">{{ data.todayLogin }}</span>
            <span class="hero-stat-label">{{ t('dashboard.todayLogin') }}</span>
          </div>
          <span class="hero-divider"></span>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ data.todayOperations }}</span>
            <span class="hero-stat-label">{{ t('dashboard.todayOperations') }}</span>
          </div>
          <span class="hero-divider"></span>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ data.unreadNotices }}</span>
            <span class="hero-stat-label">{{ t('dashboard.unreadNotices') }}</span>
          </div>
        </div>
      </div>
      <div class="hero-actions">
        <button v-permission="'users:view'" class="hero-btn" @click="go('/users')">{{ t('dashboard.navUsers') }}</button>
        <button v-permission="'notice:view'" class="hero-btn" @click="go('/notices')">{{ t('dashboard.navNotices') }}</button>
        <button v-permission="'logs:view'" class="hero-btn" @click="go('/logs')">{{ t('dashboard.navLogs') }}</button>
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
          <p class="stat-label">{{ t('dashboard.totalUsers') }}</p>
          <p class="stat-value">{{ data.totalUsers }}</p>
          <p class="stat-extra">{{ t('dashboard.enabledDisabled', { enabled: data.enabledUsers, disabled: data.disabledUsers }) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-green">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">{{ t('dashboard.totalDepts') }}</p>
          <p class="stat-value">{{ data.totalDepts }}</p>
          <p class="stat-extra">{{ t('dashboard.totalRoles', { count: data.totalRoles }) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-orange">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">{{ t('dashboard.totalFiles') }}</p>
          <p class="stat-value">{{ data.totalFiles }}</p>
          <p class="stat-extra">{{ t('dashboard.fileDesc') }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon icon-purple">
          <el-icon><Message /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">{{ t('dashboard.publishedNotices') }}</p>
          <p class="stat-value">{{ data.publishedNotices }}</p>
          <p class="stat-extra">{{ t('dashboard.unreadCount', { count: data.unreadNotices }) }}</p>
        </div>
      </div>
    </section>

    <section class="main-grid">
      <div class="card trend-card">
        <div class="card-header">
          <div>
            <h3 class="card-title">{{ t('dashboard.loginTrend') }}</h3>
            <p class="card-subtitle">{{ t('dashboard.loginTrendSubtitle') }}</p>
          </div>
          <span class="chart-legend"><i></i>{{ t('dashboard.loginCount') }}</span>
        </div>
        <div class="trend-wrapper">
          <svg class="trend-svg" viewBox="0 0 640 220" role="img" :aria-label="t('dashboard.chartAria')">
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
              stroke="var(--app-border)"
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
              <circle :cx="point.x" :cy="point.y" r="4" fill="var(--app-bg-container)" stroke="#409EFF" stroke-width="2">
                <title>{{ t('dashboard.pointTitle', { label: point.label, value: point.value }) }}</title>
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
            <h3 class="card-title">{{ t('dashboard.overview') }}</h3>
            <p class="card-subtitle">{{ t('dashboard.overviewSubtitle') }}</p>
          </div>
        </div>
        <ul class="overview-list">
          <li>
            <span class="overview-dot dot-blue"></span>
            <span class="overview-name">{{ t('dashboard.jobs') }}</span>
            <span class="overview-value">{{ t('dashboard.jobsEnabled', { enabled: data.enabledJobs, total: data.totalJobs }) }}</span>
          </li>
          <li>
            <span class="overview-dot dot-red"></span>
            <span class="overview-name">{{ t('dashboard.failedOperations') }}</span>
            <span class="overview-value" :class="{ danger: data.failedOperations > 0 }">
              {{ t('dashboard.times', { count: data.failedOperations }) }}
            </span>
          </li>
          <li>
            <span class="overview-dot dot-green"></span>
            <span class="overview-name">{{ t('dashboard.avgLoginTime') }}</span>
            <span class="overview-value">{{ data.avgLoginTime }}</span>
          </li>
          <li>
            <span class="overview-dot dot-orange"></span>
            <span class="overview-name">{{ t('dashboard.todayOperations') }}</span>
            <span class="overview-value">{{ t('dashboard.times', { count: data.todayOperations }) }}</span>
          </li>
          <li>
            <span class="overview-dot dot-purple"></span>
            <span class="overview-name">{{ t('dashboard.noticeUnread') }}</span>
            <span class="overview-value">{{ t('dashboard.items', { count: data.unreadNotices }) }}</span>
          </li>
        </ul>
      </div>
    </section>

    <section class="chart-grid">
      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">{{ t('dashboard.operationTrend') }}</h3>
            <p class="card-subtitle">{{ t('dashboard.operationTrendSubtitle') }}</p>
          </div>
          <div class="chart-legend-group">
            <span class="chart-legend"><i class="legend-success"></i>{{ t('dashboard.success') }}</span>
            <span class="chart-legend"><i class="legend-failed"></i>{{ t('dashboard.failed') }}</span>
          </div>
        </div>
        <div class="trend-wrapper">
          <svg class="trend-svg" viewBox="0 0 640 220" role="img" :aria-label="t('dashboard.operationAria')">
            <line
              v-for="line in operationChart.grid"
              :key="`op-grid-${line.y}`"
              :x1="operationChart.padLeft"
              :x2="operationChart.width - 16"
              :y1="line.y"
              :y2="line.y"
              stroke="var(--app-border)"
              stroke-dasharray="4 4"
            />
            <text
              v-for="line in operationChart.grid"
              :key="`op-label-${line.y}`"
              :x="operationChart.padLeft - 8"
              :y="line.y + 4"
              text-anchor="end"
              class="axis-text"
            >{{ line.value }}</text>
            <g v-for="bar in operationChart.bars" :key="bar.key">
              <rect
                :x="bar.x"
                :y="bar.successY"
                :width="operationChart.barWidth"
                :height="bar.successHeight"
                rx="3"
                fill="#67C23A"
              >
                <title>{{ t('dashboard.operationBarTitle', { label: bar.label, success: bar.success, failed: bar.failed }) }}</title>
              </rect>
              <rect
                :x="bar.x"
                :y="bar.y"
                :width="operationChart.barWidth"
                :height="bar.failedHeight"
                rx="3"
                fill="#F56C6C"
              >
                <title>{{ t('dashboard.operationBarTitle', { label: bar.label, success: bar.success, failed: bar.failed }) }}</title>
              </rect>
              <text :x="bar.x + operationChart.barWidth / 2" :y="bar.y - 6" text-anchor="middle" class="point-value">
                {{ bar.total }}
              </text>
              <text :x="bar.x + operationChart.barWidth / 2" :y="operationChart.height - 8" text-anchor="middle" class="axis-text">
                {{ bar.label }}
              </text>
            </g>
          </svg>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">{{ t('dashboard.roleDistribution') }}</h3>
            <p class="card-subtitle">{{ t('dashboard.roleSubtitle') }}</p>
          </div>
        </div>
        <div v-if="data.roleDistribution.length" class="donut-wrapper">
          <svg class="donut-svg" viewBox="0 0 160 160" role="img" :aria-label="t('dashboard.roleAria')">
            <circle
              cx="80"
              cy="80"
              :r="roleChart.radius"
              fill="none"
              stroke="var(--app-hover-bg)"
              :stroke-width="roleChart.stroke"
            />
            <circle
              v-for="segment in roleChart.segments"
              :key="segment.name"
              cx="80"
              cy="80"
              :r="roleChart.radius"
              fill="none"
              :stroke="segment.color"
              :stroke-width="roleChart.stroke"
              :stroke-dasharray="segment.dasharray"
              :stroke-dashoffset="segment.dashoffset"
              transform="rotate(-90 80 80)"
            >
              <title>{{ t('dashboard.roleTooltip', { name: segment.name, value: segment.value, percent: segment.percent }) }}</title>
            </circle>
            <text x="80" y="76" text-anchor="middle" class="donut-total">{{ roleChart.total }}</text>
            <text x="80" y="94" text-anchor="middle" class="donut-label">{{ t('dashboard.totalUsers') }}</text>
          </svg>
          <ul class="donut-legend">
            <li v-for="segment in roleChart.segments" :key="`legend-${segment.name}`">
              <span class="legend-dot" :style="{ background: segment.color }"></span>
              <span class="legend-name">{{ segment.name }}</span>
              <span class="legend-value">{{ segment.value }}</span>
              <span class="legend-percent">{{ segment.percent }}%</span>
            </li>
          </ul>
        </div>
        <el-empty v-else :description="t('dashboard.noRoleData')" :image-size="72" />
      </div>
    </section>

    <section class="bottom-grid">
      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">{{ t('dashboard.moduleDistribution') }}</h3>
            <p class="card-subtitle">{{ t('dashboard.moduleSubtitle') }}</p>
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
        <el-empty v-else :description="t('dashboard.noOperationData')" :image-size="72" />
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">{{ t('dashboard.recentOperations') }}</h3>
            <p class="card-subtitle">{{ t('dashboard.recentSubtitle') }}</p>
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
              {{ item.success === 1 ? t('dashboard.success') : t('dashboard.failed') }}
            </el-tag>
          </li>
        </ul>
        <el-empty v-else :description="t('dashboard.noOperationRecords')" :image-size="72" />
      </div>

      <div class="card">
        <div class="card-header">
          <div>
            <h3 class="card-title">{{ t('dashboard.latestNotices') }}</h3>
            <p class="card-subtitle">{{ t('dashboard.latestNoticesSubtitle') }}</p>
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
              <el-tag v-if="notice.pinned === 1" type="danger" size="small" class="notice-pin">{{ t('dashboard.pinned') }}</el-tag>
              <span class="notice-title">{{ notice.title }}</span>
            </div>
            <span class="notice-time">{{ formatNoticeTime(notice.publishTime) }}</span>
          </li>
        </ul>
        <el-empty v-else :description="t('dashboard.noNotices')" :image-size="72" />
      </div>
    </section>

    <el-dialog v-model="noticeVisible" :title="currentNotice.title" width="560px">
      <p class="notice-dialog-meta">{{ currentNotice.publishTime }}</p>
      <div class="notice-content" v-html="currentNotice.content"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, OfficeBuilding, FolderOpened, Message } from '@element-plus/icons-vue'
import { getDashboardApi } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()
const { t } = useI18n()

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
  avgLoginTime: '—',
  loginTrend: [],
  operationTrend: [],
  operationModules: [],
  roleDistribution: [],
  recentOperations: [],
  latestNotices: []
})

const noticeVisible = ref(false)
const currentNotice = ref({ title: '', content: '', publishTime: '' })

const moduleColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9254DE', '#36CFC9']
const displayName = computed(() => {
  const info = userStore.userInfo || {}
  return info.realName || info.username || t('dashboard.admin')
})

const dateText = computed(() => {
  const now = new Date()
  if (siteStore.locale === 'en') {
    return now.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
  }
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

const weekdayText = computed(() =>
  new Date().toLocaleDateString(siteStore.locale === 'en' ? 'en-US' : 'zh-CN', { weekday: 'short' })
)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return t('dashboard.greetingDawn')
  if (hour < 12) return t('dashboard.greetingMorning')
  if (hour < 14) return t('dashboard.greetingNoon')
  if (hour < 18) return t('dashboard.greetingAfternoon')
  return t('dashboard.greetingEvening')
})

const greetingText = computed(() =>
  `${greeting.value}${siteStore.locale === 'en' ? ', ' : '，'}${displayName.value}`
)

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

const operationChart = computed(() => {
  const list = data.value.operationTrend || []
  const width = 640
  const height = 220
  const padLeft = 36
  const innerWidth = width - padLeft - 16
  const top = 24
  const innerHeight = 150
  const max = Math.max(...list.map(item => item.success + item.failed), 4)
  const band = list.length ? innerWidth / list.length : innerWidth
  const barWidth = Math.min(30, band * 0.45)
  const baseline = top + innerHeight

  const bars = list.map((item, index) => {
    const total = item.success + item.failed
    const totalHeight = (total / max) * innerHeight
    const successHeight = total ? (item.success / total) * totalHeight : 0
    return {
      key: item.label,
      label: item.label,
      total,
      success: item.success,
      failed: item.failed,
      x: padLeft + band * index + (band - barWidth) / 2,
      y: baseline - totalHeight,
      successY: baseline - successHeight,
      successHeight,
      failedHeight: totalHeight - successHeight
    }
  })
  const grid = [0, 1, 2, 3].map(index => ({
    y: top + (innerHeight / 3) * index,
    value: Math.round(max - (max / 3) * index)
  }))

  return { width, height, padLeft, bars, grid, barWidth }
})

const roleChart = computed(() => {
  const list = data.value.roleDistribution || []
  const total = list.reduce((sum, item) => sum + item.value, 0)
  const radius = 52
  const stroke = 18
  const circumference = 2 * Math.PI * radius
  let consumed = 0

  const segments = list.map((item, index) => {
    const ratio = total ? item.value / total : 0
    const segment = {
      name: item.name,
      value: item.value,
      percent: total ? Math.round(ratio * 1000) / 10 : 0,
      color: moduleColors[index % moduleColors.length],
      dasharray: `${(ratio * circumference).toFixed(2)} ${circumference.toFixed(2)}`,
      dashoffset: (-consumed * circumference).toFixed(2)
    }
    consumed += ratio
    return segment
  })

  return { total, radius, stroke, segments }
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
  background: var(--app-hero-bg);
  box-shadow: var(--app-hero-shadow);
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
  background: var(--app-bg-container);
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
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
}

.icon-green {
  background: rgba(103, 194, 58, 0.12);
  color: #67c23a;
}

.icon-orange {
  background: rgba(230, 162, 60, 0.12);
  color: #e6a23c;
}

.icon-purple {
  background: rgba(146, 84, 222, 0.12);
  color: #9254de;
}

.stat-body {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--app-text-primary);
  line-height: 1.2;
}

.stat-extra {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-placeholder);
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
  background: var(--app-bg-container);
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
  color: var(--app-text-primary);
}

.card-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-placeholder);
}

.chart-legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.chart-legend i {
  width: 12px;
  height: 3px;
  border-radius: 2px;
  background: #409eff;
}

.chart-legend-group {
  display: flex;
  gap: 14px;
}

.chart-legend i.legend-success {
  background: #67c23a;
}

.chart-legend i.legend-failed {
  background: #f56c6c;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 16px;
}

.donut-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  flex-wrap: wrap;
  min-height: 200px;
}

.donut-svg {
  width: 168px;
  height: 168px;
  flex-shrink: 0;
}

.donut-total {
  font-size: 22px;
  font-weight: 700;
  fill: var(--app-text-primary);
}

.donut-label {
  font-size: 11px;
  fill: var(--app-text-placeholder);
}

.donut-legend {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-width: 140px;
}

.donut-legend li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-name {
  flex: 1;
  min-width: 0;
  color: var(--app-text-regular);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.legend-value {
  color: var(--app-text-primary);
  font-weight: 600;
}

.legend-percent {
  width: 44px;
  text-align: right;
  font-size: 12px;
  color: var(--app-text-placeholder);
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
  fill: var(--app-text-placeholder);
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
  color: var(--app-text-regular);
}

.overview-value {
  margin-left: auto;
  color: var(--app-text-primary);
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
  color: var(--app-text-regular);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.module-track {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: var(--app-hover-bg);
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
  color: var(--app-text-primary);
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
  border-bottom: 1px solid var(--app-border-light);
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
  color: var(--app-text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.operation-meta {
  margin-top: 3px;
  font-size: 12px;
  color: var(--app-text-placeholder);
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px solid var(--app-border-light);
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
  color: var(--app-text-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  transition: color 0.2s ease;
}

.notice-time {
  font-size: 12px;
  color: var(--app-text-placeholder);
  flex-shrink: 0;
}

.notice-dialog-meta {
  font-size: 12px;
  color: var(--app-text-placeholder);
  margin-bottom: 12px;
}

.notice-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--app-text-regular);
  white-space: pre-wrap;
}

.notice-content :deep(img) {
  max-width: 100%;
}

.notice-content :deep(a) {
  color: #409eff;
}

.notice-content :deep(blockquote) {
  margin: 8px 0;
  padding-left: 12px;
  border-left: 3px solid var(--app-border);
  color: var(--app-text-secondary);
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
  .chart-grid,
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
