<template>
  <el-dialog
    :model-value="visible"
    :title="t('forcePassword.title')"
    width="460px"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
  >
    <p class="force-tip">{{ t('forcePassword.tip') }}</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item :label="t('profile.oldPassword')" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="t('profile.newPassword')" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="t('profile.confirmPassword')" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ t('forcePassword.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePasswordApi } from '@/api/profile'

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = computed(() => ({
  oldPassword: [{ required: true, message: t('profile.oldPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('profile.newPasswordRequired'), trigger: 'blur' },
    { min: 6, max: 32, message: t('profile.newPasswordLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('profile.confirmRequired'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error(t('profile.confirmMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}))

async function submit() {
  if (submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await changePasswordApi({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success(t('forcePassword.success'))
    // 修改密码后服务端已让全部会话失效，这里清理本地登录态回到登录页
    userStore.clearAuth()
    router.push('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.force-tip {
  margin-bottom: 18px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--app-text-secondary);
}
</style>
