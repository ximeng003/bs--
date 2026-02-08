<!-- 
为了简化实现，我们将 Select 整个重构为使用原生 select 元素的封装，
但对外暴露与 Radix UI 兼容的 API 结构。
这是一种权衡，为了在不引入复杂 Headless UI 库的情况下快速运行。
-->
<script setup lang="ts">
import { cn } from '@/lib/utils'
import { inject, Ref } from 'vue'

defineProps<{
  class?: string
}>()

const open = inject<Ref<boolean>>('selectOpen')
</script>

<template>
  <div v-if="open" :class="cn('absolute top-full left-0 mt-2 z-50 min-w-[8rem] overflow-hidden rounded-md border bg-popover text-popover-foreground shadow-md animate-in fade-in-80', $props.class)">
    <div class="p-1">
        <slot />
    </div>
  </div>
</template>
