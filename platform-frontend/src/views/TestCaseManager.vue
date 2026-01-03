<script setup lang="ts">
import { ref, computed } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Search, Plus, Pencil, Trash2, Play, Code } from 'lucide-vue-next'

const emit = defineEmits<{
  (e: 'edit-case', id: string, type: string): void
  (e: 'run-case', id: string): void
}>()

interface TestCase {
  id: string;
  name: string;
  type: 'API' | 'WEB' | 'APP';
  description: string;
  environment: string;
  status: 'active' | 'inactive';
  lastRun?: string;
  result?: 'success' | 'failed' | 'pending';
}

const mockTestCases = ref<TestCase[]>([
  {
    id: '1',
    name: '用户登录接口测试',
    type: 'API',
    description: '测试用户登录接口的正常流程和异常情况',
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-02 10:30',
    result: 'success'
  },
  {
    id: '2',
    name: 'Web首页功能测试',
    type: 'WEB',
    description: '测试Web首页的关键功能和交互',
    environment: 'staging',
    status: 'active',
    lastRun: '2026-01-02 09:15',
    result: 'success'
  },
  {
    id: '3',
    name: 'APP支付流程测试',
    type: 'APP',
    description: '测试APP端的支付流程',
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-01 18:20',
    result: 'failed'
  }
])

const searchTerm = ref('')
const filterType = ref('all')

const filteredTestCases = computed(() => {
  return mockTestCases.value.filter(tc => {
    const matchesSearch = tc.name.toLowerCase().includes(searchTerm.value.toLowerCase())
    const matchesType = filterType.value === 'all' || tc.type === filterType.value
    return matchesSearch && matchesType
  })
})

const getResultBadgeColor = (result?: string) => {
  switch (result) {
    case 'success': return 'bg-green-500 hover:bg-green-600'
    case 'failed': return 'bg-red-500 hover:bg-red-600'
    case 'pending': return 'bg-yellow-500 hover:bg-yellow-600'
    default: return ''
  }
}

const getResultText = (result?: string) => {
  switch (result) {
    case 'success': return '成功'
    case 'failed': return '失败'
    case 'pending': return '待执行'
    default: return '未运行'
  }
}

const getTypeBadgeClass = (type: string) => {
  const colors: Record<string, string> = {
    'API': 'bg-blue-100 text-blue-800 hover:bg-blue-200',
    'WEB': 'bg-purple-100 text-purple-800 hover:bg-purple-200',
    'APP': 'bg-green-100 text-green-800 hover:bg-green-200'
  }
  return colors[type] || ''
}
</script>

<template>
  <div class="space-y-6 p-6">
    <!-- Search and Filter Bar -->
    <Card>
      <CardContent class="pt-6">
        <div class="flex flex-col md:flex-row gap-4">
          <div class="flex-1 relative">
            <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            <Input
              placeholder="搜索测试用例..."
              class="pl-10"
              v-model="searchTerm"
            />
          </div>
          <Select v-model="filterType">
            <SelectTrigger class="w-[180px]">
              <SelectValue placeholder="类型筛选" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部类型</SelectItem>
              <SelectItem value="API">API 接口</SelectItem>
              <SelectItem value="WEB">Web 网页</SelectItem>
              <SelectItem value="APP">移动端 App</SelectItem>
            </SelectContent>
          </Select>
          <Button>
            <Plus class="w-4 h-4 mr-2" />
            新建用例
          </Button>
        </div>
      </CardContent>
    </Card>

    <!-- Test Cases List -->
    <div class="grid grid-cols-1 gap-4">
      <Card v-for="testCase in filteredTestCases" :key="testCase.id" class="hover:shadow-md transition-shadow">
        <CardContent class="pt-6">
          <div class="flex items-start justify-between">
            <div class="flex-1 space-y-3">
              <div class="flex items-center gap-3">
                <h3 class="font-semibold text-lg">{{ testCase.name }}</h3>
                <Badge :class="getTypeBadgeClass(testCase.type)">{{ testCase.type }}</Badge>
                <Badge variant="outline">{{ testCase.environment }}</Badge>
                <Badge :class="getResultBadgeColor(testCase.result)">
                  {{ getResultText(testCase.result) }}
                </Badge>
              </div>
              <p class="text-sm text-gray-500">{{ testCase.description }}</p>
              <div class="flex items-center gap-4 text-xs text-gray-400">
                <span>ID: {{ testCase.id }}</span>
                <span>最后运行: {{ testCase.lastRun }}</span>
              </div>
            </div>
            
            <div class="flex items-center gap-2">
              <Button variant="ghost" size="sm" @click="emit('run-case', testCase.id)">
                <Play class="w-4 h-4 text-green-600" />
              </Button>
              <Button variant="ghost" size="sm" @click="emit('edit-case', testCase.id, testCase.type)">
                <Pencil class="w-4 h-4 text-blue-600" />
              </Button>
              <Button variant="ghost" size="sm">
                <Code class="w-4 h-4 text-purple-600" />
              </Button>
              <Button variant="ghost" size="sm" class="text-red-600 hover:text-red-700 hover:bg-red-50">
                <Trash2 class="w-4 h-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
