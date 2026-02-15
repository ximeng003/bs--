<script setup lang="ts">
import { computed } from 'vue'
import { toasts, confirmOpen, confirmOptions, resolveConfirm } from '@/lib/notify'
import Button from '@/components/ui/button/Button.vue'
import { CheckCircle, Info, AlertTriangle, XCircle } from 'lucide-vue-next'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'

const toastClasses = computed(() =>
  toasts.value.map(item => {
    if (item.type === 'success') {
      return 'border-green-200 bg-green-50 text-green-800'
    }
    if (item.type === 'error') {
      return 'border-red-200 bg-red-50 text-red-800'
    }
    if (item.type === 'warning') {
      return 'border-yellow-200 bg-yellow-50 text-yellow-800'
    }
    return 'border-blue-200 bg-blue-50 text-blue-800'
  })
)

const toastIcon = (type: string) => {
  if (type === 'success') return CheckCircle
  if (type === 'error') return XCircle
  if (type === 'warning') return AlertTriangle
  return Info
}

const handleConfirm = () => {
  resolveConfirm(true)
}

const handleCancel = () => {
  resolveConfirm(false)
}
</script>

<template>
  <div class="fixed inset-0 pointer-events-none z-[60]">
    <div class="absolute top-6 inset-x-0 flex justify-center">
      <div class="w-full max-w-md space-y-2 px-4">
        <div
          v-for="(toast, index) in toasts"
          :key="toast.id"
          class="pointer-events-auto flex items-start gap-3 rounded-lg border px-4 py-3 shadow-lg bg-white/90 backdrop-blur-sm"
          :class="toastClasses[index]"
        >
          <component
            :is="toastIcon(toast.type)"
            class="w-5 h-5 mt-0.5 flex-shrink-0"
          />
          <div class="flex-1 text-sm leading-relaxed">
            {{ toast.message }}
          </div>
        </div>
      </div>
    </div>

    <Dialog v-model:open="confirmOpen">
      <DialogContent class="sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle>{{ confirmOptions.title || '确认操作' }}</DialogTitle>
          <DialogDescription>
            {{ confirmOptions.message }}
          </DialogDescription>
        </DialogHeader>
        <div class="flex justify-end gap-3 mt-4">
          <Button variant="outline" @click="handleCancel">
            {{ confirmOptions.cancelText || '取消' }}
          </Button>
          <Button variant="destructive" @click="handleConfirm">
            {{ confirmOptions.confirmText || '确认' }}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>
