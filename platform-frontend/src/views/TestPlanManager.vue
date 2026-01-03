<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Plus, Play, Calendar, Clock, Pencil, Trash2, Copy } from 'lucide-vue-next'
import CreateTestPlanForm from '@/components/CreateTestPlanForm.vue'

interface TestPlan {
  id: string
  name: string
  description: string
  testCases: { id: string; name: string; type: string }[]
  schedule?: {
    enabled: boolean
    cron: string
    nextRun?: string
  }
  environment: string
  status: 'active' | 'inactive'
  lastRun?: string
  successRate?: number
}

const mockTestPlans: TestPlan[] = [
  {
    id: '1',
    name: '每日回归测试',
    description: '包含核心功能的全量回归测试',
    testCases: [
      { id: '1', name: '用户登录接口测试', type: 'API' },
      { id: '2', name: 'Web首页功能测试', type: 'WEB' },
      { id: '3', name: 'APP支付流程测试', type: 'APP' }
    ],
    schedule: {
      enabled: true,
      cron: '0 2 * * *',
      nextRun: '2026-01-03 02:00'
    },
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-02 02:00',
    successRate: 95
  },
  {
    id: '2',
    name: 'API接口测试套件',
    description: '所有API接口的完整测试',
    testCases: [
      { id: '1', name: '用户登录接口测试', type: 'API' },
      { id: '4', name: '用户注册接口测试', type: 'API' },
      { id: '5', name: '数据查询接口测试', type: 'API' }
    ],
    schedule: {
      enabled: true,
      cron: '0 */4 * * *',
      nextRun: '2026-01-02 16:00'
    },
    environment: 'staging',
    status: 'active',
    lastRun: '2026-01-02 12:00',
    successRate: 100
  },
  {
    id: '3',
    name: '移动端功能测试',
    description: 'APP端的关键业务流程测试',
    testCases: [
      { id: '3', name: 'APP支付流程测试', type: 'APP' },
      { id: '6', name: 'APP登录测试', type: 'APP' }
    ],
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-01 18:00',
    successRate: 85
  }
]

const testPlans = ref<TestPlan[]>(mockTestPlans)
const isCreateDialogOpen = ref(false)

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
            {{ testPlans.filter(p => p.schedule?.enabled).length }}
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm text-gray-600">平均成功率</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-semibold text-green-600">
            {{ Math.round(testPlans.reduce((acc, p) => acc + (p.successRate || 0), 0) / testPlans.length) }}%
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
            <DialogContent class="max-w-2xl max-h-[90vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>创建测试计划</DialogTitle>
                <DialogDescription>
                  组合多个测试用例，配置定时任务和执行策略
                </DialogDescription>
              </DialogHeader>
              <CreateTestPlanForm @close="isCreateDialogOpen = false" />
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
          <pre class="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">curl -X POST https://platform.example.com/api/v1/plans/1/execute \
  -H "Authorization: Bearer YOUR_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"environment": "production"}'</pre>
        </div>
        <div class="bg-white rounded-lg p-4 space-y-2">
          <h4 class="font-semibold text-sm">Jenkins 集成示例</h4>
          <pre class="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">pipeline {
  stage('Run Tests') {
    steps {
      sh 'curl -X POST https://platform.example.com/api/v1/plans/1/execute'
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
                <div class="flex-1 space-y-3">
                  <div>
                    <div class="flex items-center gap-2 mb-2">
                      <h3 class="font-semibold">{{ plan.name }}</h3>
                      <Badge v-if="plan.status === 'active'" class="bg-green-500">启用</Badge>
                      <Badge v-else class="bg-gray-500">禁用</Badge>
                      <Badge v-if="plan.schedule?.enabled" variant="outline" class="flex items-center gap-1">
                        <Clock class="w-3 h-3" />
                        定时任务
                      </Badge>
                    </div>
                    <p class="text-sm text-gray-600">{{ plan.description }}</p>
                  </div>
                  
                  <div class="space-y-2">
                    <div class="text-sm">
                      <span class="text-gray-600">包含用例: </span>
                      <span class="font-semibold">{{ plan.testCases.length }} 个</span>
                      <span class="text-gray-500 ml-2">
                        (API: {{ plan.testCases.filter(tc => tc.type === 'API').length }}, 
                        WEB: {{ plan.testCases.filter(tc => tc.type === 'WEB').length }}, 
                        APP: {{ plan.testCases.filter(tc => tc.type === 'APP').length }})
                      </span>
                    </div>
                    
                    <div v-if="plan.schedule?.enabled" class="flex items-center gap-4 text-sm text-gray-600">
                      <span>Cron: {{ plan.schedule.cron }}</span>
                      <span v-if="plan.schedule.nextRun" class="flex items-center gap-1">
                        <Calendar class="w-3 h-3" />
                        下次运行: {{ plan.schedule.nextRun }}
                      </span>
                    </div>
                    
                    <div class="flex items-center gap-4 text-sm">
                      <span class="text-gray-600">环境: {{ plan.environment }}</span>
                      <span v-if="plan.lastRun" class="text-gray-600">最后运行: {{ plan.lastRun }}</span>
                      <span v-if="plan.successRate !== undefined" :class="`font-semibold ${getSuccessRateColor(plan.successRate)}`">
                        成功率: {{ plan.successRate }}%
                      </span>
                    </div>
                  </div>
                </div>
                
                <div class="flex gap-2">
                  <Button size="sm" class="bg-green-600 hover:bg-green-700">
                    <Play class="w-4 h-4" />
                  </Button>
                  <Button variant="outline" size="sm">
                    <Pencil class="w-4 h-4" />
                  </Button>
                  <Button variant="outline" size="sm">
                    <Copy class="w-4 h-4" />
                  </Button>
                  <Button variant="outline" size="sm" class="text-red-600 hover:text-red-700">
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
