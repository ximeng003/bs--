
// 导出测试报告为独立的 HTML 文件
// 包含内联的 Vue 3 应用和所有必要的样式/脚本
export const generateReportHtml = (data: any) => {
  const { planSummary } = data;

  // 将数据序列化为 JSON 字符串，以便嵌入到 HTML 中
  const reportDataJson = JSON.stringify(data).replace(/</g, '\\u003c');

  return `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>测试报告 - ${planSummary?.planName || '测试报告'}</title>
  <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
  <style>
    body { background-color: #f9fafb; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
    .status-badge { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
    .status-success { background-color: #d1fae5; color: #065f46; border: 1px solid #a7f3d0; }
    .status-failed { background-color: #fee2e2; color: #991b1b; border: 1px solid #fecaca; }
    .card { background: white; border-radius: 8px; box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06); }
    .list-item { transition: background-color 0.2s; cursor: pointer; }
    .list-item:hover { background-color: #f3f4f6; }
    .list-item.active { background-color: #eff6ff; border-left: 4px solid #3b82f6; }
    
    /* Scrollbar styling */
    ::-webkit-scrollbar { width: 8px; height: 8px; }
    ::-webkit-scrollbar-track { background: #f1f1f1; }
    ::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 4px; }
    ::-webkit-scrollbar-thumb:hover { background: #9ca3af; }

    pre { white-space: pre-wrap; word-wrap: break-word; }
  </style>
</head>
<body>
  <div id="app" class="min-h-screen flex flex-col">
    <!-- Header -->
    <header class="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between sticky top-0 z-10 shadow-sm">
      <div class="flex items-center gap-4">
        <h1 class="text-xl font-bold text-gray-900">{{ planSummary?.planName || '测试报告详情' }}</h1>
        <span class="px-2 py-1 bg-gray-100 rounded text-xs text-gray-600">计划 ID: {{ planSummary?.planId }}</span>
        <span v-if="planSummary?.environment" class="px-2 py-1 bg-blue-50 text-blue-700 rounded text-xs border border-blue-100">
          {{ formatEnvironment(planSummary.environment) }}
        </span>
        <span class="px-2 py-1 bg-gray-100 rounded text-xs text-gray-600">第 {{ reportDetail?.planRunNo }} 次执行</span>
      </div>
      <div class="text-sm text-gray-500">
        生成时间: {{ new Date().toLocaleString() }}
      </div>
    </header>

    <main class="flex-1 p-6 flex gap-6 overflow-hidden h-[calc(100vh-64px)]">
      <!-- Left Column: Summary & List -->
      <div class="w-1/2 flex flex-col gap-6 h-full overflow-hidden">
        <!-- Summary Card -->
        <div class="card p-6 flex-shrink-0">
          <div class="flex items-center justify-between mb-6">
             <div class="flex gap-8">
               <!-- Pie Chart Area -->
               <div class="relative w-32 h-32">
                 <div ref="chartRef" class="w-full h-full"></div>
                 <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                   <div class="text-xs text-gray-500">通过率</div>
                   <div class="text-lg font-bold text-gray-900">{{ successRate }}%</div>
                 </div>
               </div>
               
               <!-- Stats -->
               <div class="grid grid-cols-2 gap-x-8 gap-y-2 text-sm">
                 <div class="text-gray-500">总耗时</div>
                 <div class="font-medium">{{ formatDuration(planSummary?.durationMs) }}</div>
                 
                 <div class="text-gray-500">循环数</div>
                 <div class="font-medium">执行: {{ planSummary?.total }} &nbsp; 失败: <span class="text-red-600">{{ planSummary?.failed }}</span></div>
                 
                 <div class="text-gray-500">接口请求耗时</div>
                 <div class="font-medium">{{ formatDuration(avgDuration) }} (平均)</div>
                 
                 <div class="text-gray-500">断言数</div>
                 <div class="font-medium">执行: {{ totalAssertions }} &nbsp; 失败: <span class="text-red-600">{{ failedAssertions }}</span></div>
               </div>
             </div>
          </div>
        </div>

        <!-- Filter & Search -->
        <div class="flex items-center gap-4 flex-shrink-0">
          <div class="flex bg-white rounded-lg border border-gray-200 p-1">
            <button 
              v-for="filter in ['all', 'success', 'failed']" 
              :key="filter"
              @click="currentFilter = filter"
              class="px-3 py-1 text-sm rounded-md transition-colors"
              :class="currentFilter === filter ? 'bg-gray-100 font-medium text-gray-900' : 'text-gray-600 hover:bg-gray-50'"
            >
              {{ filterLabels[filter] }} 
              <span class="text-xs ml-1 opacity-70">({{ getFilterCount(filter) }})</span>
            </button>
          </div>
          <div class="relative flex-1">
            <input 
              v-model="searchQuery"
              type="text" 
              placeholder="搜索用例名称或URL..." 
              class="w-full pl-8 pr-4 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
            <svg class="w-4 h-4 text-gray-400 absolute left-2.5 top-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
          </div>
        </div>

        <!-- Test Case List -->
        <div class="card flex-1 overflow-y-auto">
          <div v-if="filteredItems.length === 0" class="p-8 text-center text-gray-500">
            没有找到匹配的测试用例
          </div>
          <div v-else class="divide-y divide-gray-100">
            <div 
              v-for="item in filteredItems" 
              :key="item.reportId"
              @click="selectItem(item)"
              class="list-item p-4 flex items-center justify-between group"
              :class="{ 'active': selectedItem?.reportId === item.reportId }"
            >
              <div class="flex items-center gap-3 overflow-hidden">
                <span 
                  class="flex-shrink-0 px-2 py-0.5 rounded text-xs font-medium border"
                  :class="item.status === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'"
                >
                  {{ item.status === 'success' ? '通过' : '失败' }}
                </span>
                <div class="flex flex-col overflow-hidden">
                  <span class="text-sm font-medium text-gray-900 truncate" :title="item.caseName || item.url">
                    {{ item.caseName || item.url || ('测试用例 #' + item.caseId) }}
                  </span>
                  <span v-if="item.method || (item.caseName && item.url)" class="text-xs text-gray-500 font-mono mt-0.5 truncate" :title="item.url">
                    <span v-if="item.method">{{ item.method }}</span>
                    <span v-if="item.method && item.caseName && item.url" class="text-gray-400 mx-1">|</span>
                    <span v-if="item.caseName && item.url">{{ item.url }}</span>
                  </span>
                </div>
              </div>
              <div class="flex items-center gap-4 text-xs text-gray-500 flex-shrink-0">
                <span>{{ formatDuration(item.durationMs) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Details -->
      <div class="w-1/2 h-full card flex flex-col overflow-hidden border border-gray-200 shadow-sm">
        <div v-if="!selectedItem" class="h-full flex flex-col items-center justify-center text-gray-400">
          <svg class="w-12 h-12 mb-4 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
          <p>请选择左侧列表中的测试用例查看详情</p>
        </div>
        
        <div v-else class="flex flex-col h-full">
          <!-- Detail Header -->
          <div class="p-4 border-b border-gray-100 bg-gray-50">
             <div class="flex items-start justify-between">
               <div class="space-y-1 w-full overflow-hidden">
                 <h3 class="font-semibold text-gray-900 break-all pr-4">{{ selectedItem.caseName || selectedItem.url }}</h3>
                 <div class="flex flex-col gap-1 text-xs text-gray-500">
                   <div v-if="selectedItem.caseName && selectedItem.url" class="font-mono break-all">{{ selectedItem.url }}</div>
                   <div class="flex items-center gap-2">
                     <span v-if="selectedItem.method" class="font-mono bg-gray-200 px-1.5 rounded">{{ selectedItem.method }}</span>
                     <span>来自 {{ selectedItem.source || '报告 #' + selectedItem.reportId }}</span>
                   </div>
                 </div>
               </div>
               <button @click="selectedItem = null" class="text-gray-400 hover:text-gray-600 flex-shrink-0">
                 <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
               </button>
             </div>
             
             <!-- Quick Stats -->
             <div class="mt-4 flex gap-4 text-xs bg-white p-2 rounded border border-gray-100">
               <div>
                 <span class="text-gray-500">HTTP 状态码:</span>
                 <span class="font-medium ml-1" :class="selectedItem.statusCode >= 400 ? 'text-red-600' : 'text-green-600'">
                   {{ selectedItem.statusCode || '-' }}
                 </span>
               </div>
               <div>
                 <span class="text-gray-500">耗时:</span>
                 <span class="font-medium ml-1">{{ formatDuration(selectedItem.durationMs) }}</span>
               </div>
               <div>
                 <span class="text-gray-500">大小:</span>
                 <span class="font-medium ml-1">{{ formatSize(selectedItem.responseSize) }}</span>
               </div>
             </div>
          </div>

          <!-- Tabs -->
          <div class="flex border-b border-gray-200 bg-white">
            <button 
              v-for="tab in tabs" 
              :key="tab.id"
              @click="currentTab = tab.id"
              class="px-4 py-2 text-sm font-medium border-b-2 transition-colors"
              :class="currentTab === tab.id ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'"
            >
              {{ tab.label }}
            </button>
          </div>

          <!-- Tab Content -->
          <div class="flex-1 overflow-y-auto p-4 bg-white font-mono text-sm">
            <!-- Steps Tab -->
            <div v-if="currentTab === 'steps'">
               <div v-if="selectedItem.steps && selectedItem.steps.length" class="overflow-hidden border border-gray-200 rounded">
                 <table class="min-w-full divide-y divide-gray-200">
                   <thead class="bg-gray-50">
                     <tr>
                       <th scope="col" class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-16">步骤</th>
                       <th scope="col" class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">动作</th>
                       <th scope="col" class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-20">状态</th>
                       <th scope="col" class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">详情</th>
                       <th scope="col" class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">耗时</th>
                     </tr>
                   </thead>
                   <tbody class="bg-white divide-y divide-gray-200">
                     <tr v-for="(step, idx) in selectedItem.steps" :key="idx" class="hover:bg-gray-50">
                       <td class="px-3 py-2 whitespace-nowrap text-xs text-gray-500 text-center">{{ step.id || idx + 1 }}</td>
                       <td class="px-3 py-2 whitespace-nowrap text-xs font-medium text-gray-900">{{ step.action }}</td>
                       <td class="px-3 py-2 whitespace-nowrap text-xs">
                         <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full" 
                           :class="step.status === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'">
                           {{ step.status === 'success' ? '成功' : '失败' }}
                         </span>
                       </td>
                       <td class="px-3 py-2 text-xs text-gray-500 break-all">
                         <div v-if="step.details" class="whitespace-pre-wrap">{{ step.details }}</div>
                         <div v-if="step.error" class="text-red-600 mt-1">{{ step.error }}</div>
                       </td>
                       <td class="px-3 py-2 whitespace-nowrap text-xs text-gray-500">{{ step.duration ? step.duration + 'ms' : '-' }}</td>
                     </tr>
                   </tbody>
                 </table>
               </div>
               <div v-else class="text-gray-400 italic text-center mt-4">暂无步骤记录</div>
            </div>

            <!-- Body Tab -->
            <div v-else-if="currentTab === 'body'">
               <div class="flex justify-between items-center mb-2">
                 <span class="text-xs text-gray-500 font-sans">响应体</span>
                 <div class="flex gap-2" v-if="selectedItem.responseBody">
                   <button class="text-xs text-blue-600 hover:underline font-sans" @click="formatJson = !formatJson">
                     {{ formatJson ? '原始内容' : '格式化' }}
                   </button>
                 </div>
               </div>
               <div v-if="!selectedItem.responseBody" class="text-gray-400 italic p-4 text-center border border-dashed border-gray-200 rounded">暂无响应内容</div>
               <pre v-else class="bg-gray-50 p-3 rounded border border-gray-100 text-xs overflow-x-auto">{{ formatBody(selectedItem.responseBody, formatJson) }}</pre>
            </div>

            <!-- Headers Tab -->
            <div v-else-if="currentTab === 'header'">
              <h4 class="text-xs font-bold text-gray-700 mb-2 font-sans">请求头</h4>
              <div v-if="selectedItem.requestHeaders" class="space-y-1 mb-4">
                <div v-for="(val, key) in selectedItem.requestHeaders" :key="key" class="grid grid-cols-3 gap-2 border-b border-gray-50 pb-1">
                  <span class="text-gray-500 col-span-1 truncate" :title="key">{{ key }}</span>
                  <span class="text-gray-900 col-span-2 break-all">{{ val }}</span>
                </div>
              </div>
              <div v-else class="text-gray-400 italic">暂无请求头</div>

              <h4 class="text-xs font-bold text-gray-700 mb-2 mt-4 font-sans">响应头</h4>
              <div v-if="selectedItem.responseHeaders" class="space-y-1">
                <div v-for="(val, key) in selectedItem.responseHeaders" :key="key" class="grid grid-cols-3 gap-2 border-b border-gray-50 pb-1">
                  <span class="text-gray-500 col-span-1 truncate" :title="key">{{ key }}</span>
                  <span class="text-gray-900 col-span-2 break-all">{{ val }}</span>
                </div>
              </div>
               <div v-else class="text-gray-400 italic">暂无响应头</div>
            </div>

            <!-- Cookie Tab -->
            <div v-else-if="currentTab === 'cookie'">
               <div v-if="hasCookies(selectedItem)" class="space-y-2">
                 <!-- Simple key-value display for cookies -->
                 <div v-for="(val, key) in getCookies(selectedItem)" :key="key" class="flex justify-between border-b border-gray-50 pb-1">
                   <span class="font-medium text-gray-700">{{ key }}</span>
                   <span class="text-gray-600 truncate max-w-xs" :title="val">{{ val }}</span>
                 </div>
               </div>
               <div v-else class="text-gray-400 italic text-center mt-4">暂无 Cookie</div>
            </div>

            <!-- Console Tab -->
            <div v-else-if="currentTab === 'console'">
               <div v-if="selectedItem.logs && selectedItem.logs.length" class="space-y-1">
                 <div v-for="(log, idx) in selectedItem.logs" :key="idx" class="font-mono text-xs border-b border-gray-50 pb-0.5 mb-0.5">
                   <span class="text-gray-400 select-none mr-2">{{ idx + 1 }}</span>
                   <span :class="getLogClass(log)">{{ log }}</span>
                 </div>
               </div>
               <div v-else class="text-gray-400 italic text-center mt-4">暂无控制台日志</div>
            </div>

            <!-- Request Tab -->
            <div v-else-if="currentTab === 'request'">
              <div class="mb-4">
                <span class="text-xs text-gray-500 block mb-1 font-sans">URL</span>
                <div class="bg-gray-50 p-2 rounded border border-gray-100 break-all">{{ selectedItem.url }}</div>
              </div>
              <div class="mb-4">
                <span class="text-xs text-gray-500 block mb-1 font-sans">请求方法</span>
                <div class="bg-gray-50 p-2 rounded border border-gray-100 inline-block">{{ selectedItem.method }}</div>
              </div>
              <div>
                <span class="text-xs text-gray-500 block mb-1 font-sans">请求内容</span>
                <pre class="bg-gray-50 p-3 rounded border border-gray-100 text-xs overflow-x-auto">{{ formatBody(selectedItem.requestBody, true) }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>

  <script>
    // Inject Data
    window.REPORT_DATA = ${reportDataJson};

    const { createApp, ref, computed, onMounted, watch, nextTick } = Vue;

    createApp({
      setup() {
        const data = window.REPORT_DATA;
        const planSummary = ref(data.planSummary || {});
        const planItems = ref(data.planItems || []); // Array of { ...summaryItem, ...details }
        
        const currentFilter = ref('all');
        const searchQuery = ref('');
        const selectedItem = ref(null);
        const currentTab = ref('body');
        const formatJson = ref(true);
        const chartRef = ref(null);

        const filterLabels = {
          all: '全部',
          success: '通过',
          failed: '失败'
        };

        const tabs = [
          { id: 'steps', label: '步骤' },
          { id: 'body', label: '响应内容' },
          { id: 'cookie', label: 'Cookie' },
          { id: 'header', label: '头部信息' },
          { id: 'console', label: '控制台' },
          { id: 'request', label: '实际请求' }
        ];

        // Derived Stats
        const successRate = computed(() => {
          if (!planSummary.value.total) return 0;
          return Math.round((planSummary.value.success * 10000) / planSummary.value.total) / 100;
        });

        const avgDuration = computed(() => {
          return planSummary.value.avgDurationMs || 0;
        });
        
        const totalAssertions = computed(() => {
           // Placeholder logic if we had assertion counts per item
           return 0; 
        });
        
        const failedAssertions = computed(() => {
           return 0;
        });

        // Filtering
        const filteredItems = computed(() => {
          let items = planItems.value;
          
          if (currentFilter.value !== 'all') {
            items = items.filter(i => i.status === currentFilter.value);
          }
          
          if (searchQuery.value) {
            const q = searchQuery.value.toLowerCase();
            items = items.filter(i => 
              (i.caseName && i.caseName.toLowerCase().includes(q)) || 
              (i.url && i.url.toLowerCase().includes(q))
            );
          }
          
          return items;
        });

        const getFilterCount = (filter) => {
          if (filter === 'all') return planItems.value.length;
          return planItems.value.filter(i => i.status === filter).length;
        };

        const selectItem = (item) => {
          selectedItem.value = item;
          // 默认选中 Steps，如果没有则选中 Body
          currentTab.value = 'steps';
        };

        // Formatters
        const formatDuration = (ms) => {
          if (ms == null) return '-';
          if (ms < 1000) return ms + ' ms';
          return (ms / 1000).toFixed(2) + ' s';
        };

        const formatEnvironment = (env) => {
          const map = { dev: '开发环境', staging: '测试环境', production: '生产环境' };
          return map[env] || env;
        };
        
        const formatSize = (bytes) => {
           if (!bytes) return '-';
           if (bytes < 1024) return bytes + ' B';
           return (bytes / 1024).toFixed(2) + ' KB';
        };

        const formatBody = (body, pretty) => {
          if (!body) return '';
          if (typeof body === 'object') {
             return JSON.stringify(body, null, pretty ? 2 : 0);
          }
          try {
             if (pretty) {
               const obj = JSON.parse(body);
               return JSON.stringify(obj, null, 2);
             }
          } catch (e) {}
          return body;
        };
        
        const getLogClass = (log) => {
           const lower = log.toLowerCase();
           if (lower.includes('error') || lower.includes('fail')) return 'text-red-600';
           if (lower.includes('success') || lower.includes('pass')) return 'text-green-600';
           return 'text-gray-600';
        };
        
        const hasCookies = (item) => {
           return item.cookies && Object.keys(item.cookies).length > 0;
        };
        
        const getCookies = (item) => {
           return item.cookies || {};
        };

        // Chart
        const initChart = () => {
          if (!chartRef.value || !window.echarts) return;
          const chart = echarts.init(chartRef.value);
          const option = {
            series: [
              {
                type: 'pie',
                radius: ['70%', '90%'],
                avoidLabelOverlap: false,
                label: { show: false },
                data: [
                  { value: planSummary.value.success, name: '通过', itemStyle: { color: '#10b981' } },
                  { value: planSummary.value.failed, name: '失败', itemStyle: { color: '#ef4444' } }
                ]
              }
            ]
          };
          chart.setOption(option);
          
          window.addEventListener('resize', () => chart.resize());
        };

        onMounted(() => {
          nextTick(() => {
             initChart();
          });
        });

        return {
          planSummary,
          reportDetail: data.reportDetail,
          successRate,
          avgDuration,
          totalAssertions,
          failedAssertions,
          currentFilter,
          searchQuery,
          filteredItems,
          filterLabels,
          getFilterCount,
          selectedItem,
          selectItem,
          tabs,
          currentTab,
          formatDuration,
          formatEnvironment,
          formatSize,
          formatBody,
          formatJson,
          chartRef,
          getLogClass,
          hasCookies,
          getCookies
        };
      }
    }).mount('#app');
  </script>
</body>
</html>
  `;
};
