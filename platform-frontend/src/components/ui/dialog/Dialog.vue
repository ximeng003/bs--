<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { provide } from 'vue'

const props = defineProps<{
  open?: boolean
  defaultOpen?: boolean
  class?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', payload: boolean): void
}>()

const open = useVModel(props, 'open', emit, {
  passive: true,
  defaultValue: props.defaultOpen,
})

provide('dialogOpen', open)
</script>

<template>
  <slot />
</template>
