<script setup lang="ts">
import { ref, watch } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Textarea from '@/components/ui/textarea/Textarea.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Play, Save, Copy, Code } from 'lucide-vue-next'

const caseName = ref('Web首页功能测试')
const deviceType = ref('web')
const isExecuting = ref(false)
const executionLogs = ref<string[]>([])

const defaultScript = `// Web自动化测试脚本 - 关键字驱动
// 支持自然语言编写测试步骤

// 打开浏览器并访问URL
打开URL: https://example.com

// 等待页面加载完成
等待元素: #login-form, 5000

// 输入用户名和密码
输入文本: #username, testuser
输入文本: #password, Test@123456

// 点击登录按钮
点击元素: #login-button

// 等待跳转
等待元素: .dashboard, 5000

// 验证登录成功
断言文本: .welcome-message, 欢迎回来
断言URL包含: /dashboard

// 截图保存
截图: login_success.png

// 关闭浏览器
关闭浏览器`

const appScript = `// APP自动化测试脚本 - 基于UIAutomator2/WDA
// 支持Android和iOS设备

// 启动应用
启动应用: com.example.app

// 等待首页加载
等待元素: resource-id=home_page, 5000

// 点击登录按钮
点击元素: resource-id=login_btn

// 输入账号密码
输入文本: resource-id=username, 13800138000
输入文本: resource-id=password, 123456

// 提交登录
点击元素: resource-id=submit_btn

// 验证登录成功
等待元素: text=我的, 5000
断言元素存在: resource-id=user_profile

// 截图
截图: app_login_success.png`

const scriptContent = ref(defaultScript)

watch(deviceType, (newValue) => {
  scriptContent.value = newValue === 'web' ? defaultScript : appScript
})

const keywords = [
  { category: '导航操作', items: ['打开URL', '刷新页面', '后退', '前进', '关闭浏览器'] },
  { category: '元素操作', items: ['点击元素', '输入文本', '清空文本', '选择下拉框', '上传文件'] },
  { category: '等待操作', items: ['等待元素', '等待时间', '等待页面加载'] },
  { category: '断言验证', items: ['断言文本', '断言元素存在', '断言URL包含', '断言属性值'] },
  { category: '其他操作', items: ['截图', '滚动到元素', '执行JS脚本', '切换窗口'] }
]

const handleExecute = () => {
  isExecuting.value = true
  executionLogs.value = []
  
  const logs = [
    '[10:30:15] 开始执行测试用例: ' + caseName.value,
    '[10:30:15] 初始化测试引擎...',
    '[10:30:16] ✓ 打开URL: https://example.com',
    '[10:30:17] ✓ 等待元素: #login-form',
    '[10:30:18] ✓ 输入文本: #username',
    '[10:30:18] ✓ 输入文本: #password',
    '[10:30:19] ✓ 点击元素: #login-button',
    '[10:30:21] ✓ 等待元素: .dashboard',
    '[10:30:21] ✓ 断言文本: .welcome-message',
    '[10:30:22] ✓ 断言URL包含: /dashboard',
    '[10:30:22] ✓ 截图: login_success.png',
    '[10:30:23] ✓ 关闭浏览器',
    '[10:30:23] 测试执行完成 - 全部通过'
  ]

  logs.forEach((log, index) => {
    setTimeout(() => {
      executionLogs.value.push(log)
      if (index === logs.length - 1) {
        isExecuting.value = false
      }
    }, index * 500)
  })
}

const copyKeyword = (item: string) => {
  navigator.clipboard.writeText(item)
}
</script>

<template>
  <div class="p-6 space-y-4">
    <!-- Header Controls -->
    <Card class="border-gray-200">
      <CardContent class="pt-6">
        <div class="flex gap-4">
          <div class="flex-1">
            <label class="block text-sm font-medium text-gray-700 mb-2">用例名称</label>
            <Input
              v-model="caseName"
              placeholder="输入测试用例名称"
              class="border-gray-300"
            />
          </div>
          <div class="w-64">
            <label class="block text-sm font-medium text-gray-700 mb-2">设备类型</label>
            <Select v-model="deviceType">
              <SelectTrigger class="border-gray-300">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="web">Web浏览器</SelectItem>
                <SelectItem value="android">Android设备</SelectItem>
                <SelectItem value="ios">iOS设备</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="flex items-end gap-2">
            <Button
              @click="handleExecute"
              :disabled="isExecuting"
              class="bg-[#409EFF] hover:bg-[#3a8ee6]"
            >
              <Play class="w-4 h-4 mr-2" />
              {{ isExecuting ? '执行中...' : '执行' }}
            </Button>
            <Button variant="outline" class="border-gray-300">
              <Save class="w-4 h-4 mr-2" />
              保存
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- Main Editor Area -->
    <div class="grid grid-cols-12 gap-4">
      <!-- Keywords Sidebar -->
      <div class="col-span-3">
        <Card class="border-gray-200 h-full">
          <CardContent class="pt-6">
            <h3 class="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Code class="w-4 h-4" />
              关键字列表
            </h3>
            <div class="space-y-4">
              <div v-for="(category, index) in keywords" :key="index">
                <h4 class="text-sm font-medium text-gray-700 mb-2">{{ category.category }}</h4>
                <div class="space-y-1">
                  <button
                    v-for="(item, idx) in category.items"
                    :key="idx"
                    class="w-full text-left px-3 py-2 text-sm text-gray-600 hover:bg-blue-50 hover:text-[#409EFF] rounded transition-colors"
                    @click="copyKeyword(item)"
                  >
                    {{ item }}
                  </button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- Code Editor -->
      <div class="col-span-9">
        <Card class="border-gray-200 h-full">
          <CardContent class="pt-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="font-semibold text-gray-900">脚本编辑器</h3>
              <Button variant="outline" size="sm" class="border-gray-300">
                <Copy class="w-4 h-4 mr-2" />
                复制代码
              </Button>
            </div>
            <div class="relative">
              <Textarea
                v-model="scriptContent"
                class="font-mono text-sm min-h-[500px] bg-[#1e1e1e] text-[#d4d4d4] border-gray-700 resize-none pl-12"
                :style="{ lineHeight: '1.6', tabSize: '2' }"
              />
              <!-- Line numbers overlay simulation -->
              <div class="absolute left-3 top-3 text-xs text-gray-500 font-mono pointer-events-none select-none">
                <div v-for="i in 35" :key="i" style="line-height: 1.6; height: 21px;">
                  {{ i }}
                </div>
              </div>
            </div>
            <div class="mt-4 p-3 bg-blue-50 border border-blue-200 rounded text-sm text-blue-800">
              💡 提示: 使用自然语言关键字编写测试步骤，支持参数化和变量引用 ${variable}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>

    <!-- Execution Console -->
    <Card v-if="executionLogs.length > 0" class="border-gray-200">
      <CardContent class="pt-6">
        <h3 class="font-semibold text-gray-900 mb-4">执行日志</h3>
        <div class="bg-gray-900 text-green-400 p-4 rounded font-mono text-sm h-64 overflow-y-auto">
          <div v-for="(log, index) in executionLogs" :key="index" class="mb-1">
            {{ log }}
          </div>
          <div v-if="isExecuting" class="mt-2 animate-pulse">
            <span class="inline-block w-2 h-2 bg-green-400 rounded-full mr-2"></span>
            执行中...
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
