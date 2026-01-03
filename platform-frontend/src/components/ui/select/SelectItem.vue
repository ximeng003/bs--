<script setup lang="ts">
import { cn } from '@/lib/utils'
import { inject, Ref } from 'vue'

const props = defineProps<{
  value: string
  class?: string
}>()

const selectValue = inject<Ref<string>>('selectValue')

function select() {
    if (selectValue) {
        selectValue.value = props.value
    }
}
</script>

<template>
  <div
    @click="select"
    :class="cn(
      'relative flex w-full cursor-default select-none items-center rounded-sm py-1.5 pl-8 pr-2 text-sm outline-none focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50 hover:bg-accent hover:text-accent-foreground cursor-pointer',
      props.class
    )"
  >
    <span class="absolute left-2 flex h-3.5 w-3.5 items-center justify-center" v-if="selectValue === value">
      <span class="h-2 w-2 rounded-full bg-current" />
    </span>
    <slot />
  </div>
</template>
