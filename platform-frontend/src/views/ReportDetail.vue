<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Card, CardContent } from '@/components/ui/card'
import Badge from '@/components/ui/badge/Badge.vue'
import Button from '@/components/ui/button/Button.vue'
import { CheckCircle, XCircle, Clock, Download, Share2, AlertCircle } from 'lucide-vue-next'
import request from '@/api/request'

type LogLevel = 'info' | 'success' | 'error'

interface ReportDetail {
  id: number
  planId?: number | null
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

const route = useRoute()
const report = ref<ReportDetail | null>(null)
const testCase = ref<RawTestCase | null>(null)

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
  const environment = r?.environment || ''

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

const fetchReport = async () => {
  const id = route.params.id as string
  if (!id) {
    return
  }
  try {
    const res = await request.get(`/reports/${id}`)
    const detail = res as ReportDetail
    report.value = detail
    if (detail.caseId) {
      try {
        const caseRes = await request.get(`/testcases/${detail.caseId}`)
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
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- Report Header -->
    <Card class="border-gray-200">
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

        <!-- Statistics -->
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

    <div class="grid grid-cols-2 gap-6">
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
