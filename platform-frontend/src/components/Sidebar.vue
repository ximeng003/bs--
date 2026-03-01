<script setup lang="ts">
import { Home, Globe, Monitor, FileText, Settings, CirclePlay, Code, FolderCog } from 'lucide-vue-next';
import { userStore } from '@/store/userStore'
import { useProjectStore } from '@/store/projectStore'
import { computed, onMounted } from 'vue'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const projectStore = useProjectStore()
const user = userStore.user

onMounted(() => {
  projectStore.fetchProjects()
})

const handleProjectChange = (value: string) => {
  const project = projectStore.projectList.find(p => p.id === Number(value))
  if (project) {
    projectStore.setCurrentProject(project)
    window.location.reload()
  }
}

defineProps<{
  currentPage: string
}>()

const emit = defineEmits<{
  (e: 'pageChange', page: string): void
}>()

const menuItems = [
  { id: 'dashboard', icon: Home, label: '工作台' },
  { id: 'api', icon: Globe, label: '测试用例' },
  { id: 'api-new', icon: Code, label: 'API' },
  { id: 'web-app', icon: Monitor, label: 'Web/App' },
  { id: 'plans', icon: CirclePlay, label: '测试计划' },
  { id: 'report', icon: FileText, label: '测试报告' },
  { id: 'project-settings', icon: FolderCog, label: '项目设置' },
  { id: 'settings', icon: Settings, label: '系统设置' }
];

const filteredMenuItems = computed(() => {
  return menuItems.filter(item => {
    if (item.id === 'settings') {
       return user?.role === 'admin'
    }
    return true
  })
})
</script>

<template>
  <aside class="w-64 bg-[#304156] text-white flex flex-col h-screen">
    <!-- Logo -->
    <div class="px-6 py-6 border-b border-[#2b3648] shrink-0">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-primary/20 rounded-lg flex items-center justify-center border border-primary/30">
          <Monitor class="w-6 h-6 text-primary" />
        </div>
        <div>
          <div class="font-bold text-lg tracking-wide">测试平台</div>
          <div class="text-[10px] text-gray-400 uppercase tracking-wider">Automation Hub</div>
        </div>
      </div>
    </div>

    <!-- Project Switcher -->
    <div class="px-4 py-4 border-b border-[#2b3648] shrink-0">
      <div class="text-xs text-gray-500 mb-2 font-medium uppercase tracking-wider px-2">当前项目</div>
      <Select :model-value="projectStore.currentProject?.id?.toString()" @update:model-value="handleProjectChange">
        <SelectTrigger class="w-full bg-[#1f2d3d] text-white border-[#3d4c63] hover:bg-[#263445]">
          <span class="truncate text-left flex-1">{{ projectStore.currentProject?.name || '请选择项目' }}</span>
        </SelectTrigger>
        <SelectContent class="bg-[#1f2d3d] border-[#3d4c63] text-white">
          <SelectItem 
            v-for="project in projectStore.projectList" 
            :key="project.id" 
            :value="project.id.toString()"
            class="hover:bg-[#263445] focus:bg-[#263445] cursor-pointer"
          >
            {{ project.name }}
          </SelectItem>
        </SelectContent>
      </Select>
    </div>

    <!-- Navigation Menu -->
    <nav class="flex-1 px-3 py-4 overflow-y-auto">
      <ul class="space-y-1">
        <li v-for="item in filteredMenuItems" :key="item.id">
          <button
            @click="emit('pageChange', item.id)"
            class="w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 group"
            :class="[
              currentPage === item.id
                ? 'bg-primary text-white shadow-lg shadow-primary/20 font-medium'
                : 'text-gray-400 hover:bg-[#263445] hover:text-white'
            ]"
          >
            <component 
              :is="item.icon" 
              class="w-5 h-5 transition-transform group-hover:scale-110"
              :class="currentPage === item.id ? 'text-white' : 'text-gray-500 group-hover:text-white'"
            />
            <span>{{ item.label }}</span>
            
            <div v-if="currentPage === item.id" class="ml-auto w-1.5 h-1.5 rounded-full bg-white/50"></div>
          </button>
        </li>
      </ul>
    </nav>
  </aside>
</template>
