<script setup lang="ts">
import { Card, CardContent } from '@/components/ui/card'
import Badge from '@/components/ui/badge/Badge.vue'
import Button from '@/components/ui/button/Button.vue'
import { CheckCircle, XCircle, Clock, Download, Share2, AlertCircle } from 'lucide-vue-next'

const reportData = {
  status: 'failed',
  caseName: 'Web支付流程测试',
  executionTime: '2026-01-02 15:30:25',
  duration: '15.8s',
  engine: '本地引擎-张三',
  environment: 'staging',
  totalSteps: 12,
  passedSteps: 10,
  failedSteps: 2
}

const executionSteps = [
  { step: 1, action: '打开浏览器', status: 'success', time: '0.5s', details: 'Chrome 120.0.0' },
  { step: 2, action: '访问URL: https://example.com/checkout', status: 'success', time: '1.2s', details: '页面加载成功' },
  { step: 3, action: '等待元素: #product-list', status: 'success', time: '0.3s', details: '元素已出现' },
  { step: 4, action: '点击元素: .add-to-cart', status: 'success', time: '0.2s', details: '商品已添加到购物车' },
  { step: 5, action: '点击元素: .checkout-btn', status: 'success', time: '0.3s', details: '进入结算页面' },
  { step: 6, action: '等待元素: #payment-form', status: 'success', time: '0.8s', details: '支付表单已加载' },
  { step: 7, action: '输入文本: #card-number, 4111111111111111', status: 'success', time: '0.5s', details: '卡号输入完成' },
  { step: 8, action: '输入文本: #cvv, 123', status: 'success', time: '0.2s', details: 'CVV输入完成' },
  { step: 9, action: '点击元素: #submit-payment', status: 'success', time: '0.4s', details: '提交支付请求' },
  { step: 10, action: '等待元素: .payment-result', status: 'success', time: '3.5s', details: '等待支付响应' },
  { step: 11, action: '断言文本: .payment-status, 支付成功', status: 'failed', time: '0.1s', details: '期望: "支付成功", 实际: "支付处理中"', error: true },
  { step: 12, action: '截图: payment_result.png', status: 'failed', time: '0.2s', details: '由于上一步失败，测试中止', error: true }
]

const consoleLogs = [
  { time: '15:30:25.123', level: 'info', message: '[TestEngine] 开始执行测试用例: Web支付流程测试' },
  { time: '15:30:25.456', level: 'info', message: '[Browser] 启动Chrome浏览器...' },
  { time: '15:30:26.001', level: 'success', message: '[Step 1] ✓ 打开浏览器' },
  { time: '15:30:26.234', level: 'info', message: '[Navigator] 访问 https://example.com/checkout' },
  { time: '15:30:27.456', level: 'success', message: '[Step 2] ✓ 访问URL成功' },
  { time: '15:30:27.789', level: 'success', message: '[Step 3] ✓ 元素已找到: #product-list' },
  { time: '15:30:28.012', level: 'success', message: '[Step 4] ✓ 点击成功: .add-to-cart' },
  { time: '15:30:28.345', level: 'success', message: '[Step 5] ✓ 点击成功: .checkout-btn' },
  { time: '15:30:29.123', level: 'success', message: '[Step 6] ✓ 支付表单已加载' },
  { time: '15:30:29.678', level: 'success', message: '[Step 7] ✓ 卡号输入完成' },
  { time: '15:30:29.890', level: 'success', message: '[Step 8] ✓ CVV输入完成' },
  { time: '15:30:30.234', level: 'success', message: '[Step 9] ✓ 提交支付请求' },
  { time: '15:30:33.789', level: 'success', message: '[Step 10] ✓ 支付响应已返回' },
  { time: '15:30:33.890', level: 'error', message: '[Step 11] ✗ 断言失败: 文本不匹配' },
  { time: '15:30:33.891', level: 'error', message: '[Assertion] 期望值: "支付成功"' },
  { time: '15:30:33.892', level: 'error', message: '[Assertion] 实际值: "支付处理中"' },
  { time: '15:30:34.001', level: 'error', message: '[TestEngine] 测试失败，中止执行' },
  { time: '15:30:34.123', level: 'info', message: '[Browser] 关闭浏览器' },
  { time: '15:30:34.234', level: 'info', message: '[TestEngine] 生成测试报告' }
]
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- Report Header -->
    <Card class="border-gray-200">
      <CardContent class="pt-6">
        <div class="flex items-start justify-between mb-6">
          <div>
            <div class="flex items-center gap-3 mb-2">
              <h2 class="text-2xl font-semibold text-gray-900">{{ reportData.caseName }}</h2>
              <Badge v-if="reportData.status === 'success'" class="bg-green-500 text-white px-3 py-1">
                <CheckCircle class="w-4 h-4 mr-1" />
                PASS
              </Badge>
              <Badge v-else class="bg-red-500 text-white px-3 py-1">
                <XCircle class="w-4 h-4 mr-1" />
                FAIL
              </Badge>
            </div>
            <div class="flex items-center gap-4 text-sm text-gray-600">
              <span class="flex items-center gap-1">
                <Clock class="w-4 h-4" />
                {{ reportData.executionTime }}
              </span>
              <span>耗时: {{ reportData.duration }}</span>
              <span>引擎: {{ reportData.engine }}</span>
              <span>环境: {{ reportData.environment }}</span>
            </div>
          </div>
          <div class="flex gap-2">
            <Button variant="outline" size="sm" class="border-gray-300">
              <Download class="w-4 h-4 mr-2" />
              导出报告
            </Button>
            <Button variant="outline" size="sm" class="border-gray-300">
              <Share2 class="w-4 h-4 mr-2" />
              分享
            </Button>
          </div>
        </div>

        <!-- Statistics -->
        <div class="grid grid-cols-4 gap-4 p-4 bg-gray-50 rounded-lg">
          <div class="text-center">
            <div class="text-2xl font-semibold text-gray-900">{{ reportData.totalSteps }}</div>
            <div class="text-sm text-gray-600 mt-1">总步骤数</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-semibold text-green-600">{{ reportData.passedSteps }}</div>
            <div class="text-sm text-gray-600 mt-1">通过步骤</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-semibold text-red-600">{{ reportData.failedSteps }}</div>
            <div class="text-sm text-gray-600 mt-1">失败步骤</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-semibold text-blue-600">
              {{ Math.round((reportData.passedSteps / reportData.totalSteps) * 100) }}%
            </div>
            <div class="text-sm text-gray-600 mt-1">通过率</div>
          </div>
        </div>
      </CardContent>
    </Card>

    <div class="grid grid-cols-2 gap-6">
      <!-- Left Column - Execution Steps Timeline -->
      <Card class="border-gray-200">
        <CardContent class="pt-6">
          <h3 class="font-semibold text-gray-900 mb-4">执行步骤时间线</h3>
          <div class="space-y-3 max-h-[600px] overflow-y-auto">
            <div
              v-for="step in executionSteps"
              :key="step.step"
              :class="`relative pl-8 pb-3 border-l-2 ${
                step.status === 'success'
                  ? 'border-green-500'
                  : 'border-red-500'
              }`"
            >
              <!-- Step indicator -->
              <div
                :class="`absolute left-[-9px] top-0 w-4 h-4 rounded-full border-2 ${
                  step.status === 'success'
                    ? 'bg-green-500 border-green-500'
                    : 'bg-red-500 border-red-500'
                }`"
              />
              
              <div class="space-y-1">
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="text-xs text-gray-500">步骤 {{ step.step }}</span>
                      <CheckCircle v-if="step.status === 'success'" class="w-4 h-4 text-green-500" />
                      <XCircle v-else class="w-4 h-4 text-red-500" />
                    </div>
                    <div class="font-medium text-sm text-gray-900 mt-1">
                      {{ step.action }}
                    </div>
                    <div class="text-xs text-gray-600 mt-1">
                      {{ step.details }}
                    </div>
                  </div>
                  <span class="text-xs text-gray-500">{{ step.time }}</span>
                </div>
                
                <div v-if="step.error" class="mt-2 p-2 bg-red-50 border border-red-200 rounded text-xs text-red-800">
                  <div class="flex items-start gap-1">
                    <AlertCircle class="w-3 h-3 mt-0.5 flex-shrink-0" />
                    <span>{{ step.details }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Right Column - Console Logs & Screenshot -->
      <div class="space-y-6">
        <!-- Console Logs -->
        <Card class="border-gray-200">
          <CardContent class="pt-6">
            <h3 class="font-semibold text-gray-900 mb-4">控制台日志</h3>
            <div class="bg-gray-900 text-green-400 p-4 rounded font-mono text-xs h-[300px] overflow-y-auto">
              <div
                v-for="(log, index) in consoleLogs"
                :key="index"
                :class="`mb-1 ${
                  log.level === 'error'
                    ? 'text-red-400'
                    : log.level === 'success'
                    ? 'text-green-400'
                    : 'text-gray-400'
                }`"
              >
                <span class="text-gray-500">[{{ log.time }}]</span> {{ log.message }}
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- Error Screenshot -->
        <Card class="border-gray-200 border-red-300">
          <CardContent class="pt-6">
            <h3 class="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <AlertCircle class="w-5 h-5 text-red-500" />
              错误截图
            </h3>
            <div class="bg-gray-100 border-2 border-dashed border-gray-300 rounded p-8 text-center">
              <div class="space-y-3">
                <div class="w-16 h-16 bg-gray-300 rounded-lg mx-auto flex items-center justify-center">
                  <svg class="w-8 h-8 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm text-gray-600 mb-2">payment_result.png</p>
                  <p class="text-xs text-gray-500">
                    错误发生时的页面截图
                  </p>
                </div>
                <Button size="sm" variant="outline" class="border-gray-300">
                  <Download class="w-4 h-4 mr-2" />
                  下载截图
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>
