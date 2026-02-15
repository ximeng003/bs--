<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Textarea from '@/components/ui/textarea/Textarea.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Play, Save, Clock } from 'lucide-vue-next'
import request from '@/api/request'
import { showToast } from '@/lib/notify'

const route = useRoute()
const router = useRouter()
const caseId = ref<string | null>(null)
const name = ref('New API Case')

const method = ref('GET')
const url = ref('https://api.example.com')
const bodyType = ref('json')
const bodyContent = ref('{}')

const responseData = ref<any>(null)
const statusCode = ref<number | null>(null)
const responseTime = ref<number | null>(null)

// For simplicity, we manage headers/params as arrays
const headers = ref([{ key: 'Content-Type', value: 'application/json', active: true }])
const params = ref([{ key: '', value: '', active: true }])
const assertions = ref([{ type: 'status', path: '', value: '200', active: true }])

onMounted(async () => {
    const id = route.params.id as string
    if (id) {
        caseId.value = id
        await loadCase(id)
    }
})

const loadCase = async (id: string) => {
    try {
        const res: any = await request.get(`/testcases/${id}`)
        if (res) {
            name.value = res.name
            // Parse content JSON
            try {
                const content = JSON.parse(res.content || '{}')
                method.value = content.method || 'GET'
                url.value = content.url || ''
                bodyType.value = content.bodyType || 'json'
                bodyContent.value = content.body || '{}'
                headers.value = content.headers || []
                params.value = content.params || []
                assertions.value = content.assertions || []
            } catch (e) {
                console.error('Failed to parse content', e)
            }
        }
    } catch (e) {
        console.error('Failed to load case', e)
    }
}

const handleSave = async () => {
    const content = JSON.stringify({
        method: method.value,
        url: url.value,
        bodyType: bodyType.value,
        body: bodyContent.value,
        headers: headers.value,
        params: params.value,
        assertions: assertions.value
    })
    
    const payload = {
        id: caseId.value,
        name: name.value,
        type: 'API',
        content: content,
        description: 'Updated from API Editor',
        status: 'active',
        environment: 'dev' // Default
    }
    
    try {
        if (caseId.value) {
            await request.put('/testcases', payload)
        } else {
            const newId: any = await request.post('/testcases', payload)
            if (newId) {
                caseId.value = String(newId)
            }
        }
        showToast('保存成功', 'success')
        router.push('/api-cases')
    } catch (e) {
        showToast('保存失败', 'error')
    }
}

const handleSend = async () => {
  try {
    const payload = {
        method: method.value,
        url: url.value,
        bodyType: bodyType.value,
        body: bodyContent.value,
        headers: headers.value,
        params: params.value
    }
    
    // Call backend execute endpoint
    const res: any = await request.post('/testcases/execute', payload)
    
    statusCode.value = res.statusCode
    responseTime.value = res.time
    
    // Parse body if it is JSON string for better display
    let parsedBody = res.body
    try {
        if (typeof res.body === 'string' && (res.body.startsWith('{') || res.body.startsWith('['))) {
            parsedBody = JSON.parse(res.body)
        }
    } catch (e) {
        // keep as string if parse fails
    }

    responseData.value = parsedBody
    
  } catch (e: any) {
    console.error('Failed to execute request', e)
    responseData.value = {
        error: e.message || 'Unknown error'
    }
    statusCode.value = 500
    responseTime.value = 0
  }
}

const addHeader = () => headers.value.push({ key: '', value: '', active: true })
const removeHeader = (index: number) => headers.value.splice(index, 1)

const addParam = () => params.value.push({ key: '', value: '', active: true })
const removeParam = (index: number) => params.value.splice(index, 1)

const addAssertion = () => assertions.value.push({ type: 'status', path: '', value: '', active: true })
const removeAssertion = (index: number) => assertions.value.splice(index, 1)

</script>

<template>
  <div class="p-6 space-y-4">
    <!-- Top Bar -->
    <div class="flex items-center gap-4 mb-4">
        <Input v-model="name" placeholder="Case Name" class="w-64" />
    </div>

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
          <Button variant="outline" class="border-gray-300" @click="handleSave">
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
              <div v-for="(item, index) in params" :key="index" class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" v-model="item.active" class="rounded" />
                </div>
                <div class="col-span-5">
                  <Input v-model="item.key" placeholder="参数名" class="border-gray-300" />
                </div>
                <div class="col-span-5">
                  <Input v-model="item.value" placeholder="参数值" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600" @click="removeParam(index)">
                    ×
                  </Button>
                </div>
              </div>
              <Button variant="outline" size="sm" @click="addParam">Add Param</Button>
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
              <div v-for="(item, index) in headers" :key="index" class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" v-model="item.active" class="rounded" />
                </div>
                <div class="col-span-5">
                  <Input v-model="item.key" placeholder="Header Key" class="border-gray-300" />
                </div>
                <div class="col-span-5">
                  <Input v-model="item.value" placeholder="Header Value" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600" @click="removeHeader(index)">
                    ×
                  </Button>
                </div>
              </div>
              <Button variant="outline" size="sm" @click="addHeader">Add Header</Button>
            </div>
          </TabsContent>

          <TabsContent value="body" class="space-y-4">
            <div>
              <Select v-model="bodyType">
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
                v-model="bodyContent"
                class="font-mono text-sm min-h-[300px] bg-gray-900 text-green-400 border-gray-700"
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
              <div v-for="(item, index) in assertions" :key="index" class="grid grid-cols-12 gap-2 items-center">
                <div class="col-span-1 flex justify-center">
                  <input type="checkbox" v-model="item.active" class="rounded" />
                </div>
                <div class="col-span-3">
                  <Select v-model="item.type">
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
                  <Input v-model="item.path" placeholder="Path" class="border-gray-300" />
                </div>
                <div class="col-span-3">
                  <Input v-model="item.value" placeholder="Value" class="border-gray-300" />
                </div>
                <div class="col-span-1">
                  <Button variant="ghost" size="sm" class="text-gray-400 hover:text-red-600" @click="removeAssertion(index)">
                    ×
                  </Button>
                </div>
              </div>
              <Button variant="outline" size="sm" @click="addAssertion">Add Assertion</Button>
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
