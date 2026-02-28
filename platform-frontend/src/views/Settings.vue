<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import Switch from '@/components/ui/switch/Switch.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import Badge from '@/components/ui/badge/Badge.vue'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Plus, Trash2, Copy, Key, Globe, Database, Zap, Pencil, Settings, BookOpen } from 'lucide-vue-next'
import { showToast, openConfirm } from '@/lib/notify'

// Environment Settings
interface Environment {
  id?: number
  name: string
  keyName: string
  baseUrl: string
  databaseName: string
  active: boolean
  variables?: Array<{ key: string; value: string; description?: string }>
}

const environments = ref<Environment[]>([])

const fetchEnvironments = async () => {
    // Replace backend data with standard environments
    environments.value = [
        { 
            id: 1, 
            name: '开发环境', 
            keyName: 'dev', 
            baseUrl: 'http://localhost:8080', 
            databaseName: 'dev_db', 
            active: true,
            variables: [
                { key: 'DB_HOST', value: 'localhost', description: '数据库地址' }
            ]
        },
        { 
            id: 2, 
            name: '测试环境', 
            keyName: 'test', 
            baseUrl: 'https://test-api.example.com', 
            databaseName: 'test_db', 
            active: false,
            variables: []
        },
        { 
            id: 3, 
            name: '预发布环境', 
            keyName: 'staging', 
            baseUrl: 'https://staging-api.example.com', 
            databaseName: 'staging_db', 
            active: false,
            variables: [] 
        },
        { 
            id: 4, 
            name: '生产环境', 
            keyName: 'prod', 
            baseUrl: 'https://api.example.com', 
            databaseName: 'prod_db', 
            active: false,
            variables: [] 
        }
    ]
}

const isEnvDialogOpen = ref(false)
const isEditing = ref(false)
const envForm = ref<Partial<Environment>>({})

const handleEditEnv = (env: Environment) => {
    isEditing.value = true
    // Deep copy to avoid mutating original object while editing
    envForm.value = JSON.parse(JSON.stringify(env))
    if (!envForm.value.variables) {
        envForm.value.variables = []
    }
    isEnvDialogOpen.value = true
}

const handleDeleteEnv = async (id: number) => {
    const ok = await openConfirm({
        title: '删除环境',
        message: '确定要删除这个环境吗？此操作不可恢复。',
        confirmText: '删除',
    })
    if (!ok) return
    environments.value = environments.value.filter(e => e.id !== id)
    showToast('删除成功', 'success')
}

const handleAddEnv = () => {
    isEditing.value = false
    envForm.value = {
        name: '',
        keyName: '',
        baseUrl: '',
        databaseName: '',
        active: false,
        variables: []
    }
    isEnvDialogOpen.value = true
}

const handleAddEnvVariable = () => {
    if (!envForm.value.variables) {
        envForm.value.variables = []
    }
    envForm.value.variables.push({
        key: '',
        value: '',
        description: ''
    })
}

const handleDeleteEnvVariable = (index: number) => {
    if (envForm.value.variables) {
        envForm.value.variables.splice(index, 1)
    }
}

const handleSaveEnv = () => {
    if (!envForm.value.name || !envForm.value.keyName) {
        showToast('请填写完整信息', 'error')
        return
    }

    if (isEditing.value) {
        const index = environments.value.findIndex(e => e.id === envForm.value.id)
        if (index !== -1) {
            environments.value[index] = { ...environments.value[index], ...envForm.value } as Environment
            showToast('编辑成功', 'success')
        }
    } else {
        const newId = Math.max(...environments.value.map(e => e.id || 0), 0) + 1
        environments.value.push({
            id: newId,
            name: envForm.value.name!,
            keyName: envForm.value.keyName!,
            baseUrl: envForm.value.baseUrl || '',
            databaseName: envForm.value.databaseName || '',
            active: false
        })
        showToast('创建成功', 'success')
    }
    isEnvDialogOpen.value = false
}

const handleActivateEnv = (id: number) => {
    environments.value.forEach(env => {
        env.active = env.id === id
    })
    showToast('环境切换成功', 'success')
    // In a real app, you would also call an API to persist this change
    // request.post(`/environments/${id}/activate`)
}

onMounted(() => {
    fetchEnvironments()
})

// Global Variables & Parameters Persistence
const STORAGE_VARS_KEY = 'global_variables'
const STORAGE_PARAMS_KEY = 'global_parameters'

// Variables Settings
const variables = ref(JSON.parse(localStorage.getItem(STORAGE_VARS_KEY) || JSON.stringify([
  { 
    key: 'API_TOKEN', 
    value: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...', 
    type: 'variable', 
    scope: 'global', 
    description: 'API认证令牌（动态变量）', 
    isAuto: true,
    sourceType: 'api',
    sourceExpression: 'data.token'
  },
])))

const parameters = ref(JSON.parse(localStorage.getItem(STORAGE_PARAMS_KEY) || JSON.stringify([
  { key: 'DEFAULT_USER', value: 'testuser', type: 'parameter', scope: 'global', description: '默认测试用户（静态参数）' },
  { key: 'DEFAULT_PASSWORD', value: 'Test@123456', type: 'parameter', scope: 'global', description: '默认测试密码' },
  { key: 'TIMEOUT', value: '30000', type: 'parameter', scope: 'global', description: '请求超时时间（毫秒）' }
])))

watch(variables, (newVal) => {
  localStorage.setItem(STORAGE_VARS_KEY, JSON.stringify(newVal))
}, { deep: true })

watch(parameters, (newVal) => {
  localStorage.setItem(STORAGE_PARAMS_KEY, JSON.stringify(newVal))
}, { deep: true })

// Functions Settings
const functions = [
  {
    name: 'random.uuid()',
    description: '生成随机UUID',
    example: 'a1b2c3d4-e5f6-7890-1234-567890abcdef'
  },
  {
    name: 'random.timestamp()',
    description: '生成当前时间戳',
    example: '1704193200000'
  },
  {
    name: 'random.date(format)',
    description: '生成格式化日期',
    example: '2026-01-02'
  },
  {
    name: 'random.string(length)',
    description: '生成随机字符串',
    example: 'aBcDeF123'
  },
  {
    name: 'random.number(min, max)',
    description: '生成随机数字',
    example: '42'
  },
  {
    name: 'random.phone()',
    description: '生成随机手机号',
    example: '13800138000'
  },
  {
    name: 'random.email()',
    description: '生成随机邮箱',
    example: 'test123@example.com'
  },
  {
    name: 'random.username()',
    description: '生成随机用户名',
    example: 'user_a1b2c3'
  }
]

// API Settings
interface ApiKey {
  id: string
  name: string
  key: string
  permissions: string[]
  createdAt: string
  lastUsed: string
}

const savedApiKeys = localStorage.getItem('api_keys')
const apiKeys = ref<ApiKey[]>(savedApiKeys ? JSON.parse(savedApiKeys) : [
  {
    id: '1',
    name: 'Jenkins CI',
    key: 'sk_test_abcdef1234567890',
    permissions: ['read', 'execute'],
    createdAt: '2026-01-01',
    lastUsed: '2026-01-02 10:30'
  }
])

watch(apiKeys, (newKeys) => {
  localStorage.setItem('api_keys', JSON.stringify(newKeys))
}, { deep: true })

const handleRevokeKey = (id: string) => {
  apiKeys.value = apiKeys.value.filter(key => key.id !== id)
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
  } catch (e) {
    showToast('复制失败', 'error')
  }
}

const handleGenerateKey = () => {
  const randomPart = Math.random().toString(36).slice(2, 12)
  const id = Date.now().toString()
  apiKeys.value.unshift({
    id,
    name: `New Key ${apiKeys.value.length + 1}`,
    key: `sk_${randomPart}`,
    permissions: ['read', 'execute'],
    createdAt: new Date().toISOString().slice(0, 10),
    lastUsed: '未使用'
  })
}

const isUsageDialogOpen = ref(false)

const curlExample = `curl -X POST "http://your-platform.com/api/openapi/v1/run-plan" \\
  -H "X-API-KEY: sk_test_..." \\
  -H "Content-Type: application/json" \\
  -d '{ 
        "planId": "PLAN_1001", 
        "environment": "Test", 
        "callbackUrl": "http://jenkins.com/webhook/job-123" 
      }'`

const handleAddVariable = () => {
    variables.value.push({
        key: 'NEW_VAR',
        value: '',
        scope: 'global',
        type: 'variable',
        description: 'New Variable'
    })
}

const handleAddParameter = () => {
    parameters.value.push({
        key: 'NEW_PARAM',
        value: '',
        scope: 'global',
        type: 'parameter',
        description: 'New Parameter'
    })
}

const handleDeleteVariable = (index: number) => {
    // Because the list is displayed in reverse order, we need to calculate the actual index
    const realIndex = variables.value.length - 1 - index
    variables.value.splice(realIndex, 1)
}

const handleDeleteParameter = (index: number) => {
    // Because the list is displayed in reverse order, we need to calculate the actual index
    const realIndex = parameters.value.length - 1 - index
    parameters.value.splice(realIndex, 1)
}
</script>

<template>
  <div class="space-y-6 p-6">
    <Tabs default-value="environments" class="space-y-4">
      <TabsList>
        <TabsTrigger value="environments">环境配置</TabsTrigger>
        <TabsTrigger value="variables">全局配置</TabsTrigger>
        <TabsTrigger value="functions">随机函数</TabsTrigger>
        <TabsTrigger value="openapi">OpenAPI</TabsTrigger>
      </TabsList>

      <!-- Environments Tab -->
      <TabsContent value="environments" class="space-y-4">
        <div class="space-y-4">
          <Card>
            <CardHeader class="flex flex-row items-center justify-between">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <Globe class="w-5 h-5 text-blue-500" />
                  环境配置
                </CardTitle>
                <CardDescription>
                  管理不同环境的配置，支持环境关联切换
                </CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="handleAddEnv">
                <Plus class="w-4 h-4 mr-2" />
                添加新环境
              </Button>
            </CardHeader>
            <CardContent class="space-y-4">
              <div v-for="env in environments.slice().reverse()" :key="env.id" 
                :class="`border rounded-lg p-4 transition-colors ${env.active ? 'border-blue-200 bg-blue-50' : 'border-gray-200 hover:border-gray-300'}`">
                <div class="flex items-start justify-between mb-4">
                  <div class="flex items-center gap-3">
                    <h3 class="font-semibold text-gray-900">{{ env.name }}</h3>
                    <Badge variant="outline">{{ env.keyName }}</Badge>
                    <Badge v-if="env.active" class="bg-blue-500 hover:bg-blue-600">当前激活</Badge>
                    <Button 
                        v-else 
                        variant="outline" 
                        size="sm" 
                        class="text-xs h-6"
                        @click="handleActivateEnv(env.id!)"
                    >
                        设为激活
                    </Button>
                  </div>
                  <div class="flex items-center gap-2">
                    <Button variant="ghost" size="sm" @click="handleEditEnv(env)">
                        <Pencil class="w-4 h-4 mr-2" />
                        编辑
                    </Button>
                    <Button variant="ghost" size="sm" class="text-red-500 hover:text-red-600 hover:bg-red-50" @click="handleDeleteEnv(env.id!)">
                        <Trash2 class="w-4 h-4" />
                    </Button>
                  </div>
                </div>
                
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div class="space-y-2">
                    <Label class="text-xs text-gray-500">API 地址</Label>
                    <div class="flex gap-2">
                      <Input v-model="env.baseUrl" class="bg-white h-8" />
                    </div>
                  </div>
                  <div class="space-y-2">
                    <Label class="text-xs text-gray-500">数据库名</Label>
                    <div class="flex gap-2">
                      <Input v-model="env.databaseName" class="bg-white h-8" />
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

      <!-- Variables Tab -->
      <TabsContent value="variables" class="space-y-4">
        <div class="space-y-4 h-full flex flex-col">
          <!-- Global Variables Section -->
          <Card class="flex flex-col flex-1">
            <CardHeader class="flex flex-row items-center justify-between flex-shrink-0">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <Database class="w-5 h-5 text-blue-500" />
                  全局变量
                </CardTitle>
                <CardDescription>
                  管理执行过程中不断变化的动态数据（如 Token、订单 ID）
                </CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="handleAddVariable">
                <Plus class="w-4 h-4 mr-2" />
                添加变量
              </Button>
            </CardHeader>
            <CardContent class="flex-1 overflow-auto">
              <div class="space-y-4 variable-list">
                <div v-for="(variable, index) in variables.slice().reverse()" :key="index" class="flex items-start gap-4 p-4 border border-gray-100 rounded-lg hover:bg-gray-50">
                  <div class="flex-1 grid grid-cols-1 md:grid-cols-12 gap-4">
                    <div class="col-span-3 space-y-1">
                      <div class="flex items-center justify-between mb-1">
                        <Label class="text-xs text-gray-500">变量名</Label>
                        <div class="flex items-center gap-1.5">
                          <Switch v-model="variable.isAuto" class="scale-75 origin-right" />
                          <span class="text-[10px] text-gray-400 font-medium">{{ variable.isAuto ? '自动回写' : '手动' }}</span>
                        </div>
                      </div>
                      <Input v-model="variable.key" class="font-mono text-blue-600" />
                    </div>
                    <div class="col-span-5 space-y-1">
                      <Label class="text-xs text-gray-500">变量值</Label>
                      <Input v-model="variable.value" :disabled="variable.isAuto" :class="variable.isAuto ? 'bg-gray-50 text-gray-500' : ''" />
                    </div>
                    <div class="col-span-4 space-y-1">
                      <Label class="text-xs text-gray-500">描述</Label>
                      <Input v-model="variable.description" />
                    </div>
                    
                    <!-- Auto Update Configuration -->
                    <div v-if="variable.isAuto" class="col-span-12 grid grid-cols-12 gap-4 mt-1 p-3 bg-blue-50/50 rounded-md border border-blue-100 animate-in fade-in slide-in-from-top-1">
                      <div class="col-span-3 space-y-1">
                        <Label class="text-[10px] font-medium text-blue-600">提取来源</Label>
                        <Select v-model="variable.sourceType">
                          <SelectTrigger class="h-8 text-xs bg-white border-blue-200 focus:ring-blue-200">
                            <SelectValue placeholder="选择来源" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="api">API 响应提取</SelectItem>
                            <SelectItem value="db">数据库查询</SelectItem>
                            <SelectItem value="script">自定义脚本</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div class="col-span-9 space-y-1">
                        <Label class="text-[10px] font-medium text-blue-600">提取规则 (JSONPath / SQL / Script)</Label>
                        <div class="flex gap-2">
                          <Input 
                            v-model="variable.sourceExpression" 
                            class="h-8 text-xs font-mono bg-white border-blue-200 focus-visible:ring-blue-200" 
                            :placeholder="variable.sourceType === 'db' ? 'SELECT value FROM table WHERE key=...' : variable.sourceType === 'script' ? 'return customized_value' : 'data.token'" 
                          />
                          <Button size="sm" variant="outline" class="h-8 px-3 text-xs text-blue-600 border-blue-200 hover:bg-blue-100 hover:text-blue-700">
                            测试
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <Button variant="ghost" size="icon" class="mt-5 text-gray-400 hover:text-red-500" @click="handleDeleteVariable(index)">
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <!-- Global Parameters Section -->
          <Card class="flex flex-col flex-1">
            <CardHeader class="flex flex-row items-center justify-between flex-shrink-0">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <Settings class="w-5 h-5 text-green-500" />
                  全局参数
                </CardTitle>
                <CardDescription>
                  管理系统或环境的静态配置（如超时时间、默认用户）
                </CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="handleAddParameter">
                <Plus class="w-4 h-4 mr-2" />
                添加参数
              </Button>
            </CardHeader>
            <CardContent class="flex-1 overflow-auto">
              <div class="space-y-4 variable-list">
                <div v-for="(param, index) in parameters.slice().reverse()" :key="index" class="flex items-start gap-4 p-4 border border-gray-100 rounded-lg hover:bg-gray-50">
                  <div class="flex-1 grid grid-cols-1 md:grid-cols-12 gap-4">
                    <div class="col-span-3 space-y-1">
                      <Label class="text-xs text-gray-500">参数名</Label>
                      <Input v-model="param.key" class="font-mono text-green-600" />
                    </div>
                    <div class="col-span-5 space-y-1">
                      <Label class="text-xs text-gray-500">参数值</Label>
                      <Input v-model="param.value" />
                    </div>
                    <div class="col-span-4 space-y-1">
                      <Label class="text-xs text-gray-500">描述</Label>
                      <Input v-model="param.description" />
                    </div>
                  </div>
                  <Button variant="ghost" size="icon" class="mt-5 text-gray-400 hover:text-red-500" @click="handleDeleteParameter(index)">
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

      <!-- Functions Tab -->
      <TabsContent value="functions" class="space-y-4">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Zap class="w-5 h-5 text-yellow-500" />
              随机函数
            </CardTitle>
            <CardDescription>
              内置随机函数说明，支持在参数值中直接调用
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div v-for="func in functions" :key="func.name" class="p-4 border border-gray-100 rounded-lg space-y-2">
                <div class="flex items-center justify-between">
                  <code class="text-sm font-mono bg-gray-100 px-2 py-1 rounded text-purple-600">{{ func.name }}</code>
                  <Button
                    variant="ghost"
                    size="icon"
                    class="h-6 w-6"
                    @click="copyToClipboard(func.name)"
                  >
                    <Copy class="w-3 h-3" />
                  </Button>
                </div>
                <p class="text-sm text-gray-600">{{ func.description }}</p>
                <div class="text-xs text-gray-400">示例: {{ func.example }}</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <!-- OpenAPI Tab -->
      <TabsContent value="openapi" class="space-y-4">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Key class="w-5 h-5 text-green-500" />
              OpenAPI 密钥
            </CardTitle>
            <CardDescription>
              管理用于 CI/CD 集成的 API 访问密钥
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <div v-for="key in apiKeys" :key="key.id" class="flex items-center justify-between p-4 border border-gray-100 rounded-lg">
                <div class="space-y-1">
                  <div class="font-medium">{{ key.name }}</div>
                  <div class="text-sm text-gray-500 font-mono">
                    {{ key.key.substring(0, 8) }}...{{ key.key.substring(key.key.length - 4) }}
                  </div>
                </div>
                <div class="flex items-center gap-4 text-sm text-gray-500">
                  <div>创建于: {{ key.createdAt }}</div>
                  <div>最后使用: {{ key.lastUsed }}</div>
                  <Button variant="outline" size="sm" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="handleRevokeKey(key.id)">
                    撤销
                  </Button>
                </div>
              </div>
              <div class="flex gap-4">
                <Button class="flex-1" variant="outline" @click="handleGenerateKey">
                  <Plus class="w-4 h-4 mr-2" />
                  生成新密钥
                </Button>
                <Button class="flex-1" variant="secondary" @click="isUsageDialogOpen = true">
                  <BookOpen class="w-4 h-4 mr-2" />
                  查看使用文档
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>

    <Dialog :open="isUsageDialogOpen" @update:open="isUsageDialogOpen = $event">
      <DialogContent class="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>OpenAPI 调用指南</DialogTitle>
          <DialogDescription>
            通过 HTTP 接口触发测试计划
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="space-y-2">
            <Label>1. 鉴权方式</Label>
            <p class="text-sm text-gray-500">
              在请求头中添加 <code class="bg-gray-100 px-1 rounded text-purple-600 font-mono">X-API-KEY</code>
            </p>
          </div>
          <div class="space-y-2">
            <Label>2. 调用示例 (cURL)</Label>
            <div class="relative bg-gray-900 rounded-lg p-4 font-mono text-xs text-green-400 overflow-x-auto">
              <pre>{{ curlExample }}</pre>
              <Button variant="ghost" size="icon" class="absolute top-2 right-2 text-gray-400 hover:text-white" @click="copyToClipboard(curlExample)">
                <Copy class="w-4 h-4" />
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <Dialog :open="isEnvDialogOpen" @update:open="isEnvDialogOpen = $event">
      <DialogContent class="sm:max-w-[800px] max-h-[90vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{{ isEditing ? '编辑环境' : '添加环境' }}</DialogTitle>
          <DialogDescription>
            {{ isEditing ? '修改环境信息' : '创建一个新的环境配置' }}
          </DialogDescription>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto py-4 px-1">
          <div class="grid gap-4 mb-6">
            <div class="grid grid-cols-4 items-center gap-4">
              <Label class="text-right">名称</Label>
              <Input v-model="envForm.name" class="col-span-3" placeholder="例如：预发环境" />
            </div>
            <div class="grid grid-cols-4 items-center gap-4">
              <Label class="text-right">Key</Label>
              <Input v-model="envForm.keyName" class="col-span-3" placeholder="例如：staging" />
            </div>
            <div class="grid grid-cols-4 items-center gap-4">
              <Label class="text-right">API地址</Label>
              <Input v-model="envForm.baseUrl" class="col-span-3" placeholder="https://api.example.com" />
            </div>
            <div class="grid grid-cols-4 items-center gap-4">
              <Label class="text-right">数据库名</Label>
              <Input v-model="envForm.databaseName" class="col-span-3" placeholder="db_name" />
            </div>
          </div>

          <div class="space-y-4 border-t pt-4">
            <div class="flex items-center justify-between">
              <Label class="text-base font-medium">环境变量</Label>
              <Button size="sm" variant="outline" @click="handleAddEnvVariable">
                <Plus class="w-4 h-4 mr-2" />
                添加变量
              </Button>
            </div>
            
            <div class="space-y-3">
              <div v-if="!envForm.variables || envForm.variables.length === 0" class="text-center py-8 text-gray-500 bg-gray-50 rounded-lg border border-dashed">
                暂无环境变量
              </div>
              <div v-else v-for="(variable, index) in envForm.variables" :key="index" class="flex items-start gap-3 p-3 bg-gray-50 rounded-lg border">
                <div class="flex-1 grid grid-cols-3 gap-3">
                  <div class="space-y-1">
                    <Label class="text-xs text-gray-500">变量名</Label>
                    <Input v-model="variable.key" placeholder="KEY" class="h-8 font-mono text-sm" />
                  </div>
                  <div class="space-y-1">
                    <Label class="text-xs text-gray-500">变量值</Label>
                    <Input v-model="variable.value" placeholder="VALUE" class="h-8 text-sm" />
                  </div>
                  <div class="space-y-1">
                    <Label class="text-xs text-gray-500">描述</Label>
                    <Input v-model="variable.description" placeholder="描述" class="h-8 text-sm" />
                  </div>
                </div>
                <Button variant="ghost" size="icon" class="mt-6 h-8 w-8 text-gray-400 hover:text-red-500" @click="handleDeleteEnvVariable(index)">
                  <Trash2 class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="isEnvDialogOpen = false">取消</Button>
          <Button @click="handleSaveEnv">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
