<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('depts.title') }}</h2>
      <el-button v-permission="'dept:edit'" type="primary" @click="openAddDialog()">
        <el-icon><component :is="icons.Plus" /></el-icon>
        {{ t('depts.add') }}
      </el-button>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="deptTree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" :label="t('depts.columns.name')" min-width="220" />
        <el-table-column prop="leader" :label="t('depts.columns.leader')" width="140" />
        <el-table-column prop="phone" :label="t('depts.columns.phone')" width="160" />
        <el-table-column prop="sort" :label="t('depts.columns.sort')" width="90" />
        <el-table-column prop="createTime" :label="t('depts.columns.createTime')" width="180" />
        <el-table-column v-if="canEdit" :label="t('depts.columns.operation')" width="250">
          <template #default="scope">
            <el-button size="small" @click="openAddDialog(scope.row)">{{ t('depts.addChild') }}</el-button>
            <el-button size="small" @click="openEditDialog(scope.row)">{{ t('depts.edit') }}</el-button>
            <el-button size="small" type="danger" @click="deleteDept(scope.row)">{{ t('depts.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">{{ t('depts.tip') }}</p>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="520px">
      <el-form ref="deptFormRef" :model="deptForm" :rules="formRules" label-width="90px">
        <el-form-item :label="t('depts.parent')">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            :placeholder="t('depts.parentPlaceholder')"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item :label="t('depts.columns.name')" prop="name">
          <el-input v-model="deptForm.name" :placeholder="t('depts.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('depts.leader')">
          <el-input v-model="deptForm.leader" :placeholder="t('depts.leaderPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('depts.phone')">
          <el-input v-model="deptForm.phone" :placeholder="t('depts.phonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('depts.sort')">
          <el-input-number v-model="deptForm.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveDept">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getDeptTreeApi,
  createDeptApi,
  updateDeptApi,
  deleteDeptApi
} from '@/api/dept'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'

const icons = { Plus }

const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('dept:edit'))

const deptTree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const parentName = ref('')
const dialogTitle = computed(() => {
  if (isEdit.value) return t('depts.editTitle')
  return parentName.value ? t('depts.addChildTitle', { name: parentName.value }) : t('depts.addTitle')
})
const deptFormRef = ref(null)

const deptForm = reactive({
  id: null,
  parentId: null,
  name: '',
  leader: '',
  phone: '',
  sort: 0
})

const formRules = computed(() => ({
  name: [
    { required: true, message: t('depts.nameRequired'), trigger: 'blur' }
  ]
}))

// 编辑时不能把自己或自己的下级选为上级，否则会形成环
const parentOptions = computed(() => {
  if (!isEdit.value) {
    return deptTree.value
  }
  return filterTree(deptTree.value, deptForm.id)
})

onMounted(() => {
  loadDepts()
})

async function loadDepts() {
  loading.value = true
  try {
    const res = await getDeptTreeApi()
    deptTree.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function openAddDialog(parent) {
  isEdit.value = false
  parentName.value = parent ? parent.name : ''
  deptForm.id = null
  deptForm.parentId = parent ? parent.id : null
  deptForm.name = ''
  deptForm.leader = ''
  deptForm.phone = ''
  deptForm.sort = 0
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  parentName.value = ''
  Object.assign(deptForm, {
    id: row.id,
    parentId: row.parentId || null,
    name: row.name,
    leader: row.leader,
    phone: row.phone,
    sort: row.sort || 0
  })
  dialogVisible.value = true
}

async function saveDept() {
  const valid = await deptFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    parentId: deptForm.parentId || 0,
    name: deptForm.name,
    leader: deptForm.leader,
    phone: deptForm.phone,
    sort: deptForm.sort
  }

  try {
    if (isEdit.value) {
      await updateDeptApi(deptForm.id, payload)
    } else {
      await createDeptApi(payload)
    }
    ElMessage.success(isEdit.value ? t('common.updateSuccess') : t('common.createSuccess'))
    dialogVisible.value = false
    loadDepts()
  } catch (error) {
    // 拦截器已提示
  }
}

async function deleteDept(row) {
  try {
    await ElMessageBox.confirm(t('depts.deleteConfirm', { name: row.name }), t('common.confirmTitle'), { type: 'warning' })
  } catch (error) {
    return
  }

  try {
    await deleteDeptApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadDepts()
  } catch (error) {
    // 拦截器已提示（存在下级或用户时会被拦截）
  }
}

function filterTree(nodes, excludeId) {
  return nodes
    .filter(node => node.id !== excludeId)
    .map(node => ({
      ...node,
      children: node.children ? filterTree(node.children, excludeId) : []
    }))
}
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>
