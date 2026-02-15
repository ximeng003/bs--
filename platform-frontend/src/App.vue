<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '@/components/Sidebar.vue'
import { ArrowLeft, LogOut } from 'lucide-vue-next'
import Button from '@/components/ui/button/Button.vue'
import { userStore } from '@/store/userStore'
import NotificationHost from '@/components/ui/notification/NotificationHost.vue'

const route = useRoute()
const router = useRouter()

// Map routes to sidebar IDs
  const currentPage = computed(() => {
    const path = route.path
    if (path === '/' || path === '/dashboard') return 'dashboard'
    if (path.startsWith('/api-cases')) return 'api'
    if (path.startsWith('/web-app')) return 'web-app'
    if (path.startsWith('/plans')) return 'plans'
    if (path.startsWith('/reports')) return 'report'
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
      case 'web-app': router.push('/web-app'); break;
      case 'plans': router.push('/plans'); break;
      case 'report': router.push('/reports'); break;
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
      <Sidebar :currentPage="currentPage" @pageChange="handlePageChange" />
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
                <div
                  class="flex items-center gap-2 cursor-pointer hover:bg-gray-100 p-1 pr-3 rounded-full transition-colors border border-transparent hover:border-gray-200"
                  @click="goToProfile"
                  title="个人中心"
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
                <Button
                  variant="ghost"
                  size="icon"
                  @click="handleLogout"
                  title="退出登录"
                  class="text-gray-500 hover:text-red-600"
                >
                  <LogOut class="w-5 h-5" />
                </Button>
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
