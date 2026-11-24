<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('messages.title') }}</h2>
      <div class="header-actions">
        <el-button :disabled="messageStore.unreadCount === 0" @click="handleMarkAllRead">
          {{ t('messages.markAllRead') }}
        </el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          {{ t('messages.batchDelete') }}
          <span v-if="selectedIds.length">({{ selectedIds.length }})</span>
        </el-button>
      </div>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-select
          v-model="searchForm.type"
          :placeholder="t('messages.allTypes')"
          clearable
          class="filter-select"
        >
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="searchForm.readFlag"
          :placeholder="t('messages.allStatus')"
          clearable
          class="filter-select"
        >
          <el-option
            v-for="item in readOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="messageList"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column :label="t('messages.columns.type')" width="100">
          <template #default="scope">
            <el-tag :type="messageTagType(scope.row.type)" size="small">{{ typeLabel(scope.row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" :label="t('messages.columns.title')" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span :class="{ 'unread-title': scope.row.readFlag === 0 }">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" :label="t('messages.columns.content')" min-width="240" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.content || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('messages.columns.status')" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.readFlag === 1 ? 'info' : 'warning'" size="small">
              {{ scope.row.readFlag === 1 ? t('messages.read') : t('messages.unread') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('messages.columns.createTime')" width="180" />
        <el-table-column :label="t('messages.columns.operation')" width="150" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openDetail(scope.row)">{{ t('messages.view') }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">{{ t('messages.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="currentMessage.title" width="600px">
      <div class="message-detail">
        <p class="message-detail-time">{{ currentMessage.createTime || '—' }}</p>
        <p class="message-detail-content">{{ currentMessage.content || t('header.noContent') }}</p>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button v-if="currentMessage.link" type="primary" @click="goToLink">
          {{ t('header.goTo') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMessagesApi,
  markMessageReadApi,
  markAllMessagesReadApi,
  deleteMessageApi,
  batchDeleteMessagesApi
} from '@/api/message'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const { t } = useI18n()
const messageStore = useMessageStore()

const messageList = ref([])
const loading = ref(false)
const selectedIds = ref([])
const dialogVisible = ref(false)
const currentMessage = ref({})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const searchForm = reactive({
  type: '',
  readFlag: ''
})

const typeOptions = computed(() => [
  { value: 'notice', label: t('header.messageType.notice') },
  { value: 'job', label: t('header.messageType.job') },
  { value: 'kick', label: t('header.messageType.kick') },
  { value: 'system', label: t('header.messageType.system') }
])

const readOptions = computed(() => [
  { value: 0, label: t('messages.unread') },
  { value: 1, label: t('messages.read') }
])

const MESSAGE_TAG_TYPES = {
  notice: 'primary',
  job: 'warning',
  kick: 'danger',
  system: 'info'
}

function typeLabel(type) {
  const labels = {
    notice: 'header.messageType.notice',
    job: 'header.messageType.job',
    kick: 'header.messageType.kick',
    system: 'header.messageType.system'
  }
  return t(labels[type] || labels.system)
}

function messageTagType(type) {
  return MESSAGE_TAG_TYPES[type] || 'info'
}

onMounted(() => {
  loadMessages()
})

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessagesApi({
      page: pagination.page,
      size: pagination.size,
      type: searchForm.type || undefined,
      readFlag: searchForm.readFlag === '' ? undefined : searchForm.readFlag
    })
    messageList.value = res.data.list || []
    pagination.total = res.data.total
    // 删除后当前页可能已空，回退一页再加载
    if (messageList.value.length === 0 && pagination.page > 1) {
      pagination.page -= 1
      await loadMessages()
    }
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadMessages()
}

function resetSearch() {
  searchForm.type = ''
  searchForm.readFlag = ''
  pagination.page = 1
  loadMessages()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadMessages()
}

function handleCurrentChange(page) {
  pagination.page = page
  loadMessages()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map((row) => row.id)
}

// 查看详情时顺带标记已读，并同步顶栏角标
async function openDetail(row) {
  currentMessage.value = row
  dialogVisible.value = true
  if (row.readFlag === 0) {
    try {
      await markMessageReadApi(row.id)
      row.readFlag = 1
      messageStore.load()
    } catch (error) {
      // 拦截器已提示
    }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllMessagesReadApi()
    ElMessage.success(t('header.allReadSuccess'))
    messageList.value.forEach((item) => {
      item.readFlag = 1
    })
    messageStore.load()
  } catch (error) {
    // 拦截器已提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      t('messages.deleteConfirm', { name: row.title }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }
  try {
    await deleteMessageApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadMessages()
    messageStore.load()
  } catch (error) {
    // 拦截器已提示
  }
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) {
    return
  }
  try {
    await ElMessageBox.confirm(
      t('messages.batchDeleteConfirm', { count: selectedIds.value.length }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }
  try {
    await batchDeleteMessagesApi(selectedIds.value)
    ElMessage.success(t('common.deleteSuccess'))
    selectedIds.value = []
    loadMessages()
    messageStore.load()
  } catch (error) {
    // 拦截器已提示
  }
}

function goToLink() {
  const link = currentMessage.value.link
  dialogVisible.value = false
  if (link) {
    router.push(link)
  }
}
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 12px;
}

.filter-select {
  width: 150px;
}

.unread-title {
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.message-detail-time {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 12px;
}

.message-detail-content {
  font-size: 14px;
  color: var(--app-text-primary);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
