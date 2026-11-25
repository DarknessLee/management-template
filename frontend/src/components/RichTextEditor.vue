<template>
  <div class="rich-editor" :class="{ 'is-focus': focused }">
    <div class="rich-toolbar">
      <button type="button" class="tool" :title="t('notices.editor.bold')" @mousedown.prevent="exec('bold')">
        <strong>B</strong>
      </button>
      <button type="button" class="tool" :title="t('notices.editor.italic')" @mousedown.prevent="exec('italic')">
        <em>I</em>
      </button>
      <button type="button" class="tool" :title="t('notices.editor.underline')" @mousedown.prevent="exec('underline')">
        <u>U</u>
      </button>
      <button type="button" class="tool" :title="t('notices.editor.strike')" @mousedown.prevent="exec('strikeThrough')">
        <s>S</s>
      </button>
      <span class="divider"></span>
      <button type="button" class="tool" :title="t('notices.editor.heading')" @mousedown.prevent="exec('formatBlock', 'h3')">
        H3
      </button>
      <button type="button" class="tool" :title="t('notices.editor.quote')" @mousedown.prevent="exec('formatBlock', 'blockquote')">
        “”
      </button>
      <button type="button" class="tool" :title="t('notices.editor.unorderedList')" @mousedown.prevent="exec('insertUnorderedList')">
        •≡
      </button>
      <button type="button" class="tool" :title="t('notices.editor.orderedList')" @mousedown.prevent="exec('insertOrderedList')">
        1.≡
      </button>
      <span class="divider"></span>
      <button type="button" class="tool" :title="t('notices.editor.link')" @mousedown.prevent="insertLink">
        <el-icon><Link /></el-icon>
      </button>
      <button type="button" class="tool" :title="t('notices.editor.image')" @mousedown.prevent="insertImage">
        <el-icon><Picture /></el-icon>
      </button>
      <button type="button" class="tool" :title="t('notices.editor.clear')" @mousedown.prevent="clearFormat">
        <el-icon><Brush /></el-icon>
      </button>
    </div>
    <div
      ref="editorRef"
      class="rich-body"
      contenteditable="true"
      :data-placeholder="placeholder"
      @input="handleInput"
      @paste="handlePaste"
      @focus="handleFocus"
      @blur="focused = false"
    ></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link, Picture, Brush } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()
const editorRef = ref()
const focused = ref(false)

onMounted(() => {
  editorRef.value.innerHTML = props.modelValue || ''
})

watch(() => props.modelValue, (value) => {
  const html = value || ''
  // 只有外部值与编辑器内容不一致时才回写，避免输入时重置光标
  if (editorRef.value && editorRef.value.innerHTML !== html) {
    editorRef.value.innerHTML = html
  }
})

function handleFocus() {
  focused.value = true
  // 回车默认生成段落而不是 div
  document.execCommand('defaultParagraphSeparator', false, 'p')
}

function handleInput() {
  emit('update:modelValue', editorRef.value.innerHTML)
}

// 粘贴只保留纯文本，避免把外部网页的样式和脚本带进来
function handlePaste(event) {
  event.preventDefault()
  const text = (event.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

function exec(command, value = null) {
  editorRef.value.focus()
  document.execCommand('defaultParagraphSeparator', false, 'p')
  document.execCommand(command, false, value)
  handleInput()
}

function isSafeUrl(url) {
  return /^(https?:\/\/|mailto:)/i.test(url)
}

async function insertLink() {
  let url = ''
  try {
    const result = await ElMessageBox.prompt(t('notices.editor.linkPrompt'), t('notices.editor.link'), {
      inputPattern: /^(https?:\/\/|mailto:).+/i,
      inputErrorMessage: t('notices.editor.urlInvalid')
    })
    url = result.value.trim()
  } catch (error) {
    return
  }
  if (!isSafeUrl(url)) {
    ElMessage.warning(t('notices.editor.urlInvalid'))
    return
  }
  exec('createLink', url)
}

async function insertImage() {
  let url = ''
  try {
    const result = await ElMessageBox.prompt(t('notices.editor.imagePrompt'), t('notices.editor.image'), {
      inputPattern: /^https?:\/\/.+/i,
      inputErrorMessage: t('notices.editor.urlInvalid')
    })
    url = result.value.trim()
  } catch (error) {
    return
  }
  if (!isSafeUrl(url)) {
    ElMessage.warning(t('notices.editor.urlInvalid'))
    return
  }
  exec('insertImage', url)
}

function clearFormat() {
  editorRef.value.focus()
  document.execCommand('removeFormat')
  document.execCommand('formatBlock', false, 'p')
  handleInput()
}
</script>

<style scoped>
.rich-editor {
  width: 100%;
  border: 1px solid var(--app-border);
  border-radius: 4px;
  background: var(--app-bg-container);
  transition: border-color 0.2s;
}

.rich-editor.is-focus {
  border-color: #409eff;
}

.rich-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 4px 6px;
  border-bottom: 1px solid var(--app-border);
}

.tool {
  min-width: 28px;
  height: 26px;
  padding: 0 6px;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: var(--app-text-primary);
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.tool:hover {
  background: var(--app-hover-bg);
}

.divider {
  width: 1px;
  height: 16px;
  margin: 0 4px;
  background: var(--app-border);
}

.rich-body {
  min-height: 220px;
  max-height: 420px;
  overflow-y: auto;
  padding: 10px 12px;
  outline: none;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text-primary);
}

.rich-body:empty::before {
  content: attr(data-placeholder);
  color: var(--app-text-secondary);
  pointer-events: none;
}

.rich-body :deep(blockquote) {
  margin: 8px 0;
  padding-left: 12px;
  border-left: 3px solid var(--app-border);
  color: var(--app-text-secondary);
}

.rich-body :deep(img) {
  max-width: 100%;
}

.rich-body :deep(a) {
  color: #409eff;
}
</style>
