<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">数据字典</h2>
      <el-button v-permission="'dict:edit'" type="primary" @click="openAddType">
        <el-icon><component :is="icons.Plus" /></el-icon>
        新增字典
      </el-button>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="字典名称或编码"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="typeList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="字典名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" label="字典编码" min-width="160" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" v-if="canEdit || canViewItems">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openItems(scope.row)">字典项</el-button>
            <el-button v-if="canEdit" size="small" @click="openEditType(scope.row)">编辑</el-button>
            <el-button v-if="canEdit" size="small" type="danger" @click="deleteType(scope.row)">删除</el-button>
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

    <el-dialog :title="typeDialogTitle" v-model="typeDialogVisible" width="520px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="name">
          <el-input v-model="typeForm.name" maxlength="50" placeholder="如 用户状态" />
        </el-form-item>
        <el-form-item label="字典编码" prop="code">
          <el-input
            v-model="typeForm.code"
            :disabled="isEditType"
            placeholder="如 user_status"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="typeForm.remark" maxlength="255" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="560px">
      <div class="drawer-toolbar">
        <span class="drawer-code">编码：{{ currentType.code }}</span>
        <el-button v-permission="'dict:edit'" type="primary" size="small" @click="openAddItem">
          <el-icon><component :is="icons.Plus" /></el-icon>
          新增字典项
        </el-button>
      </div>

      <el-table :data="itemList" border stripe>
        <el-table-column prop="label" label="标签" min-width="120" />
        <el-table-column prop="value" label="值" min-width="120" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column v-if="canEdit" label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="openEditItem(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteItem(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="itemList.length === 0" class="drawer-empty">暂无字典项</p>
    </el-drawer>

    <el-dialog :title="itemDialogTitle" v-model="itemDialogVisible" width="480px" append-to-body>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="80px">
        <el-form-item label="标签" prop="label">
          <el-input v-model="itemForm.label" maxlength="50" placeholder="如 启用" />
        </el-form-item>
        <el-form-item label="值" prop="value">
          <el-input v-model="itemForm.value" maxlength="50" placeholder="如 1" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="itemForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getDictTypesApi,
  createDictTypeApi,
  updateDictTypeApi,
  deleteDictTypeApi,
  getDictItemsApi,
  createDictItemApi,
  updateDictItemApi,
  deleteDictItemApi
} from '@/api/dict'
import { useUserStore } from '@/stores/user'

const icons = { Plus }

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('dict:edit'))
const canViewItems = computed(() => userStore.permissions.includes('dict:view'))

const typeList = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const searchForm = reactive({
  keyword: ''
})

const typeDialogVisible = ref(false)
const typeDialogTitle = ref('新增字典')
const isEditType = ref(false)
const typeFormRef = ref(null)
const typeForm = reactive({
  id: null,
  name: '',
  code: '',
  remark: ''
})
const typeRules = {
  name: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入字典编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: '只能包含小写字母、数字和下划线，且以字母开头',
      trigger: 'blur'
    }
  ]
}

const drawerVisible = ref(false)
const currentType = ref({})
const itemList = ref([])

const itemDialogVisible = ref(false)
const itemDialogTitle = ref('新增字典项')
const itemFormRef = ref(null)
const itemForm = reactive({
  id: null,
  label: '',
  value: '',
  sort: 0
})
const itemRules = {
  label: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  value: [{ required: true, message: '请输入值', trigger: 'blur' }]
}

onMounted(() => {
  loadTypes()
})

async function loadTypes() {
  try {
    const res = await getDictTypesApi({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined
    })
    typeList.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    // 拦截器已提示
  }
}

function handleSearch() {
  pagination.page = 1
  loadTypes()
}

function resetSearch() {
  searchForm.keyword = ''
  pagination.page = 1
  loadTypes()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadTypes()
}

function handleCurrentChange(page) {
  pagination.page = page
  loadTypes()
}

function openAddType() {
  isEditType.value = false
  typeDialogTitle.value = '新增字典'
  Object.assign(typeForm, { id: null, name: '', code: '', remark: '' })
  typeDialogVisible.value = true
}

function openEditType(row) {
  isEditType.value = true
  typeDialogTitle.value = '编辑字典'
  Object.assign(typeForm, {
    id: row.id,
    name: row.name,
    code: row.code,
    remark: row.remark || ''
  })
  typeDialogVisible.value = true
}

async function saveType() {
  const valid = await typeFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    name: typeForm.name,
    code: typeForm.code,
    remark: typeForm.remark
  }

  try {
    if (isEditType.value) {
      await updateDictTypeApi(typeForm.id, payload)
    } else {
      await createDictTypeApi(payload)
    }
    ElMessage.success(isEditType.value ? '更新成功' : '创建成功')
    typeDialogVisible.value = false
    loadTypes()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteType(row) {
  try {
    await ElMessageBox.confirm(`确定要删除字典「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteDictTypeApi(row.id)
    ElMessage.success('删除成功')
    loadTypes()
  } catch (error) {
    // 拦截器已提示（如仍有字典项）
  }
}

const drawerTitle = computed(() => `字典项 - ${currentType.value.name || ''}`)

async function openItems(row) {
  currentType.value = row
  drawerVisible.value = true
  loadItems()
}

async function loadItems() {
  try {
    const res = await getDictItemsApi(currentType.value.id)
    itemList.value = res.data
  } catch (error) {
    // 拦截器已提示
  }
}

function openAddItem() {
  itemDialogTitle.value = '新增字典项'
  Object.assign(itemForm, { id: null, label: '', value: '', sort: 0 })
  itemDialogVisible.value = true
}

function openEditItem(row) {
  itemDialogTitle.value = '编辑字典项'
  Object.assign(itemForm, {
    id: row.id,
    label: row.label,
    value: row.value,
    sort: row.sort
  })
  itemDialogVisible.value = true
}

async function saveItem() {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    typeId: currentType.value.id,
    label: itemForm.label,
    value: itemForm.value,
    sort: itemForm.sort
  }

  try {
    if (itemForm.id) {
      await updateDictItemApi(itemForm.id, payload)
    } else {
      await createDictItemApi(payload)
    }
    ElMessage.success(itemForm.id ? '更新成功' : '创建成功')
    itemDialogVisible.value = false
    loadItems()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteItem(row) {
  try {
    await ElMessageBox.confirm(`确定要删除字典项「${row.label}」吗？`, '提示', { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteDictItemApi(row.id)
    ElMessage.success('删除成功')
    loadItems()
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
  width: 240px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.drawer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.drawer-code {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.drawer-empty {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>
