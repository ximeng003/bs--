<script setup lang="ts">
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { CheckCircle, XCircle, Clock, TrendingUp } from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

use([
  CanvasRenderer,
  PieChart,
  LineChart,
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
    title: '用例执行总数',
    value: '0',
    change: '-',
    icon: Clock,
    color: 'bg-yellow-500',
    textColor: 'text-yellow-600'
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
    trigger: 'axis'
  },
  legend: {
    icon: 'roundRect',
    data: [
      'API用例执行数',
      'WEB用例执行数',
      'APP用例执行数',
      'API用例通过率',
      'WEB用例通过率',
      'APP用例通过率'
    ]
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: []
  },
  yAxis: [
    {
      type: 'value',
      position: 'left',
      name: '每日执行数'
    },
    {
      type: 'value',
      name: '通过率(%)',
      position: 'right',
      min: 0,
      max: 100,
      axisLabel: {
        formatter: '{value}%'
      }
    }
  ],
  series: [
    {
      name: 'API用例执行数',
      type: 'bar',
      yAxisIndex: 0,
      data: [],
      barMaxWidth: 24,
      itemStyle: { color: '#409EFF' }
    },
    {
      name: 'WEB用例执行数',
      type: 'bar',
      yAxisIndex: 0,
      data: [],
      barMaxWidth: 24,
      itemStyle: { color: '#67C23A' }
    },
    {
      name: 'APP用例执行数',
      type: 'bar',
      yAxisIndex: 0,
      data: [],
      barMaxWidth: 24,
      itemStyle: { color: '#E6A23C' }
    },
    {
      name: 'API用例通过率',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: [],
      lineStyle: { color: '#2F80ED', type: 'dashed' },
      itemStyle: { color: '#2F80ED' }
    },
    {
      name: 'WEB用例通过率',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: [],
      lineStyle: { color: '#27AE60', type: 'dashed' },
      itemStyle: { color: '#27AE60' }
    },
    {
      name: 'APP用例通过率',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: [],
      lineStyle: { color: '#F2994A', type: 'dashed' },
      itemStyle: { color: '#F2994A' }
    }
  ]
})

const router = useRouter()

// Recent activities
const recentActivities = ref<any[]>([])
const passCount = ref(0)
const failCount = ref(0)

const backendDown = ref(false)

const fetchData = async () => {
  try {
    backendDown.value = false
    const res: any = await request.get('/dashboard/stats')
    if (!res) {
      console.error('Empty dashboard stats response')
      return
    }

    let totalCases = Number((res as any).totalCases ?? 0) || 0
    let passedCases = Number((res as any).passedCases ?? 0) || 0
    let failedCases = Number((res as any).failedCases ?? 0) || 0
    let totalExecutions = Number((res as any).totalExecutions ?? 0) || 0

    if (totalCases === 0) {
      try {
        const casesPage: any = await request.get('/testcases', {
          params: { page: 1, size: 1 }
        })
        if (casesPage && typeof casesPage.total === 'number') {
          totalCases = Number(casesPage.total) || 0
        }
      } catch (e) {
        console.error('Fallback fetch totalCases failed', e)
      }
    }

    if (passedCases === 0 || failedCases === 0) {
      try {
        const successPage: any = await request.get('/reports', {
          params: { page: 1, size: 1, status: 'success' }
        })
        if (successPage && typeof successPage.total === 'number') {
          passedCases = Number(successPage.total) || passedCases
        }
      } catch (e) {
        console.error('Fallback fetch passedCases failed', e)
      }

      try {
        const failedPage: any = await request.get('/reports', {
          params: { page: 1, size: 1, status: 'failed' }
        })
        if (failedPage && typeof failedPage.total === 'number') {
          failedCases = Number(failedPage.total) || failedCases
        }
      } catch (e) {
        console.error('Fallback fetch failedCases failed', e)
      }
    }

    if (totalCases === 0) {
      totalExecutions = 0
      passedCases = 0
      failedCases = 0
    }

    stats.value[0].value = totalCases.toString()
    stats.value[1].value = totalExecutions.toString()
    stats.value[2].value = passedCases.toString()
    stats.value[3].value = failedCases.toString()
    passCount.value = passedCases
    failCount.value = failedCases

    // Update Donut Chart
    const donutData = [
      { name: '通过', value: passedCases, color: '#67C23A' },
      { name: '失败', value: failedCases, color: '#F56C6C' }
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
    
    const dates = res.dailyTrend.map((d: any) => d.date)
    const apiCounts = res.dailyTrend.map((d: any) => Number(d.apiCount ?? 0))
    const webCounts = res.dailyTrend.map((d: any) => Number(d.webCount ?? 0))
    const appCounts = res.dailyTrend.map((d: any) => Number(d.appCount ?? 0))
    const apiPassRates = res.dailyTrend.map((d: any) => Number(d.apiPassRate ?? 0))
    const webPassRates = res.dailyTrend.map((d: any) => Number(d.webPassRate ?? 0))
    const appPassRates = res.dailyTrend.map((d: any) => Number(d.appPassRate ?? 0))
    
    barOption.value = {
      ...barOption.value,
      xAxis: {
        ...barOption.value.xAxis,
        data: dates
      },
      series: [
        {
          ...barOption.value.series[0],
          data: apiCounts
        },
        {
          ...barOption.value.series[1],
          data: webCounts
        },
        {
          ...barOption.value.series[2],
          data: appCounts
        },
        {
          ...barOption.value.series[3],
          data: apiPassRates
        },
        {
          ...barOption.value.series[4],
          data: webPassRates
        },
        {
          ...barOption.value.series[5],
          data: appPassRates
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
    backendDown.value = true
  }
}

onMounted(() => {
  fetchData()
  const timer = setInterval(fetchData, 30000)
  onUnmounted(() => clearInterval(timer))
})

const handleStatClick = (index: number) => {
  if (index === 0) {
    router.push('/api-cases')
  } else if (index === 1) {
    router.push('/reports')
  } else if (index === 2) {
    router.push({ path: '/reports', query: { status: 'success' } })
  } else if (index === 3) {
    router.push({ path: '/reports', query: { status: 'failed' } })
  } else {
    router.push('/reports')
  }
}

const goToReportsByStatus = (status: string) => {
  router.push({ path: '/reports', query: { status } })
}

const goToReportsByKeyword = (keyword: string) => {
  router.push({ path: '/reports', query: { keyword } })
}

const handleBarClick = (params: any) => {
  const date = params?.name
  if (typeof date === 'string' && date.length >= 10) {
    router.push({ path: '/reports', query: { date } })
  }
}
</script>

<template>
  <div class="p-6 space-y-6">
    <div v-if="backendDown" class="p-4 rounded-md bg-yellow-50 border border-yellow-200 text-sm text-yellow-700">
      后端服务可能未启动，当前显示的数据为 0。请先运行后台服务，然后刷新页面。
    </div>
    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <Card
        v-for="(stat, index) in stats"
        :key="index"
        class="border-gray-200 hover:shadow-md transition-shadow cursor-pointer"
        @click="handleStatClick(index)"
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
            <div class="flex items-center gap-2 cursor-pointer" @click="goToReportsByStatus('success')">
              <div class="w-3 h-3 rounded-full bg-[#67C23A]"></div>
              <span class="text-sm text-gray-600">通过: {{ passCount }}</span>
            </div>
            <div class="flex items-center gap-2 cursor-pointer" @click="goToReportsByStatus('failed')">
              <div class="w-3 h-3 rounded-full bg-[#F56C6C]"></div>
              <span class="text-sm text-gray-600">失败: {{ failCount }}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Bar Chart -->
      <Card class="border-gray-200">
        <CardHeader>
          <CardTitle>近一周用例执行数据</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="h-80">
            <VChart :option="barOption" autoresize class="w-full h-full" @click="handleBarClick" />
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
                class="border-b border-gray-100 hover:bg-gray-50 cursor-pointer"
                @click="goToReportsByKeyword(activity.case)"
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
