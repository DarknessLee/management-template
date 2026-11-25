<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('messageSend.title') }}</h2>
    </div>

    <div class="card">
      <p class="send-hint">{{ t('messageSend.hint') }}</p>
      <el-form
        ref="formRef"
        :model="sendForm"
        :rules="rules"
        label-width="100px"
        class="send-form"
      >
        <el-form-item :label="t('messageSend.scope')" prop="scope">
          <el-radio-group v-model="sendForm.scope">
            <el-radio value="all">{{ t('messageSend.scopeAll') }}</el-radio>
            <el-radio value="role">{{ t('messageSend.scopeRole') }}</el-radio>
            <el-radio value="user">{{ t('messageSend.scopeUser') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="sendForm.scope === 'role'" :label="t('messageSend.roles')" prop="roleIds">
          <el-select
            v-model="sendForm.roleIds"
            multiple
            filterable
            :placeholder="t('messageSend.rolesPlaceholder')"
            class="full-width"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="sendForm.scope === 'user'" :label="t('messageSend.users')" prop="userIds">
          <el-select
            v-model="sendForm.userIds"
            multiple
            filterable
            remote
            reserve-keyword
            :remote-method="searchUsers"
            :loading="userLoading"
            :placeholder="t('messageSend.usersPlaceholder')"
            class="full-width"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="`${user.realName}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('messageSend.titleLabel')" prop="title">
          <el-input
            v-model="sendForm.title"
            maxlength="100"
            show-word-limit
            :placeholder="t('messageSend.titlePlaceholder')"
          />
        </el-form-item>

        <el-form-item :label="t('messageSend.contentLabel')" prop="content">
          <el-input
            v-model="sendForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            :placeholder="t('messageSend.contentPlaceholder')"
          />
        </el-form-item>

        <el-form-item :label="t('messageSend.linkLabel')" prop="link">
          <el-input
            v-model="sendForm.link"
            maxlength="200"
            :placeholder="t('messageSend.linkPlaceholder')"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="sending" @click="handleSend">
            {{ t('messageSend.send') }}
          </el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { sendMessageApi } from '@/api/message'
import { getRolesApi } from '@/api/role'
import { getUsersApi } from '@/api/user'

const { t } = useI18n()

const formRef = ref()
const sending = ref(false)
const roleOptions = ref([])
const userOptions = ref([])
const userLoading = ref(false)

const sendForm = reactive({
  scope: 'all',
  roleIds: [],
  userIds: [],
  title: '',
  content: '',
  link: ''
})

const rules = {
  scope: [{ required: true, message: t('messageSend.scopeRequired'), trigger: 'change' }],
  roleIds: [{ required: true, type: 'array', message: t('messageSend.recipientsRequired'), trigger: 'change' }],
  userIds: [{ required: true, type: 'array', message: t('messageSend.recipientsRequired'), trigger: 'change' }],
  title: [
    { required: true, message: t('messageSend.titleRequired'), trigger: 'blur' },
    { max: 100, message: t('messageSend.titleTooLong'), trigger: 'blur' }
  ],
  content: [
    { required: true, message: t('messageSend.contentRequired'), trigger: 'blur' },
    { max: 500, message: t('messageSend.contentTooLong'), trigger: 'blur' }
  ],
  link: [{ max: 200, message: t('messageSend.linkTooLong'), trigger: 'blur' }]
}

onMounted(() => {
  loadRoles()
  searchUsers('')
})

async function loadRoles() {
  try {
    const res = await getRolesApi({ page: 1, size: 100 })
    roleOptions.value = res.data.list || []
  } catch (error) {
    // 拦截器已提示
  }
}

async function searchUsers(keyword) {
  userLoading.value = true
  try {
    const res = await getUsersApi({ page: 1, size: 50, keyword: keyword || undefined })
    userOptions.value = res.data.list || []
  } catch (error) {
    // 拦截器已提示
  } finally {
    userLoading.value = false
  }
}

async function handleSend() {
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }
  sending.value = true
  try {
    const res = await sendMessageApi({
      scope: sendForm.scope,
      roleIds: sendForm.scope === 'role' ? sendForm.roleIds : undefined,
      userIds: sendForm.scope === 'user' ? sendForm.userIds : undefined,
      title: sendForm.title,
      content: sendForm.content,
      link: sendForm.link || undefined
    })
    ElMessage.success(t('messageSend.sendSuccess', { count: res.data }))
    sendForm.title = ''
    sendForm.content = ''
    sendForm.link = ''
  } catch (error) {
    // 拦截器已提示
  } finally {
    sending.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  sendForm.roleIds = []
  sendForm.userIds = []
}
</script>

<style scoped>
.send-hint {
  margin-bottom: 20px;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.send-form {
  max-width: 720px;
}

.full-width {
  width: 100%;
}
</style>
