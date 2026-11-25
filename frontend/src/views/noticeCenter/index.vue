<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('noticeCenter.title') }}</h2>
      <el-tag v-if="unreadCount > 0" type="warning" effect="dark">
        {{ t('noticeCenter.unreadCount', { count: unreadCount }) }}
      </el-tag>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          clearable
          :placeholder="t('noticeCenter.searchPlaceholder')"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="noticeList" border stripe>
        <el-table-column :label="t('noticeCenter.columns.title')" min-width="320">
          <template #default="scope">
            <div class="notice-title-cell" @click="openDetail(scope.row)">
              <el-tag v-if="scope.row.pinned === 1" size="small" type="danger" effect="plain">
                {{ t('noticeCenter.pinned') }}
              </el-tag>
              <span v-if="scope.row.readFlag === 0" class="unread-dot"></span>
              <span class="notice-title-text" :class="{ unread: scope.row.readFlag === 0 }">
                {{ scope.row.title }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" :label="t('noticeCenter.columns.publishTime')" width="180" />
        <el-table-column :label="t('noticeCenter.columns.status')" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.readFlag === 1 ? 'info' : 'warning'" size="small">
              {{ scope.row.readFlag === 1 ? t('noticeCenter.read') : t('noticeCenter.unread') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('noticeCenter.columns.operation')" width="100" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openDetail(scope.row)">{{ t('notices.view') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="currentNotice.title" width="640px">
      <p class="dialog-meta">{{ currentNotice.publishTime }}</p>
      <div class="notice-html" v-html="currentNotice.content"></div>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getNoticeCenterApi, getNoticeUnreadCountApi, markNoticeReadApi } from '@/api/notice'
import { useMessageStore } from '@/stores/message'

const { t } = useI18n()
const messageStore = useMessageStore()

const noticeList = ref([])
const loading = ref(false)
const keyword = ref('')
const unreadCount = ref(0)
const dialogVisible = ref(false)
const currentNotice = ref({})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  loadNotices()
  loadUnreadCount()
})

async function loadNotices() {
  loading.value = true
  try {
    const res = await getNoticeCenterApi({
      page: pagination.page,
      size: pagination.size,
      keyword: keyword.value || undefined
    })
    noticeList.value = res.data.list || []
    pagination.total = res.data.total
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const res = await getNoticeUnreadCountApi()
    unreadCount.value = res.data || 0
  } catch (error) {
    // 拦截器已提示
  }
}

function handleSearch() {
  pagination.page = 1
  loadNotices()
}

function resetSearch() {
  keyword.value = ''
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

// 打开详情即视为已读，并同步未读数
async function openDetail(row) {
  currentNotice.value = row
  dialogVisible.value = true
  if (row.readFlag === 0) {
    try {
      await markNoticeReadApi(row.id)
      row.readFlag = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      messageStore.load()
    } catch (error) {
      // 拦截器已提示
    }
  }
}
</script>

<style scoped>
.search-input {
  width: 260px;
}

.notice-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}

.notice-title-text {
  color: var(--app-text-regular);
}

.notice-title-text.unread {
  font-weight: 600;
  color: var(--app-text-primary);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.dialog-meta {
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
</style>
