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
import { ref, onMounted } from 'vue'
import request from '@/api/request'

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
const stats = ref([
  {
    title: '总用例数',
    value: '0',
    change: '-',
    icon: TrendingUp,
    color: 'bg-blue-500',
    textColor: 'text-blue-600'
  },
  {
    title: '通过用例',
    value: '0',
    change: '-',
    icon: CheckCircle,
    color: 'bg-green-500',
    textColor: 'text-green-600'
  },
  {
    title: '失败用例',
    value: '0',
    change: '-',
    icon: XCircle,
    color: 'bg-red-500',
    textColor: 'text-red-600'
  },
  {
    title: '平均耗时',
    value: '0s',
    change: '-',
    icon: Clock,
    color: 'bg-yellow-500',
    textColor: 'text-yellow-600'
  }
])

const pieOption = ref<any>({
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
      data: []
    }
  ]
})

const barOption = ref<any>({
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
    data: []
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '通过',
      type: 'bar',
      data: [],
      itemStyle: { color: '#67C23A' },
      barMaxWidth: 40
    },
    {
      name: '失败',
      type: 'bar',
      data: [],
      itemStyle: { color: '#F56C6C' },
      barMaxWidth: 40
    }
  ]
})

// Recent activities
const recentActivities = ref<any[]>([])

const fetchData = async () => {
  try {
    const res: any = await request.get('/dashboard/stats')
    
    // Update Stats
    stats.value[0].value = res.totalCases.toString()
    stats.value[1].value = res.passedCases.toString()
    stats.value[2].value = res.failedCases.toString()
    stats.value[3].value = res.avgDuration + 's'
    
    // Update Donut Chart
    const donutData = [
      { name: '通过', value: res.passedCases, color: '#67C23A' },
      { name: '失败', value: res.failedCases, color: '#F56C6C' }
    ]
    
    pieOption.value = {
      ...pieOption.value,
      series: [{
        ...pieOption.value.series[0],
        data: donutData.map(item => ({
          value: item.value,
          name: item.name,
          itemStyle: { color: item.color }
        }))
      }]
    }
    
    // Update Bar Chart
    const dates = res.dailyTrend.map((d: any) => d.date)
    const passed = res.dailyTrend.map((d: any) => d.passed)
    const failed = res.dailyTrend.map((d: any) => d.failed)
    
    barOption.value = {
      ...barOption.value,
      xAxis: {
        type: 'category',
        data: dates
      },
      series: [
        {
          name: '通过',
          type: 'bar',
          data: passed,
          itemStyle: { color: '#67C23A' },
          barMaxWidth: 40
        },
        {
          name: '失败',
          type: 'bar',
          data: failed,
          itemStyle: { color: '#F56C6C' },
          barMaxWidth: 40
        }
      ]
    }
    
    // Update Recent Activities
    recentActivities.value = res.recentActivity.map((item: any, index: number) => ({
      id: index,
      type: item.status,
      case: item.caseName || 'Unknown Case',
      time: item.timeAgo,
      executor: item.executedBy || 'System'
    }))
    
  } catch (e) {
    console.error('Failed to fetch dashboard data', e)
  }
}

onMounted(() => {
  fetchData()
})

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
