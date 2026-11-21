<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">通知公告</h2>
      <el-button v-permission="'notice:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        新增公告
      </el-button>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="公告标题"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="全部状态"
          clearable
          class="status-select"
        >
          <el-option
            v-for="item in noticeStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="noticeList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <el-tag v-if="scope.row.pinned === 1" type="danger" size="small" class="pin-tag">置顶</el-tag>
            {{ scope.row.title }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ dictLabel(noticeStatusOptions, scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已读" width="80">
          <template #default="scope">{{ scope.row.readCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180">
          <template #default="scope">
            {{ scope.row.publishTime || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="scope">
            <el-button v-if="canEdit" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-if="canEdit" size="small" @click="togglePin(scope.row)">
              {{ scope.row.pinned === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button size="small" @click="openReadDrawer(scope.row)">已读</el-button>
            <el-button
              v-if="canEdit"
              size="small"
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '下线' : '发布' }}
            </el-button>
            <el-button v-if="canEdit" size="small" type="danger" @click="deleteNotice(scope.row)">删除</el-button>
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
      width="600px"
    >
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="formRules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="noticeForm.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="noticeForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="noticeForm.pinned" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNotice">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="readVisible"
      :title="readNotice ? `阅读情况 - ${readNotice.title}` : '阅读情况'"
      size="620px"
    >
      <div class="read-summary">
        已读 <strong>{{ readStats.readCount }}</strong> 人 ·
        未读 <strong>{{ readStats.unreadCount }}</strong> 人 ·
        共 {{ readStats.totalUsers }} 人
      </div>
      <el-table v-loading="readLoading" :data="readList" border stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="readTime" label="已读时间" />
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
  updateNoticePinnedApi,
  getNoticeReadStatsApi,
  getNoticeReadsApi,
  deleteNoticeApi
} from '@/api/notice'
import { useUserStore } from '@/stores/user'
import { useDictOptions, dictLabel } from '@/utils/dict'

const icons = { Plus }

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('notice:edit'))

// 公告状态由数据字典驱动，可在「数据字典」页维护
const noticeStatusOptions = useDictOptions('notice_status',
  [{ label: '已发布', value: 1 }, { label: '已下线', value: 0 }], Number)

const noticeList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增公告')
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
  pinned: 0
})

const formRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

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
  dialogTitle.value = '新增公告'
  noticeForm.id = null
  noticeForm.title = ''
  noticeForm.content = ''
  noticeForm.pinned = 0
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑公告'
  Object.assign(noticeForm, {
    id: row.id,
    title: row.title,
    content: row.content,
    pinned: row.pinned || 0
  })
  dialogVisible.value = true
}

async function saveNotice() {
  const valid = await noticeFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: noticeForm.title,
    content: noticeForm.content,
    pinned: noticeForm.pinned
  }

  try {
    if (isEdit.value) {
      await updateNoticeApi(noticeForm.id, payload)
    } else {
      await createNoticeApi(payload)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
}

async function toggleStatus(row) {
  const targetStatus = row.status === 1 ? 0 : 1
  const actionText = targetStatus === 1 ? '发布' : '下线'
  try {
    await ElMessageBox.confirm(`确定要${actionText}公告「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await updateNoticeStatusApi(row.id, targetStatus)
    ElMessage.success(`${actionText}成功`)
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
}

async function togglePin(row) {
  const target = row.pinned === 1 ? 0 : 1
  try {
    await updateNoticePinnedApi(row.id, target)
    ElMessage.success(target === 1 ? '置顶成功' : '已取消置顶')
    loadNotices()
  } catch (error) {
    // 拦截器已提示
  }
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
    await ElMessageBox.confirm(`确定要删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteNoticeApi(row.id)
    ElMessage.success('删除成功')
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
  color: #606266;
}

.read-summary strong {
  color: #303133;
}

.read-pager {
  margin-top: 12px;
  text-align: right;
}

.status-select {
  width: 140px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
