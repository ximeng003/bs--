<script setup lang="ts">
import { ref, computed } from 'vue'
import Sidebar from '@/components/Sidebar.vue'
import Dashboard from '@/components/Dashboard.vue'
import ApiCaseEditor from '@/views/ApiCaseEditor.vue'
import ScriptEditor from '@/views/ScriptEditor.vue'
import ReportDetail from '@/views/ReportDetail.vue'
import TestPlanManager from '@/views/TestPlanManager.vue'
import Settings from '@/views/Settings.vue'
import TestCaseManager from '@/views/TestCaseManager.vue'
import ExecutionHistory from '@/views/ExecutionHistory.vue'
import { ArrowLeft } from 'lucide-vue-next'
import Button from '@/components/ui/button/Button.vue'

const currentPage = ref('dashboard')
const currentReportId = ref<string | null>(null)
const currentCaseId = ref<string | null>(null)
const currentCaseType = ref<string | null>(null)

const pageTitle = computed(() => {
  switch (currentPage.value) {
    case 'dashboard': return '工作台'
    case 'api': return currentCaseId.value ? '编辑 API 用例' : 'API 测试用例'
    case 'web-app': return 'Web/App 脚本编辑'
    case 'report': return currentReportId.value ? '测试报告详情' : '测试报告'
    case 'plans': return '测试计划'
    case 'settings': return '系统设置'
    default: return '工作台'
  }
})

const handlePageChange = (page: string) => {
  currentPage.value = page
  // Reset sub-views when changing main pages
  currentReportId.value = null
  currentCaseId.value = null
  currentCaseType.value = null
}

const handleViewReport = (id: string) => {
  currentReportId.value = id
}

const handleEditCase = (id: string, type: string) => {
  currentCaseId.value = id
  currentCaseType.value = type
  if (type === 'API') {
    currentPage.value = 'api'
  } else {
    currentPage.value = 'web-app'
  }
}

const handleBack = () => {
  if (currentReportId.value) {
    currentReportId.value = null
  } else if (currentCaseId.value) {
    // If we are in 'web-app' but came from 'api' (TestCaseManager), we might want to go back to 'api'
    // But for simplicity, if we are in 'web-app', we just stay there? 
    // Wait, 'web-app' default view is ScriptEditor. 'api' default is TestCaseManager.
    // If we edited a WEB case, we are in 'web-app'. 
    // If we want to go back to the list, we should probably go to 'api' (TestCaseManager).
    // Or maybe 'web-app' should also have a list view?
    // Given the Sidebar structure, 'web-app' seems to be a separate module.
    // Let's assume 'api' is the Case Manager for ALL types for now, as implemented in TestCaseManager.
    // So back always goes to TestCaseManager (api page) or we reset currentCaseId.
    
    if (currentPage.value === 'web-app' && currentCaseType.value !== 'API') {
        // If we were editing a WEB app, going back might mean going back to the list in 'api' page?
        // Or maybe 'web-app' has its own list?
        // Let's just go back to 'api' page which has the list.
        currentPage.value = 'api'
    }
    currentCaseId.value = null
    currentCaseType.value = null
  }
}

const showBackButton = computed(() => {
  return !!currentReportId.value || (!!currentCaseId.value && currentPage.value === 'api') || (!!currentCaseId.value && currentPage.value === 'web-app' && currentCaseType.value !== null)
})
</script>

<template>
  <div class="flex h-screen bg-gray-50">
    <!-- Left Sidebar Navigation -->
    <Sidebar :currentPage="currentPage" @pageChange="handlePageChange" />

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Top Header -->
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
              <p class="text-sm text-gray-500 mt-1">自动化测试平台</p>
            </div>
          </div>
          <div class="flex items-center gap-4">
            <div class="flex items-center gap-2 text-sm">
              <span class="w-2 h-2 bg-green-500 rounded-full"></span>
              <span class="text-gray-600">在线</span>
            </div>
            <div class="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white text-sm">
              U
            </div>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="flex-1 overflow-auto">
        <Dashboard v-if="currentPage === 'dashboard'" />
        
        <!-- API / Test Case Manager Section -->
        <template v-else-if="currentPage === 'api'">
          <ApiCaseEditor v-if="currentCaseId && currentCaseType === 'API'" />
          <TestCaseManager v-else @edit-case="handleEditCase" />
        </template>

        <!-- Web/App Script Editor Section -->
        <template v-else-if="currentPage === 'web-app'">
           <!-- If we have a case ID, we show the editor with that case context (simulated) -->
           <!-- If not, we show ScriptEditor as default (maybe create new script) -->
           <ScriptEditor />
        </template>

        <!-- Report Section -->
        <template v-else-if="currentPage === 'report'">
          <ReportDetail v-if="currentReportId" />
          <ExecutionHistory v-else @view-report="handleViewReport" />
        </template>

        <TestPlanManager v-else-if="currentPage === 'plans'" />
        
        <Settings v-else-if="currentPage === 'settings'" />
        
        <div v-else class="p-6 flex items-center justify-center h-full text-gray-400">
          <div class="text-center">
            <h2 class="text-xl font-semibold mb-2">功能开发中</h2>
            <p>{{ pageTitle }} 模块即将上线</p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
