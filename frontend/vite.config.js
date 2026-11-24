import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 站内消息 WebSocket 走同一个 /api 前缀，需要让代理转发 Upgrade
        ws: true
        // 后端 context-path 就是 /api，因此这里不能去掉前缀
      }
    }
  }
})