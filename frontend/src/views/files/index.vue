<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('files.title') }}</h2>
      <div class="header-actions">
        <el-button
          v-if="canEdit"
          type="danger"
          plain
          :disabled="!selectedIds.length"
          @click="removeSelected"
        >
          {{ t('files.batchDelete') }}{{ selectedIds.length ? ` (${selectedIds.length})` : '' }}
        </el-button>
        <el-upload
          v-if="canEdit"
          :show-file-list="false"
          :http-request="handleUpload"
          :accept="acceptTypes"
        >
          <el-button type="primary" :loading="uploading">
            <el-icon><component :is="icons.Upload" /></el-icon>
            {{ t('files.upload') }}
          </el-button>
        </el-upload>
      </div>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          :placeholder="t('files.searchPlaceholder')"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="typeFilter"
          :placeholder="t('files.allTypes')"
          clearable
          class="type-select"
          @change="handleSearch"
        >
          <el-option
            v-for="item in typeOptions"
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
        :data="files"
        row-key="id"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column v-if="canEdit" type="selection" width="46" />
        <el-table-column prop="originalName" :label="t('files.columns.name')" min-width="260" show-overflow-tooltip />
        <el-table-column :label="t('files.columns.type')" width="90">
          <template #default="scope">
            <el-tag size="small">{{ fileExtension(scope.row.originalName) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('files.columns.size')" width="110">
          <template #default="scope">{{ formatSize(scope.row.size) }}</template>
        </el-table-column>
        <el-table-column :label="t('files.columns.uploader')" width="120">
          <template #default="scope">{{ scope.row.uploaderName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('files.columns.createTime')" width="180" />
        <el-table-column :label="t('files.columns.operation')" width="220" fixed="right">
          <template #default="scope">
            <el-button v-if="canPreview(scope.row)" size="small" @click="previewFile(scope.row)">{{ t('files.preview') }}</el-button>
            <el-button size="small" @click="downloadFile(scope.row)">{{ t('files.download') }}</el-button>
            <el-button
              v-if="canEdit"
              size="small"
              type="danger"
              @click="removeFile(scope.row)"
            >
              {{ t('files.delete') }}
            </el-button>
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

      <p class="tip">{{ t('files.tip') }}</p>
    </div>

    <el-dialog
      v-model="previewVisible"
      :title="previewName"
      width="820px"
      @closed="closePreview"
    >
      <img
        v-if="previewKind === 'image'"
        :src="previewUrl"
        class="preview-image"
        :alt="t('files.previewAlt')"
      />
      <iframe
        v-else
        :src="previewUrl"
        class="preview-frame"
        :title="t('files.pdfPreview')"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getFilesApi, uploadFileApi, deleteFileApi, deleteFilesApi, downloadFileApi, previewFileApi } from '@/api/file'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const icons = { Upload }

const acceptTypes = '.png,.jpg,.jpeg,.gif,.webp,.bmp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.zip,.rar,.7z'

const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('file:edit'))

const IMAGE_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp']

const typeOptions = computed(() => [
  { label: t('files.typeImage'), value: 'image' },
  { label: t('files.typePdf'), value: 'pdf' },
  { label: t('files.typeOffice'), value: 'office' },
  { label: t('files.typeText'), value: 'text' },
  { label: t('files.typeArchive'), value: 'archive' }
])

const files = ref([])
const keyword = ref('')
const typeFilter = ref('')
const selectedIds = ref([])
const loading = ref(false)
const uploading = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewName = ref('')
const previewKind = ref('image')
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  loadFiles()
})

async function loadFiles() {
  loading.value = true
  try {
    const res = await getFilesApi({
      page: pagination.page,
      size: pagination.size,
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined
    })
    files.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadFiles()
}

function resetSearch() {
  keyword.value = ''
  typeFilter.value = ''
  pagination.page = 1
  loadFiles()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(row => row.id)
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadFiles()
}

function handleCurrentChange(page) {
  pagination.page = page
  loadFiles()
}

async function handleUpload({ file }) {
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning(t('files.sizeLimit'))
    return
  }

  uploading.value = true
  try {
    await uploadFileApi(file)
    ElMessage.success(t('files.uploadSuccess'))
    pagination.page = 1
    loadFiles()
  } catch (error) {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

async function downloadFile(row) {
  try {
    const response = await downloadFileApi(row.id)
    saveBlob(response, row.originalName)
  } catch (error) {
    // 拦截器已提示
  }
}

async function removeFile(row) {
  try {
    await ElMessageBox.confirm(t('files.deleteConfirm', { name: row.originalName }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteFileApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    selectedIds.value = []
    loadFiles()
  } catch (error) {
    // 拦截器已提示
  }
}

async function removeSelected() {
  const ids = selectedIds.value
  if (!ids.length) {
    return
  }
  try {
    await ElMessageBox.confirm(t('files.batchDeleteConfirm', { count: ids.length }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteFilesApi(ids)
    ElMessage.success(t('common.deleteSuccess'))
    selectedIds.value = []
    loadFiles()
  } catch (error) {
    // 拦截器已提示
  }
}

function canPreview(row) {
  const ext = extensionOf(row.originalName)
  return IMAGE_EXTENSIONS.includes(ext) || ext === 'pdf'
}

async function previewFile(row) {
  try {
    const res = await previewFileApi(row.id)
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value)
    }
    previewUrl.value = URL.createObjectURL(res.data)
    previewName.value = row.originalName
    previewKind.value = extensionOf(row.originalName) === 'pdf' ? 'pdf' : 'image'
    previewVisible.value = true
  } catch (error) {
    // 拦截器已提示
  }
}

function closePreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

function extensionOf(name) {
  if (!name || !name.includes('.')) {
    return ''
  }
  return name.substring(name.lastIndexOf('.') + 1).toLowerCase()
}

function saveBlob(response, fallbackName) {
  const disposition = response.headers['content-disposition'] || ''
  // 中文文件名由后端以 RFC 5987 的 filename*=UTF-8'' 形式返回
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  const asciiMatch = disposition.match(/filename="?([^";]+)"?/i)
  let filename = fallbackName
  if (utf8Match) {
    filename = decodeURIComponent(utf8Match[1])
  } else if (asciiMatch) {
    filename = decodeURIComponent(asciiMatch[1])
  }

  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function fileExtension(name) {
  if (!name || !name.includes('.')) {
    return t('files.file')
  }
  return name.substring(name.lastIndexOf('.') + 1).toUpperCase()
}

function formatSize(size) {
  if (!size) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.keyword-input {
  width: 260px;
}

.type-select {
  width: 140px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-image {
  display: block;
  max-width: 100%;
  margin: 0 auto;
}

.preview-frame {
  width: 100%;
  height: 60vh;
  border: none;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>
