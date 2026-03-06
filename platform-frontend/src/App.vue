<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '@/components/Sidebar.vue'
import { ArrowLeft, LogOut, User, Settings } from 'lucide-vue-next'
import Button from '@/components/ui/button/Button.vue'
import { userStore } from '@/store/userStore'
import NotificationHost from '@/components/ui/notification/NotificationHost.vue'

const route = useRoute()
const router = useRouter()
const showUserMenu = ref(false)
const menuRef = ref<HTMLElement | null>(null)

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const closeUserMenu = (e: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', closeUserMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeUserMenu)
})

// Map routes to sidebar IDs
const currentPage = computed(() => {
  const path = route.path
  if (path === '/' || path === '/dashboard') return 'dashboard'
  if (path.startsWith('/api-cases/edit')) return 'api-new'
  if (path.startsWith('/api-cases')) return 'api'
  if (path.startsWith('/web-app')) return 'web-app'
  if (path.startsWith('/plans')) return 'plans'
  if (path.startsWith('/reports')) return 'report'
  if (path.startsWith('/project-settings')) return 'project-settings'
  if (path.startsWith('/settings')) return 'settings'
  return 'dashboard'
})

const pageTitle = computed(() => {
  if (route.name === 'Dashboard') return '工作台'
  if (route.name === 'ApiCaseEditor') return '编辑 API 用例'
  if (route.name === 'ApiTest') return '测试用例'
  if (route.name === 'WebApp') return 'Web/App 脚本编辑'
  if (route.name === 'ReportDetail') return '测试报告详情'
  if (route.name === 'Reports') return '测试报告'
  if (route.name === 'TestPlans') return '测试计划'
  if (route.name === 'ProjectSettings') return '项目设置'
  if (route.name === 'Settings') return '系统设置'
  return '工作台'
})

const showLayout = computed(() => {
  return route.meta.layout !== 'blank'
})

const handlePageChange = (page: string) => {
  switch (page) {
    case 'dashboard': router.push('/'); break;
    case 'api': router.push('/api-cases'); break;
    case 'api-new': router.push('/api-cases/edit'); break;
    case 'web-app': router.push('/web-app'); break;
    case 'plans': router.push('/plans'); break;
    case 'report': router.push('/reports'); break;
    case 'project-settings': router.push('/project-settings'); break;
    case 'settings': router.push('/settings'); break;
  }
}

const handleBack = () => {
  router.back()
}

const showBackButton = computed(() => {
  // Show back button on detail pages
  return ['ApiCaseEditor', 'ReportDetail'].includes(route.name as string)
})

const handleLogout = () => {
  userStore.clearUser()
  router.push('/login')
}

const goToProfile = () => {
  router.push('/profile')
}
</script>

<template>
  <div>
    <NotificationHost />
    <div v-if="!showLayout" class="min-h-screen bg-gray-100">
      <router-view />
    </div>

    <div v-else class="flex h-screen bg-gray-50">
      <Sidebar :current-page="currentPage" @page-change="handlePageChange" />
      <div class="flex-1 flex flex-col overflow-hidden">
        <header class="bg-white border-b border-gray-200 px-6 py-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-4">
              <Button v-if="showBackButton" variant="ghost" size="icon" @click="handleBack">
                <ArrowLeft class="w-5 h-5" />
              </Button>
              <div>
                <h1 class="text-xl font-semibold text-gray-900">
                  {{ pageTitle }}
                </h1>
              </div>
            </div>
            <div class="flex items-center gap-4">
              <div class="flex items-center gap-2" />
              <div class="flex items-center gap-3">
                <div class="relative" ref="menuRef">
                  <div
                    class="flex items-center gap-2 cursor-pointer hover:bg-gray-100 p-1 pr-3 rounded-full transition-colors border border-transparent hover:border-gray-200"
                    @click="toggleUserMenu"
                    title="用户菜单"
                  >
                    <div class="w-8 h-8 rounded-full bg-blue-100 overflow-hidden border border-gray-200">
                      <img
                        :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${userStore.user?.username || 'user'}`"
                        alt="Avatar"
                        class="w-full h-full object-cover"
                      />
                    </div>
                    <span class="text-sm text-gray-700 font-medium">{{ userStore.user?.username || 'User' }}</span>
                  </div>

                  <!-- Dropdown Menu -->
                  <div v-if="showUserMenu" class="absolute right-0 top-full mt-2 w-56 bg-white rounded-md shadow-lg py-1 z-50 border border-gray-200">
                    <div class="px-4 py-3 border-b border-gray-100 bg-gray-50/50">
                      <p class="text-sm font-medium text-gray-900 truncate">{{ userStore.user?.username }}</p>
                      <p class="text-xs text-gray-500 truncate">{{ userStore.user?.role === 'admin' ? '管理员' : '普通用户' }}</p>
                    </div>
                    
                    <div class="py-1">
                      <button 
                        @click="goToProfile(); showUserMenu = false" 
                        class="flex items-center gap-2 w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
                      >
                        <User class="w-4 h-4 text-gray-500" />
                        个人中心
                      </button>
                      
                      <button 
                        v-if="userStore.user?.role === 'admin'"
                        @click="router.push('/settings'); showUserMenu = false" 
                        class="flex items-center gap-2 w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
                      >
                        <Settings class="w-4 h-4 text-gray-500" />
                        后台管理
                      </button>
                      
                      <div class="border-t border-gray-100 my-1"></div>

                      <button 
                        @click="handleLogout" 
                        class="flex items-center gap-2 w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <LogOut class="w-4 h-4" />
                        退出登录
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </header>
        <main class="flex-1 overflow-auto p-6">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>
