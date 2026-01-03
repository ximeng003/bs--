<script setup lang="ts">
import { cn } from '@/lib/utils'
import { ChevronDown } from 'lucide-vue-next'

const props = defineProps<{
  class?: string
}>()

// 简单模拟，实际这里应该有一个触发下拉的逻辑
// 但为了快速实现，我们用一个原生的 select 覆盖在上面，或者更简单的：我们只做 UI 展示，逻辑后续完善
// 这里为了最快迁移，我们使用原生 select 封装，但为了保持 API 兼容，我们需要做一些 hack
</script>

<template>
  <button
    type="button"
    :class="cn(
      'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
      props.class
    )"
  >
    <slot />
    <ChevronDown class="h-4 w-4 opacity-50" />
  </button>
</template>
