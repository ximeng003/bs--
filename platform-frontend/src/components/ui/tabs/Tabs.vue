<script setup lang="ts">
import { cn } from '@/lib/utils'
import { provide, ref, watch } from 'vue'

const props = defineProps<{
  defaultValue?: string
  modelValue?: string
  class?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', payload: string): void
}>()

const activeTab = ref(props.modelValue || props.defaultValue || '')

watch(() => props.modelValue, (val) => {
  if (val) activeTab.value = val
})

function setActiveTab(value: string) {
  activeTab.value = value
  emit('update:modelValue', value)
}

provide('activeTab', activeTab)
provide('setActiveTab', setActiveTab)
</script>

<template>
  <div :class="cn('', props.class)">
    <slot />
  </div>
</template>
