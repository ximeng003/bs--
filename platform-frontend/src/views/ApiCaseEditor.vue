<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Textarea from '@/components/ui/textarea/Textarea.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Play, Save, Clock } from 'lucide-vue-next'

const method = ref('GET')
const url = ref('https://api.example.com/users/login')
const responseData = ref<any>(null)
const statusCode = ref<number | null>(null)
const responseTime = ref<number | null>(null)

const handleSend = () => {
  // Simulate API call
  statusCode.value = 200
  responseTime.value = 342
  responseData.value = {
    success: true,
    data: {
      userId: "12345",
      username: "testuser",
      token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      expiresIn: 3600
    },
    message: "登录成功"
  }
}
</script>

<template>
  <div class="p-6 space-y-4">
    <!-- URL Input Section -->
    <Card class="border-gray-200">
      <CardContent class="pt-6">
        <div class="flex gap-2">
          <Select v-model="method">
            <SelectTrigger class="w-32 border-gray-300">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="GET">GET</SelectItem>
              <SelectItem value="POST">POST</SelectItem>
              <SelectItem value="PUT">PUT</SelectItem>
              <SelectItem value="DELETE">DELETE</SelectItem>
              <SelectItem value="PATCH">PATCH</SelectItem>
            </SelectContent>
          </Select>
          <Input
            v-model="url"
            placeholder="输入请求URL"
            class="flex-1 border-gray-300"
          />
          <Button
            @click="handleSend"
            class="bg-[#409EFF] hover:bg-[#3a8ee6] px-8"
          >
            <Play class="w-4 h-4 mr-2" />
            发送
          </Button>
          <Button variant="outline" class="border-gray-300">
            <Save class="w-4 h-4 mr-2" />
            保存
          </Button>
        </div>
      </CardContent>
    </Card>

    <!-- Request Configuration -->
    <Card class="border-gray-200">
      <CardContent class="pt-6">
        <Tabs default-value="params" class="w-full">
          <TabsList class="grid w-full grid-cols-4 bg-gray-100">
            <TabsTrigger value="params">Params</TabsTrigger>
            <TabsTrigger value="headers">Headers</TabsTrigger>
            <TabsTrigger value="body">Body</TabsTrigger>
            <TabsTrigger value="assertions">Assertions</TabsTrigger>
          </TabsList>

          <TabsContent value="params" class="space-y-4">
            <div class="space-y-3">
              <div class="grid grid-cols-12 gap-2 text-sm font-semibold text-gray-700 px-2">
                <div class="col-span-1"></div>
                <div class="col-span-5">KEY</div>
                <div class="col-span-5">VALUE</div>
                <div class="col-span-1"></div>
              </div>
              <div v-for="i in 3" :key="i" class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" checked class="rounded" />
                </div>
                <div class="col-span-5">
                  <Input placeholder="参数名" class="border-gray-300" />
                </div>
                <div class="col-span-5">
                  <Input placeholder="参数值" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600">
                    ×
                  </Button>
                </div>
              </div>
            </div>
          </TabsContent>

          <TabsContent value="headers" class="space-y-4">
            <div class="space-y-3">
              <div class="grid grid-cols-12 gap-2 text-sm font-semibold text-gray-700 px-2">
                <div class="col-span-1"></div>
                <div class="col-span-5">KEY</div>
                <div class="col-span-5">VALUE</div>
                <div class="col-span-1"></div>
              </div>
              <div class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" checked class="rounded" />
                </div>
                <div class="col-span-5">
                  <Input model-value="Content-Type" class="border-gray-300" />
                </div>
                <div class="col-span-5">
                  <Input model-value="application/json" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600">
                    ×
                  </Button>
                </div>
              </div>
              <div class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" checked class="rounded" />
                </div>
                <div class="col-span-5">
                  <Input model-value="Authorization" class="border-gray-300" />
                </div>
                <div class="col-span-5">
                  <Input model-value="Bearer ${API_TOKEN}" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600">
                    ×
                  </Button>
                </div>
              </div>
            </div>
          </TabsContent>

          <TabsContent value="body" class="space-y-4">
            <div>
              <Select default-value="json">
                <SelectTrigger class="w-48 border-gray-300 mb-3">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="json">JSON</SelectItem>
                  <SelectItem value="form">Form Data</SelectItem>
                  <SelectItem value="raw">Raw Text</SelectItem>
                </SelectContent>
              </Select>
              <Textarea
                class="font-mono text-sm min-h-[300px] bg-gray-900 text-green-400 border-gray-700"
                :model-value="JSON.stringify({
                  username: 'testuser',
                  password: 'Test@123456'
                }, null, 2)"
              />
            </div>
          </TabsContent>

          <TabsContent value="assertions" class="space-y-4">
            <div class="space-y-3">
              <div class="grid grid-cols-12 gap-2 text-sm font-semibold text-gray-700 px-2">
                <div class="col-span-1"></div>
                <div class="col-span-3">类型</div>
                <div class="col-span-4">路径/字段</div>
                <div class="col-span-3">期望值</div>
                <div class="col-span-1"></div>
              </div>
              <div class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" checked class="rounded" />
                </div>
                <div class="col-span-3">
                  <Select default-value="status">
                    <SelectTrigger class="border-gray-300">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="status">状态码</SelectItem>
                      <SelectItem value="json">JSON路径</SelectItem>
                      <SelectItem value="time">响应时间</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div class="col-span-4">
                  <Input model-value="-" disabled class="border-gray-300 bg-gray-50" />
                </div>
                <div class="col-span-3">
                  <Input model-value="200" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600">
                    ×
                  </Button>
                </div>
              </div>
              <div class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" checked class="rounded" />
                </div>
                <div class="col-span-3">
                  <Select default-value="json">
                    <SelectTrigger class="border-gray-300">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="status">状态码</SelectItem>
                      <SelectItem value="json">JSON路径</SelectItem>
                      <SelectItem value="time">响应时间</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div class="col-span-4">
                  <Input model-value="$.data.success" class="border-gray-300" />
                </div>
                <div class="col-span-3">
                  <Input model-value="true" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600">
                    ×
                  </Button>
                </div>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>

    <!-- Response Panel -->
    <Card class="border-gray-200">
      <CardContent class="pt-6">
        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="font-semibold text-gray-900">响应结果</h3>
            <div v-if="statusCode" class="flex items-center gap-4">
              <span :class="`inline-flex items-center gap-1 px-3 py-1 rounded ${
                statusCode === 200 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
              } text-sm font-medium`">
                Status: {{ statusCode }} {{ statusCode === 200 ? 'OK' : 'Error' }}
              </span>
              <span class="flex items-center gap-1 text-sm text-gray-600">
                <Clock class="w-4 h-4" />
                {{ responseTime }}ms
              </span>
            </div>
          </div>

          <div v-if="responseData" class="bg-gray-900 text-green-400 p-4 rounded font-mono text-sm overflow-x-auto">
            <pre>{{ JSON.stringify(responseData, null, 2) }}</pre>
          </div>
          <div v-else class="bg-gray-50 border-2 border-dashed border-gray-300 rounded p-8 text-center text-gray-500">
            点击"发送"按钮执行请求查看响应结果
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
