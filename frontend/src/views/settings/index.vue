<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">{{ t('settings.title') }}</h2>
    </div>

    <div class="settings-grid">
      <div class="setting-card">
        <h3 class="setting-title">{{ t('settings.systemInfo') }}</h3>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">{{ t('settings.siteName') }}</span>
            <span class="info-value">{{ basicForm.siteTitle }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('settings.siteVersion') }}</span>
            <span class="info-value">v1.0.0</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('settings.techStack') }}</span>
            <span class="info-value">Vue3 + Element Plus</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('settings.runtime') }}</span>
            <span class="info-value">development</span>
          </div>
        </div>
      </div>

      <div class="setting-card">
        <h3 class="setting-title">{{ t('settings.security') }}</h3>
        <el-form :model="securityForm" label-width="120px">
          <el-form-item :label="t('settings.loginTimeout')">
            <el-input-number 
              v-model="securityForm.loginTimeout" 
              :min="10" 
              :max="120" 
              :step="10"
              style="width: 150px;"
            />
            <span style="margin-left: 8px;">{{ t('settings.minute') }}</span>
            <span class="setting-hint">{{ t('settings.loginTimeoutHint') }}</span>
          </el-form-item>
          <el-form-item :label="t('settings.passwordExpire')">
            <el-input-number 
              v-model="securityForm.passwordExpire" 
              :min="7" 
              :max="365" 
              :step="7"
              style="width: 150px;"
            />
            <span style="margin-left: 8px;">{{ t('settings.day') }}</span>
          </el-form-item>
          <el-form-item :label="t('settings.lockOnFail')">
            <el-switch v-model="securityForm.lockOnFail" />
            <span class="setting-hint">{{ t('settings.lockOnFailHint') }}</span>
          </el-form-item>
          <el-form-item :label="t('settings.maxFailCount')">
            <el-input-number 
              v-model="securityForm.maxFailCount" 
              :min="3" 
              :max="10" 
              :step="1"
              style="width: 150px;"
            />
            <span style="margin-left: 8px;">{{ t('settings.times') }}</span>
            <span class="setting-hint">{{ t('settings.maxFailCountHint') }}</span>
          </el-form-item>
          <el-form-item :label="t('settings.watermark')">
            <el-switch v-model="securityForm.watermark" />
            <span class="setting-hint">{{ t('settings.watermarkHint') }}</span>
          </el-form-item>
        </el-form>
      </div>

      <div class="setting-card">
        <h3 class="setting-title">{{ t('settings.basic') }}</h3>
        <el-form :model="basicForm" label-width="120px">
          <el-form-item :label="t('settings.siteTitle')">
            <el-input v-model="basicForm.siteTitle" :placeholder="t('settings.siteTitlePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('settings.siteLogo')">
            <el-input v-model="basicForm.siteLogo" :placeholder="t('settings.siteLogoPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('settings.defaultTheme')">
            <el-select v-model="basicForm.defaultTheme" :placeholder="t('settings.defaultThemePlaceholder')">
              <el-option :label="t('settings.themeLight')" value="light" />
              <el-option :label="t('settings.themeDark')" value="dark" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('settings.language')">
            <el-select
              v-model="basicForm.language"
              :placeholder="t('settings.languagePlaceholder')"
              @change="handleLanguageChange"
            >
              <el-option label="中文" value="zh-CN" />
              <el-option label="English" value="en" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <div class="setting-card">
        <h3 class="setting-title">{{ t('settings.notification') }}</h3>
        <el-form :model="notificationForm" label-width="120px">
          <el-form-item :label="t('settings.emailNotify')">
            <el-switch v-model="notificationForm.emailNotify" />
          </el-form-item>
          <el-form-item :label="t('settings.smsNotify')">
            <el-switch v-model="notificationForm.smsNotify" />
          </el-form-item>
          <el-form-item :label="t('settings.systemNotify')">
            <el-switch v-model="notificationForm.systemNotify" />
          </el-form-item>
          <el-form-item :label="t('settings.pushNotify')">
            <el-switch v-model="notificationForm.pushNotify" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="btn-group" style="margin-top: 24px;">
      <el-button v-permission="'settings:edit'" type="primary" @click="saveSettings">{{ t('settings.save') }}</el-button>
      <el-button v-permission="'settings:edit'" @click="resetSettings">{{ t('settings.reset') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getSettingsApi, updateSettingsApi } from '@/api/settings'
import { useSiteStore } from '@/stores/site'
import { localizedTitle } from '@/utils/title'

// 与后端 SettingService.defaults() 保持一致
const defaultSecurity = {
  loginTimeout: 30,
  passwordExpire: 90,
  lockOnFail: true,
  maxFailCount: 5,
  watermark: false
}

const defaultBasic = {
  siteTitle: '后台管理系统',
  siteLogo: '',
  defaultTheme: 'light',
  language: 'zh-CN'
}

const defaultNotification = {
  emailNotify: true,
  smsNotify: false,
  systemNotify: true,
  pushNotify: false
}

const securityForm = reactive({ ...defaultSecurity })
const basicForm = reactive({ ...defaultBasic })
const notificationForm = reactive({ ...defaultNotification })

const route = useRoute()
const siteStore = useSiteStore()
const { t } = useI18n()

onMounted(() => {
  loadSettings()
})

// 切换主题下拉框即时预览，保存后由 siteStore 统一写缓存并全局生效
watch(() => basicForm.defaultTheme, (theme) => {
  siteStore.setTheme(theme, false)
})

async function loadSettings() {
  try {
    const res = await getSettingsApi()
    Object.assign(securityForm, res.data.security)
    Object.assign(basicForm, res.data.basic)
    Object.assign(notificationForm, res.data.notification)
  } catch (error) {
    // 拦截器已提示
  }
}

// 语言下拉即时生效并写入本地偏好；这里用 change 而非 watch，避免加载表单时覆盖用户已有偏好
function handleLanguageChange(language) {
  siteStore.setLocale(language)
}

async function saveSettings() {
  try {
    await updateSettingsApi({
      security: { ...securityForm },
      basic: { ...basicForm },
      notification: { ...notificationForm }
    })
    ElMessage.success(t('settings.saved'))
    // 品牌名称、Logo、主题和语言立即同步到登录页、侧边栏和浏览器标题
    await siteStore.load(true)
    const siteTitle = basicForm.siteTitle || '后台管理系统'
    const pageTitle = localizedTitle(route.meta, siteStore.locale)
    document.title = pageTitle ? `${pageTitle} - ${siteTitle}` : siteTitle
  } catch (error) {
    // 拦截器已提示
  }
}

function resetSettings() {
  Object.assign(securityForm, defaultSecurity)
  Object.assign(basicForm, defaultBasic)
  Object.assign(notificationForm, defaultNotification)
  ElMessage.info(t('settings.resetDone'))
}
</script>

<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.setting-card {
  background: var(--app-bg-container);
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}

.setting-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--app-text-placeholder);
}

.setting-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--app-border);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 14px;
  color: var(--app-text-regular);
}

.info-value {
  font-size: 14px;
  color: var(--app-text-primary);
  font-weight: 500;
}
</style>
