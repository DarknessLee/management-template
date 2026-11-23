<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('menus.title') }}</h2>
      <el-button v-permission="'menu:edit'" type="primary" @click="openAddDialog">
        <el-icon><component :is="icons.Plus" /></el-icon>
        {{ t('menus.add') }}
      </el-button>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="menus"
        border
        stripe
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column :label="t('menus.columns.title')" min-width="160">
          <template #default="scope">
            <span class="menu-title">
              <el-icon><component :is="icons[scope.row.icon] || icons.Menu" /></el-icon>
              {{ scope.row.title }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="t('menus.columns.titleEn')" width="150">
          <template #default="scope">{{ scope.row.titleEn || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('menus.columns.type')" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.component ? 'success' : 'info'" size="small">
              {{ scope.row.component ? t('menus.typeMenu') : t('menus.typeDir') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" :label="t('menus.columns.path')" width="150" />
        <el-table-column prop="component" :label="t('menus.columns.component')" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.component || '—' }}</template>
        </el-table-column>
        <el-table-column prop="permission" :label="t('menus.columns.permission')" width="140">
          <template #default="scope">{{ scope.row.permission || '—' }}</template>
        </el-table-column>
        <el-table-column prop="sort" :label="t('menus.columns.sort')" width="80" />
        <el-table-column :label="t('menus.columns.visible')" width="90">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.visible"
              :active-value="1"
              :inactive-value="0"
              :disabled="!canEdit"
              @change="value => toggleVisible(scope.row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="canEdit" :label="t('menus.columns.operation')" width="160" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">{{ t('menus.edit') }}</el-button>
            <el-button size="small" type="danger" @click="deleteMenu(scope.row)">{{ t('menus.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">{{ t('menus.tip') }}</p>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
      <el-form ref="menuFormRef" :model="menuForm" :rules="formRules" label-width="100px">
        <el-form-item :label="t('menus.parent')">
          <el-select v-model="menuForm.parentId" style="width: 100%;">
            <el-option :value="0" :label="t('menus.topLevel')" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('menus.form.title')" prop="title">
          <el-input v-model="menuForm.title" :placeholder="t('menus.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menus.form.titleEn')">
          <el-input v-model="menuForm.titleEn" :placeholder="t('menus.titleEnPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menus.form.path')" prop="path">
          <el-input v-model="menuForm.path" :placeholder="t('menus.pathPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menus.form.component')" prop="component">
          <el-input v-model="menuForm.component" :placeholder="t('menus.componentPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('menus.form.icon')">
          <el-select
            v-model="menuForm.icon"
            filterable
            allow-create
            clearable
            default-first-option
            :placeholder="t('menus.iconPlaceholder')"
            style="width: 100%;"
          >
            <el-option v-for="name in iconOptions" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('menus.form.permission')">
          <el-select
            v-model="menuForm.permission"
            filterable
            allow-create
            clearable
            default-first-option
            :placeholder="t('menus.permissionPlaceholder')"
            style="width: 100%;"
          >
            <el-option
              v-for="item in permissionOptions"
              :key="item.code"
              :label="t('menus.permissionOption', { code: item.code, name: item.name })"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('menus.form.sort')">
          <el-input-number v-model="menuForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item :label="t('menus.form.visible')">
          <el-switch v-model="menuForm.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveMenu">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Odometer,
  User,
  UserFilled,
  Operation,
  Key,
  Document,
  Clock,
  Bell,
  Message,
  Collection,
  Menu,
  Monitor,
  Connection,
  OfficeBuilding,
  Timer,
  Setting,
  Tickets,
  DataLine,
  ChatDotRound
} from '@element-plus/icons-vue'
import {
  getMenusApi,
  createMenuApi,
  updateMenuApi,
  deleteMenuApi
} from '@/api/menu'
import { getPermissionsApi } from '@/api/permission'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const icons = {
  Plus,
  Odometer,
  User,
  UserFilled,
  Operation,
  Key,
  Document,
  Clock,
  Bell,
  Message,
  Collection,
  Menu,
  Monitor,
  Connection,
  OfficeBuilding,
  Timer,
  Setting,
  Tickets,
  DataLine,
  ChatDotRound
}

const iconOptions = Object.keys(icons).filter(name => name !== 'Plus')

const userStore = useUserStore()
const { t } = useI18n()
const canEdit = computed(() => userStore.permissions.includes('menu:edit'))

const menus = ref([])
const permissionOptions = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => isEdit.value ? t('menus.editTitle') : t('menus.addTitle'))
const menuFormRef = ref(null)

const menuForm = reactive({
  id: null,
  parentId: 0,
  title: '',
  titleEn: '',
  path: '',
  component: '',
  icon: 'Menu',
  permission: '',
  sort: 0,
  visible: 1
})

// 只有目录可以作为上级菜单，编辑时排除自己
const parentOptions = computed(() => menus.value.filter(
  item => !item.component && item.id !== menuForm.id
))

const formRules = computed(() => ({
  title: [
    { required: true, message: t('menus.titleRequired'), trigger: 'blur' }
  ],
  path: [
    { required: true, message: t('menus.pathRequired'), trigger: 'blur' },
    {
      pattern: /^\/[a-zA-Z0-9\-_/]*$/,
      message: t('menus.pathPattern'),
      trigger: 'blur'
    }
  ],
  component: [
    {
      validator: (rule, value, callback) => {
        if (menuForm.parentId && !value) {
          callback(new Error(t('menus.componentRequired')))
          return
        }
        if (value && !/^views\/[a-zA-Z0-9\-_/]+\.vue$/.test(value)) {
          callback(new Error(t('menus.componentPattern')))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}))

onMounted(() => {
  loadMenus()
  loadPermissionOptions()
})

async function loadMenus() {
  loading.value = true
  try {
    const res = await getMenusApi()
    menus.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function loadPermissionOptions() {
  try {
    const res = await getPermissionsApi()
    permissionOptions.value = res.data
  } catch (error) {
    permissionOptions.value = []
  }
}

function openAddDialog() {
  isEdit.value = false
  Object.assign(menuForm, {
    id: null,
    parentId: 0,
    title: '',
    titleEn: '',
    path: '',
    component: '',
    icon: 'Menu',
    permission: '',
    sort: 0,
    visible: 1
  })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(menuForm, {
    id: row.id,
    parentId: row.parentId || 0,
    title: row.title,
    titleEn: row.titleEn || '',
    path: row.path,
    component: row.component || '',
    icon: row.icon || '',
    permission: row.permission || '',
    sort: row.sort || 0,
    visible: row.visible
  })
  dialogVisible.value = true
}

async function saveMenu() {
  const valid = await menuFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    parentId: menuForm.parentId,
    title: menuForm.title,
    titleEn: menuForm.titleEn,
    path: menuForm.path,
    component: menuForm.component,
    icon: menuForm.icon,
    permission: menuForm.permission,
    sort: menuForm.sort,
    visible: menuForm.visible
  }

  try {
    if (isEdit.value) {
      await updateMenuApi(menuForm.id, payload)
    } else {
      await createMenuApi(payload)
    }
    ElMessage.success(t('menus.saveSuccess'))
    dialogVisible.value = false
    loadMenus()
  } catch (error) {
    // 拦截器已提示
  }
}

async function toggleVisible(row, value) {
  try {
    await updateMenuApi(row.id, {
      parentId: row.parentId || 0,
      title: row.title,
      titleEn: row.titleEn,
      path: row.path,
      component: row.component,
      icon: row.icon,
      permission: row.permission,
      sort: row.sort,
      visible: value
    })
    ElMessage.success(value === 1 ? t('menus.shown') : t('menus.hidden'))
  } catch (error) {
    // 拦截器已提示
  }
  loadMenus()
}

async function deleteMenu(row) {
  try {
    await ElMessageBox.confirm(
      t('menus.deleteConfirm', { name: row.title }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }

  try {
    await deleteMenuApi(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadMenus()
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.menu-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>
