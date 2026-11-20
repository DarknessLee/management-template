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
          <el-option label="已发布" :value="1" />
          <el-option label="已下线" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="noticeList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.status === 1 ? '已发布' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180">
          <template #default="scope">
            {{ scope.row.publishTime || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column v-if="canEdit" label="操作" width="240">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button
              size="small"
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '下线' : '发布' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteNotice(scope.row)">删除</el-button>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNotice">确定</el-button>
      </template>
    </el-dialog>
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
  deleteNoticeApi
} from '@/api/notice'
import { useUserStore } from '@/stores/user'

const icons = { Plus }

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('notice:edit'))

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
  content: ''
})

const formRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

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
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  dialogTitle.value = '编辑公告'
  Object.assign(noticeForm, {
    id: row.id,
    title: row.title,
    content: row.content
  })
  dialogVisible.value = true
}

async function saveNotice() {
  const valid = await noticeFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: noticeForm.title,
    content: noticeForm.content
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

.status-select {
  width: 140px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
