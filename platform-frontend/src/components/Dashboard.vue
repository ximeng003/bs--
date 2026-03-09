<script setup lang="ts">
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { CheckCircle, XCircle, Clock, TrendingUp, RefreshCw, PlayCircle, AlertCircle, Activity } from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
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
    type: 'scroll',
    top: 5,
    left: 'center',
    itemGap: 35,
    padding: [0, 10],
    data: [
      { name: 'API用例执行数', icon: 'roundRect' },
      { name: 'WEB用例执行数', icon: 'roundRect' },
      { name: 'APP用例执行数', icon: 'roundRect' },
      { name: 'API用例通过率', icon: 'circle' },
      { name: 'WEB用例通过率', icon: 'circle' },
      { name: 'APP用例通过率', icon: 'circle' }
    ]
  },
  grid: {
    top: 70,
    left: '3%',
    right: '15%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: true,
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
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#2F80ED', width: 2, type: 'solid' },
      itemStyle: { color: '#2F80ED' },
      emphasis: { focus: 'series' }
    },
    {
      name: 'WEB用例通过率',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: [],
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#27AE60', width: 2, type: 'solid' },
      itemStyle: { color: '#27AE60' },
      emphasis: { focus: 'series' }
    },
    {
      name: 'APP用例通过率',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: [],
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#F2994A', width: 2, type: 'solid' },
      itemStyle: { color: '#F2994A' },
      emphasis: { focus: 'series' }
    }
  ]
})

const router = useRouter()

// Recent activities
const recentActivities = ref<any[]>([])
const myRunningTasks = ref<any[]>([])
const myFailedPlans = ref<any[]>([])
const passCount = ref(0)
const failCount = ref(0)
const loading = ref(false)

const backendDown = ref(false)
const healthScore = ref(0)
const healthMeta = ref({ passRate: 0, coverage: 0, stability: 0 })
const topFailedCases = ref<any[]>([])

const fetchData = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res: any = await request.get('/dashboard/stats', {
      params: { _t: Date.now() } // Prevent caching
    })
    console.log('Dashboard stats updated:', res)
    backendDown.value = false
    if (!res) {
      console.error('Empty dashboard stats response')
      return
    }

    // Update My Running Tasks
    if (res.myRunningTasks) {
      myRunningTasks.value = res.myRunningTasks.map((task: any) => ({
        id: task.id,
        name: task.name,
        type: task.type,
        reportId: task.reportId,
        startTime: new Date(task.lastRunTime).toLocaleString()
      }))
    }

    // Update My Failed Plans
    if (res.myFailedPlans) {
      myFailedPlans.value = res.myFailedPlans.map((plan: any) => ({
        id: plan.id,
        name: plan.name,
        type: plan.type,
        reportId: plan.reportId,
        failTime: new Date(plan.lastRunTime).toLocaleString()
      }))
    }
    
    // Top Failed Cases
    if (Array.isArray(res.topFailedCases)) {
      topFailedCases.value = res.topFailedCases
    } else {
      topFailedCases.value = []
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
      // Don't zero out execution stats just because total cases is 0
      // It might be that cases were deleted but reports exist?
      // Or maybe API returned correct execution counts but 0 cases?
      // totalExecutions = 0
      // passedCases = 0
      // failedCases = 0
    }

    stats.value[0].value = totalCases.toString()
    stats.value[1].value = totalExecutions.toString()
    stats.value[2].value = passedCases.toString()
    stats.value[3].value = failedCases.toString()
    passCount.value = passedCases
    failCount.value = failedCases
    
    // Health score
    healthScore.value = Number((res as any).healthScore ?? 0)
    healthMeta.value = {
      passRate: Number((res as any).passRate ?? 0),
      coverage: Number((res as any).coverage ?? 0),
      stability: Number((res as any).stability ?? 0),
    }

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
    recentActivities.value = res.recentActivity.map((item: any, index: number) => {
      let timeStr = item.timeAgo
      if (item.timestamp) {
        const date = new Date(item.timestamp)
        // Format as YYYY-MM-DD HH:mm:ss
        const y = date.getFullYear()
        const m = String(date.getMonth() + 1).padStart(2, '0')
        const d = String(date.getDate()).padStart(2, '0')
        const h = String(date.getHours()).padStart(2, '0')
        const min = String(date.getMinutes()).padStart(2, '0')
        const s = String(date.getSeconds()).padStart(2, '0')
        timeStr = `${y}-${m}-${d} ${h}:${min}:${s}`
      }
      
      return {
        id: index,
        type: item.status,
        case: item.caseName || 'Unknown Case',
        time: timeStr,
        executor: item.executedBy || 'System'
      }
    })
    
  } catch (e: any) {
    console.error('Failed to fetch dashboard data', e)
    // Only show backend down message if it's a network error (no response)
    // Check for e.config (Axios error) and !e.response (no response received)
    if (e.config && !e.response) {
      backendDown.value = true
    } else {
      backendDown.value = false
    }
  } finally {
    loading.value = false
  }
}

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    fetchData()
  }
}

onMounted(() => {
  fetchData()
  const timer = setInterval(fetchData, 30000)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  onUnmounted(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })
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

const handleRunningTaskClick = (task: any) => {
   if (task.reportId) {
     router.push(`/reports/${task.reportId}`)
   } else if (task.type === 'Plan') {
     router.push('/plans')
   } else {
     router.push('/api-cases')
   }
 }
 
 const handleFailedItemClick = (item: any) => {
   if (item.reportId) {
     router.push(`/reports/${item.reportId}`)
   } else if (item.type === 'Plan') {
     router.push('/reports?status=failed')
   } else {
     // For single case failure, we can search by case name
     const pureName = item.name.replace('[单例] ', '')
     router.push({ path: '/reports', query: { status: 'failed', keyword: pureName } })
   }
 }
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-2xl font-bold text-gray-900">仪表盘</h1>
      <Button variant="outline" size="sm" @click="fetchData" :disabled="loading">
        <RefreshCw class="w-4 h-4 mr-2" :class="{ 'animate-spin': loading }" />
        刷新数据
      </Button>
    </div>
    
    <Card class="border-gray-200">
      <CardContent class="py-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <Activity class="w-6 h-6 text-indigo-600" />
            <div>
              <p class="text-sm text-gray-600">项目质量健康分</p>
              <div class="text-4xl font-semibold text-gray-900">{{ Math.round(healthScore) }}</div>
            </div>
          </div>
          <div class="grid grid-cols-3 gap-8">
            <div>
              <p class="text-xs text-gray-500">通过率</p>
              <div class="text-lg font-medium text-green-600">{{ Math.round(healthMeta.passRate) }}%</div>
            </div>
            <div>
              <p class="text-xs text-gray-500">覆盖率</p>
              <div class="text-lg font-medium text-blue-600">{{ Math.round(healthMeta.coverage) }}%</div>
            </div>
            <div>
              <p class="text-xs text-gray-500">稳定性</p>
              <div class="text-lg font-medium text-purple-600">{{ Math.round(healthMeta.stability) }}%</div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>

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

    <!-- Workbench Snapshot -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- My Running Tasks -->
      <Card class="border-gray-200">
        <CardHeader class="flex flex-row items-center justify-between pb-2">
          <CardTitle class="text-lg font-medium">我的任务 (运行中)</CardTitle>
          <PlayCircle class="w-5 h-5 text-blue-500" />
        </CardHeader>
        <CardContent>
          <div v-if="myRunningTasks.length > 0" class="space-y-4">
            <div v-for="task in myRunningTasks" :key="task.id" 
              class="flex items-center justify-between p-3 bg-blue-50 rounded-lg cursor-pointer hover:bg-blue-100 transition-colors"
              @click="handleRunningTaskClick(task)"
            >
              <div class="flex flex-col">
                <span class="font-medium text-gray-900">{{ task.name }}</span>
                <span class="text-xs text-gray-500">开始时间: {{ task.startTime }}</span>
              </div>
              <div class="flex items-center">
                <span class="animate-pulse inline-block w-2 h-2 rounded-full bg-blue-500 mr-2"></span>
                <span class="text-sm text-blue-600">运行中</span>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-8 text-gray-500 text-sm">
            暂无运行中的任务
          </div>
        </CardContent>
      </Card>
      
      <!-- Top Failed Cases -->
      <Card class="border-gray-200">
        <CardHeader class="flex flex-row items-center justify-between pb-2">
          <CardTitle>失败分布 TOP N</CardTitle>
        </CardHeader>
        <CardContent>
          <div v-if="topFailedCases.length === 0" class="text-sm text-gray-500">暂无失败集中项</div>
          <div v-else class="space-y-2">
            <div v-for="item in topFailedCases" :key="item.id" class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Badge variant="outline" class="text-xs">失败次数 {{ item.count || 0 }}</Badge>
                <span class="text-sm text-gray-700">{{ item.name }}</span>
              </div>
              <Button variant="ghost" size="sm" @click="goToReportsByKeyword(item.name)">查看报告</Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Exception Reminders -->
      <Card class="border-gray-200">
        <CardHeader class="flex flex-row items-center justify-between pb-2">
          <CardTitle class="text-lg font-medium">异常提醒 (最近失败)</CardTitle>
          <AlertCircle class="w-5 h-5 text-red-500" />
        </CardHeader>
        <CardContent>
          <div v-if="myFailedPlans.length > 0" class="space-y-4">
            <div v-for="plan in myFailedPlans" :key="plan.id" 
              class="flex items-center justify-between p-3 bg-red-50 rounded-lg cursor-pointer hover:bg-red-100 transition-colors" 
              @click="handleFailedItemClick(plan)"
            >
              <div class="flex flex-col">
                <span class="font-medium text-gray-900">{{ plan.name }}</span>
                <span class="text-xs text-gray-500">失败时间: {{ plan.failTime }}</span>
              </div>
              <div class="flex items-center">
                <span class="inline-block w-2 h-2 rounded-full bg-red-500 mr-2"></span>
                <span class="text-sm text-red-600">执行失败</span>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-8 text-gray-500 text-sm">
            暂无失败的任务计划
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
