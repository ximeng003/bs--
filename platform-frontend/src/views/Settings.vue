<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import Badge from '@/components/ui/badge/Badge.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Plus, Trash2, Copy, Key, Globe, Database, Zap } from 'lucide-vue-next'

// Environment Settings
const environments = ref([
  {
    name: '开发环境',
    key: 'dev',
    baseUrl: 'https://dev.example.com',
    database: 'dev_database',
    active: false
  },
  {
    name: '测试环境',
    key: 'staging',
    baseUrl: 'https://staging.example.com',
    database: 'staging_database',
    active: true
  },
  {
    name: '生产环境',
    key: 'production',
    baseUrl: 'https://api.example.com',
    database: 'prod_database',
    active: false
  }
])

// Variables Settings
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
</script>

<template>
  <div class="space-y-6">
    <Tabs default-value="environment" class="w-full">
      <TabsList class="grid w-full grid-cols-4">
        <TabsTrigger value="environment">环境配置</TabsTrigger>
        <TabsTrigger value="variables">公共参数</TabsTrigger>
        <TabsTrigger value="functions">随机函数</TabsTrigger>
        <TabsTrigger value="api">OpenAPI</TabsTrigger>
      </TabsList>

      <TabsContent value="environment" class="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Globe class="w-5 h-5" />
              环境配置
            </CardTitle>
            <CardDescription>
              管理不同环境的配置，支持环境关联和切换
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <Card v-for="(env, index) in environments" :key="index" :class="env.active ? 'border-blue-500 bg-blue-50' : ''">
                <CardContent class="pt-6">
                  <div class="flex items-start justify-between">
                    <div class="flex-1 space-y-2">
                      <div class="flex items-center gap-2">
                        <h3 class="font-semibold">{{ env.name }}</h3>
                        <Badge variant="outline">{{ env.key }}</Badge>
                        <Badge v-if="env.active" class="bg-blue-500">当前激活</Badge>
                      </div>
                      <div class="space-y-1 text-sm text-gray-600">
                        <div>API地址: {{ env.baseUrl }}</div>
                        <div>数据库: {{ env.database }}</div>
                      </div>
                    </div>
                    <div class="flex gap-2">
                      <Button variant="outline" size="sm">
                        编辑
                      </Button>
                      <Button 
                        variant="outline" 
                        size="sm" 
                        class="text-red-600 hover:text-red-700"
                      >
                        <Trash2 class="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
            <Button class="w-full mt-4">
              <Plus class="w-4 h-4 mr-2" />
              添加新环境
            </Button>
          </CardContent>
        </Card>

        <Card class="border-yellow-200 bg-yellow-50">
          <CardHeader>
            <CardTitle class="text-yellow-900">环境变量说明</CardTitle>
          </CardHeader>
          <CardContent class="space-y-2 text-sm text-yellow-900">
            <p>• 环境配置支持自动切换，在测试计划和用例中可以选择目标环境</p>
            <p>• 支持环境变量引用，使用 ${env.变量名} 语法</p>
            <p>• 例如: API地址可以写成 ${env.baseUrl}/users/login</p>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="variables" class="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Database class="w-5 h-5" />
              公共参数
            </CardTitle>
            <CardDescription>
              定义全局可用的参数，可在所有测试用例中引用
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <div class="grid grid-cols-1 gap-4">
                <Card v-for="(variable, index) in variables" :key="index">
                  <CardContent class="pt-6">
                    <div class="flex items-start justify-between gap-4">
                      <div class="flex-1 grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div>
                          <Label class="text-xs text-gray-600">参数名</Label>
                          <div class="mt-1 font-mono text-sm">{{ variable.key }}</div>
                        </div>
                        <div>
                          <Label class="text-xs text-gray-600">参数值</Label>
                          <div class="mt-1 text-sm font-mono bg-gray-100 px-2 py-1 rounded truncate">
                            {{ variable.value }}
                          </div>
                        </div>
                        <div>
                          <Label class="text-xs text-gray-600">说明</Label>
                          <div class="mt-1 text-sm text-gray-600">{{ variable.description }}</div>
                        </div>
                      </div>
                      <div class="flex gap-2">
                        <Button variant="outline" size="sm">
                          <Copy class="w-4 h-4" />
                        </Button>
                        <Button variant="outline" size="sm" class="text-red-600 hover:text-red-700">
                          <Trash2 class="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>添加新参数</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="space-y-2">
                  <Label>参数名</Label>
                  <Input placeholder="例如: API_KEY" />
                </div>
                <div class="space-y-2">
                  <Label>作用域</Label>
                  <Select default-value="global">
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="global">全局</SelectItem>
                      <SelectItem value="project">项目级</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div class="space-y-2">
                <Label>参数值</Label>
                <Input placeholder="输入参数值" />
              </div>
              <div class="space-y-2">
                <Label>说明</Label>
                <Input placeholder="参数说明（可选）" />
              </div>
              <Button class="w-full">
                <Plus class="w-4 h-4 mr-2" />
                添加参数
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card class="border-blue-200 bg-blue-50">
          <CardHeader>
            <CardTitle class="text-blue-900">使用方法</CardTitle>
          </CardHeader>
          <CardContent class="space-y-2 text-sm text-blue-900">
            <p>• 在测试用例中使用 ${变量名} 引用公共参数</p>
            <p>• 例如: 请求头设置为 Authorization: Bearer ${API_TOKEN}</p>
            <p>• 支持嵌套引用和组合使用</p>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="functions" class="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Zap class="w-5 h-5" />
              内置随机函数
            </CardTitle>
            <CardDescription>
              使用内置函数生成动态测试数据
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Card v-for="(func, index) in functions" :key="index">
                <CardContent class="pt-6">
                  <div class="space-y-2">
                    <div class="font-mono text-sm font-semibold text-blue-600">
                      ${{ func.name }}
                    </div>
                    <div class="text-sm text-gray-600">{{ func.description }}</div>
                    <div class="text-xs text-gray-500">
                      <span class="text-gray-600">示例输出: </span>
                      <span class="font-mono bg-gray-100 px-2 py-1 rounded">{{ func.example }}</span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </CardContent>
        </Card>

        <Card class="border-green-200 bg-green-50">
          <CardHeader>
            <CardTitle class="text-green-900">使用示例</CardTitle>
          </CardHeader>
          <CardContent class="space-y-3">
            <div class="bg-white rounded p-3 space-y-2">
              <div class="font-semibold text-sm">API请求体示例</div>
              <pre class="text-xs bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
{
  "userId": "${random.uuid()}",
  "username": "${random.username()}",
  "email": "${random.email()}",
  "phone": "${random.phone()}",
  "timestamp": ${random.timestamp()}
}
              </pre>
            </div>
            <div class="bg-white rounded p-3 space-y-2">
              <div class="font-semibold text-sm">WEB测试步骤示例</div>
              <pre class="text-xs bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
输入文本: #email, ${random.email()}
输入文本: #phone, ${random.phone()}
输入文本: #code, ${random.number(1000, 9999)}
              </pre>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <TabsContent value="api" class="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Key class="w-5 h-5" />
              OpenAPI 密钥
            </CardTitle>
            <CardDescription>
              生成API密钥用于CI/CD集成和外部调用
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <Card v-for="apiKey in apiKeys" :key="apiKey.id">
                <CardContent class="pt-6">
                  <div class="flex items-start justify-between gap-4">
                    <div class="flex-1 space-y-2">
                      <div class="flex items-center gap-2">
                        <h3 class="font-semibold">{{ apiKey.name }}</h3>
                        <Badge variant="outline">
                          {{ apiKey.permissions.join(', ') }}
                        </Badge>
                      </div>
                      <div class="font-mono text-sm bg-gray-100 px-3 py-2 rounded">
                        {{ apiKey.key }}
                      </div>
                      <div class="flex items-center gap-4 text-xs text-gray-500">
                        <span>创建时间: {{ apiKey.createdAt }}</span>
                        <span>最后使用: {{ apiKey.lastUsed }}</span>
                      </div>
                    </div>
                    <div class="flex gap-2">
                      <Button variant="outline" size="sm">
                        <Copy class="w-4 h-4" />
                      </Button>
                      <Button variant="outline" size="sm" class="text-red-600 hover:text-red-700">
                        <Trash2 class="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>创建新密钥</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <div class="space-y-2">
                <Label>密钥名称</Label>
                <Input placeholder="例如: Jenkins CI" />
              </div>
              <div class="space-y-2">
                <Label>权限</Label>
                <div class="space-y-2">
                  <label class="flex items-center gap-2">
                    <input type="checkbox" checked class="rounded" />
                    <span class="text-sm">读取测试用例</span>
                  </label>
                  <label class="flex items-center gap-2">
                    <input type="checkbox" checked class="rounded" />
                    <span class="text-sm">执行测试计划</span>
                  </label>
                  <label class="flex items-center gap-2">
                    <input type="checkbox" class="rounded" />
                    <span class="text-sm">创建/修改用例</span>
                  </label>
                  <label class="flex items-center gap-2">
                    <input type="checkbox" class="rounded" />
                    <span class="text-sm">删除用例</span>
                  </label>
                </div>
              </div>
              <Button class="w-full">
                <Plus class="w-4 h-4 mr-2" />
                生成新密钥
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card class="border-purple-200 bg-purple-50">
          <CardHeader>
            <CardTitle class="text-purple-900">API接口文档</CardTitle>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="bg-white rounded p-3 space-y-2">
              <div class="font-semibold text-sm">执行测试计划</div>
              <pre class="text-xs bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
POST /api/v1/plans/:planId/execute
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json

{
  "environment": "production",
  "engineId": "optional"
}
              </pre>
            </div>
            <div class="bg-white rounded p-3 space-y-2">
              <div class="font-semibold text-sm">执行单个用例</div>
              <pre class="text-xs bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
POST /api/v1/testcases/:caseId/execute
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json

{
  "environment": "staging"
}
              </pre>
            </div>
            <div class="bg-white rounded p-3 space-y-2">
              <div class="font-semibold text-sm">查询执行结果</div>
              <pre class="text-xs bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
GET /api/v1/executions/:executionId
Authorization: Bearer YOUR_API_KEY
              </pre>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  </div>
</template>
