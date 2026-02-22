<script setup lang="ts">
import { inject, Ref, computed } from 'vue'

const props = defineProps<{
  placeholder?: string
}>()

const selectValue = inject<Ref<string>>('selectValue')

const mapEnvironment = (value: string) => {
  switch (value) {
    case 'dev':
      return '开发环境'
    case 'staging':
      return '测试环境'
    case 'production':
      return '生产环境'
    default:
      return value
  }
}

const displayValue = computed(() => {
  if (selectValue && selectValue.value) {
    return mapEnvironment(selectValue.value)
  }
  return props.placeholder || ''
})
</script>

<template>
  <span style="pointer-events: none;">{{ displayValue }}</span>
</template>
