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
}

const testCases = ref<TestCase[]>([])
const loading = ref(false)
const searchTerm = ref('')
const filterType = ref('all')
const editDialogOpen = ref(false)
const editingCase = ref<TestCase | null>(null)

const fetchTestCases = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/testcases', {
      params: {
        keyword: searchTerm.value.trim(),
        type: filterType.value === 'all' ? undefined : filterType.value
      }
    })
    // Handle Mybatis Plus Page result: { records: [], total: ... }
    if (res && res.records) {
        testCases.value = res.records
    } else if (Array.isArray(res)) {
        testCases.value = res
    }
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
  if (!confirm('确定要删除这个测试用例吗？')) return
  try {
    await request.delete(`/testcases/${id}`)
    await fetchTestCases()
  } catch (e) {
    alert('删除失败')
  }
}

const handleRun = async (id: string) => {
    // In a real app, this would trigger an async job
    // For now, we simulate success
    alert('开始执行用例 ' + id)
    // You might call an endpoint here: await request.post(`/testcases/${id}/execute`)
}

const navigateToEditor = (id: string, type: string) => {
    if (type === 'API') {
        router.push(`/api-test/edit/${id}`)
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
        alert('保存成功')
    } catch (e) {
        alert('保存失败')
    }
}

const handleCreate = () => {
    // Default to creating API case, user can change type?
    // Or navigate to a create selection page.
    // Given the UI, we'll go to API editor for now as it's the main one.
    router.push('/api-test/edit')
}

onMounted(() => {
  fetchTestCases()
})

watch([searchTerm, filterType], () => {
  fetchTestCases()
})

const filteredTestCases = computed(() => {
  const normalizedSearch = searchTerm.value.trim().toLowerCase()
  return testCases.value.filter(tc => {
    const name = (tc.name || '').toLowerCase()
    const matchesSearch = normalizedSearch === '' || name.includes(normalizedSearch)
    const matchesType = filterType.value === 'all' || tc.type === filterType.value
    return matchesSearch && matchesType
  })
})

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
      </div>
      <Button @click="handleCreate">
        <Plus class="w-4 h-4 mr-2" />
        新建API测试用例
      </Button>
    </div>

    <!-- Test Cases List -->
    <div class="space-y-4">
      <Card v-for="testCase in filteredTestCases" :key="testCase.id" class="hover:shadow-md transition-shadow">
        <CardContent class="p-6">
          <div class="flex items-center justify-between">
            <div class="space-y-1">
              <div class="flex items-center gap-3">
                <h3 class="font-semibold text-lg text-gray-900">{{ testCase.name }}</h3>
                <Badge :class="getTypeBadgeClass(testCase.type)">
                  {{ testCase.type }}
                </Badge>
                <Badge variant="outline" class="text-xs">
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
      <div v-if="filteredTestCases.length === 0 && !loading" class="text-center py-12 text-gray-500 bg-white rounded-lg border border-dashed border-gray-200">
        <div class="flex justify-center mb-4">
          <Search class="w-12 h-12 text-gray-300" />
        </div>
        <p>没有找到匹配的测试用例</p>
      </div>
    </div>
  </div>
</template>
