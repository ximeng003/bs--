<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from '@/lib/notify'
import { generateReportHtml } from '@/utils/exportReport'
import { Card, CardContent } from '@/components/ui/card'
import Badge from '@/components/ui/badge/Badge.vue'
import Button from '@/components/ui/button/Button.vue'
import { CheckCircle, XCircle, Clock, Download, Share2, AlertCircle, FileText } from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import request from '@/api/request'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])
type LogLevel = 'info' | 'success' | 'error'

interface ReportDetail {
  id: number
  planId?: number | null
  planRunNo?: number | null
  caseId?: number | null
  caseName?: string | null
  caseType?: string | null
  status?: string | null
  executionTime?: number | null
  logs?: string | null
  executedAt?: string | null
  executedBy?: string | null
  environment?: string | null
}

interface PlanItemForReport {
  reportId: number
  caseId?: number | null
  caseName?: string | null
  caseType?: string | null
  status?: string | null
  durationMs?: number | null
}

interface PlanSummaryForReport {
  planId: number
  planName?: string | null
  environment?: string | null
  total: number
  success: number
  failed: number
  durationMs: number
  avgDurationMs: number
  items: PlanItemForReport[]
}

interface ConsoleLog {
  time: string
  level: LogLevel
  message: string
}

interface ExecutionStep {
  step: number
  action: string
  status: 'success' | 'failed'
  time: string
  details: string
  error?: boolean
}

interface RawTestCase {
  id: number
  type?: string | null
  content?: string | null
  name?: string | null
  description?: string | null
  environment?: string | null
}

const formatDuration = (ms?: number | null) => {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}

const route = useRoute()
const router = useRouter()
const report = ref<ReportDetail | null>(null)
const testCase = ref<RawTestCase | null>(null)
const planSummary = ref<PlanSummaryForReport | null>(null)

const formatEnvironment = (env: string | null | undefined) => {
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

const parsedCaseContent = computed(() => {
  if (!testCase.value || !testCase.value.content) {
    return null as unknown
  }
  const raw = String(testCase.value.content).trim()
  if (!raw) {
    return null as unknown
  }
  try {
    return JSON.parse(raw) as unknown
  } catch {
    return raw as unknown
  }
})

const logLines = computed(() => {
  if (!report.value || !report.value.logs) {
    return [] as string[]
  }
  return String(report.value.logs)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
})

const consoleLogs = computed<ConsoleLog[]>(() => {
  if (!logLines.value.length) {
    return []
  }
  return logLines.value.map((line, index) => {
    const lower = line.toLowerCase()
    let level: LogLevel = 'info'
    if (/(error|失败|异常|fail)/i.test(lower)) {
      level = 'error'
    } else if (/(success|成功|pass|通过|ok)/i.test(lower)) {
      level = 'success'
    }
    return {
      time: `${index + 1}`.padStart(2, '0'),
      level,
      message: line
    }
  })
})

const apiErrorText = computed(() => {
  const lines = logLines.value
  if (!lines.length) {
    return ''
  }
  const found = lines.find((line) => /assert/i.test(line)) || ''
  return found
})

function applyStepStatuses(
  steps: ExecutionStep[],
  executedCount: number,
  isSuccess: boolean
): ExecutionStep[] {
  if (!steps.length) {
    return steps
  }
  if (isSuccess) {
    return steps.map((s) => ({
      ...s,
      status: 'success',
      error: false
    }))
  }
  const total = steps.length
  const executed = Math.max(0, Math.min(executedCount, total))
  const failIndex = executed < total ? executed : total - 1
  return steps.map((s, idx) => {
    if (idx < executed) {
      return {
        ...s,
        status: 'success',
        error: false
      }
    }
    if (idx === failIndex) {
      return {
        ...s,
        status: 'failed',
        error: true
      }
    }
    return {
      ...s,
      status: 'failed',
      error: false
    }
  })
}

function buildApiSteps(
  content: any,
  isSuccess: boolean,
  errorText: string
): ExecutionStep[] {
  const steps: ExecutionStep[] = []
  if (!content || typeof content !== 'object') {
    return steps
  }
  const method = String((content.method || 'GET') as string).toUpperCase()
  const url = String(content.url || '')
  let index = 1
  steps.push({
    step: index++,
    action: `发送请求 ${method} ${url}`,
    status: 'success',
    time: '',
    details: `Method: ${method} URL: ${url}`,
    error: false
  })
  const assertions = Array.isArray(content.assertions)
    ? content.assertions
    : []
  const activeAssertions = assertions.filter(
    (a: any) => a && a.active !== false
  )
  assertions.forEach((a: any) => {
    if (!a || (a.active === false)) {
      return
    }
    const t = String(a.type || '').toLowerCase()
    if (t === 'status') {
      const expected = a.value
      steps.push({
        step: index++,
        action: '断言状态码',
        status: 'success',
        time: '',
        details: `期望状态码 = ${expected}`,
        error: false
      })
    } else if (t === 'json') {
      const path = a.path
      const expected = a.value
      steps.push({
        step: index++,
        action: '断言 JSON 字段',
        status: 'success',
        time: '',
        details: `路径 ${path} = ${JSON.stringify(expected)}`,
        error: false
      })
    }
  })
  if (isSuccess || !activeAssertions.length) {
    return steps.map((s) => ({
      ...s,
      status: 'success',
      error: false
    }))
  }
  let failIndex = -1
  const lower = errorText.toLowerCase()
  if (lower.startsWith('status assert failed')) {
    const match = errorText.match(/expected\s+(\d+)/i)
    const expected = match ? match[1] : null
    if (expected != null) {
      activeAssertions.forEach((a: any, idx: number) => {
        if (
          String((a.type || '')).toLowerCase() === 'status' &&
          String(a.value) === expected &&
          failIndex < 0
        ) {
          failIndex = 1 + idx
        }
      })
    }
  } else if (lower.startsWith('json assert failed at')) {
    const match = errorText.match(/at\s+(.+)$/i)
    const path = match ? match[1].trim() : ''
    if (path) {
      activeAssertions.forEach((a: any, idx: number) => {
        if (
          String((a.type || '')).toLowerCase() === 'json' &&
          String(a.path || '') === path &&
          failIndex < 0
        ) {
          failIndex = 1 + idx
        }
      })
    }
  } else if (lower.startsWith('json assert error')) {
    activeAssertions.forEach((a: any, idx: number) => {
      if (
        String((a.type || '')).toLowerCase() === 'json' &&
        failIndex < 0
      ) {
        failIndex = 1 + idx
      }
    })
  } else if (lower.startsWith('time assert failed')) {
    activeAssertions.forEach((a: any, idx: number) => {
      if (
        String((a.type || '')).toLowerCase() === 'time' &&
        failIndex < 0
      ) {
        failIndex = 1 + idx
      }
    })
  }
  if (failIndex < 0) {
    failIndex = steps.length - 1
  }
  return steps.map((s, idx) => {
    if (idx < failIndex) {
      return {
        ...s,
        status: 'success',
        error: false
      }
    }
    if (idx === failIndex) {
      return {
        ...s,
        status: 'failed',
        error: true
      }
    }
    return {
      ...s,
      status: 'failed',
      error: false
    }
  })
}

function buildWebSteps(
  content: unknown,
  executedCount: number,
  isSuccess: boolean
): ExecutionStep[] {
  let script = ''
  if (typeof content === 'string') {
    script = content
  } else if (content && typeof content === 'object') {
    const anyContent = content as any
    if (typeof anyContent.script === 'string') {
      script = anyContent.script
    }
  }
  const lines = script
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
  const steps: ExecutionStep[] = lines.map((line, idx) => {
    const original = line
    let action = ''
    let details = ''
    if (line.startsWith('打开URL:')) {
      const url = line.split(':', 1)[0] ? line.split(':', 2)[1] || '' : ''
      action = '打开页面'
      details = url.trim()
    } else if (line.startsWith('刷新页面')) {
      action = '刷新页面'
      details = ''
    } else if (/click\(/i.test(line) || line.startsWith('点击元素') || /点击/i.test(line)) {
      action = '点击元素'
      details = line
    } else if (/input/i.test(line) || /send_keys/i.test(line) || /输入/.test(line)) {
      action = '输入文本'
      details = line
    } else if (/assert/i.test(line) || /断言/.test(line)) {
      action = '断言'
      details = line
    } else if (/wait/i.test(line) || /sleep/i.test(line) || /等待/.test(line)) {
      action = '等待'
      details = line
    } else {
      action = original.length > 40 ? `${original.slice(0, 40)}...` : original
      details = original
    }
    return {
      step: idx + 1,
      action,
      status: 'success',
      time: '',
      details: details || action,
      error: false
    }
  })
  return applyStepStatuses(steps, executedCount, isSuccess)
}

function buildAppSteps(
  content: unknown,
  executedCount: number,
  isSuccess: boolean
): ExecutionStep[] {
  if (!content || typeof content !== 'object') {
    return []
  }
  const anyContent = content as any
  const rawSteps = Array.isArray(anyContent.steps) ? anyContent.steps : []
  const steps: ExecutionStep[] = rawSteps.map((s: any, idx: number) => {
    const actionType = String(s?.action || '').toLowerCase()
    const by = s?.by ? String(s.by) : ''
    const value =
      s?.value || s?.selector || s?.xpath || s?.path || s?.file || s?.name || ''
    const text = s?.text ? String(s.text) : ''
    let action = ''
    let details = ''
    if (actionType === 'click') {
      action = '点击元素'
      details = value ? `${by || 'selector'}: ${value}` : ''
    } else if (actionType === 'input') {
      action = '输入文本'
      details = text ? `输入 "${text}"` : ''
    } else if (actionType === 'wait') {
      action = '等待元素'
      const ms = s?.ms != null ? Number(s.ms) : 1000
      details = `等待 ${ms} ms`
    } else if (actionType === 'back') {
      action = '返回'
    } else if (actionType === 'launch') {
      action = '启动应用'
    } else if (actionType === 'close') {
      action = '关闭应用'
    } else if (actionType === 'screenshot') {
      action = '截图'
      details = value ? String(value) : 'screenshot.png'
    } else if (actionType === 'assert_exists') {
      action = '断言元素存在'
      details = value ? `${by || 'selector'}: ${value}` : ''
    } else {
      action = actionType || '步骤'
    }
    const detailText = details || action
    return {
      step: idx + 1,
      action,
      status: 'success',
      time: '',
      details: detailText,
      error: false
    }
  })
  return applyStepStatuses(steps, executedCount, isSuccess)
}

const executionSteps = computed<ExecutionStep[]>(() => {
  const r = report.value
  const tc = testCase.value
  const caseType = (tc?.type || r?.caseType || '').toUpperCase()
  const status = (r?.status || '').toLowerCase()
  const isSuccess = status === 'success'
  const executedCount = logLines.value.length

  if (caseType === 'API') {
    const content = parsedCaseContent.value
    return buildApiSteps(content as any, isSuccess, apiErrorText.value)
  }

  if (caseType === 'WEB') {
    const content = parsedCaseContent.value
    return buildWebSteps(content, executedCount, isSuccess)
  }

  if (caseType === 'APP') {
    const content = parsedCaseContent.value
    return buildAppSteps(content, executedCount, isSuccess)
  }

  if (!consoleLogs.value.length) {
    return []
  }
  return consoleLogs.value.map((log, index) => ({
    step: index + 1,
    action: log.message,
    status: log.level === 'error' ? 'failed' : 'success',
    time: '',
    details: log.message,
    error: log.level === 'error'
  }))
})

const reportData = computed(() => {
  const r = report.value
  const status = r?.status || 'failed'
  const caseName =
    r?.caseName || (r?.caseId ? `测试用例 #${r.caseId}` : '测试报告')
  const executionTime = r?.executedAt
    ? String(r.executedAt).replace('T', ' ')
    : ''
  const duration =
    typeof r?.executionTime === 'number' && r.executionTime >= 0
      ? `${r.executionTime}ms`
      : '-'
  const engine = r?.executedBy || 'System'
  const environment = formatEnvironment(r?.environment || '')

  const totalSteps =
    executionSteps.value.length > 0 ? executionSteps.value.length : 1
  const failedSteps = executionSteps.value.filter(
    (item) => item.status === 'failed'
  ).length
  const passedSteps =
    totalSteps - failedSteps >= 0 ? totalSteps - failedSteps : 0

  return {
    status,
    caseName,
    executionTime,
    duration,
    engine,
    environment,
    totalSteps,
    passedSteps,
    failedSteps
  }
})

const planSuccessRate = computed(() => {
  const data = planSummary.value
  if (!data || !data.total) return 0
  return Math.round((data.success * 10000) / data.total) / 100
})

const planPieOption = computed(() => {
  const data = planSummary.value
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

const planItems = computed(() => {
  return planSummary.value?.items || []
})

const fetchReport = async () => {
  const id = route.params.id as string
  if (!id) {
    return
  }
  try {
    const res: any = await request.get(`/reports/${id}`)
    const detail = res as ReportDetail
    report.value = detail
    if (detail.planId && detail.planRunNo) {
      try {
        const pageRes: any = await request.get('/reports', {
          params: { page: 1, size: 1000, planId: detail.planId, planRunNo: detail.planRunNo }
        })
        const records = (pageRes && pageRes.records) ? pageRes.records as any[] : []
        if (records.length > 0) {
          let total = 0
          let successCount = 0
          let failedCount = 0
          let totalDuration = 0
          const items: PlanItemForReport[] = []
          records.forEach((r: any) => {
            if (!r || r.caseId == null) {
              return
            }
            total++
            const status = String(r.status || '').toLowerCase()
            if (status === 'success') {
              successCount++
            } else {
              failedCount++
            }
            if (typeof r.executionTime === 'number') {
              totalDuration += r.executionTime
            }
            items.push({
              reportId: r.id,
              caseId: r.caseId,
              caseName: r.caseName || null,
              caseType: r.caseType || null,
              status: r.status || null,
              durationMs: typeof r.executionTime === 'number' ? r.executionTime : null
            })
          })
          if (total > 0) {
            planSummary.value = {
              planId: detail.planId as number,
              planName: records[0].planName || null,
              environment: records[0].environment || null,
              total,
              success: successCount,
              failed: failedCount,
              durationMs: totalDuration,
              avgDurationMs: total > 0 ? Math.round(totalDuration / total) : 0,
              items
            }
          }
        }
      } catch (e) {
        console.error(e)
      }
    }
    if (detail.caseId) {
      try {
        const caseRes: any = await request.get(`/testcases/${detail.caseId}`)
        testCase.value = caseRes as RawTestCase
      } catch (e) {
        console.error(e)
      }
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(fetchReport)

const goBackToReports = () => {
  router.push('/reports')
}

const selectedReportItem = ref<any>(null)
const isDetailLoading = ref(false)
const currentTab = ref('steps')
const formatJson = ref(true)

const formatBody = (content: any, pretty: boolean = true) => {
  if (!content) return ''
  try {
    const obj = typeof content === 'string' ? JSON.parse(content) : content
    return pretty ? JSON.stringify(obj, null, 2) : JSON.stringify(obj)
  } catch {
    return String(content)
  }
}

const fetchAndProcessItem = async (item: PlanItemForReport) => {
  try {
    // 1. 获取报告详情 (包含 logs)
    const reportRes: any = await request.get(`/reports/${item.reportId}`)
    const reportDetail = reportRes as ReportDetail
    
    // 2. 获取测试用例定义 (包含 url, method, body, script 等)
    let testCaseContent: any = null
    let testCaseType = item.caseType || 'API'
    
    if (item.caseId) {
      try {
        const caseRes: any = await request.get(`/testcases/${item.caseId}`)
        const rawCase = caseRes as RawTestCase
        testCaseType = rawCase.type || testCaseType
        
        if (rawCase.content) {
          const raw = String(rawCase.content).trim()
          try {
            testCaseContent = JSON.parse(raw)
          } catch {
            testCaseContent = raw
          }
        }
      } catch (e) {
        console.warn(`Failed to fetch test case ${item.caseId}`, e)
      }
    }
    
    // 3. 解析日志
    const logsStr = String(reportDetail.logs || '')
    const logLines = logsStr.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0)
    
    // 4. 构建步骤 (复用组件内的逻辑)
    const isSuccess = (reportDetail.status || '').toLowerCase() === 'success'
    const type = (testCaseType || '').toUpperCase()
    let steps: ExecutionStep[] = []
    
    if (type === 'API') {
      const errorText = logLines.find((line) => /assert/i.test(line)) || ''
      steps = buildApiSteps(testCaseContent, isSuccess, errorText)
    } else if (type === 'WEB') {
      steps = buildWebSteps(testCaseContent, logLines.length, isSuccess)
    } else if (type === 'APP') {
      steps = buildAppSteps(testCaseContent, logLines.length, isSuccess)
    } else {
      // Fallback steps from logs
      steps = logLines.map((msg, idx) => ({
        step: idx + 1,
        action: msg,
        status: /error|fail/i.test(msg) ? 'failed' : 'success',
        time: '',
        details: msg,
        error: /error|fail/i.test(msg)
      }))
    }
    
    // 5. 提取请求信息
    let url = item.caseName
    let method = ''
    let requestBody = ''
    let requestHeaders = {}
    let statusCode = isSuccess ? 200 : 500
    
    // 尝试从日志中提取 Response Body
    let responseBody = ''
    const responseStartIdx = logLines.findIndex(l => /Response Body:/i.test(l))
    if (responseStartIdx !== -1) {
       const line = logLines[responseStartIdx]
       // 简单的单行提取尝试
       const match = line.match(/Response Body:\s*(.*)/i)
       if (match && match[1]) {
          responseBody = match[1].trim()
       }
       // 如果单行没内容，或者是 JSON 对象的开始，可能跨行
       if ((!responseBody || responseBody === '{' || responseBody === '[') && responseStartIdx + 1 < logLines.length) {
          // 简单地把后面几行当做 body，直到遇到下一个明显不是 body 的行 (这里简化处理，只取后一行或几行)
          // 实际情况日志可能很复杂，这里只做简单尝试
          const nextLines = logLines.slice(responseStartIdx + 1, responseStartIdx + 10) // 取最多10行
          responseBody = nextLines.join('\n')
       }
    }
    
    if (type === 'API' && testCaseContent && typeof testCaseContent === 'object') {
      url = testCaseContent.url || url
      method = (testCaseContent.method || 'GET').toUpperCase()
      requestBody = testCaseContent.body || ''
      requestHeaders = testCaseContent.headers || {}
    } else if (logLines.length > 0) {
      // 尝试从日志第一行提取 method url
      // 假设日志格式: POST http://...
      const first = logLines[0]
      if (/^(GET|POST|PUT|DELETE|PATCH)\s+http/i.test(first)) {
        const parts = first.split(/\s+/)
        if (parts.length >= 2) {
          method = parts[0].toUpperCase()
          url = parts[1]
        }
      }
    }

    return {
      ...item,
      url: url || item.caseName || `Test Case #${item.caseId}`,
      method: method || (type === 'API' ? 'GET' : type),
      statusCode,
      requestBody,
      requestHeaders,
      responseBody: responseBody || '', 
      responseHeaders: {},
      logs: logLines,
      steps
    }
  } catch (err) {
    console.error(`Process item ${item.reportId} error:`, err)
    return item // 降级返回基础信息
  }
}

const selectReportItem = async (item: PlanItemForReport) => {
  if (selectedReportItem.value?.reportId === item.reportId) return
  
  isDetailLoading.value = true
  selectedReportItem.value = null
  currentTab.value = 'steps'
  
  try {
    selectedReportItem.value = await fetchAndProcessItem(item)
  } catch (e) {
    console.error(e)
    showToast('获取详情失败', 'error')
  } finally {
    isDetailLoading.value = false
  }
}

const handleExport = async () => {
  if (!planSummary.value) {
    showToast('暂无报告数据可导出', 'warning')
    return
  }
  
  showToast('正在获取完整报告数据，请稍候...', 'info')
  
  try {
    const items = planSummary.value.items || []
    
    // 并行获取每个用例的详细报告和定义，以生成真实数据
    const detailedItemsPromises = items.map(fetchAndProcessItem)
    
    const detailedItems = await Promise.all(detailedItemsPromises)

    const exportData = {
      reportDetail: report.value,
      planSummary: planSummary.value,
      planItems: detailedItems,
      exportedAt: new Date().toISOString()
    }
    
    const htmlContent = generateReportHtml(exportData)
    const blob = new Blob([htmlContent], { type: 'text/html' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `report-plan-${planSummary.value.planId}-${report.value?.planRunNo || 'latest'}.html`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast('报告导出成功', 'success')
  } catch (e) {
    console.error('Export failed:', e)
    showToast('报告导出失败', 'error')
  }
}

const handleShare = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href)
    showToast('链接已复制到剪贴板', 'success')
  } catch (e) {
    console.error('Copy failed:', e)
    showToast('复制链接失败，请手动复制', 'error')
  }
}
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="mb-2">
      <Button
        variant="outline"
        size="sm"
        class="flex items-center gap-1"
        @click="goBackToReports"
      >
        <span>←</span>
        <span>返回测试报告页面</span>
      </Button>
    </div>
    <Card v-if="planSummary && report?.planId" class="border-gray-200">
      <CardContent class="pt-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <h2 class="text-xl font-semibold text-gray-900">
              测试计划执行报告
            </h2>
            <Badge variant="outline" class="text-xs">
              计划 ID: {{ planSummary.planId }}
            </Badge>
            <Badge v-if="planSummary.planName" variant="outline" class="text-xs">
              {{ planSummary.planName }}
            </Badge>
            <Badge v-if="report?.planRunNo" variant="outline" class="text-xs">
              第 {{ report?.planRunNo }} 次执行报告
            </Badge>
            <Badge v-if="planSummary.environment" variant="outline" class="text-xs">
              环境: {{ formatEnvironment(planSummary.environment || '') }}
            </Badge>
          </div>
          <div class="flex gap-2">
            <Button variant="outline" size="sm" class="border-gray-300" @click="handleExport">
              <Download class="w-4 h-4 mr-2" />
              导出报告
            </Button>
            <Button variant="outline" size="sm" class="border-gray-300" @click="handleShare">
              <Share2 class="w-4 h-4 mr-2" />
              分享
            </Button>
          </div>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
          <div class="flex justify-center">
            <div class="relative w-40 h-40">
              <VChart :option="planPieOption" autoresize class="w-40 h-40" />
              <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                <div class="text-xs text-gray-500">通过率</div>
                <div class="text-2xl font-semibold text-gray-900">
                  {{ planSuccessRate }}%
                </div>
              </div>
            </div>
          </div>
          <div class="space-y-2">
            <div class="flex items-center gap-2 text-sm">
              <CheckCircle class="w-4 h-4 text-green-500" />
              <span class="text-gray-600">通过用例：</span>
              <span class="font-semibold text-green-700">{{ planSummary.success }}</span>
              <span class="text-xs text-gray-400">
                ({{ planSummary.total ? Math.round((planSummary.success * 10000) / planSummary.total) / 100 : 0 }}%)
              </span>
            </div>
            <div class="flex items-center gap-2 text-sm">
              <XCircle class="w-4 h-4 text-red-500" />
              <span class="text-gray-600">失败用例：</span>
              <span class="font-semibold text-red-700">{{ planSummary.failed }}</span>
            </div>
            <div class="flex items-center gap-2 text-sm">
              <Clock class="w-4 h-4 text-gray-500" />
              <span class="text-gray-600">总耗时：</span>
              <span class="font-semibold text-gray-900">{{ formatDuration(planSummary.durationMs) }}</span>
            </div>
            <div class="flex items-center gap-2 text-sm">
              <span class="text-gray-600">平均单用例耗时：</span>
              <span class="font-semibold text-gray-900">{{ formatDuration(planSummary.avgDurationMs) }}</span>
            </div>
          </div>
          <div class="space-y-2 text-sm text-gray-600">
            <div>已执行用例数：<span class="font-semibold text-gray-900">{{ planSummary.total }}</span></div>
            <div>执行人：<span class="font-semibold text-gray-900">{{ report?.executedBy || '当前登录用户' }}</span></div>
          </div>
        </div>
        <div class="mt-6 flex flex-col lg:flex-row gap-6 h-[800px]">
          <!-- Left Side: Case List -->
          <div class="w-full lg:w-1/3 border rounded-lg overflow-hidden flex flex-col bg-white shadow-sm">
            <div class="p-3 bg-gray-50 border-b font-medium text-sm flex justify-between items-center">
              <span>测试用例列表 ({{ planItems.length }})</span>
            </div>
            <div class="flex-1 overflow-y-auto">
              <div
                v-for="(item, index) in planItems"
                :key="item.reportId || index"
                @click="selectReportItem(item)"
                class="p-3 border-b hover:bg-gray-50 cursor-pointer transition-colors"
                :class="selectedReportItem?.reportId === item.reportId ? 'bg-blue-50 border-l-4 border-l-blue-500' : 'border-l-4 border-l-transparent'"
              >
                <div class="flex justify-between items-start mb-1">
                  <div class="font-medium text-sm text-gray-900 truncate flex-1 mr-2" :title="item.caseName || `用例 #${item.caseId}`">
                    {{ index + 1 }}. {{ item.caseName || `用例 #${item.caseId}` }}
                  </div>
                  <Badge 
                    variant="outline" 
                    class="text-xs shrink-0"
                    :class="String(item.status || '').toLowerCase() === 'success' 
                      ? 'text-green-700 bg-green-50 border-green-200' 
                      : 'text-red-700 bg-red-50 border-red-200'"
                  >
                    {{ String(item.status || '').toLowerCase() === 'success' ? 'PASS' : 'FAIL' }}
                  </Badge>
                </div>
                <div class="flex justify-between items-center text-xs text-gray-500">
                  <div class="flex gap-2">
                    <span class="bg-gray-100 px-1.5 py-0.5 rounded">{{ (item.caseType || 'API').toUpperCase() }}</span>
                    <span>ID: {{ item.caseId || '-' }}</span>
                  </div>
                  <span>{{ formatDuration(item.durationMs) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Right Side: Details -->
          <div class="w-full lg:w-2/3 border rounded-lg overflow-hidden flex flex-col bg-white shadow-sm">
            <div v-if="!selectedReportItem && !isDetailLoading" class="flex-1 flex flex-col items-center justify-center text-gray-400">
              <FileText class="w-16 h-16 mb-4 opacity-20" />
              <p>请在左侧选择一个测试用例查看详情</p>
            </div>
            
            <div v-else-if="isDetailLoading" class="flex-1 flex items-center justify-center">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>

            <div v-else class="flex flex-col h-full">
              <!-- Detail Header -->
              <div class="p-4 border-b bg-gray-50">
                <div class="flex items-start gap-3 mb-2">
                  <Badge 
                    :class="selectedReportItem.method === 'GET' ? 'bg-blue-100 text-blue-800' : 
                            selectedReportItem.method === 'POST' ? 'bg-green-100 text-green-800' : 
                            selectedReportItem.method === 'DELETE' ? 'bg-red-100 text-red-800' : 
                            'bg-gray-100 text-gray-800'"
                    class="mt-0.5"
                  >
                    {{ selectedReportItem.method }}
                  </Badge>
                  <div class="flex-1 break-all font-mono text-sm text-gray-700">
                    {{ selectedReportItem.url }}
                  </div>
                </div>
                <div class="flex gap-4 text-xs text-gray-500">
                  <span class="flex items-center gap-1">
                    <span class="w-2 h-2 rounded-full" :class="selectedReportItem.statusCode >= 200 && selectedReportItem.statusCode < 300 ? 'bg-green-500' : 'bg-red-500'"></span>
                    Status: {{ selectedReportItem.statusCode }}
                  </span>
                  <span class="flex items-center gap-1">
                    <Clock class="w-3 h-3" />
                    Time: {{ formatDuration(selectedReportItem.durationMs) }}
                  </span>
                </div>
              </div>

              <!-- Tabs -->
              <div class="flex border-b bg-white sticky top-0 z-10">
                <button 
                  v-for="tab in ['steps', 'request', 'response', 'logs']" 
                  :key="tab"
                  @click="currentTab = tab"
                  class="px-4 py-2 text-sm font-medium border-b-2 transition-colors capitalize"
                  :class="currentTab === tab ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'"
                >
                  {{ tab }}
                </button>
              </div>

              <!-- Tab Content -->
              <div class="flex-1 overflow-y-auto p-4 bg-gray-50">
                <!-- Steps Tab -->
                <div v-if="currentTab === 'steps'" class="space-y-4">
                  <div v-if="selectedReportItem.steps && selectedReportItem.steps.length > 0">
                    <div 
                      v-for="step in selectedReportItem.steps" 
                      :key="step.step"
                      class="bg-white p-3 rounded border border-gray-100 shadow-sm"
                    >
                      <div class="flex items-start gap-3">
                        <div class="mt-0.5">
                          <CheckCircle v-if="step.status === 'success'" class="w-5 h-5 text-green-500" />
                          <XCircle v-else class="w-5 h-5 text-red-500" />
                        </div>
                        <div class="flex-1">
                          <div class="text-sm font-medium text-gray-900">{{ step.action }}</div>
                          <div class="text-xs text-gray-500 mt-1 font-mono break-all">{{ step.details }}</div>
                        </div>
                        <span class="text-xs text-gray-400">{{ step.step }}</span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="text-center text-gray-400 py-8">暂无步骤信息</div>
                </div>

                <!-- Request Tab -->
                <div v-else-if="currentTab === 'request'" class="space-y-4">
                  <div v-if="selectedReportItem.requestHeaders && Object.keys(selectedReportItem.requestHeaders).length > 0">
                    <h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">Headers</h4>
                    <pre class="bg-white p-3 rounded border border-gray-200 text-xs overflow-x-auto">{{ formatBody(selectedReportItem.requestHeaders) }}</pre>
                  </div>
                  <div>
                    <h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">Body</h4>
                    <pre v-if="selectedReportItem.requestBody" class="bg-white p-3 rounded border border-gray-200 text-xs overflow-x-auto">{{ formatBody(selectedReportItem.requestBody) }}</pre>
                    <div v-else class="text-gray-400 text-sm italic">No request body</div>
                  </div>
                </div>

                <!-- Response Tab -->
                <div v-else-if="currentTab === 'response'" class="space-y-4">
                  <div class="flex justify-end mb-2">
                    <button 
                      @click="formatJson = !formatJson"
                      class="text-xs px-2 py-1 border rounded bg-white hover:bg-gray-50"
                    >
                      {{ formatJson ? 'Show Raw' : 'Format JSON' }}
                    </button>
                  </div>
                  <pre v-if="selectedReportItem.responseBody" class="bg-white p-3 rounded border border-gray-200 text-xs overflow-x-auto min-h-[200px]">{{ formatBody(selectedReportItem.responseBody, formatJson) }}</pre>
                  <div v-else class="text-center py-10 text-gray-400 italic border border-dashed rounded bg-white">
                    No response body recorded
                  </div>
                </div>

                <!-- Logs Tab -->
                <div v-else-if="currentTab === 'logs'" class="space-y-2">
                  <div 
                    v-for="(log, idx) in selectedReportItem.logs" 
                    :key="idx"
                    class="font-mono text-xs p-2 rounded bg-white border border-gray-100 break-all"
                  >
                    {{ log }}
                  </div>
                  <div v-if="!selectedReportItem.logs?.length" class="text-center text-gray-400 py-8">暂无日志</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>

    <Card v-else class="border-gray-200">
      <CardContent class="pt-6">
        <div class="flex items-start justify-between mb-6">
          <div>
            <div class="flex items-center gap-3 mb-2">
              <h2 class="text-2xl font-semibold text-gray-900">{{ reportData.caseName }}</h2>
              <Badge v-if="reportData.status === 'success'" class="bg-green-500 text-white px-3 py-1">
                <CheckCircle class="w-4 h-4 mr-1" />
                PASS
              </Badge>
              <Badge v-else class="bg-red-500 text-white px-3 py-1">
                <XCircle class="w-4 h-4 mr-1" />
                FAIL
              </Badge>
            </div>
            <div class="flex items-center gap-4 text-sm text-gray-600">
              <span class="flex items-center gap-1">
                <Clock class="w-4 h-4" />
                {{ reportData.executionTime }}
              </span>
              <span>耗时: {{ reportData.duration }}</span>
              <span>引擎: {{ reportData.engine }}</span>
              <span>环境: {{ reportData.environment }}</span>
            </div>
          </div>
          <div class="flex gap-2">
            <Button variant="outline" size="sm" class="border-gray-300">
              <Download class="w-4 h-4 mr-2" />
              导出报告
            </Button>
            <Button variant="outline" size="sm" class="border-gray-300">
              <Share2 class="w-4 h-4 mr-2" />
              分享
            </Button>
          </div>
        </div>

        <div class="grid grid-cols-4 gap-4 p-4 bg-gray-50 rounded-lg">
          <div class="text-center">
            <div class="text-2xl font-semibold text-gray-900">{{ reportData.totalSteps }}</div>
            <div class="text-sm text-gray-600 mt-1">总步骤数</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-semibold text-green-600">{{ reportData.passedSteps }}</div>
            <div class="text-sm text-gray-600 mt-1">通过步骤</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-semibold text-red-600">{{ reportData.failedSteps }}</div>
            <div class="text-sm text-gray-600 mt-1">失败步骤</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-semibold text-blue-600">
              {{ Math.round((reportData.passedSteps / reportData.totalSteps) * 100) }}%
            </div>
            <div class="text-sm text-gray-600 mt-1">通过率</div>
          </div>
        </div>
      </CardContent>
    </Card>

    <div v-if="!planSummary || !report?.planId" class="grid grid-cols-2 gap-6">
      <!-- Left Column - Execution Steps Timeline -->
      <Card class="border-gray-200">
        <CardContent class="pt-6">
          <h3 class="font-semibold text-gray-900 mb-4">执行步骤时间线</h3>
          <div class="space-y-3 max-h-[600px] overflow-y-auto">
            <div
              v-for="step in executionSteps"
              :key="step.step"
              :class="`relative pl-8 pb-3 border-l-2 ${
                step.status === 'success'
                  ? 'border-green-500'
                  : 'border-red-500'
              }`"
            >
              <!-- Step indicator -->
              <div
                :class="`absolute left-[-9px] top-0 w-4 h-4 rounded-full border-2 ${
                  step.status === 'success'
                    ? 'bg-green-500 border-green-500'
                    : 'bg-red-500 border-red-500'
                }`"
              />
              
              <div class="space-y-1">
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="text-xs text-gray-500">步骤 {{ step.step }}</span>
                      <CheckCircle v-if="step.status === 'success'" class="w-4 h-4 text-green-500" />
                      <XCircle v-else class="w-4 h-4 text-red-500" />
                    </div>
                    <div class="font-medium text-sm text-gray-900 mt-1">
                      {{ step.action }}
                    </div>
                    <div class="text-xs text-gray-600 mt-1">
                      {{ step.details }}
                    </div>
                  </div>
                  <span class="text-xs text-gray-500">{{ step.time }}</span>
                </div>
                
                <div v-if="step.error" class="mt-2 p-2 bg-red-50 border border-red-200 rounded text-xs text-red-800">
                  <div class="flex items-start gap-1">
                    <AlertCircle class="w-3 h-3 mt-0.5 flex-shrink-0" />
                    <span>{{ step.details }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Right Column - Console Logs & Screenshot -->
      <div class="space-y-6">
        <!-- Console Logs -->
        <Card class="border-gray-200">
          <CardContent class="pt-6">
            <h3 class="font-semibold text-gray-900 mb-4">控制台日志</h3>
            <div class="bg-gray-900 text-green-400 p-4 rounded font-mono text-xs h-[300px] overflow-y-auto">
              <div
                v-for="(log, index) in consoleLogs"
                :key="index"
                :class="`mb-1 ${
                  log.level === 'error'
                    ? 'text-red-400'
                    : log.level === 'success'
                    ? 'text-green-400'
                    : 'text-gray-400'
                }`"
              >
                <span class="text-gray-500">[{{ log.time }}]</span> {{ log.message }}
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- Error Screenshot -->
        <Card class="border-gray-200 border-red-300">
          <CardContent class="pt-6">
            <h3 class="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <AlertCircle class="w-5 h-5 text-red-500" />
              错误截图
            </h3>
            <div class="bg-gray-100 border-2 border-dashed border-gray-300 rounded p-8 text-center">
              <div class="space-y-3">
                <div class="w-16 h-16 bg-gray-300 rounded-lg mx-auto flex items-center justify-center">
                  <svg class="w-8 h-8 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm text-gray-600 mb-2">payment_result.png</p>
                  <p class="text-xs text-gray-500">
                    错误发生时的页面截图
                  </p>
                </div>
                <Button size="sm" variant="outline" class="border-gray-300">
                  <Download class="w-4 h-4 mr-2" />
                  下载截图
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>
