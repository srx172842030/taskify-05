import { onBeforeUnmount, onMounted } from 'vue'
import { useNotifications as useNotificationsStore } from '@/stores/notifications'

/**
 * 通知实时订阅：挂载时建立 SSE 连接并刷新未读计数，卸载时断开。
 */
export function useNotifications() {
  const store = useNotificationsStore()

  onMounted(() => {
    store.refreshUnread()
    store.connectStream()
  })
  onBeforeUnmount(() => store.disconnectStream())

  return store
}
