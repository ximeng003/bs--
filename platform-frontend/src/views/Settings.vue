<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import Badge from '@/components/ui/badge/Badge.vue'
import { Plus, Trash2, Copy, Key, Globe, Database, Zap, Pencil } from 'lucide-vue-next'
import request from '@/api/request'

// Environment Settings
interface Environment {
  id?: number
  name: string
  keyName: string
  baseUrl: string
  databaseName: string
  active: boolean
}

const environments = ref<Environment[]>([])

const fetchEnvironments = async () => {
    try {
        const res: any = await request.get('/environments')
        if (Array.isArray(res)) {
            environments.value = res
        }
    } catch (e) {
        console.error(e)
    }
}

const handleEditEnv = (env: Environment) => {
    // In a real app, open a dialog. For now, prompt.
    const newName = prompt('输入新名称', env.name)
    if (newName) {
        env.name = newName
        request.put('/environments', env).then(fetchEnvironments)
    }
}

const handleDeleteEnv = async (id: number) => {
    if (!confirm('确定删除?')) return
    try {
        await request.delete(`/environments/${id}`)
        fetchEnvironments()
    } catch (e) {
        alert('删除失败')
    }
}

const handleAddEnv = async () => {
    const name = prompt('输入环境名称 (如: 预发环境)')
    if (!name) return
    const key = prompt('输入Key (如: pre)')
    if (!key) return
    
    try {
        await request.post('/environments', {
            name,
            keyName: key,
            baseUrl: 'https://',
            databaseName: '',
            active: false
        })
        fetchEnvironments()
    } catch (e) {
        alert('创建失败')
    }
}

onMounted(() => {
    fetchEnvironments()
})

// Variables Settings (Mock for now or use same pattern)
const variables = ref([
  { key: 'API_TOKEN', value: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...', scope: 'global', description: 'API认证令牌' },
  { key: 'DEFAULT_USER', value: 'testuser', scope: 'global', description: '默认测试用户' },
  { key: 'DEFAULT_PASSWORD', value: 'Test@123456', scope: 'global', description: '默认测试密码' },
  { key: 'TIMEOUT', value: '30000', scope: 'global', description: '请求超时时间（毫秒）' }
])

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
const apiKeys = ref([
  {
    id: '1',
    name: 'Jenkins CI',
    key: 'sk_test_abcdef1234567890',
    permissions: ['read', 'execute'],
    createdAt: '2026-01-01',
    lastUsed: '2026-01-02 10:30'
  }
])

const handleRevokeKey = (id: string) => {
  apiKeys.value = apiKeys.value.filter(key => key.id !== id)
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
const handleAddVariable = () => {
    variables.value.push({
        key: 'NEW_VAR',
        value: '',
        scope: 'global',
        description: 'New Variable'
    })
}
</script>

<template>
  <div class="space-y-6 p-6">
    <Tabs default-value="environments" class="space-y-4">
      <TabsList>
        <TabsTrigger value="environments">环境配置</TabsTrigger>
        <TabsTrigger value="variables">公共参数</TabsTrigger>
        <TabsTrigger value="functions">随机函数</TabsTrigger>
        <TabsTrigger value="openapi">OpenAPI</TabsTrigger>
      </TabsList>

      <!-- Environments Tab -->
      <TabsContent value="environments" class="space-y-4">
        <div class="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle class="flex items-center gap-2">
                <Globe class="w-5 h-5 text-blue-500" />
                环境配置
              </CardTitle>
              <CardDescription>
                管理不同环境的配置，支持环境关联切换
              </CardDescription>
            </CardHeader>
            <CardContent class="space-y-4">
              <div v-for="env in environments" :key="env.id" 
                :class="`border rounded-lg p-4 transition-colors ${env.active ? 'border-blue-200 bg-blue-50' : 'border-gray-200 hover:border-gray-300'}`">
                <div class="flex items-start justify-between mb-4">
                  <div class="flex items-center gap-3">
                    <h3 class="font-semibold text-gray-900">{{ env.name }}</h3>
                    <Badge variant="outline">{{ env.keyName }}</Badge>
                    <Badge v-if="env.active" class="bg-blue-500 hover:bg-blue-600">当前激活</Badge>
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

              <Button class="w-full" variant="outline" @click="handleAddEnv">
                <Plus class="w-4 h-4 mr-2" />
                添加新环境
              </Button>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

      <!-- Variables Tab -->
      <TabsContent value="variables" class="space-y-4">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Database class="w-5 h-5 text-purple-500" />
              公共参数
            </CardTitle>
            <CardDescription>
              全局变量配置，可在测试用例中使用 ${variable} 引用
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-4 variable-list max-h-[500px] overflow-y-auto">
              <div v-for="(variable, index) in variables" :key="index" class="flex items-start gap-4 p-4 border border-gray-100 rounded-lg hover:bg-gray-50">
                <div class="flex-1 grid grid-cols-12 gap-4">
                  <div class="col-span-3 space-y-1">
                    <Label class="text-xs text-gray-500">变量名</Label>
                    <Input v-model="variable.key" class="font-mono" />
                  </div>
                  <div class="col-span-5 space-y-1">
                    <Label class="text-xs text-gray-500">变量值</Label>
                    <Input v-model="variable.value" />
                  </div>
                  <div class="col-span-4 space-y-1">
                    <Label class="text-xs text-gray-500">描述</Label>
                    <Input v-model="variable.description" />
                  </div>
                </div>
                <Button variant="ghost" size="icon" class="mt-5 text-gray-400 hover:text-red-500">
                  <Trash2 class="w-4 h-4" />
                </Button>
              </div>
              <Button class="w-full" variant="outline" @click="handleAddVariable">
                <Plus class="w-4 h-4 mr-2" />
                添加变量
              </Button>
            </div>
          </CardContent>
        </Card>
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
                  <Button variant="ghost" size="icon" class="h-6 w-6">
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
              <Button class="w-full" variant="outline" @click="handleGenerateKey">
                <Plus class="w-4 h-4 mr-2" />
                生成新密钥
              </Button>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  </div>
</template>
