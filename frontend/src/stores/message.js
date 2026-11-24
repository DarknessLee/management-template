import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getMessagesApi,
  getUnreadMessageCountApi,
  markMessageReadApi,
  markAllMessagesReadApi,
  deleteMessageApi
} from '@/api/message'
import { getToken } from '@/utils/token'

const PAGE_SIZE = 10
const HEARTBEAT_INTERVAL = 25000
const MAX_RETRY = 5

/**
 * 站内消息：列表 + 未读数 + WebSocket 实时接收。
 * 断线后按指数退避重连，最多重试 5 次；页面重新可见时立即补一次连接。
 */
export const useMessageStore = defineStore('message', () => {
  const messages = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)

  let socket = null
  let reconnectTimer = null
  let heartbeatTimer = null
  let retryCount = 0
  let stopped = true
  let kickoutHandler = null

  async function load() {
    loading.value = true
    try {
      const [listRes, countRes] = await Promise.all([
        getMessagesApi({ page: 1, size: PAGE_SIZE }),
        getUnreadMessageCountApi()
      ])
      messages.value = listRes.data.list || []
      unreadCount.value = Number(countRes.data) || 0
    } catch (error) {
      // 拦截器已提示
    } finally {
      loading.value = false
    }
  }

  async function markRead(message) {
    if (!message || message.readFlag === 1) {
      return
    }
    try {
      await markMessageReadApi(message.id)
      const target = messages.value.find((item) => item.id === message.id)
      if (target) {
        target.readFlag = 1
      }
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      // 拦截器已提示
    }
  }

  async function markAllRead() {
    if (unreadCount.value === 0) {
      return
    }
    try {
      await markAllMessagesReadApi()
      messages.value.forEach((item) => {
        item.readFlag = 1
      })
      unreadCount.value = 0
    } catch (error) {
      // 拦截器已提示
    }
  }

  async function remove(message) {
    try {
      await deleteMessageApi(message.id)
      messages.value = messages.value.filter((item) => item.id !== message.id)
      if (message.readFlag === 0) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (error) {
      // 拦截器已提示
    }
  }

  function setKickoutHandler(handler) {
    kickoutHandler = handler
  }

  function start() {
    stopped = false
    load()
    connect()
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }

  function stop() {
    stopped = true
    clearTimeout(reconnectTimer)
    reconnectTimer = null
    teardownSocket()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    messages.value = []
    unreadCount.value = 0
    retryCount = 0
  }

  function connect() {
    const token = getToken()
    if (stopped || !token || socket) {
      return
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    try {
      socket = new WebSocket(
        `${protocol}//${window.location.host}/api/ws?token=${encodeURIComponent(token)}`
      )
    } catch (error) {
      scheduleReconnect()
      return
    }
    socket.onopen = () => {
      retryCount = 0
      startHeartbeat()
    }
    socket.onmessage = (event) => handleSocketMessage(event.data)
    socket.onclose = () => {
      teardownSocket()
      scheduleReconnect()
    }
    socket.onerror = () => {
      // onerror 之后浏览器会继续触发 onclose，这里不重复处理
      if (socket) {
        socket.close()
      }
    }
  }

  function scheduleReconnect() {
    if (stopped || retryCount >= MAX_RETRY) {
      return
    }
    const delay = Math.min(1000 * 2 ** retryCount, 30000)
    retryCount += 1
    clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(connect, delay)
  }

  function teardownSocket() {
    stopHeartbeat()
    if (!socket) {
      return
    }
    socket.onopen = null
    socket.onmessage = null
    socket.onclose = null
    socket.onerror = null
    try {
      socket.close()
    } catch (error) {
      // 连接可能已经关闭
    }
    socket = null
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send('ping')
      }
    }, HEARTBEAT_INTERVAL)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  function handleSocketMessage(raw) {
    let payload
    try {
      payload = JSON.parse(raw)
    } catch (error) {
      return
    }
    if (payload.event !== 'message' || !payload.data) {
      return
    }
    const message = payload.data
    if (typeof payload.unreadCount === 'number') {
      unreadCount.value = payload.unreadCount
    }
    if (!messages.value.some((item) => item.id === message.id)) {
      messages.value = [message, ...messages.value].slice(0, PAGE_SIZE)
    }
    // 强制下线：先停止重连，再交给布局页清理登录态并跳转
    if (message.type === 'kick') {
      stop()
      if (kickoutHandler) {
        kickoutHandler(message)
      }
    }
  }

  function handleVisibilityChange() {
    if (document.visibilityState === 'visible' && !stopped && !socket) {
      retryCount = 0
      clearTimeout(reconnectTimer)
      connect()
    }
  }

  return {
    messages,
    unreadCount,
    loading,
    load,
    markRead,
    markAllRead,
    remove,
    setKickoutHandler,
    start,
    stop
  }
})
