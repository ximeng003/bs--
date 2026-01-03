<script setup lang="ts">
import { cn } from '@/lib/utils'
import { inject, Ref } from 'vue'

const props = defineProps<{
  value: string
  class?: string
}>()

const activeTab = inject<Ref<string>>('activeTab')
</script>

<template>
  <div
    v-show="activeTab === value"
    :class="cn(
      'mt-2 ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
      props.class
    )"
  >
    <slot />
  </div>
</template>
