<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useProjectStore } from '@/store/projectStore'
import request from '@/api/request'
import Button from '@/components/ui/button/Button.vue'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import Switch from '@/components/ui/switch/Switch.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Plus, Trash2, Copy, Eye, EyeOff } from 'lucide-vue-next'
import { showToast } from '@/lib/notify'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { Badge } from '@/components/ui/badge'
import { userStore } from '@/store/userStore'

const projectStore = useProjectStore()

const canEdit = computed(() => {
  const user = userStore.user
  if (user?.role === 'admin') return true
  return projectStore.currentProject?.role === 'admin'
})

const confirmDialog = ref({
  open: false,
  title: '',
  description: '',
  variant: 'default' as 'default' | 'destructive',
  confirmAction: (() => {}) as () => void | Promise<void>
})

const openConfirm = (title: string, description: string, action: () => void | Promise<void>, variant: 'default' | 'destructive' = 'default') => {
  confirmDialog.value = {
    open: true,
    title,
    description,
    variant,
    confirmAction: action
  }
}

const handleConfirmAction = async () => {
  await confirmDialog.value.confirmAction()
  confirmDialog.value.open = false
}

interface Environment {
  id?: number
  name: string
  keyName: string
  baseUrl: string
  databaseName: string
  active: boolean
}

const environments = ref<Environment[]>([])
const showEnvDialog = ref(false)
const envForm = ref<Environment>({
  id: undefined,
  name: '',
  keyName: '',
  baseUrl: '',
  databaseName: '',
  active: false,
})

const resetEnvForm = () => {
  envForm.value = {
    id: undefined,
    name: '',
    keyName: '',
    baseUrl: '',
    databaseName: '',
    active: false,
  }
}

const fetchEnvironments = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const res: any = await request.get('/environments')
    environments.value = Array.isArray(res) ? res : []
  } catch (e: any) {
    showToast(e.message || '获取环境失败', 'error')
  }
}

const openCreateEnv = () => {
  resetEnvForm()
  showEnvDialog.value = true
}

const openEditEnv = (env: Environment) => {
  envForm.value = { ...env }
  showEnvDialog.value = true
}

const applyActiveUniqueness = async (activeEnvId?: number) => {
  const pendingUpdates = environments.value
    .map(e => ({ ...e, active: e.id === activeEnvId }))
    .filter((next, idx) => environments.value[idx]?.active !== next.active)

  for (const env of pendingUpdates) {
    await request.put('/environments', env)
  }
}

const handleSaveEnvironment = async () => {
  if (!projectStore.currentProject?.id) return
  if (!envForm.value.name || !envForm.value.keyName || !envForm.value.baseUrl) {
    showToast('请填写环境名称、Key 与 Base URL', 'error')
    return
  }

  const isCreate = !envForm.value.id
  const shouldBeActive = envForm.value.active
  const editingId = envForm.value.id
  const editingKeyName = envForm.value.keyName

  try {
    if (envForm.value.id) {
      await request.put('/environments', envForm.value)
      showToast('更新成功', 'success')
    } else {
      const payload: Environment = {
        name: envForm.value.name,
        keyName: envForm.value.keyName,
        baseUrl: envForm.value.baseUrl,
        databaseName: envForm.value.databaseName,
        active: envForm.value.active,
      }
      await request.post('/environments', payload)
      showToast('创建成功', 'success')
    }

    showEnvDialog.value = false
    resetEnvForm()
    await fetchEnvironments()

    if (shouldBeActive) {
      if (isCreate) {
        const created = environments.value.find(e => e.keyName === editingKeyName)
        if (created?.id) {
          await applyActiveUniqueness(created.id)
          await fetchEnvironments()
        }
      } else if (editingId) {
        await applyActiveUniqueness(editingId)
        await fetchEnvironments()
      }
    }
  } catch (e: any) {
    showToast(e.message || '保存失败', 'error')
  }
}

const handleActivateEnv = (env: Environment) => {
  if (!env.id) return
  openConfirm('切换环境', `确定将环境切换为「${env.name}」吗？`, async () => {
    try {
      await request.put('/environments', { ...env, active: true })
      await fetchEnvironments()
      await applyActiveUniqueness(env.id)
      await fetchEnvironments()
      showToast('环境已切换', 'success')
    } catch (e: any) {
      showToast(e.message || '切换失败', 'error')
    }
  })
}

const handleDeleteEnv = (id: number) => {
  openConfirm('删除环境', '确定删除该环境吗？', async () => {
    try {
      await request.delete(`/environments/${id}`)
      showToast('删除成功', 'success')
      fetchEnvironments()
    } catch (e: any) {
      showToast(e.message || '删除失败', 'error')
    }
  }, 'destructive')
}

const variables = ref<any[]>([])
const showVarDialog = ref(false)
const varForm = ref({
  id: undefined as number | undefined,
  keyName: '',
  value: '',
  description: ''
})
const globalKeys = ref<string[]>([])

const fetchVariables = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const res: any = await request.get('/project/variables', {
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

const handleDeleteVariable = (id: number) => {
  openConfirm('删除变量', '确定删除该变量吗？', async () => {
    try {
      await request.delete(`/project/variables/${id}`)
      showToast('删除成功', 'success')
      fetchVariables()
    } catch (e: any) {
      showToast(e.message || '删除失败', 'error')
    }
  }, 'destructive')
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

const apiKeys = ref<any[]>([])
const showKeyDialog = ref(false)
const keyForm = ref({
  description: '',
  expiresAt: '' // YYYY-MM-DD HH:mm:ss
})
const generatedKey = ref('')
const selectedKeyId = ref<number | null>(null)
const plans = ref<any[]>([])
const selectedPlanId = ref<number | null>(null)
const apiBaseUrl = ref<string>(window.location.origin)
const curlExample = computed(() => {
  const key = apiKeys.value.find(k => k.id === selectedKeyId.value)
  const apiKey = key?.apiKey || 'pk_...'
  const planId = selectedPlanId.value ?? 'PLAN_ID'
  const projectId = projectStore.currentProject?.id ?? 'PROJECT_ID'
  return `curl -X POST "${apiBaseUrl.value}/api/plans/${planId}/execute" \\\n  -H "X-API-KEY: ${apiKey}" \\\n  -H "X-Project-Id: ${projectId}" \\\n  -H "Content-Type: application/json"`
})

const fetchApiKeys = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const res: any = await request.get('/project/keys', {
      params: { projectId: projectStore.currentProject.id }
    })
    apiKeys.value = res.records || []
  } catch (e: any) {
    showToast(e.message || '获取密钥失败', 'error')
  }
}

const fetchPlans = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const res: any = await request.get('/plans', { params: { page: 1, size: 50 } })
    plans.value = res.records || []
  } catch (_e: any) {
    plans.value = []
  }
}

const handleGenerateKey = async () => {
  if (!projectStore.currentProject?.id) return
  try {
    const toBackendDate = (val: string) => {
      if (!val) return null
      const base = val.replace(/\//g, '-')
      // Ensure seconds exist and keep 'T' for ISO-like LocalDateTime parsing
      if (base.length === 16) {
        return `${base}:00`
      }
      if (base.length === 19) {
        return base
      }
      return base
    }
    const payload = {
      projectId: projectStore.currentProject.id,
      description: keyForm.value.description,
      expiresAt: toBackendDate(keyForm.value.expiresAt)
    }
    const res: any = await request.post('/project/keys', payload)
    showToast('密钥生成成功', 'success')
    generatedKey.value = res.apiKey
    showKeyDialog.value = false
    fetchApiKeys()
    keyForm.value = { description: '', expiresAt: '' }
  } catch (e: any) {
    showToast(e.message || '生成失败', 'error')
  }
}

const handleDeleteKey = (id: number) => {
  openConfirm('删除密钥', '确定删除该密钥吗？', async () => {
    try {
      await request.delete(`/project/keys/${id}`)
      showToast('删除成功', 'success')
      fetchApiKeys()
    } catch (e: any) {
      showToast(e.message || '删除失败', 'error')
    }
  }, 'destructive')
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text)
  showToast('已复制', 'success')
}
const visibleEnv = new Set<number>()
const toggleEnv = (id?: number) => {
  if (!id) return
  if (visibleEnv.has(id)) visibleEnv.delete(id)
  else visibleEnv.add(id)
  if (id && visibleEnv.has(id)) {
    try {
      void request.get(`/environments/${id}/view`)
    } catch {}
  }
}
const toggleDb = (id?: number) => {
  if (!id) return
  const key = (id || 0) * 1000
  if (visibleEnv.has(key)) visibleEnv.delete(key)
  else visibleEnv.add(key)
}

watch(() => projectStore.currentProject, (newVal) => {
  if (newVal?.id) {
    fetchEnvironments()
    fetchVariables()
    fetchApiKeys()
    fetchPlans()
  }
}, { immediate: true })

onMounted(() => {
  // Initial fetch handled by watcher with immediate: true
  try {
    const gvars = JSON.parse(localStorage.getItem('global_variables') || '[]')
    const gparams = JSON.parse(localStorage.getItem('global_parameters') || '[]')
    const keys = [...gvars, ...gparams].map((x: any) => x.key).filter((k: any) => typeof k === 'string')
    globalKeys.value = keys
  } catch {}
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
        <TabsTrigger value="environments">环境配置</TabsTrigger>
        <TabsTrigger value="variables">项目变量</TabsTrigger>
        <TabsTrigger value="apikeys">API 密钥</TabsTrigger>
      </TabsList>

      <TabsContent value="environments">
        <Card>
          <CardHeader class="flex flex-row items-center justify-between">
            <div>
              <CardTitle>环境配置</CardTitle>
              <CardDescription>每个项目独立维护 Dev / Staging / Prod 等环境</CardDescription>
            </div>
            <Button v-if="canEdit" @click="openCreateEnv">
              <Plus class="w-4 h-4 mr-2" /> 新增环境
            </Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>名称</TableHead>
                  <TableHead>Key</TableHead>
                  <TableHead>Base URL</TableHead>
                  <TableHead>数据库</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead v-if="canEdit" class="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="e in environments" :key="e.id">
                  <TableCell class="font-medium">{{ e.name }}</TableCell>
                  <TableCell class="font-mono">{{ e.keyName }}</TableCell>
                  <TableCell class="max-w-[320px]">
                    <span v-if="!visibleEnv.has(e.id || -1)">******</span>
                    <span v-else class="truncate" :title="e.baseUrl">{{ e.baseUrl }}</span>
                    <Button variant="ghost" size="sm" class="ml-2" @click="toggleEnv(e.id)">
                      <Eye v-if="!visibleEnv.has(e.id || -1)" class="w-4 h-4" />
                      <EyeOff v-else class="w-4 h-4" />
                    </Button>
                  </TableCell>
                  <TableCell class="max-w-[240px]">
                    <span v-if="!visibleEnv.has((e.id || -1) * 1000)">******</span>
                    <span v-else class="truncate" :title="e.databaseName">{{ e.databaseName || '-' }}</span>
                    <Button variant="ghost" size="sm" class="ml-2" @click="toggleDb(e.id)">
                      <Eye v-if="!visibleEnv.has((e.id || -1) * 1000)" class="w-4 h-4" />
                      <EyeOff v-else class="w-4 h-4" />
                    </Button>
                  </TableCell>
                  <TableCell>
                    <span :class="e.active ? 'text-primary font-medium' : 'text-muted-foreground'">
                      {{ e.active ? '激活' : '未激活' }}
                    </span>
                  </TableCell>
                  <TableCell v-if="canEdit" class="text-right space-x-2">
                    <Button v-if="!e.active" variant="outline" size="sm" @click="handleActivateEnv(e)">
                      设为激活
                    </Button>
                    <Button variant="ghost" size="sm" @click="openEditEnv(e)">编辑</Button>
                    <Button variant="ghost" size="sm" class="text-red-500" @click="handleDeleteEnv(e.id!)">
                      <Trash2 class="w-4 h-4" />
                    </Button>
                  </TableCell>
                </TableRow>
                <TableRow v-if="environments.length === 0">
                  <TableCell colspan="6" class="text-center text-gray-500 py-8">暂无环境</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="variables">
        <Card>
          <CardHeader class="flex flex-row items-center justify-between">
            <div>
              <CardTitle>项目变量</CardTitle>
              <CardDescription>管理仅在当前项目中生效的环境变量 (优先级高于全局变量)</CardDescription>
            </div>
            <Button v-if="canEdit" @click="() => { resetVarForm(); showVarDialog = true }">
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
                  <TableHead v-if="canEdit" class="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="v in variables" :key="v.id">
                  <TableCell class="font-mono">
                    {{ v.keyName }}
                    <Badge v-if="globalKeys.includes(v.keyName)" variant="outline" class="ml-2">覆盖自全局</Badge>
                  </TableCell>
                  <TableCell class="max-w-[200px] truncate" :title="v.value">{{ v.value }}</TableCell>
                  <TableCell>{{ v.description || '-' }}</TableCell>
                  <TableCell v-if="canEdit" class="text-right space-x-2">
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

      <TabsContent value="apikeys">
        <Card>
          <CardHeader class="flex flex-row items-center justify-between">
            <div>
              <CardTitle>项目 API 密钥</CardTitle>
              <CardDescription>用于 CI/CD 集成，代表项目权限执行测试计划</CardDescription>
            </div>
            <Button v-if="canEdit" @click="showKeyDialog = true">
              <Plus class="w-4 h-4 mr-2" /> 生成新密钥
            </Button>
          </CardHeader>
          <CardContent class="space-y-4">
            <div v-if="canEdit" class="p-4 rounded border border-gray-100">
              <div class="flex items-center justify-between mb-3">
                <div>
                  <div class="text-sm text-gray-600">选择一个密钥生成 cURL 示例</div>
                </div>
                <div class="flex items-center gap-3">
                  <select v-model="selectedPlanId" class="h-9 px-3 rounded-md border border-input bg-background text-sm">
                    <option :value="null">请选择计划</option>
                    <option v-for="p in plans" :key="p.id" :value="p.id">#{{ p.id }} - {{ p.name }}</option>
                  </select>
                  <select v-model="selectedKeyId" class="h-9 px-3 rounded-md border border-input bg-background text-sm">
                    <option :value="null">请选择密钥</option>
                    <option v-for="k in apiKeys" :key="k.id" :value="k.id">#{{ k.id }} - {{ k.description || '未命名' }}</option>
                  </select>
                </div>
              </div>
              <div class="flex items-center gap-3 mb-3">
                <Label class="text-sm text-gray-600">API Base</Label>
                <Input v-model="apiBaseUrl" placeholder="例如: http://127.0.0.1:18080" class="h-9" />
              </div>
              <div class="relative bg-gray-900 rounded-lg p-4 font-mono text-xs text-green-400 overflow-x-auto">
                <pre>{{ curlExample }}</pre>
                <Button variant="ghost" size="icon" class="absolute top-2 right-2 text-gray-400 hover:text-white" @click="copyToClipboard(curlExample)">
                  <Copy class="w-4 h-4" />
                </Button>
              </div>
            </div>
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
                  <TableHead>描述</TableHead>
                  <TableHead>API Key</TableHead>
                  <TableHead>过期时间</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead v-if="canEdit" class="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="k in apiKeys" :key="k.id">
                  <TableCell class="font-medium">{{ k.description || '-' }}</TableCell>
                  <TableCell class="font-mono">{{ k.apiKey.substring(0, 8) }}****************</TableCell>
                  <TableCell>{{ k.expiresAt ? new Date(k.expiresAt).toLocaleString() : '永久有效' }}</TableCell>
                  <TableCell>{{ new Date(k.createdAt).toLocaleString() }}</TableCell>
                  <TableCell v-if="canEdit" class="text-right">
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

    <div v-if="showEnvDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md shadow-xl">
        <h3 class="text-lg font-semibold mb-4">{{ envForm.id ? '编辑环境' : '新增环境' }}</h3>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>环境名称</Label>
            <Input v-model="envForm.name" placeholder="例如: 开发环境" />
          </div>
          <div class="space-y-2">
            <Label>Key (唯一标识)</Label>
            <Input v-model="envForm.keyName" placeholder="例如: dev / staging / prod" :disabled="!!envForm.id" />
          </div>
          <div class="space-y-2">
            <Label>Base URL</Label>
            <Input v-model="envForm.baseUrl" placeholder="例如: https://api.example.com" />
          </div>
          <div class="space-y-2">
            <Label>数据库名 (可选)</Label>
            <Input v-model="envForm.databaseName" placeholder="例如: test_db" />
          </div>
          <div class="flex items-center justify-between">
            <Label>设为激活</Label>
            <Switch v-model="envForm.active" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <Button variant="outline" @click="showEnvDialog = false">取消</Button>
          <Button @click="handleSaveEnvironment">保存</Button>
        </div>
      </div>
    </div>

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
    <ConfirmDialog 
      :open="confirmDialog.open"
      :title="confirmDialog.title"
      :description="confirmDialog.description"
      :variant="confirmDialog.variant"
      @update:open="(val) => confirmDialog.open = val"
      @confirm="handleConfirmAction"
    />
  </div>
</div>
</template>
