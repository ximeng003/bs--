<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Plus, Play, Calendar, Clock, Pencil, Trash2, Copy } from 'lucide-vue-next'
import CreateTestPlanForm from '@/components/CreateTestPlanForm.vue'
import request from '@/api/request'

interface TestPlan {
  id: string
  name: string
  description: string
  // For simplicity, backend might not return full testCases list in list view, but let's assume
  testCases?: { id: string; name: string; type: string }[] 
  schedule?: {
    enabled: boolean
    cron: string
    nextRun?: string
  }
  cronExpression?: string // Backend field
  environment: string
  status: 'active' | 'inactive'
  lastRun?: string
  successRate?: number
}

const testPlans = ref<TestPlan[]>([])
const isCreateDialogOpen = ref(false)

const fetchTestPlans = async () => {
    try {
        const res: any = await request.get('/plans')
        if (res && res.records) {
            testPlans.value = res.records
        } else if (Array.isArray(res)) {
            testPlans.value = res
        }
    } catch (e) {
        console.error(e)
    }
}

const handleDelete = async (id: string) => {
    if (!confirm('确定删除?')) return
    try {
        await request.delete(`/plans/${id}`)
        fetchTestPlans()
    } catch (e) {
        alert('删除失败')
    }
}

const handleExecute = async (id: string) => {
    const plan = testPlans.value.find(item => item.id === id)
    const planName = plan?.name || id
    alert('开始执行计划 ' + planName)
    const now = new Date().toLocaleString('zh-CN', { hour12: false })
    if (plan) {
        plan.lastRun = now
    }
    // await request.post(`/plans/${id}/execute`)
}

const handleCreateSuccess = () => {
    isCreateDialogOpen.value = false
    fetchTestPlans()
}

onMounted(() => {
    fetchTestPlans()
})

const getSuccessRateColor = (rate?: number) => {
  if (!rate) return 'text-gray-500'
  if (rate >= 90) return 'text-green-600'
  if (rate >= 70) return 'text-yellow-600'
  return 'text-red-600'
}
</script>

<template>
  <div class="space-y-6 p-6">
    <!-- Statistics -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm text-gray-600">总测试计划</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-semibold">{{ testPlans.length }}</div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm text-gray-600">启用计划</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-semibold text-green-600">
            {{ testPlans.filter(p => p.status === 'active').length }}
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm text-gray-600">定时任务</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-semibold text-blue-600">
            {{ testPlans.filter(p => !!p.cronExpression).length }}
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm text-gray-600">平均成功率</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-semibold text-green-600">
            {{ testPlans.length ? Math.round(testPlans.reduce((acc, p) => acc + (p.successRate || 0), 0) / testPlans.length) : 0 }}%
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- Actions -->
    <Card>
      <CardContent class="pt-6">
        <div class="flex gap-4">
          <Dialog :open="isCreateDialogOpen" @update:open="isCreateDialogOpen = $event">
            <DialogTrigger as-child>
              <Button @click="isCreateDialogOpen = true">
                <Plus class="w-4 h-4 mr-2" />
                创建测试计划
              </Button>
            </DialogTrigger>
            <DialogContent class="max-w-2xl h-[90vh] flex flex-col">
              <DialogHeader>
                <DialogTitle>创建测试计划</DialogTitle>
                <DialogDescription>
                  组合多个测试用例，配置定时任务和执行策略
                </DialogDescription>
              </DialogHeader>
              <div class="flex-1 overflow-y-auto pr-1">
                <CreateTestPlanForm @close="isCreateDialogOpen = false" @success="handleCreateSuccess" />
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </CardContent>
    </Card>

    <!-- CI/CD Integration -->
    <Card class="border-purple-200 bg-purple-50">
      <CardHeader>
        <CardTitle class="text-purple-900">CI/CD 集成</CardTitle>
        <CardDescription class="text-purple-700">
          通过 OpenAPI 或 Webhook 集成到您的 CI/CD 流程
        </CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="bg-white rounded-lg p-4 space-y-2">
          <h4 class="font-semibold text-sm">OpenAPI 触发</h4>
          <pre class="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">curl -X POST http://localhost:8080/api/v1/plans/1/execute \
  -H "Authorization: Bearer YOUR_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"environment": "production"}'</pre>
        </div>
        <div class="bg-white rounded-lg p-4 space-y-2">
          <h4 class="font-semibold text-sm">Jenkins 集成示例</h4>
          <pre class="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">pipeline {
  stage('Run Tests') {
    steps {
      sh 'curl -X POST http://localhost:8080/api/v1/plans/1/execute'
    }
  }
}</pre>
        </div>
      </CardContent>
    </Card>

    <!-- Test Plans List -->
    <Card>
      <CardHeader>
        <CardTitle>测试计划列表</CardTitle>
        <CardDescription>管理和执行所有测试计划</CardDescription>
      </CardHeader>
      <CardContent>
        <div class="space-y-4">
          <Card v-for="plan in testPlans" :key="plan.id" class="hover:shadow-md transition-shadow">
            <CardContent class="pt-6">
              <div class="flex flex-col md:flex-row md:items-start justify-between gap-4">
                <div class="space-y-2 flex-1">
                  <div class="flex items-center gap-3">
                    <h3 class="font-semibold text-lg">{{ plan.name }}</h3>
                    <Badge :variant="plan.status === 'active' ? 'default' : 'secondary'">
                      {{ plan.status === 'active' ? '启用' : '禁用' }}
                    </Badge>
                    <Badge variant="outline">{{ plan.environment }}</Badge>
                  </div>
                  <p class="text-sm text-gray-500">{{ plan.description }}</p>
                  
                  <div class="flex flex-wrap gap-4 text-xs text-gray-500 pt-2">
                    <div class="flex items-center gap-1">
                      <Calendar class="w-3 h-3" />
                      上次运行: {{ plan.lastRun || '从未' }}
                    </div>
                    <div v-if="plan.cronExpression" class="flex items-center gap-1 text-blue-600">
                      <Clock class="w-3 h-3" />
                      定时任务: {{ plan.cronExpression }}
                    </div>
                    <div v-if="plan.successRate !== undefined" :class="getSuccessRateColor(plan.successRate)">
                      成功率: {{ plan.successRate }}%
                    </div>
                  </div>
                </div>

                <div class="flex items-center gap-2">
                  <Button size="sm" class="bg-green-600 hover:bg-green-700" @click="handleExecute(plan.id)">
                    <Play class="w-4 h-4 mr-2" />
                    执行
                  </Button>
                  <Button variant="outline" size="icon" title="编辑">
                    <Pencil class="w-4 h-4" />
                  </Button>
                  <Button variant="outline" size="icon" title="复制">
                    <Copy class="w-4 h-4" />
                  </Button>
                  <Button variant="outline" size="icon" class="text-red-600 hover:text-red-700" @click="handleDelete(plan.id)">
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <div v-if="testPlans.length === 0" class="text-center py-8 text-gray-500">
            暂无测试计划，请点击"创建测试计划"
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
