<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { CheckCircle, XCircle, Clock, ArrowLeft } from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

interface PlanItem {
  reportId?: number | null
  caseId?: number | null
  caseName?: string
  type?: string
  status?: string
  durationMs?: number | null
}

interface PlanSummary {
  planId: number | string
  planName: string
  environment?: string
  executedBy?: string
  total: number
  success: number
  failed: number
  durationMs?: number
  avgDurationMs?: number
  items?: PlanItem[]
}

const route = useRoute()
const router = useRouter()

const summary = ref<PlanSummary | null>(null)
const loading = ref(false)
const autoRefresh = ref(false)
let refreshTimer: number | null = null
const initialTotal = ref<number | null>(null)
const completedCount = ref<number>(0)
const progressRate = computed(() => {
  if (!initialTotal.value || initialTotal.value <= 0) return 0
  return Math.round((completedCount.value * 10000) / initialTotal.value) / 100
})

const successRate = computed(() => {
  const data = summary.value
  if (!data || !data.total) return 0
  return Math.round((data.success * 10000) / data.total) / 100
})

const pieOption = computed(() => {
  const data = summary.value
  const success = data?.success || 0
  const failed = data?.failed || 0
  return {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      left: 'center',
      data: ['通过', '失败']
    },
    series: [
      {
        name: '用例结果',
        type: 'pie',
        radius: ['55%', '80%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        label: {
          show: false,
          position: 'center'
        },
        labelLine: {
          show: false
        },
        data: [
          { value: success, name: '通过' },
          { value: failed, name: '失败' }
        ]
      }
    ]
  }
})

const cases = computed(() => {
  return summary.value?.items || []
})

const formatDuration = (ms?: number | null) => {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}

const getStatusInfo = (status?: string) => {
  const v = String(status || '').toLowerCase()
  if (v === 'success') {
    return { text: '通过', color: 'text-green-700 bg-green-50 border-green-200' }
  }
  if (v === 'failed') {
    return { text: '失败', color: 'text-red-700 bg-red-50 border-red-200' }
  }
  return { text: '未知', color: 'text-gray-700 bg-gray-50 border-gray-200' }
}

const handleViewReport = (reportId?: number | null) => {
  if (!reportId) return
  router.push(`/reports/${reportId}`)
}

const handleBack = () => {
  router.push('/plans')
}

const rebuildFromReports = (planId: number | string, planName: string, environment: string | undefined, records: any[]) => {
  if (!Array.isArray(records) || records.length === 0) return
  // 选取最新的一次运行（按 planRunNo 最大；若为空则按 executed_at/id 最大）
  let latestRunNo = -1
  records.forEach(r => {
    if (typeof r.planRunNo === 'number') {
      latestRunNo = Math.max(latestRunNo, r.planRunNo)
    }
  })
  let sameRun = records
  if (latestRunNo >= 0) {
    sameRun = records.filter(r => r.planRunNo === latestRunNo)
  }
  const total = sameRun.length
  const success = sameRun.filter(r => String(r.status || '').toLowerCase() === 'success').length
  const failed = total - success
  const totalDuration = sameRun.reduce((acc, r) => acc + (Number(r.executionTime) || 0), 0)
  const items: PlanItem[] = sameRun.map((r: any) => ({
    reportId: r.id,
    caseId: r.caseId,
    caseName: r.caseName || r.caseId,
    type: r.caseType || 'api',
    status: r.status,
    durationMs: typeof r.executionTime === 'number' ? r.executionTime : null
  }))
  completedCount.value = total
  summary.value = {
    planId,
    planName,
    environment,
    total: initialTotal.value ?? total,
    success,
    failed,
    durationMs: totalDuration,
    avgDurationMs: total > 0 ? Math.round(totalDuration / (initialTotal.value ?? total)) : 0,
    items
  }
}

const refreshFromServer = async () => {
  const s = summary.value
  if (!s) return
  loading.value = true
  try {
    const res: any = await (await import('@/api/request')).default.get('/reports', {
      params: { page: 1, size: 200, planId: s.planId }
    })
    if (res && res.records && res.records.length > 0) {
      rebuildFromReports(s.planId, s.planName, s.environment, res.records)
    }
  } catch {}
  loading.value = false
}

const setupAutoRefresh = () => {
  if (refreshTimer != null) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (autoRefresh.value) {
    refreshTimer = window.setInterval(refreshFromServer, 3000)
  }
}

onMounted(() => {
  const planId = route.query.planId as string | undefined
  const key = route.query.key as string | undefined
  if (!planId || !key) {
    return
  }
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) {
      return
    }
    const parsed = JSON.parse(raw) as PlanSummary
    summary.value = parsed
    initialTotal.value = typeof parsed.total === 'number' ? parsed.total : null
    completedCount.value = Array.isArray(parsed.items) ? parsed.items.length : 0
    // 初始加载后开启一次刷新以与后端对齐
    refreshFromServer()
  } catch {
  }
  setupAutoRefresh()
})

watch(autoRefresh, setupAutoRefresh)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center gap-3">
      <Button variant="outline" size="sm" @click="handleBack">
        <ArrowLeft class="w-4 h-4 mr-1" />
        返回测试计划
      </Button>
      <h1 class="text-xl font-semibold text-gray-900">
        测试计划执行报告
      </h1>
    </div>

    <div v-if="!summary" class="text-gray-500 text-sm">
      当前执行结果已失效，请在测试计划列表中重新执行计划查看最新报告。
    </div>

    <div v-else class="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle class="flex items-center gap-3">
            <span>{{ summary.planName }}</span>
            <Badge variant="outline" class="text-xs">
              计划 ID: {{ summary.planId }}
            </Badge>
            <Badge v-if="summary.environment" variant="outline" class="text-xs">
              环境: {{ summary.environment }}
            </Badge>
            <div class="ml-auto flex items-center gap-2">
              <Button size="sm" variant="outline" @click="refreshFromServer" :disabled="loading">
                {{ loading ? '刷新中...' : '手动刷新' }}
              </Button>
              <label class="text-xs text-gray-600 flex items-center gap-1">
                <input type="checkbox" v-model="autoRefresh" />
                自动刷新
              </label>
            </div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
            <div class="flex justify-center">
              <div class="relative w-40 h-40">
                <VChart :option="pieOption" autoresize class="w-40 h-40" />
                <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                  <div class="text-xs text-gray-500">通过率</div>
                  <div class="text-2xl font-semibold text-gray-900">
                    {{ successRate }}%
                  </div>
                </div>
              </div>
            </div>
            <div class="space-y-2">
              <div class="flex items-center gap-2 text-sm">
                <CheckCircle class="w-4 h-4 text-green-500" />
                <span class="text-gray-600">通过用例：</span>
                <span class="font-semibold text-green-700">{{ summary.success }}</span>
                <span class="text-xs text-gray-400">
                  ({{ summary.total ? Math.round((summary.success * 10000) / summary.total) / 100 : 0 }}%)
                </span>
              </div>
              <div class="flex items-center gap-2 text-sm">
                <XCircle class="w-4 h-4 text-red-500" />
                <span class="text-gray-600">失败用例：</span>
                <span class="font-semibold text-red-700">{{ summary.failed }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm">
                <Clock class="w-4 h-4 text-gray-500" />
                <span class="text-gray-600">总耗时：</span>
                <span class="font-semibold text-gray-900">{{ formatDuration(summary.durationMs) }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm">
                <span class="text-gray-600">平均单用例耗时：</span>
                <span class="font-semibold text-gray-900">{{ formatDuration(summary.avgDurationMs) }}</span>
              </div>
              <div class="flex items-center gap-2 text-sm" v-if="initialTotal != null">
                <span class="text-gray-600">执行进度：</span>
                <span class="font-semibold text-gray-900">{{ completedCount }}/{{ initialTotal }}</span>
                <span class="text-xs text-gray-400">({{ progressRate }}%)</span>
              </div>
            </div>
            <div class="space-y-2 text-sm text-gray-600">
              <div>已执行用例数：<span class="font-semibold text-gray-900">{{ summary.total }}</span></div>
              <div>执行人：<span class="font-semibold text-gray-900">{{ summary.executedBy || '当前登录用户' }}</span></div>
              <div class="text-xs text-gray-400">
                详细单用例报告可通过右侧“查看报告”按钮打开。
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>用例执行明细</CardTitle>
        </CardHeader>
        <CardContent>
          <div v-if="cases.length === 0" class="text-sm text-gray-500">
            本次计划执行未找到任何用例结果。
          </div>
          <div v-else class="space-y-2">
            <div class="grid grid-cols-12 text-xs text-gray-500 border-b border-gray-100 pb-2">
              <div class="col-span-1">序号</div>
              <div class="col-span-4">用例名称 / ID</div>
              <div class="col-span-2">类型</div>
              <div class="col-span-2">状态</div>
              <div class="col-span-2">耗时</div>
              <div class="col-span-1 text-right">操作</div>
            </div>
            <div
              v-for="(item, index) in cases"
              :key="item.reportId || index"
              class="grid grid-cols-12 items-center text-sm py-2 border-b border-gray-50"
            >
              <div class="col-span-1 text-xs text-gray-500">
                {{ index + 1 }}
              </div>
              <div class="col-span-4">
                <div class="text-gray-900 truncate">
                  {{ item.caseName || `用例 #${item.caseId}` }}
                </div>
                <div class="text-xs text-gray-400">
                  ID: {{ item.caseId || '-' }}
                </div>
              </div>
              <div class="col-span-2">
                <Badge variant="outline" class="text-xs">
                  {{ (item.type || '未知').toUpperCase() }}
                </Badge>
              </div>
              <div class="col-span-2">
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded-full border text-xs"
                  :class="getStatusInfo(item.status).color"
                >
                  {{ getStatusInfo(item.status).text }}
                </span>
              </div>
              <div class="col-span-2">
                {{ formatDuration(item.durationMs) }}
              </div>
              <div class="col-span-1 text-right">
                <Button
                  v-if="item.reportId"
                  size="sm"
                  variant="outline"
                  class="text-xs"
                  @click="handleViewReport(item.reportId)"
                >
                  查看报告
                </Button>
                <span v-else class="text-xs text-gray-400">-</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
