<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('dicts.title') }}</h2>
      <el-button v-permission="'dict:edit'" type="primary" @click="openAddType">
        <el-icon><component :is="icons.Plus" /></el-icon>
        {{ t('dicts.add') }}
      </el-button>
    </div>

    <div class="card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          :placeholder="t('dicts.searchPlaceholder')"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="resetSearch">{{ t('common.reset') }}</el-button>
      </div>

      <el-table :data="typeList" border stripe>
        <el-table-column prop="id" :label="t('dicts.columns.id')" width="80" />
        <el-table-column prop="name" :label="t('dicts.columns.name')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" :label="t('dicts.columns.code')" min-width="160" />
        <el-table-column prop="remark" :label="t('dicts.columns.remark')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" :label="t('dicts.columns.createTime')" width="180" />
        <el-table-column :label="t('dicts.columns.operation')" width="240" v-if="canEdit || canViewItems">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openItems(scope.row)">{{ t('dicts.items') }}</el-button>
            <el-button v-if="canEdit" size="small" @click="openEditType(scope.row)">{{ t('dicts.edit') }}</el-button>
            <el-button v-if="canEdit" size="small" type="danger" @click="deleteType(scope.row)">{{ t('dicts.delete') }}</el-button>
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
        <el-form-item :label="t('dicts.form.name')" prop="name">
          <el-input v-model="typeForm.name" maxlength="50" :placeholder="t('dicts.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dicts.form.code')" prop="code">
          <el-input
            v-model="typeForm.code"
            :disabled="isEditType"
            :placeholder="t('dicts.codePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('dicts.form.remark')" prop="remark">
          <el-input v-model="typeForm.remark" maxlength="255" :placeholder="t('dicts.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveType">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="560px">
      <div class="drawer-toolbar">
        <span class="drawer-code">{{ t('dicts.drawerCode', { code: currentType.code }) }}</span>
        <el-button v-permission="'dict:edit'" type="primary" size="small" @click="openAddItem">
          <el-icon><component :is="icons.Plus" /></el-icon>
          {{ t('dicts.addItem') }}
        </el-button>
      </div>

      <el-table :data="itemList" border stripe>
        <el-table-column prop="label" :label="t('dicts.itemColumns.label')" min-width="120" />
        <el-table-column prop="value" :label="t('dicts.itemColumns.value')" min-width="120" />
        <el-table-column prop="sort" :label="t('dicts.itemColumns.sort')" width="80" />
        <el-table-column v-if="canEdit" :label="t('dicts.itemColumns.operation')" width="150">
          <template #default="scope">
            <el-button size="small" @click="openEditItem(scope.row)">{{ t('dicts.edit') }}</el-button>
            <el-button size="small" type="danger" @click="deleteItem(scope.row)">{{ t('dicts.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="itemList.length === 0" class="drawer-empty">{{ t('dicts.noItems') }}</p>
    </el-drawer>

    <el-dialog :title="itemDialogTitle" v-model="itemDialogVisible" width="480px" append-to-body>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="80px">
        <el-form-item :label="t('dicts.itemForm.label')" prop="label">
          <el-input v-model="itemForm.label" maxlength="50" :placeholder="t('dicts.labelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dicts.itemForm.value')" prop="value">
          <el-input v-model="itemForm.value" maxlength="50" :placeholder="t('dicts.valuePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('dicts.itemForm.sort')" prop="sort">
          <el-input-number v-model="itemForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveItem">{{ t('common.confirm') }}</el-button>
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
import { useI18n } from 'vue-i18n'

const icons = { Plus }

const userStore = useUserStore()
const { t } = useI18n()
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
const isEditType = ref(false)
const typeDialogTitle = computed(() => isEditType.value ? t('dicts.editTitle') : t('dicts.addTitle'))
const typeFormRef = ref(null)
const typeForm = reactive({
  id: null,
  name: '',
  code: '',
  remark: ''
})
const typeRules = computed(() => ({
  name: [{ required: true, message: t('dicts.nameRequired'), trigger: 'blur' }],
  code: [
    { required: true, message: t('dicts.codeRequired'), trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: t('dicts.codePattern'),
      trigger: 'blur'
    }
  ]
}))

const drawerVisible = ref(false)
const currentType = ref({})
const itemList = ref([])

const itemDialogVisible = ref(false)
const itemFormRef = ref(null)
const itemForm = reactive({
  id: null,
  label: '',
  value: '',
  sort: 0
})
const itemDialogTitle = computed(() => itemForm.id ? t('dicts.editItemTitle') : t('dicts.addItemTitle'))
const itemRules = computed(() => ({
  label: [{ required: true, message: t('dicts.labelRequired'), trigger: 'blur' }],
  value: [{ required: true, message: t('dicts.valueRequired'), trigger: 'blur' }]
}))

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
  Object.assign(typeForm, { id: null, name: '', code: '', remark: '' })
  typeDialogVisible.value = true
}

function openEditType(row) {
  isEditType.value = true
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
    ElMessage.success(isEditType.value ? t('common.updateSuccess') : t('common.createSuccess'))
    typeDialogVisible.value = false
    loadTypes()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteType(row) {
  try {
    await ElMessageBox.confirm(t('dicts.deleteTypeConfirm', { name: row.name }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteDictTypeApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadTypes()
  } catch (error) {
    // 拦截器已提示（如仍有字典项）
  }
}

const drawerTitle = computed(() => t('dicts.drawerTitle', { name: currentType.value.name || '' }))

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
  Object.assign(itemForm, { id: null, label: '', value: '', sort: 0 })
  itemDialogVisible.value = true
}

function openEditItem(row) {
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
    ElMessage.success(itemForm.id ? t('common.updateSuccess') : t('common.createSuccess'))
    itemDialogVisible.value = false
    loadItems()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteItem(row) {
  try {
    await ElMessageBox.confirm(t('dicts.deleteItemConfirm', { label: row.label }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteDictItemApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
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
