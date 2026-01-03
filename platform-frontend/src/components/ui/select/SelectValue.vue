<script setup lang="ts">
import { inject, Ref, computed } from 'vue'

const selectValue = inject<Ref<string>>('selectValue')
const placeholder = inject<string>('placeholder', '')

const displayValue = computed(() => {
    return selectValue?.value || placeholder
})
</script>

<template>
  <span style="pointer-events: none;">{{ displayValue }}</span>
</template>
