<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('onlineUsers.title') }}</h2>
      <el-button :loading="loading" @click="loadOnlineUsers">
        <el-icon><component :is="icons.Refresh" /></el-icon>
        {{ t('onlineUsers.refresh') }}
      </el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="onlineUsers" border stripe>
        <el-table-column prop="username" :label="t('onlineUsers.columns.username')" width="150" />
        <el-table-column prop="realName" :label="t('onlineUsers.columns.realName')" width="150" />
        <el-table-column :label="t('onlineUsers.columns.role')" width="130">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.role || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" :label="t('onlineUsers.columns.ip')" width="170" />
        <el-table-column prop="loginTime" :label="t('onlineUsers.columns.loginTime')" width="180" />
        <el-table-column prop="sessionCount" :label="t('onlineUsers.columns.sessionCount')" width="100" />
        <el-table-column :label="t('onlineUsers.columns.operation')" width="140">
          <template #default="scope">
            <el-button
              v-if="canKick && scope.row.userId !== currentUserId"
              size="small"
              type="danger"
              @click="kickUser(scope.row)"
            >
              {{ t('onlineUsers.kickout') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip">{{ t('onlineUsers.tip') }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getOnlineUsersApi } from '@/api/online'
import { kickoutUserApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'

const icons = { Refresh }

const { t } = useI18n()
const userStore = useUserStore()
const canKick = computed(() => userStore.permissions.includes('users:edit'))
const currentUserId = computed(() => userStore.userInfo?.id)

const onlineUsers = ref([])
const loading = ref(false)

onMounted(() => {
  loadOnlineUsers()
})

async function loadOnlineUsers() {
  loading.value = true
  try {
    const res = await getOnlineUsersApi()
    onlineUsers.value = res.data
  } catch (error) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function kickUser(row) {
  try {
    await ElMessageBox.confirm(
      t('onlineUsers.kickoutConfirm', { name: row.username }),
      t('common.confirmTitle'),
      { type: 'warning' }
    )
  } catch (error) {
    return
  }

  try {
    const res = await kickoutUserApi(row.userId)
    ElMessage.success(t('onlineUsers.kicked', { count: res.data }))
    loadOnlineUsers()
  } catch (error) {
    // 拦截器已提示
  }
}
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 13px;
  color: var(--app-text-secondary);
}
</style>
