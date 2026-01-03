<script setup lang="ts">
import { cn } from '@/lib/utils'
import { useVModel } from '@vueuse/core'

const props = defineProps<{
  defaultValue?: string
  modelValue?: string
  class?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', payload: string): void
}>()

// 提供给子组件使用
import { provide } from 'vue'
const value = useVModel(props, 'modelValue', emit, {
  passive: true,
  defaultValue: props.defaultValue,
})
provide('selectValue', value)
</script>

<template>
  <div :class="cn('relative', props.class)">
    <slot />
  </div>
</template>
