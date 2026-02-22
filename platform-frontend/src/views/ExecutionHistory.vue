<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Search, CircleCheck, CircleX, Clock, CirclePlay, Eye, Trash2 } from 'lucide-vue-next'
import { Progress } from '@/components/ui/progress'
import request from '@/api/request'
import { showToast, openConfirm } from '@/lib/notify'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

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
  caseType?: string;
}

const executions = ref<ExecutionRecord[]>([])
const keyword = ref('')
const statusFilter = ref('all')
const envFilter = ref('all')
const caseTypeFilter = ref('all')
const dateFilter = ref<string>('')
const dateInputKey = ref(0)
const loading = ref(false)
const pageSizeStr = ref('10')
const pageSizeOptions = ['10', '50', '100']
const currentPage = ref(1)
const totalCount = ref(0)
const selectedIds = ref<string[]>([])

const formatEnvironment = (env: string) => {
  if (!env) return ''
  switch (env) {
    case 'dev':
      return '开发环境'
    case 'staging':
      return '测试环境'
    case 'production':
      return '生产环境'
    default:
      return env
  }
}

const pageSize = computed(() => Number(pageSizeStr.value) || 10)

const isAllSelected = computed(
  () => executions.value.length > 0 && executions.value.every(r => selectedIds.value.includes(r.id))
)

const isValidDateFilter = (value: string) => {
  if (!value) return true
  const v = String(value).slice(0, 10).replace(/\//g, '-')
  return /^\d{4}-\d{2}-\d{2}$/.test(v)
}

const fetchReports = async (skipDateValidation = false) => {
    loading.value = true
    try {
        if (!skipDateValidation && dateFilter.value && !isValidDateFilter(dateFilter.value)) {
            totalCount.value = 0
            executions.value = []
            return
        }
        const res: any = await request.get('/reports', {
            params: {
                page: currentPage.value,
                size: pageSize.value,
                keyword: keyword.value,
                status: statusFilter.value,
                date: dateFilter.value || undefined
            }
        })
        if (res && res.records) {
            let records = res.records as any[]
            if (dateFilter.value) {
                const targetDate = String(dateFilter.value).slice(0, 10).replace(/\//g, '-')
                records = records.filter((r: any) => {
                    const executed = String(r.executedAt || r.executed_at || '')
                    const executedDate = executed.slice(0, 10).replace(/\//g, '-')
                    return executedDate === targetDate
                })
            }

            const caseInfoMap: Record<string, { name?: string; type?: string; lastRun?: string }> = {}
            const caseIds: number[] = []
            records.forEach((r: any) => {
                if (r.caseId != null) {
                    const idNum = Number(r.caseId)
                    if (!Number.isNaN(idNum) && !caseIds.includes(idNum)) {
                        caseIds.push(idNum)
                    }
                }
            })
            if (caseIds.length > 0) {
                try {
                    const responses = await Promise.allSettled(
                        caseIds.map(id => request.get(`/testcases/${id}`))
                    )
                    responses.forEach((resItem, idx) => {
                        if (resItem.status === 'fulfilled' && resItem.value) {
                            const id = caseIds[idx]
                            const data = resItem.value as any
                            caseInfoMap[String(id)] = {
                                name: data.name,
                                type: data.type,
                                lastRun: data.lastRun || data.last_run || ''
                            }
                        }
                    })
                } catch (e) {
                    console.error(e)
                }
            }

            totalCount.value = typeof (res as any).total === 'number' ? Number((res as any).total) : records.length
            executions.value = records.map((r: any) => {
                const idStr = String(r.id)
                const caseIdKey = r.caseId != null ? String(r.caseId) : ''
                const isPlan = !!r.planId
                const caseInfo = caseIdKey ? caseInfoMap[caseIdKey] : undefined
                const caseNameFromBackend = r.caseName && String(r.caseName).trim().length > 0 ? String(r.caseName) : ''
                const caseNameFromApi = !isPlan && caseInfo && caseInfo.name && String(caseInfo.name).trim().length > 0
                  ? String(caseInfo.name)
                  : ''
                const planName =
                  isPlan
                    ? (r.planName || `测试计划 #${r.planId}`)
                    : (caseNameFromBackend || caseNameFromApi || `测试用例 #${r.caseId}`)
                const caseTypeRaw = r.caseType || (caseInfo && caseInfo.type) || ''
                const caseType = String(caseTypeRaw || '').toUpperCase()
                const executedAt = r.executedAt || r.executed_at || (caseInfo && caseInfo.lastRun) || r.lastRun || ''
                const executedBy = r.executedBy || r.executed_by || 'System'

                return {
                    id: idStr,
                    planName,
                    type: isPlan ? 'plan' : 'single',
                    status: r.status,
                    startTime: executedAt ? String(executedAt).replace('T', ' ') : '',
                    duration: typeof r.executionTime === 'number' && r.executionTime >= 0 ? `${r.executionTime}ms` : '-',
                    totalCases: 1,
                    passedCases: r.status === 'success' ? 1 : 0,
                    failedCases: r.status === 'failed' ? 1 : 0,
                    engine: 'Server',
                    environment: r.environment || '未设置',
                    executor: executedBy,
                    caseType: caseType || '未知类型'
                }
            })
            selectedIds.value = []
        } else {
             totalCount.value = 0
             executions.value = []
             selectedIds.value = []
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const clearDate = () => {
  dateFilter.value = ''
  dateInputKey.value += 1
  currentPage.value = 1
  fetchReports(true)
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = executions.value.map(r => r.id)
  }
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) {
    showToast('请先选择要删除的测试报告', 'warning')
    return
  }
  const ok = await openConfirm({
    title: '批量删除测试报告',
    message: `确定要删除选中的 ${selectedIds.value.length} 条测试报告吗？此操作不可恢复。`,
    confirmText: '删除',
  })
  if (!ok) return
  try {
    await Promise.all(selectedIds.value.map(id => request.delete(`/reports/${id}`)))
    selectedIds.value = []
    await fetchReports()
  } catch (e) {
    showToast('批量删除失败', 'error')
  }
}

const handleDeleteAll = async () => {
  const ok = await openConfirm({
    title: '清空测试报告',
    message: '确定要删除所有测试报告吗？此操作不可恢复。',
    confirmText: '清空',
  })
  if (!ok) return
  try {
    await request.delete('/reports')
    selectedIds.value = []
    await fetchReports()
  } catch (e) {
    showToast('清空失败', 'error')
  }
}

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

watch([keyword, statusFilter, dateFilter, pageSizeStr], () => {
    currentPage.value = 1
    fetchReports()
})

watch(currentPage, () => {
    fetchReports()
})

onMounted(() => {
    const k = route.query.keyword as string | undefined
    const s = route.query.status as string | undefined
    const d = route.query.date as string | undefined
    if (typeof k === 'string') {
        keyword.value = k
    }
    if (typeof s === 'string') {
        statusFilter.value = s
    }
    if (typeof d === 'string') {
        dateFilter.value = d
    }
    fetchReports()
})

const environmentOptions = computed(() => {
  const set = new Set<string>()
  executions.value.forEach(r => {
    if (r.environment && r.environment !== '未设置') {
      set.add(r.environment)
    }
  })
  return Array.from(set)
})

const caseTypeOptions = computed(() => {
  const set = new Set<string>()
  executions.value.forEach(r => {
    if (r.caseType) {
      set.add(String(r.caseType).toUpperCase())
    }
  })
  return Array.from(set)
})

const filteredExecutions = computed(() => {
  return executions.value.filter(r => {
    const envOk = envFilter.value === 'all' || r.environment === envFilter.value
    const typeOk =
      caseTypeFilter.value === 'all' ||
      String(r.caseType || '').toUpperCase() === caseTypeFilter.value
    return envOk && typeOk
  })
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

const getCaseTypeBadgeClass = (type: string) => {
  const upper = String(type || '').toUpperCase()
  const colors: Record<string, string> = {
    API: 'bg-blue-100 text-blue-800 border-blue-200',
    WEB: 'bg-purple-100 text-purple-800 border-purple-200',
    APP: 'bg-green-100 text-green-800 border-green-200'
  }
  return colors[upper] || 'bg-gray-50 text-gray-600 border-gray-200'
}
</script>

<template>
  <div class="space-y-6 p-6">
    <!-- Filters -->
    <div class="flex items-center justify-between gap-4 bg-white p-4 rounded-lg shadow-sm border border-gray-100">
      <div class="flex items-center gap-4 flex-1">
        <div class="relative flex-1 max-w-md">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
          <Input v-model="keyword" placeholder="搜索执行记录..." class="pl-9" />
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs text-gray-500">日期</span>
          <Input
            :key="dateInputKey"
            v-model="dateFilter"
            type="date"
            class="w-[180px]"
          />
          <Button variant="outline" @click="clearDate">清除日期</Button>
          <span class="text-xs text-gray-400">格式：YYYY-MM-DD</span>
        </div>
        <Select v-model="statusFilter">
          <SelectTrigger class="w-[150px]">
            <SelectValue placeholder="所有状态" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">所有状态</SelectItem>
            <SelectItem value="success">执行成功</SelectItem>
            <SelectItem value="failed">执行失败</SelectItem>
            <SelectItem value="running">执行中</SelectItem>
          </SelectContent>
        </Select>
        <Select v-model="envFilter">
          <SelectTrigger class="w-[150px]">
            <span style="pointer-events: none;">
              {{ envFilter === 'all' ? '所有环境' : formatEnvironment(envFilter) }}
            </span>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">所有环境</SelectItem>
            <SelectItem
              v-for="env in environmentOptions"
              :key="env"
              :value="env"
            >
              {{ formatEnvironment(env) }}
            </SelectItem>
          </SelectContent>
        </Select>
        <Select v-model="caseTypeFilter">
          <SelectTrigger class="w-[150px]">
            <SelectValue placeholder="所有类型" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">所有类型</SelectItem>
            <SelectItem
              v-for="t in caseTypeOptions"
              :key="t"
              :value="t"
            >
              {{ t }}
            </SelectItem>
          </SelectContent>
        </Select>
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
          :disabled="selectedIds.length === 0"
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

    <!-- Execution List -->
    <div class="space-y-4">
      <Card v-for="record in filteredExecutions" :key="record.id" class="hover:shadow-md transition-shadow">
        <CardContent class="p-6">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-3">
              <input
                type="checkbox"
                class="rounded border-gray-300"
                :value="record.id"
                v-model="selectedIds"
              />
              <component
                :is="getStatusIcon(record.status)"
                :class="`w-5 h-5 ${record.status === 'success' ? 'text-green-500' : record.status === 'failed' ? 'text-red-500' : 'text-blue-500'}`"
              />
              <h3 class="font-semibold text-lg text-gray-900">
                {{ record.planName }}
              </h3>
              <Badge :class="getStatusColor(record.status) + ' border'">
                {{ record.status === 'success' ? '成功' : record.status === 'failed' ? '失败' : '执行中' }}
              </Badge>
              <Badge
                v-if="record.caseType"
                :class="getCaseTypeBadgeClass(record.caseType) + ' text-xs'"
              >
                {{ String(record.caseType).toUpperCase() }}
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
              <div>环境: {{ formatEnvironment(record.environment) }}</div>
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

      <!-- Pagination -->
      <div v-if="totalCount > 0" class="flex items-center justify-between bg-white rounded-lg border border-gray-100 px-4 py-3">
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
  </div>
</template>
