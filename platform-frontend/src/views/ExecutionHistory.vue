<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Search, CircleCheck, CircleX, Clock, CirclePlay, Eye } from 'lucide-vue-next'
import { Progress } from '@/components/ui/progress'
import request from '@/api/request'
import { useRouter } from 'vue-router'

const router = useRouter()

interface ExecutionRecord {
  id: string;
  planName: string; // or case name
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

const executions = ref<ExecutionRecord[]>([])
const keyword = ref('')
const statusFilter = ref('all')
const loading = ref(false)

const fetchReports = async () => {
    loading.value = true
    try {
        const res: any = await request.get('/reports', {
            params: {
                keyword: keyword.value,
                status: statusFilter.value
            }
        })
        if (res && res.records) {
            executions.value = res.records.map((r: any) => ({
                id: r.id.toString(),
                planName: r.planId ? `测试计划 #${r.planId}` : `测试用例 #${r.caseId}`,
                type: r.planId ? 'plan' : 'single',
                status: r.status,
                startTime: r.executedAt,
                duration: r.executionTime ? `${r.executionTime}ms` : '-',
                totalCases: 1, // Mock for now as backend doesn't store summary yet
                passedCases: r.status === 'success' ? 1 : 0,
                failedCases: r.status === 'failed' ? 1 : 0,
                engine: 'Server',
                environment: 'production', // Mock
                executor: r.executedBy || 'System'
            }))
        } else {
             executions.value = []
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

watch([keyword, statusFilter], () => {
    fetchReports()
})

onMounted(() => {
    fetchReports()
})

const getStatusColor = (status: string) => {
  switch (status) {
    case 'success': return 'text-green-600 bg-green-50 border-green-200'
    case 'failed': return 'text-red-600 bg-red-50 border-red-200'
    case 'running': return 'text-blue-600 bg-blue-50 border-blue-200'
    default: return 'text-gray-600 bg-gray-50 border-gray-200'
  }
}

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'success': return CircleCheck
    case 'failed': return CircleX
    case 'running': return CirclePlay
    default: return Clock
  }
}

const handleViewReport = (id: string) => {
    router.push(`/reports/${id}`)
}
</script>

<template>
  <div class="space-y-6 p-6">
    <!-- Filters -->
    <div class="flex items-center gap-4 bg-white p-4 rounded-lg shadow-sm border border-gray-100">
      <div class="relative flex-1 max-w-md">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
        <Input v-model="keyword" placeholder="搜索执行记录..." class="pl-9" />
      </div>
      <Select v-model="statusFilter">
        <SelectTrigger class="w-[180px]">
          <SelectValue placeholder="所有状态" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">所有状态</SelectItem>
          <SelectItem value="success">执行成功</SelectItem>
          <SelectItem value="failed">执行失败</SelectItem>
          <SelectItem value="running">执行中</SelectItem>
        </SelectContent>
      </Select>
    </div>

    <!-- Execution List -->
    <div class="space-y-4">
      <Card v-for="record in executions" :key="record.id" class="hover:shadow-md transition-shadow">
        <CardContent class="p-6">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-3">
              <component :is="getStatusIcon(record.status)" 
                :class="`w-5 h-5 ${record.status === 'success' ? 'text-green-500' : record.status === 'failed' ? 'text-red-500' : 'text-blue-500'}`" 
              />
              <h3 class="font-semibold text-lg text-gray-900">{{ record.planName }}</h3>
              <Badge :class="getStatusColor(record.status) + ' border'">
                {{ record.status === 'success' ? '成功' : record.status === 'failed' ? '失败' : '执行中' }}
              </Badge>
              <Badge variant="outline" class="text-xs">
                {{ record.type === 'plan' ? '计划' : '单例' }}
              </Badge>
            </div>
            <div class="text-xs text-gray-500 flex flex-col items-end">
              <div>{{ record.startTime }}</div>
              <div v-if="record.duration">耗时: {{ record.duration }}</div>
            </div>
          </div>

          <div class="space-y-4">
            <div class="flex items-center gap-8 text-sm text-gray-600">
              <div>ID: #{{ record.id }}</div>
              <div>环境: {{ record.environment }}</div>
              <div>执行人: {{ record.executor }}</div>
              <div v-if="record.status !== 'running'">
                通过率: {{ Math.round((record.passedCases / record.totalCases) * 100) }}%
              </div>
            </div>

            <!-- Progress Bar -->
            <div class="space-y-1">
              <div class="flex justify-between text-xs text-gray-500">
                <span>执行进度: {{ record.passedCases + record.failedCases }}/{{ record.totalCases }}</span>
                <span>{{ Math.round(((record.passedCases + record.failedCases) / record.totalCases) * 100) }}%</span>
              </div>
              <Progress :model-value="((record.passedCases + record.failedCases) / record.totalCases) * 100" 
                :class="record.status === 'failed' ? 'bg-red-100 [&>div]:bg-red-500' : 'bg-green-100 [&>div]:bg-green-500'" 
              />
            </div>
          </div>

          <div class="flex justify-end mt-4 pt-4 border-t border-gray-100">
            <Button variant="outline" size="sm" @click="handleViewReport(record.id)">
              <Eye class="w-4 h-4 mr-2" />
              查看报告
            </Button>
          </div>
        </CardContent>
      </Card>

      <!-- Empty State -->
      <div v-if="executions.length === 0 && !loading" class="text-center py-12 text-gray-500 bg-white rounded-lg border border-dashed border-gray-200">
        <div class="flex justify-center mb-4">
            <Clock class="w-12 h-12 text-gray-300" />
        </div>
        <p>暂无执行记录</p>
      </div>
    </div>
  </div>
</template>
