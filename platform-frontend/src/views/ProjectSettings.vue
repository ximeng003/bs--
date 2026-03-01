<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useProjectStore } from '@/store/projectStore'
import request from '@/api/request'
import Button from '@/components/ui/button/Button.vue'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Plus, Trash2, Copy, Key } from 'lucide-vue-next'
import { showToast } from '@/lib/notify'

const projectStore = useProjectStore()

// --- Variables ---
const variables = ref<any[]>([])
const showVarDialog = ref(false)
const varForm = ref({
  id: undefined as number | undefined,
  keyName: '',
  value: '',
  description: ''
})

const fetchVariables = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const res = await request.get('/project/variables', {
      params: { projectId: projectStore.currentProject.id }
    })
    variables.value = res.records || []
  } catch (e: any) {
    showToast(e.message || '获取变量失败', 'error')
  }
}

const handleSaveVariable = async () => {
  if (!projectStore.currentProject?.id) return
  if (!varForm.value.keyName || !varForm.value.value) {
    showToast('请输入键名和值', 'error')
    return
  }
  
  try {
    const payload = {
      ...varForm.value,
      projectId: projectStore.currentProject.id
    }
    
    if (varForm.value.id) {
      await request.put('/project/variables', payload)
      showToast('更新成功', 'success')
    } else {
      await request.post('/project/variables', payload)
      showToast('创建成功', 'success')
    }
    showVarDialog.value = false
    fetchVariables()
    resetVarForm()
  } catch (e: any) {
    showToast(e.message || '保存失败', 'error')
  }
}

const handleDeleteVariable = async (id: number) => {
  if (!confirm('确定删除该变量吗？')) return
  try {
    await request.delete(`/project/variables/${id}`)
    showToast('删除成功', 'success')
    fetchVariables()
  } catch (e: any) {
    showToast(e.message || '删除失败', 'error')
  }
}

const resetVarForm = () => {
  varForm.value = {
    id: undefined,
    keyName: '',
    value: '',
    description: ''
  }
}

const openEditVar = (v: any) => {
  varForm.value = { ...v }
  showVarDialog.value = true
}

// --- API Keys ---
const apiKeys = ref<any[]>([])
const showKeyDialog = ref(false)
const keyForm = ref({
  description: '',
  expiresAt: '' // YYYY-MM-DD HH:mm:ss
})
const generatedKey = ref('')

const fetchApiKeys = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const res = await request.get('/project/keys', {
      params: { projectId: projectStore.currentProject.id }
    })
    apiKeys.value = res.records || []
  } catch (e: any) {
    showToast(e.message || '获取密钥失败', 'error')
  }
}

const handleGenerateKey = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const payload = {
      projectId: projectStore.currentProject.id,
      description: keyForm.value.description,
      expiresAt: keyForm.value.expiresAt ? keyForm.value.expiresAt.replace('T', ' ') : null
    }
    const res = await request.post('/project/keys', payload)
    showToast('密钥生成成功', 'success')
    generatedKey.value = res.apiKey
    showKeyDialog.value = false
    fetchApiKeys()
    keyForm.value = { description: '', expiresAt: '' }
  } catch (e: any) {
    showToast(e.message || '生成失败', 'error')
  }
}

const handleDeleteKey = async (id: number) => {
  if (!confirm('确定删除该密钥吗？')) return
  try {
    await request.delete(`/project/keys/${id}`)
    showToast('删除成功', 'success')
    fetchApiKeys()
  } catch (e: any) {
    showToast(e.message || '删除失败', 'error')
  }
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text)
  showToast('已复制', 'success')
}

// Watch for project changes
watch(() => projectStore.currentProject, (newVal) => {
  if (newVal?.id) {
    fetchVariables()
    fetchApiKeys()
  }
}, { immediate: true })

onMounted(() => {
  // Initial fetch handled by watcher with immediate: true
})
</script>

<template>
  <div class="p-6">
    <div v-if="!projectStore.currentProject" class="flex flex-col items-center justify-center h-[60vh] text-gray-500">
      <div class="mb-4 text-lg">请先选择一个项目</div>
      <p class="text-sm">在左侧菜单选择项目以进行设置</p>
    </div>

    <div v-else>
      <div class="mb-6 flex justify-between items-center">
      <div>
        <h1 class="text-3xl font-bold">项目设置</h1>
        <p class="text-gray-500 mt-1">
          当前项目: <span class="font-medium text-primary">{{ projectStore.currentProject?.name }}</span>
        </p>
      </div>
    </div>

    <Tabs default-value="variables" class="space-y-6">
      <TabsList>
        <TabsTrigger value="variables">项目变量</TabsTrigger>
        <TabsTrigger value="apikeys">API 密钥</TabsTrigger>
      </TabsList>

      <!-- Variables Tab -->
      <TabsContent value="variables">
        <Card>
          <CardHeader class="flex flex-row items-center justify-between">
            <div>
              <CardTitle>项目变量</CardTitle>
              <CardDescription>管理仅在当前项目中生效的环境变量 (优先级高于全局变量)</CardDescription>
            </div>
            <Button @click="() => { resetVarForm(); showVarDialog = true }">
              <Plus class="w-4 h-4 mr-2" /> 新增变量
            </Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>变量名</TableHead>
                  <TableHead>值</TableHead>
                  <TableHead>描述</TableHead>
                  <TableHead class="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="v in variables" :key="v.id">
                  <TableCell class="font-mono">{{ v.keyName }}</TableCell>
                  <TableCell class="max-w-[200px] truncate" :title="v.value">{{ v.value }}</TableCell>
                  <TableCell>{{ v.description || '-' }}</TableCell>
                  <TableCell class="text-right space-x-2">
                    <Button variant="ghost" size="sm" @click="openEditVar(v)">编辑</Button>
                    <Button variant="ghost" size="sm" class="text-red-500" @click="handleDeleteVariable(v.id)">
                      <Trash2 class="w-4 h-4" />
                    </Button>
                  </TableCell>
                </TableRow>
                <TableRow v-if="variables.length === 0">
                  <TableCell colspan="4" class="text-center text-gray-500 py-8">暂无变量</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </TabsContent>

      <!-- API Keys Tab -->
      <TabsContent value="apikeys">
        <Card>
          <CardHeader class="flex flex-row items-center justify-between">
            <div>
              <CardTitle>API 密钥</CardTitle>
              <CardDescription>用于通过 API 触发该项目的测试计划</CardDescription>
            </div>
            <Button @click="showKeyDialog = true">
              <Key class="w-4 h-4 mr-2" /> 生成密钥
            </Button>
          </CardHeader>
          <CardContent class="space-y-4">
            <div v-if="generatedKey" class="bg-green-50 p-4 rounded border border-green-200 mb-4 flex justify-between items-center">
              <div>
                <p class="text-sm text-green-800 font-bold">新密钥生成成功！请立即保存，之后将不再显示。</p>
                <code class="text-lg font-mono text-green-900 mt-1 block">{{ generatedKey }}</code>
              </div>
              <Button variant="outline" size="sm" @click="copyToClipboard(generatedKey)">
                <Copy class="w-4 h-4 mr-2" /> 复制
              </Button>
            </div>

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>密钥描述</TableHead>
                  <TableHead>创建人</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>过期时间</TableHead>
                  <TableHead class="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="k in apiKeys" :key="k.id">
                  <TableCell>{{ k.description || '未命名密钥' }}</TableCell>
                  <TableCell>{{ k.userId }}</TableCell>
                  <TableCell>{{ new Date(k.createdAt).toLocaleString() }}</TableCell>
                  <TableCell>{{ k.expiresAt ? new Date(k.expiresAt).toLocaleString() : '永久有效' }}</TableCell>
                  <TableCell class="text-right">
                    <Button variant="ghost" size="sm" class="text-red-500" @click="handleDeleteKey(k.id)">
                      <Trash2 class="w-4 h-4" />
                    </Button>
                  </TableCell>
                </TableRow>
                <TableRow v-if="apiKeys.length === 0">
                  <TableCell colspan="5" class="text-center text-gray-500 py-8">暂无密钥</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>

    <!-- Variable Dialog -->
    <div v-if="showVarDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md shadow-xl">
        <h3 class="text-lg font-semibold mb-4">{{ varForm.id ? '编辑变量' : '新增变量' }}</h3>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>变量名 (Key)</Label>
            <Input v-model="varForm.keyName" placeholder="例如: BASE_URL" :disabled="!!varForm.id" />
          </div>
          <div class="space-y-2">
            <Label>变量值 (Value)</Label>
            <Input v-model="varForm.value" placeholder="变量值" />
          </div>
          <div class="space-y-2">
            <Label>描述</Label>
            <Input v-model="varForm.description" placeholder="可选描述" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <Button variant="outline" @click="showVarDialog = false">取消</Button>
          <Button @click="handleSaveVariable">保存</Button>
        </div>
      </div>
    </div>

    <!-- Key Dialog -->
    <div v-if="showKeyDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md shadow-xl">
        <h3 class="text-lg font-semibold mb-4">生成新 API 密钥</h3>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>描述</Label>
            <Input v-model="keyForm.description" placeholder="例如: CI Pipeline Key" />
          </div>
          <div class="space-y-2">
            <Label>过期时间 (可选)</Label>
            <Input type="datetime-local" v-model="keyForm.expiresAt" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <Button variant="outline" @click="showKeyDialog = false">取消</Button>
          <Button @click="handleGenerateKey">生成</Button>
        </div>
      </div>
    </div>
  </div>
</div>
</template>