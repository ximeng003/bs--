<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = withDefaults(
  defineProps<{
    modelValue?: number
    max?: number
    class?: string
  }>(),
  {
    modelValue: 0,
    max: 100,
  }
)

const percentage = computed(() => {
  if (!props.modelValue) return 0
  return Math.min(Math.max((props.modelValue / props.max) * 100, 0), 100)
})
</script>

<template>
  <div
    :class="cn(
      'relative h-4 w-full overflow-hidden rounded-full bg-secondary bg-gray-100',
      props.class
    )"
  >
    <div
      class="h-full w-full flex-1 bg-primary bg-blue-600 transition-all"
      :style="`transform: translateX(-${100 - percentage}%)`"
    ></div>
  </div>
</template>
