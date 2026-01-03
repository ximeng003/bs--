<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Search, CircleCheck, CircleX, Clock, CirclePlay, Eye } from 'lucide-vue-next'
import { Progress } from '@/components/ui/progress'

const emit = defineEmits<{
  (e: 'view-report', id: string): void
}>()

interface ExecutionRecord {
  id: string;
  planName: string;
  type: 'plan' | 'single';
  status: 'success' | 'failed' | 'running' | 'pending';
  startTime: string;
  endTime?: string;
  duration?: string;
  totalCases: number;
  passedCases: number;
  failedCases: number;
  engine: string;
  environment: string;
  executor: string;
}

const mockExecutions = ref<ExecutionRecord[]>([
  {
    id: '1',
    planName: '每日回归测试',
    type: 'plan',
    status: 'success',
    startTime: '2026-01-02 02:00:00',
    endTime: '2026-01-02 02:15:23',
    duration: '15分23秒',
    totalCases: 15,
    passedCases: 15,
    failedCases: 0,
    engine: '服务器引擎-01',
    environment: 'production',
    executor: '定时任务'
  },
  {
    id: '2',
    planName: 'API接口测试套件',
    type: 'plan',
    status: 'success',
    startTime: '2026-01-02 12:00:00',
    endTime: '2026-01-02 12:08:45',
    duration: '8分45秒',
    totalCases: 12,
    passedCases: 12,
    failedCases: 0,
    engine: '服务器引擎-02',
    environment: 'staging',
    executor: '定时任务'
  },
  {
    id: '3',
    planName: 'Web首页功能测试',
    type: 'single',
    status: 'running',
    startTime: '2026-01-02 10:35:12',
    totalCases: 8,
    passedCases: 5,
    failedCases: 0,
    engine: '本地引擎-张三',
    environment: 'staging',
    executor: '张三'
  },
  {
    id: '4',
    planName: '移动端功能测试',
    type: 'plan',
    status: 'failed',
    startTime: '2026-01-01 18:00:00',
    endTime: '2026-01-01 18:12:34',
    duration: '12分34秒',
    totalCases: 10,
    passedCases: 8,
    failedCases: 2,
    engine: '服务器引擎-01',
    environment: 'production',
    executor: '定时任务'
  },
  {
    id: '5',
    planName: '用户登录接口测试',
    type: 'single',
    status: 'success',
    startTime: '2026-01-02 09:20:15',
    endTime: '2026-01-02 09:20:18',
    duration: '3秒',
    totalCases: 1,
    passedCases: 1,
    failedCases: 0,
    engine: '本地引擎-李四',
    environment: 'dev',
    executor: '李四'
  }
])

const searchTerm = ref('')
const filterStatus = ref('all')

const getStatusColor = (status: string) => {
  switch (status) {
    case 'success': return 'bg-green-500 hover:bg-green-600'
    case 'failed': return 'bg-red-500 hover:bg-red-600'
    case 'running': return 'bg-blue-500 hover:bg-blue-600'
    case 'pending': return 'bg-yellow-500 hover:bg-yellow-600'
    default: return ''
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'success': return '成功'
    case 'failed': return '失败'
    case 'running': return '执行中'
    case 'pending': return '等待中'
    default: return status
  }
}
</script>

<template>
  <div class="space-y-6 p-6">
    <div class="flex flex-col md:flex-row gap-4 justify-between items-center">
      <div class="flex gap-4 w-full md:w-auto">
        <div class="relative w-full md:w-64">
          <Search class="absolute left-2 top-2.5 h-4 w-4 text-gray-400" />
          <Input placeholder="搜索执行记录..." class="pl-8" v-model="searchTerm" />
        </div>
        <Select v-model="filterStatus">
          <SelectTrigger class="w-[180px]">
            <SelectValue placeholder="状态筛选" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">全部状态</SelectItem>
            <SelectItem value="success">成功</SelectItem>
            <SelectItem value="failed">失败</SelectItem>
            <SelectItem value="running">执行中</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>

    <div class="space-y-4">
      <Card v-for="execution in mockExecutions" :key="execution.id">
        <CardContent class="p-6">
          <div class="flex flex-col md:flex-row gap-6">
            <!-- Status Icon -->
            <div class="flex-shrink-0">
              <div v-if="execution.status === 'success'" class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
                <CircleCheck class="w-6 h-6 text-green-600" />
              </div>
              <div v-else-if="execution.status === 'failed'" class="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
                <CircleX class="w-6 h-6 text-red-600" />
              </div>
              <div v-else-if="execution.status === 'running'" class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center animate-pulse">
                <CirclePlay class="w-6 h-6 text-blue-600" />
              </div>
              <div v-else class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center">
                <Clock class="w-6 h-6 text-gray-600" />
              </div>
            </div>

            <!-- Main Info -->
            <div class="flex-1 space-y-4">
              <div class="flex justify-between items-start">
                <div>
                  <div class="flex items-center gap-2 mb-1">
                    <h3 class="font-semibold text-lg">{{ execution.planName }}</h3>
                    <Badge :class="getStatusColor(execution.status)">{{ getStatusText(execution.status) }}</Badge>
                    <Badge variant="outline">{{ execution.type === 'plan' ? '计划' : '单例' }}</Badge>
                  </div>
                  <div class="text-sm text-gray-500 flex gap-4">
                    <span>ID: #{{ execution.id }}</span>
                    <span>环境: {{ execution.environment }}</span>
                    <span>执行人: {{ execution.executor }}</span>
                  </div>
                </div>
                <div class="text-right text-sm text-gray-500">
                  <div>{{ execution.startTime }}</div>
                  <div v-if="execution.duration">耗时: {{ execution.duration }}</div>
                </div>
              </div>

              <!-- Progress Bar -->
              <div class="space-y-2">
                <div class="flex justify-between text-sm">
                  <span>执行进度: {{ execution.passedCases + execution.failedCases }}/{{ execution.totalCases }}</span>
                  <span :class="execution.failedCases > 0 ? 'text-red-600' : 'text-green-600'">
                    通过率: {{ Math.round((execution.passedCases / execution.totalCases) * 100) }}%
                  </span>
                </div>
                <Progress :model-value="(execution.passedCases + execution.failedCases) / execution.totalCases * 100" class="h-2" />
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center">
              <Button variant="outline" @click="emit('view-report', execution.id)">
                <Eye class="w-4 h-4 mr-2" />
                查看报告
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
