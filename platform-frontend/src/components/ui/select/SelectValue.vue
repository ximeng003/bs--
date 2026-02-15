<script setup lang="ts">
import { inject, Ref, computed } from 'vue'

const props = defineProps<{
  placeholder?: string
}>()

const selectValue = inject<Ref<string>>('selectValue')

const displayValue = computed(() => {
  if (selectValue && selectValue.value) {
    return selectValue.value
  }
  return props.placeholder || ''
})
</script>

<template>
  <span style="pointer-events: none;">{{ displayValue }}</span>
</template>
