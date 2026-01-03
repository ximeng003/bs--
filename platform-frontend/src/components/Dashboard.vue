<script setup lang="ts">
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { CheckCircle, XCircle, Clock, TrendingUp } from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { ref } from 'vue'

use([
  CanvasRenderer,
  PieChart,
  BarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
])

// Statistics data
const stats = [
  {
    title: '总用例数',
    value: '1,246',
    change: '+12.5%',
    icon: TrendingUp,
    color: 'bg-blue-500',
    textColor: 'text-blue-600'
  },
  {
    title: '通过用例',
    value: '1,108',
    change: '+8.2%',
    icon: CheckCircle,
    color: 'bg-green-500',
    textColor: 'text-green-600'
  },
  {
    title: '失败用例',
    value: '138',
    change: '-3.1%',
    icon: XCircle,
    color: 'bg-red-500',
    textColor: 'text-red-600'
  },
  {
    title: '平均耗时',
    value: '2.8s',
    change: '-15.3%',
    icon: Clock,
    color: 'bg-yellow-500',
    textColor: 'text-yellow-600'
  }
]

// Donut chart data
const donutData = [
  { name: '通过', value: 1108, color: '#67C23A' },
  { name: '失败', value: 138, color: '#F56C6C' }
]

const pieOption = ref({
  tooltip: {
    trigger: 'item'
  },
  series: [
    {
      name: '测试通过率',
      type: 'pie',
      radius: ['60%', '90%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: donutData.map(item => ({
        value: item.value,
        name: item.name,
        itemStyle: { color: item.color }
      }))
    }
  ]
})

// Bar chart data
const barData = [
  { date: '01-27', passed: 145, failed: 12 },
  { date: '01-28', passed: 158, failed: 15 },
  { date: '01-29', passed: 162, failed: 10 },
  { date: '01-30', passed: 148, failed: 18 },
  { date: '01-31', passed: 172, failed: 14 },
  { date: '02-01', passed: 165, failed: 11 },
  { date: '02-02', passed: 158, failed: 16 }
]

const barOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  legend: {},
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: barData.map(d => d.date)
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '通过',
      type: 'bar',
      data: barData.map(d => d.passed),
      itemStyle: { color: '#67C23A' },
      barMaxWidth: 40
    },
    {
      name: '失败',
      type: 'bar',
      data: barData.map(d => d.failed),
      itemStyle: { color: '#F56C6C' },
      barMaxWidth: 40
    }
  ]
})

// Recent activities
const recentActivities = [
  { id: 1, type: 'success', case: '用户登录API测试', time: '5分钟前', executor: '张三' },
  { id: 2, type: 'failed', case: 'Web支付流程测试', time: '15分钟前', executor: '李四' },
  { id: 3, type: 'success', case: 'APP首页加载测试', time: '30分钟前', executor: '王五' },
  { id: 4, type: 'success', case: '数据查询接口测试', time: '1小时前', executor: '赵六' },
  { id: 5, type: 'success', case: 'Web表单提交测试', time: '2小时前', executor: '张三' },
  { id: 6, type: 'failed', case: 'APP推送功能测试', time: '3小时前', executor: '李四' }
]
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <Card
        v-for="(stat, index) in stats"
        :key="index"
        class="border-gray-200 hover:shadow-md transition-shadow"
      >
        <CardContent class="pt-6">
          <div class="flex items-start justify-between">
            <div>
              <p class="text-sm text-gray-600 mb-1">{{ stat.title }}</p>
              <h3 class="text-3xl font-semibold text-gray-900">{{ stat.value }}</h3>
              <p
                class="text-sm mt-2"
                :class="stat.change.startsWith('+') ? 'text-green-600' : 'text-red-600'"
              >
                {{ stat.change }} vs 上周
              </p>
            </div>
            <div
              :class="[stat.color, 'w-12 h-12 rounded-lg flex items-center justify-center']"
            >
              <component :is="stat.icon" class="w-6 h-6 text-white" />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- Charts -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Donut Chart -->
      <Card class="border-gray-200">
        <CardHeader>
          <CardTitle>测试通过率分布</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="h-80 flex items-center justify-center">
            <VChart :option="pieOption" autoresize class="w-full h-full" />
          </div>
          <div class="flex justify-center gap-8 mt-4">
            <div class="flex items-center gap-2">
              <div class="w-3 h-3 rounded-full bg-[#67C23A]"></div>
              <span class="text-sm text-gray-600">通过: 1,108</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="w-3 h-3 rounded-full bg-[#F56C6C]"></div>
              <span class="text-sm text-gray-600">失败: 138</span>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Bar Chart -->
      <Card class="border-gray-200">
        <CardHeader>
          <CardTitle>每日执行趋势</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="h-80">
            <VChart :option="barOption" autoresize class="w-full h-full" />
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- Recent Activities Table -->
    <Card class="border-gray-200">
      <CardHeader>
        <CardTitle>最近活动</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-200">
                <th class="text-left py-3 px-4 text-sm font-semibold text-gray-700">状态</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-gray-700">测试用例</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-gray-700">执行人</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-gray-700">时间</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="activity in recentActivities"
                :key="activity.id"
                class="border-b border-gray-100 hover:bg-gray-50"
              >
                <td class="py-3 px-4">
                  <span
                    v-if="activity.type === 'success'"
                    class="inline-flex items-center gap-1 px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-medium"
                  >
                    <CheckCircle class="w-3 h-3" />
                    成功
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center gap-1 px-2 py-1 bg-red-100 text-red-700 rounded text-xs font-medium"
                  >
                    <XCircle class="w-3 h-3" />
                    失败
                  </span>
                </td>
                <td class="py-3 px-4 text-sm text-gray-900">{{ activity.case }}</td>
                <td class="py-3 px-4 text-sm text-gray-600">{{ activity.executor }}</td>
                <td class="py-3 px-4 text-sm text-gray-500">{{ activity.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
