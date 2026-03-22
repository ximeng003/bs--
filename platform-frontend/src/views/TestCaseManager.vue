<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Badge from '@/components/ui/badge/Badge.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import Label from '@/components/ui/label/Label.vue'
import { Search, Pencil, Trash2, Play, Code, Loader2, Square } from 'lucide-vue-next'
import request from '@/api/request'
import { showToast, openConfirm } from '@/lib/notify'
import Switch from '@/components/ui/switch/Switch.vue'

const router = useRouter()

interface TestCase {
  id: string;
  displayIndex?: number;
  name: string;
  type: 'API' | 'WEB' | 'APP';
  description: string;
  environment: string;
  status: 'active' | 'inactive';
  lastRun?: string;
  lastResult?: 'success' | 'failed' | 'pending' | 'running';
  createdAt?: string;
  updatedAt?: string;
}

interface EnvironmentItem {
  id?: number
  name: string
  keyName: string
}

const testCases = ref<TestCase[]>([])
const loading = ref(false)
const executingCaseId = ref<string | null>(null)
const searchTerm = ref('')
const filterType = ref('all')
const SORT_STORAGE_KEY = 'testcase_sort_mode'
const storedSort = typeof window !== 'undefined' ? localStorage.getItem(SORT_STORAGE_KEY) : null
const sortMode = ref<'created' | 'updated'>(storedSort === 'created' ? 'created' : 'updated')
const editDialogOpen = ref(false)
const editingCase = ref<TestCase | null>(null)
const logDialogOpen = ref(false)
const logDialogTitle = ref('')
const logDialogMessage = ref('')
const logDialogDetail = ref('')
const logDialogTab = ref<'logs' | 'body'>('logs')
const logBodyRaw = ref('')
const isBodyPretty = ref(true)
const lastReportId = ref<number | null>(null)
const lastRunEnv = ref<string>('') 
const lastRunDurationMs = ref<number | null>(null)
const isLogFullscreen = ref(false)
const logSearch = ref('')
const stepsOnly = ref(false)
const coloredLogLines = computed(() => {
  const txt = (logDialogDetail.value || '').toString()
  const lines = txt.split(/\r?\n/)
  const kw = (logSearch.value || '').trim().toLowerCase()
  const out: { text: string; level: 'error'|'warn'|'assert'|'info'; visible: boolean }[] = []
  for (const line of lines) {
    if (!line) continue
    const lower = line.toLowerCase()
    let level: 'error'|'warn'|'assert'|'info' = 'info'
    if (/\b(error|失败)/i.test(line) || lower.includes('[error]')) level = 'error'
    else if (/\b(warn|重试|slow)/i.test(line) || lower.includes('[warn]')) level = 'warn'
    else if (/\b(assert|断言)/i.test(line) || lower.includes('[assert]')) level = 'assert'
    const isStep = /步骤\s*\d+/.test(line) || /\b(断言|assert)\b/i.test(line)
    const matchesSearch = !kw || lower.includes(kw)
    const isVisible = (stepsOnly.value ? isStep : true) && matchesSearch
    out.push({ text: line, level, visible: isVisible })
  }
  return out
})
const copyLogs = async () => {
  const text = logDialogDetail.value || ''
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      showToast('已复制日志', 'success')
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      showToast('已复制日志', 'success')
    }
  } catch {
    showToast('复制失败', 'error')
  }
}

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

const simplifyError = (raw: string | undefined | null): string => {
  if (!raw) return '执行失败'
  if (raw.includes('【驱动版本不匹配】')) {
    return raw.split('\n')[0]
  }
  if (/invalid payload/i.test(raw)) {
    return '执行引擎收到的 JSON 格式不合法，请检查请求 Body 格式'
  }
  const firstLine = raw.split('\n')[0] || raw
  const cutIndex = firstLine.indexOf(' at [Source')
  const base = cutIndex > 0 ? firstLine.slice(0, cutIndex) : firstLine
  return base.length > 160 ? base.slice(0, 160) + '…' : base
}

const pageSizeStr = ref('10')
const currentPage = ref(1)
const pageSizeOptions = ['10', '50', '100']
const pageSize = computed(() => Number(pageSizeStr.value) || 10)
const totalCount = ref(0)

const selectedCaseIds = ref<string[]>([])
const isAllSelected = computed(
  () => visibleTestCases.value.length > 0 && visibleTestCases.value.every(tc => selectedCaseIds.value.includes(tc.id))
)

const sanitizeDesc = (d: any) => {
  const s = (d ?? '').toString().trim()
  if (!s) return ''
  if (s === 'Updated from API Editor' || s === 'Updated from Script Editor') return ''
  return s
}

const fetchTestCases = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/testcases', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        keyword: searchTerm.value.trim(),
        type: filterType.value === 'all' ? undefined : filterType.value,
        sort: sortMode.value
      }
    })
    if (res && res.records) {
      totalCount.value = typeof res.total === 'number' ? res.total : res.records.length
      const indexOffset = (currentPage.value - 1) * pageSize.value
      const mapped = res.records.map((r: any, idx: number) => ({
        ...r,
        description: sanitizeDesc(r.description),
        displayIndex: indexOffset + idx + 1,
        createdAt: r.createdAt || r.created_at || r.createdAtStr || r.created || '',
        updatedAt: r.updatedAt || r.updated_at || r.updatedAtStr || r.updated || ''
      }))
      const sorted = [...mapped].sort((a, b) => {
        const key = sortMode.value === 'created' ? 'createdAt' : 'updatedAt'
        const ta = new Date(String(a[key as 'createdAt' | 'updatedAt'] || '')).getTime() || 0
        const tb = new Date(String(b[key as 'createdAt' | 'updatedAt'] || '')).getTime() || 0
        return tb - ta
      })
      testCases.value = sorted
    } else if (Array.isArray(res)) {
      totalCount.value = res.length
      const indexOffset = (currentPage.value - 1) * pageSize.value
      const mapped = res.map((r: any, idx: number) => ({
        ...r,
        description: sanitizeDesc(r.description),
        displayIndex: indexOffset + idx + 1,
        createdAt: r.createdAt || r.created_at || r.createdAtStr || r.created || '',
        updatedAt: r.updatedAt || r.updated_at || r.updatedAtStr || r.updated || ''
      }))
      const sorted = [...mapped].sort((a, b) => {
        const key = sortMode.value === 'created' ? 'createdAt' : 'updatedAt'
        const ta = new Date(String(a[key as 'createdAt' | 'updatedAt'] || '')).getTime() || 0
        const tb = new Date(String(b[key as 'createdAt' | 'updatedAt'] || '')).getTime() || 0
        return tb - ta
      })
      testCases.value = sorted
    } else {
      totalCount.value = 0
      testCases.value = []
    }
    selectedCaseIds.value = []
  } catch (e: any) {
    console.error('Failed to fetch test cases', e)
    showToast(e.message || '获取用例列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  searchTerm.value = ''
  filterType.value = 'all'
  fetchTestCases()
}

const handleDelete = async (id: string) => {
  const ok = await openConfirm({
    title: '删除测试用例',
    message: '确定要删除这个测试用例吗？此操作不可恢复。',
    confirmText: '删除',
  })
  if (!ok) return
  try {
    await request.delete(`/testcases/${id}`)
    await fetchTestCases()
    showToast('删除成功', 'success')
  } catch (_e) {
    showToast('删除失败', 'error')
  }
}

const handleBatchDelete = async () => {
  if (!selectedCaseIds.value.length) {
    showToast('请先选择要删除的测试用例', 'warning')
    return
  }
  const ok = await openConfirm({
    title: '批量删除测试用例',
    message: `确定要删除选中的 ${selectedCaseIds.value.length} 个测试用例吗？此操作不可恢复。`,
    confirmText: '删除',
  })
  if (!ok) return
  try {
    await Promise.all(selectedCaseIds.value.map(id => request.delete(`/testcases/${id}`)))
    selectedCaseIds.value = []
    await fetchTestCases()
  } catch (_e) {
    showToast('批量删除失败', 'error')
  }
}

const handleDeleteAll = async () => {
  const ok = await openConfirm({
    title: '清空测试用例',
    message: '确定要删除所有测试用例吗？此操作不可恢复。',
    confirmText: '清空',
  })
  if (!ok) return
  try {
    await request.delete('/testcases')
    selectedCaseIds.value = []
    await fetchTestCases()
  } catch (_e) {
    showToast('清空失败', 'error')
  }
}

const handleRun = async (id: string) => {
  if (executingCaseId.value) return // Prevent multiple executions
  executingCaseId.value = id
  showToast('正在执行中，请稍候...', 'info')
  try {
    const res: any = await request.post(`/testcases/${id}/execute`)
    const tc = testCases.value.find(t => t.id === id)
    lastRunEnv.value = tc?.environment ? formatEnvironment(tc.environment) : ''
    lastReportId.value = typeof res?.reportId === 'number' ? res.reportId : null
    lastRunDurationMs.value = typeof res?.durationMs === 'number' ? res.durationMs : null
    logDialogTab.value = 'logs'
    logBodyRaw.value = ''
    await fetchTestCases()
    if (res?.status === 'success') {
      showToast('执行成功', 'success')
      const resp = res?.response || {}
      const statusCode = resp.statusCode ?? resp.status ?? ''
      const duration = res?.durationMs ?? ''
      const aTotal = typeof res?.assertsTotal === 'number' ? res.assertsTotal : null
      const aPass = typeof res?.assertsPassed === 'number' ? res.assertsPassed : null
      const titleParts: string[] = []
      if (statusCode !== '') {
        titleParts.push(`状态码 ${statusCode}`)
      }
      if (duration !== '') {
        titleParts.push(`耗时 ${duration} ms`)
      }
      if (aTotal !== null && aPass !== null) {
        titleParts.push(`断言 ${aPass}/${aTotal}`)
      }
      logDialogTitle.value = titleParts.length ? `执行成功（${titleParts.join('，')}）` : '执行成功'
      logDialogMessage.value = ''
      const logs = typeof res?.logs === 'string' ? res.logs : ''
      let bodyPreview = ''
      if (resp && typeof resp.body === 'string' && resp.body) {
        let pretty = resp.body
        const trimmed = resp.body.trim()
        if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
          try {
            const parsed = JSON.parse(resp.body)
            pretty = JSON.stringify(parsed, null, 2)
          } catch {
          }
        }
        bodyPreview = pretty.length > 2000 ? pretty.slice(0, 2000) + '\n...（已截断）' : pretty
      }
      logBodyRaw.value = bodyPreview || (resp?.body || '')
      logDialogDetail.value = logs || bodyPreview || '执行成功，无额外日志'
      logDialogOpen.value = true
    } else {
      const msg = simplifyError(res?.error)
      showToast('执行失败', 'error')
      const aTotal = typeof res?.assertsTotal === 'number' ? res.assertsTotal : null
      const aPass = typeof res?.assertsPassed === 'number' ? res.assertsPassed : null
      logDialogTitle.value = aTotal !== null && aPass !== null
        ? `执行失败（断言 ${aPass}/${aTotal}）`
        : '执行失败'
      logDialogMessage.value = msg
      const rawError = typeof res?.error === 'string' ? res.error : ''
      const logs = typeof res?.logs === 'string' ? res.logs : ''
      const resp = res?.response || {}
      let body = ''
      if (resp && typeof resp.body === 'string' && resp.body) {
        try {
          const trimmed = resp.body.trim()
          if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
            body = JSON.stringify(JSON.parse(resp.body), null, 2)
          } else {
            body = resp.body
          }
        } catch {
          body = resp.body
        }
      }
      logBodyRaw.value = body
      logDialogDetail.value = logs || rawError || body
      logDialogOpen.value = true
    }
  } catch (e: any) {
    showToast('执行失败', 'error')
    logDialogTitle.value = '执行异常'
    logDialogMessage.value = '后端返回错误，请查看下方详细日志'
    logDialogDetail.value = e?.response?.data || e?.message || ''
    logBodyRaw.value = ''
    logDialogOpen.value = true
  } finally {
    executingCaseId.value = null
  }
}

const handleStop = async (id: string) => {
  try {
    await request.post(`/testcases/${id}/stop`)
    showToast('停止成功！', 'success')
    executingCaseId.value = null
    await fetchTestCases()
  } catch (e: any) {
    showToast(e.message || '停止失败', 'error')
  }
}

const navigateToEditor = (id: string, type: string) => {
    if (type === 'API') {
        router.push(`/api-cases/edit/${id}`)
    } else {
        router.push(`/web-app?id=${id}`) // ScriptEditor uses query param or we can change route
    }
}

const handleEdit = (id: string) => {
    const testCase = testCases.value.find(c => c.id === id)
    if (testCase) {
        editingCase.value = { ...testCase } // Clone to avoid direct mutation
        editDialogOpen.value = true
    }
}

const handleCode = (id: string) => {
    const testCase = testCases.value.find(c => c.id === id)
    if (testCase) {
        navigateToEditor(id, testCase.type)
    }
}

const handleSaveEdit = async () => {
    if (!editingCase.value) return
    try {
        const payload = {
          id: editingCase.value.id,
          name: editingCase.value.name,
          type: editingCase.value.type,
          description: editingCase.value.description,
          environment: editingCase.value.environment,
          status: editingCase.value.status
        }
        await request.put('/testcases', payload)
        editDialogOpen.value = false
        fetchTestCases()
        showToast('保存成功', 'success')
    } catch (_e) {
        showToast('保存失败', 'error')
    }
}

const envOptions = ref<EnvironmentItem[]>([])
const fetchEnvOptions = async () => {
  try {
    const res: any = await request.get('/environments')
    const arr = Array.isArray(res) ? res : []
    envOptions.value = arr.map((e: any) => ({ id: e.id, name: e.name, keyName: e.keyName }))
  } catch {
    envOptions.value = []
  }
}
// DDT Dialog
const ddtDialogOpen = ref(false)
const ddtCaseId = ref<string | null>(null)
const ddtCsv = ref('username,password\nuser1,pass1\nuser2,pass2')
const ddtConcurrency = ref('2')
const openDdtDialog = (id: string) => {
  ddtCaseId.value = id
  ddtDialogOpen.value = true
}
const submitDdt = async () => {
  if (!ddtCaseId.value) return
  try {
    const res: any = await request.post(`/testcases/${ddtCaseId.value}/execute-batch`, {
      csv: ddtCsv.value,
      concurrency: Number(ddtConcurrency.value || '1')
    })
    showToast('批量执行已提交', 'success')
    ddtDialogOpen.value = false
    // Optionally present summary
    logDialogTitle.value = '批量执行摘要'
    logDialogMessage.value = ''
    logDialogDetail.value = JSON.stringify(res, null, 2)
    logDialogOpen.value = true
    await fetchTestCases()
  } catch (e: any) {
    showToast(e?.message || '批量执行失败', 'error')
  }
}

onMounted(() => {
  fetchTestCases()
  fetchEnvOptions()
})

watch([searchTerm, filterType, sortMode, pageSizeStr], () => {
  currentPage.value = 1
  fetchTestCases()
})
watch(sortMode, (val) => {
  try {
    localStorage.setItem(SORT_STORAGE_KEY, val)
  } catch {}
})

watch(currentPage, () => {
  fetchTestCases()
})

const visibleTestCases = computed(() => testCases.value)

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedCaseIds.value = []
  } else {
    selectedCaseIds.value = visibleTestCases.value.map(tc => tc.id)
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

const fmtTime = (v?: string) => {
  if (!v) return ''
  return String(v).replace('T', ' ')
}

const getResultBadgeColor = (result?: string) => {
  switch (result) {
    case 'success': return 'bg-green-500 hover:bg-green-600'
    case 'failed': return 'bg-red-500 hover:bg-red-600'
    case 'pending': return 'bg-yellow-500 hover:bg-yellow-600'
    case 'running': return 'bg-blue-500 hover:bg-blue-600'
    default: return ''
  }
}

const getResultText = (result?: string) => {
  switch (result) {
    case 'success': return '成功'
    case 'failed': return '失败'
    case 'pending': return '待执行'
    case 'running': return '运行中'
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
    <Dialog v-model:open="logDialogOpen">
      <DialogContent :class="isLogFullscreen ? 'sm:max-w-[90vw] w-[90vw] max-h-[80vh]' : 'sm:max-w-[560px]'">
        <DialogHeader>
          <DialogTitle>{{ logDialogTitle || '执行日志' }}</DialogTitle>
        </DialogHeader>
        <div :class="['mt-4 space-y-4', isLogFullscreen ? 'max-w-none mx-0' : 'max-w-[420px] mx-auto']">
          <div class="flex items-center justify-between text-xs text-gray-500" v-if="lastRunEnv || lastRunDurationMs !== null">
            <div v-if="lastRunEnv">环境: {{ lastRunEnv }}</div>
            <div v-if="lastRunDurationMs !== null">耗时: {{ lastRunDurationMs }} ms</div>
          </div>
          <div v-if="logDialogMessage">
            <div class="text-xs text-gray-500 mb-1">错误信息</div>
            <div
              class="rounded-md border border-red-200 bg-red-50 text-xs text-red-800 p-3 max-h-[140px] overflow-y-auto whitespace-pre-wrap break-all"
            >
              {{ logDialogMessage }}
            </div>
          </div>
          <div>
            <div class="flex items-center justify-between mb-1 flex-wrap gap-2">
              <div class="flex items-center gap-3">
                <div class="inline-flex bg-gray-100 rounded p-0.5">
                  <button
                    class="px-3 py-1 text-xs rounded"
                    :class="logDialogTab==='logs' ? 'bg-white text-blue-600 shadow border' : 'text-gray-600'"
                    @click="logDialogTab='logs'">Logs</button>
                  <button
                    class="px-3 py-1 text-xs rounded"
                    :class="logDialogTab==='body' ? 'bg-white text-blue-600 shadow border' : 'text-gray-600'"
                    @click="logDialogTab='body'">Body</button>
                </div>
                <div v-if="logDialogTab==='logs'" class="hidden md:flex items-center gap-2 text-[11px] text-gray-500">
                  <span class="inline-flex items-center gap-1"><span class="inline-block w-2 h-2 rounded-full bg-green-400"></span>ASSERT</span>
                  <span class="inline-flex items-center gap-1"><span class="inline-block w-2 h-2 rounded-full bg-yellow-300"></span>WARN</span>
                  <span class="inline-flex items-center gap-1"><span class="inline-block w-2 h-2 rounded-full bg-red-400"></span>ERROR</span>
                </div>
              </div>
              <div class="flex items-center gap-2 shrink-0" v-if="logDialogTab==='logs'">
                <div class="flex items-center gap-1 text-xs text-gray-600">
                  <Switch v-model="stepsOnly" />
                  <span>仅步骤</span>
                </div>
                <Input v-model="logSearch" placeholder="搜索日志/步骤" class="h-8 w-36 sm:w-44" />
                <Button variant="outline" size="sm" class="h-8 px-3" @click="copyLogs">复制</Button>
                <Button variant="outline" size="sm" class="h-8 px-3" @click="isLogFullscreen = !isLogFullscreen">{{ isLogFullscreen ? '退出全屏' : '全屏' }}</Button>
              </div>
              <div class="flex items-center gap-2" v-else>
                <Button variant="outline" size="sm" @click="isBodyPretty = !isBodyPretty">{{ isBodyPretty ? '原文' : '格式化' }}</Button>
              </div>
            </div>
            <template v-if="logDialogTab==='logs'">
              <div :class="['bg-gray-900 rounded-md p-4 font-mono overflow-y-auto', isLogFullscreen ? 'h-[60vh] text-[14px] leading-7' : 'text-xs min-h-[220px] max-h-[260px]']">
                <div v-for="(l, idx) in coloredLogLines" :key="idx" v-show="l.visible"
                     :class="{
                       'text-red-400': l.level==='error',
                       'text-yellow-300': l.level==='warn',
                       'text-green-400': l.level==='assert',
                       'text-gray-300': l.level==='info'
                     }"
                     class="whitespace-pre-wrap">
                  {{ l.text }}
                </div>
                <div v-if="!coloredLogLines.length" class="text-gray-500">暂无日志</div>
              </div>
            </template>
            <template v-else>
              <div :class="['bg-gray-900 rounded-md p-4 font-mono overflow-y-auto w-full', isLogFullscreen ? 'h-[60vh] text-[14px] leading-7' : 'text-xs min-h-[220px] max-h-[260px]']">
                <pre class="text-gray-200 whitespace-pre-wrap w-full">
{{ isBodyPretty ? logBodyRaw : (logBodyRaw || '') }}
                </pre>
              </div>
            </template>
          </div>
        </div>
        <div class="flex justify-between mt-4 max-w-[420px] mx-auto">
          <div />
          <div class="flex gap-2">
            <Button v-if="lastReportId !== null" variant="outline" @click="() => { router.push(`/reports/${lastReportId}`); }">
              查看详情报告
            </Button>
            <Button @click="logDialogOpen = false">关闭</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
    <!-- DDT Dialog -->
    <Dialog v-model:open="ddtDialogOpen">
      <DialogContent class="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>批量执行（CSV）</DialogTitle>
          <DialogDescription>将首行作为列名，后续行作为变量行进行批量执行</DialogDescription>
        </DialogHeader>
        <div class="space-y-3">
          <Label>CSV 数据</Label>
          <Textarea v-model="ddtCsv" rows="8" placeholder="username,password&#10;u1,p1&#10;u2,p2" />
          <div class="flex items-center gap-2">
            <Label>并发</Label>
            <Select v-model="ddtConcurrency">
              <SelectTrigger class="w-[100px]">
                <SelectValue placeholder="并发" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="1">1</SelectItem>
                <SelectItem value="2">2</SelectItem>
                <SelectItem value="3">3</SelectItem>
                <SelectItem value="5">5</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="flex justify-end gap-2">
            <Button variant="outline" @click="ddtDialogOpen = false">取消</Button>
            <Button class="bg-amber-600 hover:bg-amber-700" @click="submitDdt">开始批量执行</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
    <Dialog v-model:open="editDialogOpen">
      <DialogContent class="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>编辑测试用例</DialogTitle>
          <DialogDescription>修改测试用例信息</DialogDescription>
        </DialogHeader>
        <div class="grid gap-4 py-4" v-if="editingCase">
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">名称</Label>
            <Input v-model="editingCase.name" class="col-span-3" />
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">类型</Label>
            <Select v-model="editingCase.type">
              <SelectTrigger class="col-span-3">
                <SelectValue placeholder="选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="API">API 接口测试</SelectItem>
                <SelectItem value="WEB">Web 自动化</SelectItem>
                <SelectItem value="APP">App 自动化</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">环境</Label>
            <Select v-model="editingCase.environment">
              <SelectTrigger class="col-span-3">
                <SelectValue placeholder="选择环境" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="e in envOptions" :key="e.keyName" :value="e.keyName">
                  {{ e.name }}（{{ e.keyName }}）
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">描述</Label>
            <Input v-model="editingCase.description" class="col-span-3" />
          </div>
        </div>
        <div class="flex justify-end">
          <Button @click="handleSaveEdit">保存</Button>
        </div>
      </DialogContent>
    </Dialog>

    <!-- Search and Filter Bar -->
    <div class="flex items-center justify-between gap-4 bg-white p-4 rounded-lg shadow-sm border border-gray-100">
      <div class="flex items-center gap-4 flex-1">
        <div class="relative flex-1 max-w-md">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
          <Input v-model="searchTerm" placeholder="搜索测试用例..." class="pl-9" />
        </div>
        <Button variant="outline" @click="resetFilters">重置</Button>
        <Select v-model="filterType">
          <SelectTrigger class="w-[180px]">
            <SelectValue placeholder="所有类型" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">所有类型</SelectItem>
            <SelectItem value="API">API 接口测试</SelectItem>
            <SelectItem value="WEB">Web 自动化</SelectItem>
            <SelectItem value="APP">App 自动化</SelectItem>
          </SelectContent>
        </Select>
        <div class="flex items-center gap-1">
          <span class="text-xs text-gray-500">排序</span>
          <Button :variant="sortMode === 'updated' ? undefined : 'outline'" @click="sortMode = 'updated'">
            按更新时间
          </Button>
          <Button :variant="sortMode === 'created' ? undefined : 'outline'" @click="sortMode = 'created'">
            按创建时间
          </Button>
        </div>
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
          :disabled="selectedCaseIds.length === 0"
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

    <!-- Test Cases List -->
    <div class="space-y-4">
      <Card v-for="testCase in visibleTestCases" :key="testCase.id" class="hover:shadow-md transition-shadow">
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div class="flex items-start gap-3">
              <input
                type="checkbox"
                class="mt-1 rounded border-gray-300"
                :value="testCase.id"
                v-model="selectedCaseIds"
              />
              <div class="space-y-1">
                <div class="flex items-center gap-3">
                  <h3 class="font-semibold text-lg text-gray-900">{{ testCase.name }}</h3>
                  <Badge :class="getTypeBadgeClass(testCase.type)">
                    {{ testCase.type }}
                  </Badge>
                  <Badge v-if="testCase.environment" variant="outline" class="text-xs">
                    {{ formatEnvironment(testCase.environment) }}
                  </Badge>
                  <Badge v-if="testCase.lastResult" :class="getResultBadgeColor(testCase.lastResult) + ' text-white border-0'">
                    {{ getResultText(testCase.lastResult) }}
                  </Badge>
                </div>
                <p v-if="testCase.description && String(testCase.description).trim().length"
                   class="text-sm text-gray-500">
                  {{ testCase.description }}
                </p>
                <div class="flex items-center gap-4 text-xs text-gray-400 mt-2">
                  <span>序号: #{{ testCase.displayIndex ?? (Number(testCase.id) || 0) }}</span>
                  <span v-if="testCase.lastRun">最后运行: {{ testCase.lastRun }}</span>
                  <span v-if="testCase.createdAt">创建时间: {{ fmtTime(testCase.createdAt) }}</span>
                  <span v-if="testCase.updatedAt">更新时间: {{ fmtTime(testCase.updatedAt) }}</span>
                </div>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <Button 
                v-if="executingCaseId === testCase.id || testCase.lastResult === 'running'"
                variant="ghost" 
                size="icon" 
                class="text-red-600 hover:text-red-700 hover:bg-red-50" 
                @click="handleStop(testCase.id)"
                title="停止执行"
              >
                <Square class="w-4 h-4" />
              </Button>
              <Button 
                v-else
                variant="ghost" 
                size="icon" 
                class="text-green-600 hover:text-green-700 hover:bg-green-50" 
                @click="handleRun(testCase.id)"
              >
                <Play class="w-4 h-4" />
              </Button>
              <Button variant="ghost" size="sm" class="text-amber-600 hover:text-amber-700 hover:bg-amber-50" @click="openDdtDialog(testCase.id)">
                批量执行
              </Button>
              <Button variant="ghost" size="icon" class="text-blue-600 hover:text-blue-700 hover:bg-blue-50" @click="handleEdit(testCase.id)">
                <Pencil class="w-4 h-4" />
              </Button>
              <Button variant="ghost" size="icon" class="text-purple-600 hover:text-purple-700 hover:bg-purple-50" @click="handleCode(testCase.id)">
                <Code class="w-4 h-4" />
              </Button>
              <Button variant="ghost" size="icon" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="handleDelete(testCase.id)">
                <Trash2 class="w-4 h-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
      
      <!-- Empty State -->
      <div v-if="totalCount === 0 && !loading" class="text-center py-12 text-gray-500 bg-white rounded-lg border border-dashed border-gray-200">
        <div class="flex justify-center mb-4">
          <Search class="w-12 h-12 text-gray-300" />
        </div>
        <p>没有找到匹配的测试用例</p>
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
