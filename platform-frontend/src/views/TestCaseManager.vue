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
import { Search, Plus, Pencil, Trash2, Play, Code } from 'lucide-vue-next'
import request from '@/api/request'
import { showToast, openConfirm } from '@/lib/notify'

const router = useRouter()

interface TestCase {
  id: string;
  name: string;
  type: 'API' | 'WEB' | 'APP';
  description: string;
  environment: string;
  status: 'active' | 'inactive';
  lastRun?: string;
  lastResult?: 'success' | 'failed' | 'pending';
  createdAt?: string;
  updatedAt?: string;
}

const testCases = ref<TestCase[]>([])
const loading = ref(false)
const searchTerm = ref('')
const filterType = ref('all')
const SORT_STORAGE_KEY = 'testcase_sort_mode'
const storedSort = typeof window !== 'undefined' ? localStorage.getItem(SORT_STORAGE_KEY) : null
const sortMode = ref<'created' | 'updated'>(storedSort === 'created' ? 'created' : 'updated')
const editDialogOpen = ref(false)
const editingCase = ref<TestCase | null>(null)

const pageSizeStr = ref('10')
const currentPage = ref(1)
const pageSizeOptions = ['10', '50', '100']
const pageSize = computed(() => Number(pageSizeStr.value) || 10)
const totalCount = ref(0)

const selectedCaseIds = ref<string[]>([])
const isAllSelected = computed(
  () => visibleTestCases.value.length > 0 && visibleTestCases.value.every(tc => selectedCaseIds.value.includes(tc.id))
)

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
      testCases.value = res.records.map((r: any) => ({
        ...r,
        createdAt: r.createdAt || r.created_at || r.createdAtStr || r.created || '',
        updatedAt: r.updatedAt || r.updated_at || r.updatedAtStr || r.updated || ''
      }))
    } else if (Array.isArray(res)) {
      totalCount.value = res.length
      testCases.value = res.map((r: any) => ({
        ...r,
        createdAt: r.createdAt || r.created_at || r.createdAtStr || r.created || '',
        updatedAt: r.updatedAt || r.updated_at || r.updatedAtStr || r.updated || ''
      }))
    } else {
      totalCount.value = 0
      testCases.value = []
    }
    selectedCaseIds.value = []
  } catch (e) {
    console.error('Failed to fetch test cases', e)
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
  } catch (e) {
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
  } catch (e) {
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
  } catch (e) {
    showToast('清空失败', 'error')
  }
}

const handleRun = async (id: string) => {
    try {
        const res: any = await request.post(`/testcases/${id}/execute`)
        await fetchTestCases()
        if (res?.status === 'success') {
            showToast('执行成功', 'success')
        } else {
            showToast(res?.error || '执行失败', 'error')
        }
    } catch (e: any) {
        showToast(e?.message || '执行失败', 'error')
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
        await request.put('/testcases', editingCase.value)
        editDialogOpen.value = false
        fetchTestCases()
        showToast('保存成功', 'success')
    } catch (e) {
        showToast('保存失败', 'error')
    }
}

const handleCreate = () => {
    // Default to creating API case, user can change type?
    // Or navigate to a create selection page.
    // Given the UI, we'll go to API editor for now as it's the main one.
    router.push('/api-cases/edit')
}

onMounted(() => {
  fetchTestCases()
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
    default: return ''
  }
}

const getResultText = (result?: string) => {
  switch (result) {
    case 'success': return '成功'
    case 'failed': return '失败'
    case 'pending': return '待执行'
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
            <Input v-model="editingCase.environment" class="col-span-3" />
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
        <Button @click="handleCreate">
          <Plus class="w-4 h-4 mr-2" />
          新建API测试用例
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
                    {{ testCase.environment }}
                  </Badge>
                  <Badge v-if="testCase.lastResult" :class="getResultBadgeColor(testCase.lastResult) + ' text-white border-0'">
                    {{ getResultText(testCase.lastResult) }}
                  </Badge>
                </div>
                <p class="text-sm text-gray-500">{{ testCase.description }}</p>
                <div class="flex items-center gap-4 text-xs text-gray-400 mt-2">
                  <span>ID: {{ testCase.id }}</span>
                  <span v-if="testCase.lastRun">最后运行: {{ testCase.lastRun }}</span>
                  <span v-if="testCase.createdAt">创建时间: {{ fmtTime(testCase.createdAt) }}</span>
                  <span v-if="testCase.updatedAt">更新时间: {{ fmtTime(testCase.updatedAt) }}</span>
                </div>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <Button variant="ghost" size="icon" class="text-green-600 hover:text-green-700 hover:bg-green-50" @click="handleRun(testCase.id)">
                <Play class="w-4 h-4" />
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
