<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('notices.title') }}</h2>
      <el-button v-permission="'notice:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        {{ t('notices.add') }}
      </el-button>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          :placeholder="t('notices.searchPlaceholder')"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.status"
          :placeholder="t('notices.allStatus')"
          clearable
          class="status-select"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <el-table :data="noticeList" border stripe>
        <el-table-column prop="id" :label="t('notices.columns.id')" width="80" />
        <el-table-column prop="title" :label="t('notices.columns.title')" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-tag v-if="scope.row.pinned === 1" type="danger" size="small" class="pin-tag">{{ t('notices.pinned') }}</el-tag>
            {{ scope.row.title }}
          </template>
        </el-table-column>
        <el-table-column :label="t('notices.columns.status')" width="100">
          <template #default="scope">
            <el-tag :type="rowStatusType(scope.row)" size="small">
              {{ rowStatusText(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('notices.columns.readCount')" width="80">
          <template #default="scope">{{ scope.row.readCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="publishTime" :label="t('notices.columns.publishTime')" width="180">
          <template #default="scope">
            {{ scope.row.publishTime || '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('notices.columns.scheduledTime')" width="180">
          <template #default="scope">{{ scope.row.scheduledTime || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('notices.columns.expireTime')" width="180">
          <template #default="scope">{{ scope.row.expireTime || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('notices.columns.createTime')" width="180" />
        <el-table-column :label="t('notices.columns.operation')" width="400" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openPreview(scope.row)">{{ t('notices.view') }}</el-button>
            <el-button v-if="canEdit" size="small" @click="openEditDialog(scope.row)">{{ t('notices.edit') }}</el-button>
            <el-button v-if="canEdit" size="small" @click="togglePin(scope.row)">
              {{ scope.row.pinned === 1 ? t('notices.unpin') : t('notices.pinned') }}
            </el-button>
            <el-button size="small" @click="openReadDrawer(scope.row)">{{ t('notices.reads') }}</el-button>
            <el-button
              v-if="canEdit"
              size="small"
              :type="scope.row.status === 1 ? 'warning' : (scope.row.scheduled ? 'info' : 'success')"
              @click="handlePublishAction(scope.row)"
            >
              {{ scope.row.status === 1 ? t('notices.offline') : (scope.row.scheduled ? t('notices.cancelSchedule') : t('notices.publish')) }}
            </el-button>
            <el-button v-if="canEdit" size="small" type="danger" @click="deleteNotice(scope.row)">{{ t('notices.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="760px"
    >
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="formRules" label-width="90px">
        <el-form-item :label="t('notices.form.title')" prop="title">
          <el-input v-model="noticeForm.title" maxlength="100" show-word-limit :placeholder="t('notices.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('notices.form.content')" prop="content">
          <RichTextEditor v-model="noticeForm.content" :placeholder="t('notices.contentPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('notices.form.pinned')">
          <el-switch v-model="noticeForm.pinned" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="t('notices.form.expireTime')">
          <el-date-picker
            v-model="noticeForm.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notices.form.expireTime')"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveNotice">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" :title="t('notices.publishDialogTitle')" width="480px">
      <el-form label-width="110px">
        <el-form-item :label="t('notices.publishTimeLabel')" required>
          <el-date-picker
            v-model="publishForm.scheduledTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notices.publishTimeLabel')"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item :label="t('notices.expireTimeLabel')">
          <el-date-picker
            v-model="publishForm.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notices.expireTimeLabel')"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <p class="publish-tip">{{ t('notices.publishTip') }}</p>
      <template #footer>
        <el-button @click="publishVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="publishing" @click="submitPublish">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewNotice.title" width="640px">
      <p class="preview-meta">{{ previewNotice.publishTime || previewNotice.createTime }}</p>
      <div class="notice-html" v-html="previewNotice.content"></div>
    </el-dialog>

    <el-drawer
      v-model="readVisible"
      :title="readNotice ? t('notices.readDrawerTitle', { name: readNotice.title }) : t('notices.readDrawerTitleEmpty')"
      size="620px"
    >
      <div class="read-summary">
        {{ t('notices.readSummary', { read: readStats.readCount, unread: readStats.unreadCount, total: readStats.totalUsers }) }}
      </div>
      <el-table v-loading="readLoading" :data="readList" border stripe>
        <el-table-column prop="username" :label="t('notices.readColumns.username')" width="140" />
        <el-table-column prop="realName" :label="t('notices.readColumns.realName')" width="120" />
        <el-table-column prop="readTime" :label="t('notices.readColumns.readTime')" />
      </el-table>
      <div class="read-pager">
        <el-pagination
          v-model:current-page="readPage"
          :page-size="readSize"
          :total="readTotal"
          layout="total, prev, pager, next"
          @current-change="loadReads"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getNoticesApi,
  createNoticeApi,
  updateNoticeApi,
  updateNoticeStatusApi,
  scheduleNoticeApi,
  updateNoticePinnedApi,
  getNoticeReadStatsApi,
  getNoticeReadsApi,
  deleteNoticeApi
} from '@/api/notice'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { useI18n } from 'vue-i18n'
import { useDictOptions, dictLabel } from '@/utils/dict'
import RichTextEditor from '@/components/RichTextEditor.vue'

const icons = { Plus }

const userStore = useUserStore()
const siteStore = useSiteStore()
const { t } = useI18n()
const canEdit = computed(() => userStore.permissions.includes('notice:edit'))

// 公告状态由数据字典驱动，可在「数据字典」页维护
const noticeStatusOptions = useDictOptions('notice_status',
  [{ label: '已发布', value: 1 }, { label: '已下线', value: 0 }], Number)

// 状态文案：中文沿用数据字典，英文使用内置词条
function statusText(value) {
  if (siteStore.locale === 'en') {
    return t(value === 1 ? 'notices.statusPublished' : 'notices.statusOffline')
  }
  return dictLabel(noticeStatusOptions, value)
}

// 筛选下拉的选项也跟随语言切换
const statusOptions = computed(() => noticeStatusOptions.value.map(item => ({
  ...item,
  label: statusText(item.value)
})))

// 列表行状态：已定时未到点显示「待发布」
function rowStatusText(row) {
  if (row.scheduled) {
    return t('notices.statusPending')
  }
  return statusText(row.status)
}

function rowStatusType(row) {
  if (row.scheduled) {
    return 'warning'
  }
  return row.status === 1 ? 'success' : 'info'
}

const noticeList = ref([])
const dialogVisible = ref(false)
const dialogTitle = computed(() => isEdit.value ? t('notices.editTitle') : t('notices.addTitle'))
const isEdit = ref(false)
const noticeFormRef = ref(null)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const searchForm = reactive({
  keyword: '',
  status: ''
})

const noticeForm = reactive({
  id: null,
  title: '',
  content: '',
  pinned: 0,
  expireTime: null
})

const publishVisible = ref(false)
const publishing = ref(false)
const publishTarget = ref(null)
const publishForm = reactive({
  scheduledTime: '',
  expireTime: ''
})

const formRules = computed(() => ({
  title: [{ required: true, message: t('notices.titleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('notices.contentRequired'), trigger: 'blur' }]
}))

const previewVisible = ref(false)
const previewNotice = ref({})

const readVisible = ref(false)
const readLoading = ref(false)
const readNotice = ref(null)
const readStats = ref({ readCount: 0, unreadCount: 0, totalUsers: 0 })
const readList = ref([])
const readPage = ref(1)
const readSize = 10
const readTotal = ref(0)

onMounted(() => {
  loadNotices()
})

async function loadNotices() {
  try {
    const res = await getNoticesApi({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status === '' ? undefined : searchForm.status
    })
    noticeList.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    // 拦截器已提示
  }
}

function handleSearch() {
  pagination.page = 1
  loadNotices()
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.status = ''
  pagination.page = 1
  loadNotices()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadNotices()
}

function handleCurrentChange(page) {
  pagination.page = page
  loadNotices()
}

function openAddDialog() {
  isEdit.value = false
  noticeForm.id = null
  noticeForm.title = ''
  noticeForm.content = ''
  noticeForm.pinned = 0
  noticeForm.expireTime = null
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(noticeForm, {
    id: row.id,
    title: row.title,
    content: row.content,
    pinned: row.pinned || 0,
    expireTime: row.expireTime || null
  })
  dialogVisible.value = true
}

async function saveNotice() {
  const valid = await noticeFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: noticeForm.title,
    content: noticeForm.content,
    pinned: noticeForm.pinned,
    expireTime: noticeForm.expireTime || null
  }

  try {
    if (isEdit.value) {
      await updateNoticeApi(noticeForm.id, payload)
    } else {
      await createNoticeApi(payload)
    }
    ElMessage.success(isEdit.value ? t('common.updateSuccess') : t('common.createSuccess'))
    dialogVisible.value = false
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
}

function nowString() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function handlePublishAction(row) {
  if (row.status === 1) {
    offlineNotice(row)
    return
  }
  if (row.scheduled) {
    cancelSchedule(row)
    return
  }
  openPublishDialog(row)
}

async function offlineNotice(row) {
  try {
    await ElMessageBox.confirm(
      t('notices.offlineConfirm', { name: row.title }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }
  try {
    await updateNoticeStatusApi(row.id, 0)
    ElMessage.success(t('notices.offlineSuccess'))
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
}

async function cancelSchedule(row) {
  try {
    await ElMessageBox.confirm(
      t('notices.cancelScheduleConfirm', { name: row.title }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }
  try {
    await updateNoticeStatusApi(row.id, 0)
    ElMessage.success(t('notices.cancelScheduleSuccess'))
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
}

function openPublishDialog(row) {
  publishTarget.value = row
  publishForm.scheduledTime = nowString()
  publishForm.expireTime = row.expireTime || ''
  publishVisible.value = true
}

async function submitPublish() {
  if (!publishForm.scheduledTime) {
    ElMessage.warning(t('notices.scheduleTimeRequired'))
    return
  }
  const future = publishForm.scheduledTime > nowString()
  publishing.value = true
  try {
    await scheduleNoticeApi(publishTarget.value.id, {
      scheduledTime: publishForm.scheduledTime,
      expireTime: publishForm.expireTime || null
    })
    ElMessage.success(t(future ? 'notices.scheduleSuccess' : 'notices.publishSuccess'))
    publishVisible.value = false
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  } finally {
    publishing.value = false
  }
}

async function togglePin(row) {
  const target = row.pinned === 1 ? 0 : 1
  try {
    await updateNoticePinnedApi(row.id, target)
    ElMessage.success(target === 1 ? t('notices.pinSuccess') : t('notices.unpinSuccess'))
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
}

function openPreview(row) {
  previewNotice.value = row
  previewVisible.value = true
}

function openReadDrawer(row) {
  readNotice.value = row
  readPage.value = 1
  readStats.value = { readCount: 0, unreadCount: 0, totalUsers: 0 }
  readVisible.value = true
  loadReadStats()
  loadReads()
}

async function loadReadStats() {
  if (!readNotice.value) return
  try {
    const res = await getNoticeReadStatsApi(readNotice.value.id)
    readStats.value = res.data
  } catch (error) {
    // 拦截器已提示
  }
}

async function loadReads() {
  if (!readNotice.value) return
  readLoading.value = true
  try {
    const res = await getNoticeReadsApi(readNotice.value.id, {
      page: readPage.value,
      size: readSize
    })
    readList.value = res.data.list
    readTotal.value = res.data.total
  } catch (error) {
    readList.value = []
    readTotal.value = 0
  } finally {
    readLoading.value = false
  }
}

async function deleteNotice(row) {
  try {
    await ElMessageBox.confirm(t('notices.deleteConfirm', { name: row.title }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteNoticeApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.keyword-input {
  width: 220px;
}

.pin-tag {
  margin-right: 6px;
}

.read-summary {
  margin-bottom: 12px;
  color: var(--app-text-regular);
}

.read-summary strong {
  color: var(--app-text-primary);
}

.read-pager {
  margin-top: 12px;
  text-align: right;
}

.preview-meta {
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--app-text-placeholder);
}

.notice-html {
  font-size: 14px;
  line-height: 1.8;
  color: var(--app-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.notice-html :deep(img) {
  max-width: 100%;
}

.notice-html :deep(a) {
  color: #409eff;
}

.notice-html :deep(blockquote) {
  margin: 8px 0;
  padding-left: 12px;
  border-left: 3px solid var(--app-border);
  color: var(--app-text-secondary);
}

.status-select {
  width: 140px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.publish-tip {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-secondary);
}
</style>
