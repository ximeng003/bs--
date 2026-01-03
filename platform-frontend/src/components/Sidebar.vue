<script setup lang="ts">
import { Home, Globe, Monitor, FileText, Settings, CirclePlay } from 'lucide-vue-next';

defineProps<{
  currentPage: string
}>()

const emit = defineEmits<{
  (e: 'pageChange', page: string): void
}>()

const menuItems = [
  { id: 'dashboard', icon: Home, label: '工作台' },
  { id: 'api', icon: Globe, label: 'API 测试' },
  { id: 'web-app', icon: Monitor, label: 'Web/App' },
  { id: 'plans', icon: CirclePlay, label: '测试计划' },
  { id: 'report', icon: FileText, label: '测试报告' },
  { id: 'settings', icon: Settings, label: '系统设置' }
];
</script>

<template>
  <aside class="w-64 bg-[#304156] text-white flex flex-col">
    <!-- Logo -->
    <div class="px-6 py-6 border-b border-gray-700">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-[#409EFF] rounded-lg flex items-center justify-center">
          <Monitor class="w-6 h-6 text-white" />
        </div>
        <div>
          <div class="font-semibold text-lg">测试平台</div>
          <div class="text-xs text-gray-400">Automation Hub</div>
        </div>
      </div>
    </div>

    <!-- Navigation Menu -->
    <nav class="flex-1 px-3 py-4">
      <ul class="space-y-1">
        <li v-for="item in menuItems" :key="item.id">
          <button
            @click="emit('pageChange', item.id)"
            class="w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors"
            :class="[
              currentPage === item.id
                ? 'bg-[#409EFF] text-white'
                : 'text-gray-300 hover:bg-[#263445] hover:text-white'
            ]"
          >
            <component :is="item.icon" class="w-5 h-5" />
            <span>{{ item.label }}</span>
          </button>
        </li>
      </ul>
    </nav>

    <!-- Footer -->
    <div class="px-6 py-4 border-t border-gray-700">
      <div class="text-xs text-gray-400">
        <div>版本 v2.0.1</div>
        <div class="mt-1">© 2026 测试平台</div>
      </div>
    </div>
  </aside>
</template>
