<template>
  <div class="tags-view">
    <div class="tags-scroll">
      <div
        v-for="tag in tags"
        :key="tag.path"
        class="tag-item"
        :class="{ active: tag.path === route.path }"
        @click="goTo(tag)"
      >
        <span>{{ tagTitle(tag) }}</span>
        <el-icon
          v-if="!tag.affix"
          class="tag-close"
          @click.stop="closeTag(tag)"
        >
          <Close />
        </el-icon>
      </div>
    </div>
    <el-dropdown trigger="click" @command="handleCommand">
      <el-button size="small" text class="tags-more">
        <el-icon><ArrowDown /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="refresh">{{ t('tags.refresh') }}</el-dropdown-item>
          <el-dropdown-item command="closeOthers">{{ t('tags.closeOthers') }}</el-dropdown-item>
          <el-dropdown-item command="closeAll">{{ t('tags.closeAll') }}</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Close, ArrowDown } from '@element-plus/icons-vue'
import { useSiteStore } from '@/stores/site'
import { localizedTitle } from '@/utils/title'

const emit = defineEmits(['refresh'])

const route = useRoute()
const router = useRouter()
const siteStore = useSiteStore()
const { t } = useI18n()

// 仪表盘常驻，不可关闭
const AFFIX_TAG = { path: '/dashboard', affix: true }
const tags = ref([{ ...AFFIX_TAG }])

watch(
  () => route.path,
  () => addTag(),
  { immediate: true }
)

// 标题按当前语言实时解析，切换语言时标签文案自动跟随
function tagTitle(tag) {
  return localizedTitle(router.resolve(tag.path).meta, siteStore.locale)
}

function addTag() {
  if (!route.meta || !route.meta.title) return
  if (tags.value.some(tag => tag.path === route.path)) return
  tags.value.push({ path: route.path, affix: false })
}

function goTo(tag) {
  if (tag.path !== route.path) {
    router.push(tag.path)
  }
}

function closeTag(tag) {
  const index = tags.value.findIndex(item => item.path === tag.path)
  if (index === -1) return
  tags.value.splice(index, 1)
  if (route.path === tag.path) {
    // 优先跳到右侧标签，没有就回到左侧，最后落到仪表盘
    const next = tags.value[index] || tags.value[index - 1] || AFFIX_TAG
    router.push(next.path)
  }
}

function handleCommand(command) {
  if (command === 'refresh') {
    emit('refresh')
    return
  }
  if (command === 'closeOthers') {
    tags.value = tags.value.filter(tag => tag.affix || tag.path === route.path)
    return
  }
  if (command === 'closeAll') {
    tags.value = [{ ...AFFIX_TAG }]
    if (route.path !== AFFIX_TAG.path) {
      router.push(AFFIX_TAG.path)
    }
  }
}
</script>

<style scoped>
.tags-view {
  height: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  background: var(--app-bg-container);
  border-bottom: 1px solid var(--app-border);
}

.tags-scroll {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}

.tags-scroll::-webkit-scrollbar {
  display: none;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  border: 1px solid var(--app-border);
  border-radius: 4px;
  background: var(--app-bg-container);
  font-size: 12px;
  color: var(--app-text-regular);
  cursor: pointer;
  white-space: nowrap;
  user-select: none;
}

.tag-item:hover {
  color: #409EFF;
  border-color: #B3D8FF;
}

.tag-item.active {
  background: #409EFF;
  border-color: #409EFF;
  color: #fff;
}

.tag-close {
  font-size: 12px;
  border-radius: 50%;
}

.tag-close:hover {
  background: rgba(0, 0, 0, 0.15);
}

.tags-more {
  padding: 4px;
  color: var(--app-text-secondary);
}
</style>
