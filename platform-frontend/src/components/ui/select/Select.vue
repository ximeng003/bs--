<script setup lang="ts">
import { cn } from '@/lib/utils'
import { useVModel, onClickOutside } from '@vueuse/core'
import { ref, provide } from 'vue'

const props = defineProps<{
  defaultValue?: string
  modelValue?: string
  class?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', payload: string): void
}>()

const value = useVModel(props, 'modelValue', emit, {
  passive: true,
  defaultValue: props.defaultValue,
})

const open = ref(false)
const containerRef = ref<HTMLElement | null>(null)

onClickOutside(containerRef, () => {
  open.value = false
})

const toggleOpen = () => {
  open.value = !open.value
}

provide('selectValue', value)
provide('selectOpen', open)
provide('toggleSelect', toggleOpen)
</script>

<template>
  <div ref="containerRef" :class="cn('relative', props.class)">
    <slot />
  </div>
</template>
