import { useUserStore } from '@/stores/user'

/**
 * v-permission="'users:delete'" 或 v-permission="['users:add', 'users:edit']"
 * 无权限时直接移除元素。
 *
 * 注意：这只是界面收敛，防止用户点了才报错；
 * 真正的安全边界是后端的 @RequirePermission，因为接口可以被直接调用。
 */
export default {
  mounted(el, binding) {
    const userStore = useUserStore()
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    const allowed = required.some((permission) => userStore.permissions.includes(permission))
    if (!allowed) {
      el.parentNode?.removeChild(el)
    }
  }
}
