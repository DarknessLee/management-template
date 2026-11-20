import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentMenusApi } from '@/api/menu'

export const useMenuStore = defineStore('menu', () => {
  const menus = ref([])
  const loaded = ref(false)

  async function loadMenus() {
    const res = await getCurrentMenusApi()
    menus.value = res.data || []
    loaded.value = true
  }

  function reset() {
    menus.value = []
    loaded.value = false
  }

  return { menus, loaded, loadMenus, reset }
})
