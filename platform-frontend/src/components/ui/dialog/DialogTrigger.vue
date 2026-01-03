<script setup lang="ts">
import { inject, Ref } from 'vue'

const open = inject<Ref<boolean>>('dialogOpen')
</script>

<template>
  <div @click="open = true">
    <slot />
  </div>
</template>
