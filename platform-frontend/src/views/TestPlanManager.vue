<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Plus, Play, Calendar, Clock, Pencil, Trash2, Copy } from 'lucide-vue-next'
import CreateTestPlanForm from '@/components/CreateTestPlanForm.vue'
import request from '@/api/request'
import { showToast, openConfirm } from '@/lib/notify'

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
  lastRunTime?: string
  lastRunStatus?: string
  successRate?: number
  apiId?: string
  allowOpenApi?: boolean
}

const router = useRouter()
const testPlans = ref<TestPlan[]>([])
const concurrencyMap = ref<Record<string, string>>({})
const isCreateDialogOpen = ref(false)
const isEditDialogOpen = ref(false)
const editingPlan = ref<TestPlan | null>(null)
const pageSizeStr = ref('10')
const pageSizeOptions = ['10', '50', '100']
const currentPage = ref(1)
const totalCount = ref(0)
const pageSize = computed(() => Number(pageSizeStr.value) || 10)
const selectedPlanIds = ref<string[]>([])

const fetchTestPlans = async () => {
  try {
    const res: any = await request.get('/plans', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    if (res && res.records) {
      totalCount.value = typeof res.total === 'number' ? res.total : res.records.length
      testPlans.value = res.records
      // Initialize concurrency defaults
      testPlans.value.forEach(p => {
        if (!concurrencyMap.value[p.id]) concurrencyMap.value[p.id] = '1'
      })
    } else if (Array.isArray(res)) {
      totalCount.value = res.length
      testPlans.value = res
      testPlans.value.forEach(p => {
        if (!concurrencyMap.value[p.id]) concurrencyMap.value[p.id] = '1'
      })
    } else {
      totalCount.value = 0
      testPlans.value = []
    }
    selectedPlanIds.value = []
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (id: string) => {
  const ok = await openConfirm({
    title: '删除测试计划',
    message: '确定要删除这个测试计划吗？此操作不可恢复。',
    confirmText: '删除',
  })
  if (!ok) return
  try {
    await request.delete(`/plans/${id}`)
    fetchTestPlans()
    showToast('删除成功', 'success')
  } catch (_e) {
    showToast('删除失败', 'error')
  }
}

const handleBatchDelete = async () => {
  if (!selectedPlanIds.value.length) {
    showToast('请先选择要删除的测试计划', 'warning')
    return
  }
  const ok = await openConfirm({
    title: '批量删除测试计划',
    message: `确定要删除选中的 ${selectedPlanIds.value.length} 个测试计划吗？此操作不可恢复。`,
    confirmText: '删除',
  })
  if (!ok) return
  try {
    await Promise.all(selectedPlanIds.value.map(id => request.delete(`/plans/${id}`)))
    selectedPlanIds.value = []
    await fetchTestPlans()
  } catch (_e) {
    showToast('批量删除失败', 'error')
  }
}

const handleDeleteAll = async () => {
  const ok = await openConfirm({
    title: '清空测试计划',
    message: '确定要删除所有测试计划吗？此操作不可恢复。',
    confirmText: '清空',
  })
  if (!ok) return
  try {
    await request.delete('/plans')
    selectedPlanIds.value = []
    await fetchTestPlans()
    showToast('清空测试计划成功', 'success')
  } catch (e: any) {
    if (e && e.response && e.response.status === 405) {
      try {
        const ids = testPlans.value.map(p => p.id)
        if (ids.length) {
          await Promise.all(ids.map(id => request.delete(`/plans/${id}`)))
        }
        selectedPlanIds.value = []
        await fetchTestPlans()
        showToast('清空测试计划成功', 'success')
        return
      } catch {
        showToast('清空失败', 'error')
        return
      }
    }
    showToast('清空失败', 'error')
  }
}

const handleExecute = async (id: string) => {
  const plan = testPlans.value.find(item => item.id === id)
  const planName = plan?.name || id
  showToast('开始执行计划 ' + planName, 'info')
  try {
    const c = Number(concurrencyMap.value[id] || '1')
    const res: any = await request.post(`/plans/${id}/execute`, null, { params: { concurrency: c } })
    const nowStr = new Date().toLocaleString('zh-CN', { hour12: false })
    if (plan) {
      plan.lastRun = nowStr
    }
    if (res && typeof res === 'object') {
      const key = `plan-summary-${id}-${Date.now()}`
      try {
        sessionStorage.setItem(key, JSON.stringify(res))
      } catch {}
      router.push({ path: '/plan-reports', query: { planId: String(id), key } })
    } else {
      showToast('执行完成，正在跳转到报告列表', 'info')
      router.push('/reports')
    }
  } catch (_e) {
    console.error(_e)
    showToast('执行计划失败', 'error')
  }
}

const handleEditPlan = (plan: TestPlan) => {
  editingPlan.value = { ...plan }
  isEditDialogOpen.value = true
}

const handleEditSuccess = () => {
  isEditDialogOpen.value = false
  fetchTestPlans()
}

const handleDuplicatePlan = async (plan: TestPlan) => {
  const payload: any = {
    name: `${plan.name} 副本`,
    description: plan.description,
    environment: plan.environment,
    cronExpression: plan.cronExpression || null,
    status: plan.status || 'inactive',
  }
  try {
    await request.post('/plans', payload)
    showToast('复制计划成功', 'success')
    fetchTestPlans()
  } catch (_e) {
    showToast('复制计划失败', 'error')
  }
}

const handleCopyPlan = async (plan: TestPlan) => {
  const text = `curl -X POST http://localhost:18080/api/plans/${plan.id}/execute`
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    showToast('复制成功', 'success')
  } catch (_e) {
    showToast('复制失败', 'error')
  }
}

const handleCreateSuccess = () => {
    isCreateDialogOpen.value = false
    fetchTestPlans()
}

onMounted(() => {
  fetchTestPlans()
})

const totalPages = computed(() => {
  const total = totalCount.value
  return total > 0 ? Math.max(1, Math.ceil(total / pageSize.value)) : 1
})

const goPrev = () => {
  if (currentPage.value > 1) currentPage.value -= 1
}
const goNext = () => {
  if (currentPage.value < totalPages.value) currentPage.value += 1
}
const setPage = (p: number) => {
  if (p >= 1 && p <= totalPages.value) currentPage.value = p
}

watch(pageSizeStr, () => {
  currentPage.value = 1
  fetchTestPlans()
})

watch(currentPage, () => {
  fetchTestPlans()
})

const isAllSelected = computed(
  () => testPlans.value.length > 0 && testPlans.value.every(p => selectedPlanIds.value.includes(p.id))
)

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedPlanIds.value = []
  } else {
    selectedPlanIds.value = testPlans.value.map(p => p.id)
  }
}

const getSuccessRateColor = (rate?: number) => {
  if (!rate) return 'text-gray-500'
  if (rate >= 90) return 'text-green-600'
  if (rate >= 70) return 'text-yellow-600'
  return 'text-red-600'
}

// CI/CD Integration Logic
const savedApiKeys = localStorage.getItem('api_keys')
const defaultApiKeys = [
  {
    id: '1',
    name: 'Jenkins CI',
    key: 'sk_test_abcdef1234567890',
    permissions: ['read', 'execute'],
    createdAt: '2026-01-01',
    lastUsed: '2026-01-02 10:30'
  }
]
const apiKeys = ref(savedApiKeys ? JSON.parse(savedApiKeys) : defaultApiKeys)
const selectedKey = ref(apiKeys.value.length > 0 ? apiKeys.value[0].key : 'sk_test_...')
const selectedKeyName = ref(apiKeys.value.length > 0 ? apiKeys.value[0].name : 'Default Key')
const selectedPlanId = ref('PLAN_1001')
const selectedPlanName = ref('示例计划')
const getSelectedPlanConcurrency = () => {
  const plan = testPlans.value.find(p => p.apiId === selectedPlanId.value || p.id === selectedPlanId.value)
  if (!plan) return 1
  const val = concurrencyMap.value[plan.id]
  const num = Number(val || '1')
  return isNaN(num) ? 1 : num
}

// Auto-select first available plan with API ID
watch(testPlans, (plans) => {
  const planWithApi = plans.find(p => p.apiId && p.allowOpenApi)
  if (planWithApi && planWithApi.apiId) {
    selectedPlanId.value = planWithApi.apiId
    selectedPlanName.value = planWithApi.name
  } else if (plans.length > 0) {
    // Fallback to first plan ID if no API ID configured
    selectedPlanId.value = plans[0].id
    selectedPlanName.value = plans[0].name
  }
}, { immediate: true })

const updateSelectedKey = (key: string) => {
  const k = apiKeys.value.find((k: any) => k.key === key)
  if (k) {
    selectedKey.value = k.key
    selectedKeyName.value = k.name
  }
}

const updateSelectedPlan = (id: string) => {
  // Check if it's an API ID or internal ID
  const plan = testPlans.value.find(p => p.apiId === id || p.id === id)
  if (plan) {
    selectedPlanId.value = plan.apiId || plan.id
    selectedPlanName.value = plan.name
  }
}

const copyToClipboard = async (text: string) => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      showToast('复制成功', 'success')
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      showToast('复制成功', 'success')
    }
  } catch (_e) {
    showToast('复制失败', 'error')
  }
}

const getOpenApiCurl = () => {
  const c = getSelectedPlanConcurrency()
  return `curl -X POST http://localhost:18080/api/openapi/plans/${selectedPlanId.value}/execute?concurrency=${c} \\
  -H 'X-API-KEY: ${selectedKey.value}' \\
  -H 'Content-Type: application/json'`
}

const getJenkinsPipeline = () => {
  return `pipeline {
  stage('Run Tests') {
    steps {
      sh "curl -X POST http://localhost:18080/api/plans/${selectedPlanId.value}/execute -H 'X-API-KEY: ${selectedKey.value}'"
    }
  }
}`
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

    <Dialog :open="isEditDialogOpen" @update:open="isEditDialogOpen = $event">
      <DialogContent class="max-w-2xl h-[90vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>编辑测试计划</DialogTitle>
          <DialogDescription>
            修改测试计划信息
          </DialogDescription>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto pr-1">
          <CreateTestPlanForm
            v-if="editingPlan"
            :mode="'edit'"
            :plan="editingPlan"
            @close="isEditDialogOpen = false"
            @success="handleEditSuccess"
          />
        </div>
      </DialogContent>
    </Dialog>

    <!-- CI/CD Integration -->
    <Card class="border-purple-200 bg-purple-50">
      <CardHeader>
        <div class="flex items-center justify-between">
          <div>
            <CardTitle class="text-purple-900">CI/CD 集成</CardTitle>
            <CardDescription class="text-purple-700">
              通过 OpenAPI 或 Webhook 集成到您的 CI/CD 流程
            </CardDescription>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm text-purple-800 font-medium">选择密钥:</span>
            <Select v-model="selectedKey" @update:modelValue="updateSelectedKey">
              <SelectTrigger class="w-[200px] bg-white border-purple-200 text-purple-900 h-8">
                <SelectValue :placeholder="selectedKeyName" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="key in apiKeys" :key="key.id" :value="key.key">
                  {{ key.name }}
                </SelectItem>
              </SelectContent>
            </Select>
            <span class="text-sm text-purple-800 font-medium ml-4">选择计划:</span>
            <Select :modelValue="selectedPlanId" @update:modelValue="updateSelectedPlan">
              <SelectTrigger class="w-[200px] bg-white border-purple-200 text-purple-900 h-8">
                <SelectValue :placeholder="selectedPlanName" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="plan in testPlans" :key="plan.id" :value="plan.apiId || plan.id">
                  {{ plan.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <Tabs default-value="curl" class="w-full">
          <TabsList class="grid w-full grid-cols-2 bg-purple-100 p-1">
            <TabsTrigger value="curl" class="data-[state=active]:bg-white data-[state=active]:text-purple-700 data-[state=active]:shadow-sm">OpenAPI 触发</TabsTrigger>
            <TabsTrigger value="jenkins" class="data-[state=active]:bg-white data-[state=active]:text-purple-700 data-[state=active]:shadow-sm">Jenkins 集成</TabsTrigger>
          </TabsList>
          
          <TabsContent value="curl" class="mt-4">
            <div class="bg-gray-900 rounded-lg p-4 relative group overflow-hidden">
              <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                <Button variant="ghost" size="icon" class="h-8 w-8 text-gray-400 hover:text-white hover:bg-gray-800" @click="copyToClipboard(getOpenApiCurl())">
                  <Copy class="w-4 h-4" />
                </Button>
              </div>
              <div class="font-mono text-xs leading-relaxed">
                <div class="text-purple-400"># 立即执行测试计划 ({{ selectedPlanName }})</div>
                <div class="text-yellow-400">curl</div> <span class="text-gray-300">-X POST</span> <span class="text-green-400">http://localhost:18080/api/openapi/plans/{{ selectedPlanId }}/execute?concurrency={{ getSelectedPlanConcurrency() }}</span> \
                <div class="pl-4"><span class="text-gray-300">-H</span> <span class="text-green-400">"X-API-KEY: <span class="text-white font-bold">{{ selectedKey }}</span>"</span> \</div>
                <div class="pl-4"><span class="text-gray-300">-H</span> <span class="text-green-400">"Content-Type: application/json"</span></div>
              </div>
            </div>
          </TabsContent>

          <TabsContent value="jenkins" class="mt-4">
            <div class="bg-gray-900 rounded-lg p-4 relative group overflow-hidden">
              <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                <Button variant="ghost" size="icon" class="h-8 w-8 text-gray-400 hover:text-white hover:bg-gray-800" @click="copyToClipboard(getJenkinsPipeline())">
                  <Copy class="w-4 h-4" />
                </Button>
              </div>
              <div class="font-mono text-xs leading-relaxed text-gray-300">
                <div class="text-blue-400">pipeline</div> {
                <div class="pl-4"><span class="text-blue-400">stage</span>(<span class="text-green-400">'Run Tests'</span>) {</div>
                <div class="pl-8"><span class="text-blue-400">steps</span> {</div>
                <div class="pl-12"><span class="text-yellow-400">sh</span> <span class="text-green-400">"curl -X POST http://localhost:18080/api/openapi/plans/{{ selectedPlanId }}/execute \</span></div>
                <div class="pl-16"><span class="text-green-400">-H 'X-API-KEY: <span class="text-white font-bold">{{ selectedKey }}</span>'"</span></div>
                <div class="pl-8">}</div>
                <div class="pl-4">}</div>
                <div>}</div>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>

    <!-- Test Plans List -->
    <Card>
      <CardHeader>
        <CardTitle>测试计划列表</CardTitle>
        <CardDescription>管理和执行所有测试计划</CardDescription>
      </CardHeader>
      <CardContent>
        <div class="flex items-center justify-between mb-4">
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
          <div class="flex items-center gap-3">
            <label class="flex items-center gap-2 text-sm text-gray-600">
              <input
                type="checkbox"
                class="rounded border-gray-300"
                :checked="isAllSelected"
                @change="toggleSelectAll"
              />
              全选本页
            </label>
            <Button
              variant="outline"
              class="text-red-600 border-red-200 hover:bg-red-50"
              :disabled="selectedPlanIds.length === 0"
              @click="handleBatchDelete"
            >
              <Trash2 class="w-4 h-4 mr-2" />
              删除选中
            </Button>
            <Button
              variant="outline"
              class="text-red-600 border-red-200 hover:bg-red-50"
              @click="handleDeleteAll"
            >
              <Trash2 class="w-4 h-4 mr-2" />
              删除全部
            </Button>
          </div>
        </div>

        <div class="space-y-4">
          <Card v-for="plan in testPlans" :key="plan.id" class="hover:shadow-md transition-shadow">
            <CardContent class="pt-6">
              <div class="flex flex-col md:flex-row md:items-start justify-between gap-4">
                <div class="space-y-2 flex-1">
                  <div class="flex items-center gap-3">
                    <input
                      type="checkbox"
                      class="rounded border-gray-300"
                      :value="plan.id"
                      v-model="selectedPlanIds"
                    />
                    <h3 class="font-semibold text-lg">{{ plan.name }}</h3>
                    <Badge :variant="plan.status === 'active' ? 'default' : 'secondary'">
                      {{ plan.status === 'active' ? '启用' : '禁用' }}
                    </Badge>
                    <Badge v-if="plan.environment" variant="outline">{{ plan.environment }}</Badge>
                  </div>
                  <p class="text-sm text-gray-500">{{ plan.description }}</p>
                  
                  <div class="flex flex-wrap gap-4 text-xs text-gray-500 pt-2">
                    <div class="flex items-center gap-1">
                      <Calendar class="w-3 h-3" />
                      上次运行: {{ plan.lastRunTime ? new Date(plan.lastRunTime).toLocaleString() : (plan.lastRun || '从未') }}
                      <span v-if="plan.lastRunStatus" :class="{
                        'text-green-600': plan.lastRunStatus === 'Success',
                        'text-red-600': plan.lastRunStatus === 'Failed',
                        'text-blue-600': plan.lastRunStatus === 'Running'
                      }" class="ml-1 font-medium">
                        [{{ plan.lastRunStatus }}]
                      </span>
                    </div>
                    <div v-if="plan.cronExpression" class="flex items-center gap-1 text-blue-600">
                      <Clock class="w-3 h-3" />
                      定时任务: {{ plan.cronExpression }}
                    </div>
                    <div v-if="plan.successRate !== undefined" :class="getSuccessRateColor(plan.successRate)">
                      成功率: {{ plan.successRate }}%
                    </div>
                    <div v-if="plan.apiId" class="flex items-center gap-2 ml-2">
                      <span class="font-mono bg-gray-100 px-2 py-0.5 rounded text-gray-600">{{ plan.apiId }}</span>
                      <span v-if="plan.allowOpenApi" class="text-green-600 text-[10px] border border-green-200 bg-green-50 px-1 rounded">OpenAPI 开启</span>
                      <span v-else class="text-gray-400 text-[10px] border border-gray-200 bg-gray-50 px-1 rounded">OpenAPI 关闭</span>
                    </div>
                  </div>
                </div>

                <div class="flex items-center gap-2">
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500">并发</span>
                <Select v-model="concurrencyMap[plan.id]">
                  <SelectTrigger class="w-[90px] h-8">
                    <SelectValue placeholder="并发" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1">1</SelectItem>
                    <SelectItem value="2">2</SelectItem>
                    <SelectItem value="3">3</SelectItem>
                    <SelectItem value="5">5</SelectItem>
                    <SelectItem value="8">8</SelectItem>
                  </SelectContent>
                </Select>
              </div>
                  <Button size="sm" class="bg-green-600 hover:bg-green-700" @click="handleExecute(plan.id)">
                    <Play class="w-4 h-4 mr-2" />
                    执行
                  </Button>
                  <Button variant="outline" size="sm" @click="handleDuplicatePlan(plan)">
                    <Copy class="w-4 h-4 mr-2" />
                    复制计划
                  </Button>
                  <Button variant="outline" size="icon" title="复制 curl" @click="handleCopyPlan(plan)">
                    <Copy class="w-4 h-4" />
                  </Button>
                  <Button variant="outline" size="icon" title="编辑" @click="handleEditPlan(plan)">
                    <Pencil class="w-4 h-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="icon"
                    class="text-red-600 hover:text-red-700"
                    @click="handleDelete(plan.id)"
                  >
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
          
          <div v-if="testPlans.length === 0" class="text-center py-8 text-gray-500">
            暂无测试计划，请点击"创建测试计划"
          </div>

          <div v-if="totalCount > 0" class="flex items-center justify-between border-t border-gray-100 pt-4 mt-2">
            <div class="text-sm text-gray-600">
              共 {{ totalCount }} 条，当前第 {{ currentPage }} / {{ totalPages }} 页
            </div>
            <div class="flex items-center gap-3">
              <div class="flex items-center">
                <select
                  v-model="pageSizeStr"
                  class="h-10 w-[120px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  <option v-for="s in pageSizeOptions" :key="s" :value="s">
                    {{ s }} 条/页
                  </option>
                </select>
              </div>
              <Button variant="outline" size="sm" :disabled="currentPage === 1" @click="goPrev">上一页</Button>
              <div class="flex items-center gap-1">
                <Button
                  v-for="p in Math.min(5, totalPages)"
                  :key="p"
                  variant="outline"
                  size="sm"
                  :class="p === currentPage ? 'bg-blue-50 border-blue-500 text-blue-600' : ''"
                  @click="setPage(p)"
                >
                  {{ p }}
                </Button>
              </div>
              <Button variant="outline" size="sm" :disabled="currentPage === totalPages" @click="goNext">下一页</Button>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
