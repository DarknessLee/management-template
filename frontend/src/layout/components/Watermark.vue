<template>
  <div
    v-if="url"
    class="page-watermark"
    :style="{ backgroundImage: `url(${url})` }"
    aria-hidden="true"
  ></div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  text: {
    type: String,
    default: ''
  }
})

const url = ref('')

function draw() {
  if (!props.text) {
    url.value = ''
    return
  }
  const dpr = window.devicePixelRatio || 1
  const width = 320
  const height = 180
  const canvas = document.createElement('canvas')
  canvas.width = width * dpr
  canvas.height = height * dpr
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)
  ctx.translate(24, 130)
  ctx.rotate((-18 * Math.PI) / 180)
  ctx.font = '13px "Microsoft YaHei", sans-serif'
  ctx.fillStyle = 'rgba(0, 0, 0, 0.07)'
  ctx.textBaseline = 'middle'
  ctx.fillText(props.text, 0, 0)
  url.value = canvas.toDataURL('image/png')
}

watch(() => props.text, draw, { immediate: true })
</script>

<style scoped>
.page-watermark {
  position: fixed;
  inset: 0;
  z-index: 3000;
  pointer-events: none;
  background-repeat: repeat;
  background-size: 320px 180px;
}
</style>
