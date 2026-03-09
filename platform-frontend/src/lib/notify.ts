import { ref } from 'vue'

type ToastType = 'success' | 'error' | 'info' | 'warning'

interface ToastItem {
  id: number
  type: ToastType
  message: string
}

interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
}

export const toasts = ref<ToastItem[]>([])

let toastId = 1

export const showToast = (message: string, type: ToastType = 'info', duration = 3000) => {
  const id = toastId++
  
  // 如果是同一个类型的消息或者正在获取数据/导出相关的消息，考虑先清理旧的
  if (message.includes('获取') || message.includes('导出')) {
    toasts.value = toasts.value.filter(t => !t.message.includes('获取') && !t.message.includes('导出'))
  }

  toasts.value.push({ id, type, message })
  if (duration > 0) {
    setTimeout(() => {
      toasts.value = toasts.value.filter(item => item.id !== id)
    }, duration)
  }
}

export const confirmOpen = ref(false)
export const confirmOptions = ref<ConfirmOptions>({
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
})

let confirmResolver: ((value: boolean) => void) | null = null

export const openConfirm = (options: ConfirmOptions): Promise<boolean> => {
  confirmOptions.value = {
    title: options.title,
    message: options.message,
    confirmText: options.confirmText || '确认',
    cancelText: options.cancelText || '取消',
  }
  confirmOpen.value = true
  return new Promise(resolve => {
    confirmResolver = resolve
  })
}

export const resolveConfirm = (result: boolean) => {
  if (confirmResolver) {
    confirmResolver(result)
    confirmResolver = null
  }
  confirmOpen.value = false
}

