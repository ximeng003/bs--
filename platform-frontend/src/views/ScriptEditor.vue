<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Card, CardContent } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import Switch from '@/components/ui/switch/Switch.vue'
import { Play, Save, Copy, Code, Square, Loader2 } from 'lucide-vue-next'
import request from '@/api/request'
import { showToast } from '@/lib/notify'

const route = useRoute()

const caseId = ref<string | null>(null)
const caseName = ref('Web首页功能测试')
const deviceType = ref('web')
const selfHealingEnabled = ref(true)
const headedEnabled = ref(false)
const environment = ref('dev')
const saveError = ref('')
const existingDescription = ref<string | null>(null)
const isExecuting = ref(false)
const executionLogs = ref<string[]>([])
const scriptContent = ref('')
const pythonExample = `# 直接编写 Selenium Python 代码
# 已注入: driver, By, WebDriverWait, EC, time, log
# 无需自行创建或退出 driver

driver.get("https://example.com")
log("opened example.com")
assert "Example" in driver.title
`
const placeholderText = computed(() => (deviceType.value === 'web' ? pythonExample : appScript))
const appiumConf = ref({
    remoteUrl: 'http://127.0.0.1:4723',
    platformName: 'Android',
    automationName: 'UiAutomator2',
    deviceName: 'emulator-5554',
    appPackage: '',
    appActivity: ''
})

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

onMounted(async () => {
    // Check if ID in query or params (we used query in TestCaseManager)
    const id = route.query.id as string
    if (id) {
        caseId.value = id
        await loadCase(id)
    }
})

const loadCase = async (id: string) => {
    try {
        const res: any = await request.get(`/testcases/${id}`)
        if (res) {
            caseName.value = res.name
            deviceType.value = res.type === 'APP' ? 'app' : 'web'
            existingDescription.value = res.description || null
            environment.value = res.environment || 'dev'
            const raw = res.content || ''
            const formatSelector = (s: any) => {
                if (!s) {
                    return ''
                }
                if (s.by === 'id') {
                    return `resource-id=${s.value}`
                }
                if (s.by === 'accessibility_id') {
                    return `accessibility-id=${s.value}`
                }
                if (s.by === 'xpath') {
                    return `xpath=${s.value}`
                }
                return s.value || ''
            }
            try {
                const obj = JSON.parse(raw)
                if (obj.appium) {
                    appiumConf.value = {
                        remoteUrl: obj.appium.remoteUrl || obj.appium.remote_url || appiumConf.value.remoteUrl,
                        platformName: obj.appium.capabilities?.platformName || appiumConf.value.platformName,
                        automationName: obj.appium.capabilities?.automationName || appiumConf.value.automationName,
                        deviceName: obj.appium.capabilities?.deviceName || appiumConf.value.deviceName,
                        appPackage: obj.appium.capabilities?.appPackage || '',
                        appActivity: obj.appium.capabilities?.appActivity || ''
                    }
                }
                if (typeof obj.headless === 'boolean') {
                    headedEnabled.value = obj.headless === false
                }
                if (typeof obj.selfHealing === 'boolean') {
                    selfHealingEnabled.value = obj.selfHealing
                }
                if (obj.script) {
                    scriptContent.value = obj.script
                } else if (Array.isArray(obj.steps)) {
                    scriptContent.value = obj.steps.map((s: any) => {
                        const a = (s.action || '').toLowerCase()
                        if (a === 'click') {
                            return `点击元素: ${formatSelector(s)}`
                        } else if (a === 'input') {
                            return `输入文本: ${formatSelector(s)}, ${s.text || ''}`
                        } else if (a === 'wait') {
                            return s.value ? `等待元素: ${formatSelector(s)}, ${s.ms || 1000}` : `等待时间: ${s.ms || 1000}`
                        } else if (a === 'back') {
                            return '后退'
                        } else if (a === 'launch') {
                            return '启动应用'
                        } else if (a === 'close') {
                            return '关闭应用'
                        } else if (a === 'screenshot') {
                            return `截图: ${s.path || s.file || s.name || 'screenshot.png'}`
                        } else if (a === 'assert_exists') {
                            return `断言元素存在: ${formatSelector(s)}`
                        }
                        return `未知动作: ${a}`
                    }).join('\n')
                } else {
                    scriptContent.value = raw || (res.type === 'APP' ? appScript : defaultScript)
                }
            } catch {
                scriptContent.value = raw || (res.type === 'APP' ? appScript : defaultScript)
            }
        }
    } catch (e) {
        console.error('Failed to load case', e)
    }
}

watch(deviceType, () => {
    // 新建用例：不再预填脚本，交给 placeholder 展示示例
    if (!caseId.value) {
        scriptContent.value = ''
    }
})

const parseSelector = (raw: string) => {
    const value = (raw || '').trim()
    if (value.startsWith('resource-id=')) {
        return { by: 'id', value: value.replace('resource-id=', '') }
    }
    if (value.startsWith('id=')) {
        return { by: 'id', value: value.replace('id=', '') }
    }
    if (value.startsWith('accessibility-id=')) {
        return { by: 'accessibility_id', value: value.replace('accessibility-id=', '') }
    }
    if (value.startsWith('textContains=')) {
        const t = value.replace('textContains=', '')
        const esc = t.replace(/'/g, "\\'")
        return { by: 'xpath', value: `//*[contains(@text,'${esc}') or contains(@content-desc,'${esc}')]` }
    }
    if (value.startsWith('xpath=')) {
        return { by: 'xpath', value: value.replace('xpath=', '') }
    }
    if (value.startsWith('text=')) {
        const textValue = value.replace('text=', '')
        return { by: 'xpath', value: `//*[@text='${textValue}']` }
    }
    return { by: 'xpath', value }
}

const parseScriptToSteps = (text: string) => {
    const lines = (text || '').split('\n').map(l => l.trim()).filter(Boolean)
    const steps: any[] = []
    for (const line of lines) {
        const norm = line.replace(/：/g, ':')
        if (norm.startsWith('//') || norm.startsWith('#')) {
            continue
        }
        if (norm.startsWith('点击元素:') || norm.startsWith('点击按钮:')) {
            const raw = norm.split(':')[1]?.trim() || ''
            const partsAll = raw.split('|').map(p => p.trim()).filter(Boolean)
            const primary = partsAll[0] || ''
            const selector = parseSelector(primary)
            const fallbacks: any[] = []
            for (let i = 1; i < partsAll.length; i++) {
                const fb = partsAll[i]
                const sel = parseSelector(fb)
                fallbacks.push(sel)
            }
            const step: any = { action: 'click', ...selector }
            if (fallbacks.length) step.fallbacks = fallbacks
            steps.push(step)
        } else if (norm.startsWith('输入文本:')) {
            const payload = norm.split(':')[1] || ''
            const firstComma = payload.indexOf(',')
            const selectorRaw = firstComma >= 0 ? payload.slice(0, firstComma).trim() : payload.trim()
            const textPart = firstComma >= 0 ? payload.slice(firstComma + 1).trim() : ''
            const selectorTokens = selectorRaw.split('|').map(p => p.trim()).filter(Boolean)
            const primarySel = selectorTokens[0] || ''
            const selector = parseSelector(primarySel)
            const fallbacks: any[] = []
            for (let i = 1; i < selectorTokens.length; i++) {
                const fb = selectorTokens[i]
                const sel = parseSelector(fb)
                fallbacks.push(sel)
            }
            const step: any = { action: 'input', ...selector, text: textPart || '', clear: true }
            if (fallbacks.length) step.fallbacks = fallbacks
            steps.push(step)
        } else if (norm.startsWith('等待元素:')) {
            const payload = norm.split(':')[1] || ''
            const firstComma = payload.indexOf(',')
            const selectorRaw = firstComma >= 0 ? payload.slice(0, firstComma).trim() : payload.trim()
            const msStr = firstComma >= 0 ? payload.slice(firstComma + 1).trim() : '1000'
            const selectorTokens = selectorRaw.split('|').map(p => p.trim()).filter(Boolean)
            const primarySel = selectorTokens[0] || ''
            const selector = parseSelector(primarySel)
            const fallbacks: any[] = []
            for (let i = 1; i < selectorTokens.length; i++) {
                const fb = selectorTokens[i]
                const sel = parseSelector(fb)
                fallbacks.push(sel)
            }
            const step: any = { action: 'wait', ...selector, ms: isNaN(parseInt(msStr)) ? 1000 : parseInt(msStr) }
            if (fallbacks.length) step.fallbacks = fallbacks
            steps.push(step)
        } else if (norm.startsWith('等待时间:')) {
            const msStr = norm.split(':')[1] || '1000'
            const ms = parseInt(msStr)
            steps.push({ action: 'wait', ms: isNaN(ms) ? 1000 : ms })
        } else if (norm.startsWith('后退')) {
            steps.push({ action: 'back' })
        } else if (norm.startsWith('启动应用')) {
            steps.push({ action: 'launch' })
        } else if (norm.startsWith('关闭应用')) {
            steps.push({ action: 'close' })
        } else if (norm.startsWith('断言元素存在:')) {
            const value = norm.split(':')[1]?.trim() || ''
            const selector = parseSelector(value)
            steps.push({ action: 'assert_exists', ...selector })
        } else if (norm.startsWith('截图:')) {
            const value = norm.split(':')[1]?.trim() || 'screenshot.png'
            steps.push({ action: 'screenshot', path: value })
        }
    }
    return steps
}

const handleSave = async () => {
    saveError.value = ''
    try {
        const descRaw = existingDescription.value ? existingDescription.value.trim() : ''
        const descClean = descRaw === 'Updated from Script Editor' ? '' : descRaw
    const contentObj = deviceType.value === 'web'
        ? { language: 'python', script: scriptContent.value, selfHealing: selfHealingEnabled.value, headless: !headedEnabled.value }
            : {
                appium: {
                    remoteUrl: appiumConf.value.remoteUrl,
                    capabilities: {
                        platformName: appiumConf.value.platformName,
                        automationName: appiumConf.value.automationName,
                        deviceName: appiumConf.value.deviceName,
                        appPackage: appiumConf.value.appPackage,
                        appActivity: appiumConf.value.appActivity
                    }
                },
                steps: parseScriptToSteps(scriptContent.value),
                selfHealing: selfHealingEnabled.value
            }
        const payload = {
            id: caseId.value ? Number(caseId.value) : null,
            name: caseName.value,
            type: deviceType.value.toUpperCase(),
            content: JSON.stringify(contentObj),
            description: descClean,
            environment: environment.value,
            status: 'active'
        }
        
        if (caseId.value) {
            await request.put('/testcases', payload)
        } else {
            const newId: any = await request.post('/testcases', payload)
            if (newId) {
                caseId.value = String(newId)
                // Update URL without reloading to keep state clean
                const newUrl = new URL(window.location.href)
                newUrl.searchParams.set('id', String(newId))
                window.history.pushState({}, '', newUrl.toString())
            }
        }
        showToast('保存成功', 'success')
        if (caseId.value) {
            await loadCase(caseId.value)
        }
    } catch (e: any) {
        const respData = e?.response?.data
        const backendMsg = typeof respData === 'string' ? respData : respData?.message
        const msg = e?.message || backendMsg || '保存失败'
        saveError.value = msg
        showToast(msg, 'error')
    }
}

const appKeywords = [
  { category: '应用操作', items: [
      { label: '启动应用', value: '启动应用' },
      { label: '关闭应用', value: '关闭应用' },
      { label: '后退', value: '后退' }
  ]},
  { category: '元素操作', items: [
      { label: '点击元素', value: '点击元素: resource-id=login_btn' },
      { label: '输入文本', value: '输入文本: resource-id=username, testuser' }
  ]},
  { category: '等待操作', items: [
      { label: '等待元素', value: '等待元素: resource-id=home_page, 5000' },
      { label: '等待时间', value: '等待时间: 1000' }
  ]},
  { category: '断言验证', items: [
      { label: '断言元素存在', value: '断言元素存在: resource-id=user_profile' }
  ]},
  { category: '其他操作', items: [
      { label: '截图', value: '截图: app_login_success.png' }
  ]}
]

// Python（Selenium）快捷片段
const pythonKeywords = [
  { category: '步骤/断言', items: [
      { label: '步骤：描述', value: 'step("正在执行：描述这里")' },
      { label: '断言相等', value: 'assert_equal("预期值", actual_value, "断言说明")' },
      { label: '断言标题包含', value: 'assert_title_contains("关键字")' },
      { label: '记录日志', value: 'log("记录一条说明")' },
    ]},
  { category: '导航操作', items: [
      { label: '打开URL', value: 'driver.get("https://example.com")' },
      { label: '刷新页面', value: 'driver.refresh()' },
    ]},
  { category: '元素操作', items: [
      { label: '查找并点击', value: 'driver.find_element(By.CSS_SELECTOR, "#login-button").click()' },
      { label: '输入文本', value: 'el = driver.find_element(By.NAME, "username")\nel.clear()\nel.send_keys("tester")' },
      { label: '获取文本', value: 'text = driver.find_element(By.CSS_SELECTOR, ".title").text\nlog(text)' },
    ]},
  { category: '等待/断言', items: [
      { label: '等待元素出现', value: 'WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.CSS_SELECTOR, ".dashboard")))' },
      { label: '断言标题包含', value: 'assert "Dashboard" in driver.title' },
    ]},
  { category: '其他操作', items: [
      { label: '执行JS', value: 'driver.execute_script("window.scrollTo(0, document.body.scrollHeight)")' },
      { label: '打印日志', value: 'log("step ok")' },
    ]},
]

const keywordGroups = computed(() => {
  if (deviceType.value === 'app') return appKeywords
  if (deviceType.value === 'web') return pythonKeywords
  return [] as any[]
})

const handleExecute = async () => {
  if (!caseId.value) {
    showToast('请先保存用例', 'warning')
    return
  }
  
  // 自动保存当前配置，确保执行的是最新状态
  await handleSave()
  
  isExecuting.value = true
  executionLogs.value = []
  try {
    const res: any = await request.post(`/testcases/${caseId.value}/execute`)
    const logs = res?.logs ? String(res.logs).split('\n') : []
    executionLogs.value = logs.length ? logs : ['执行完成']
    showToast(res?.status === 'success' ? '执行成功' : (res?.error || '执行失败'), res?.status === 'success' ? 'success' : 'error')
  } catch (e: any) {
    showToast(e?.message || '执行失败', 'error')
  } finally {
    isExecuting.value = false
  }
}

const handleStop = async () => {
  if (!caseId.value) return
  try {
    await request.post(`/testcases/${caseId.value}/stop`)
    showToast('停止成功！', 'success')
    isExecuting.value = false
  } catch (e) {
    showToast('停止失败', 'error')
  }
}

const handleCopyCode = () => {
    navigator.clipboard.writeText(scriptContent.value)
    showToast('代码已复制到剪贴板', 'success')
}
</script>

<template>
  <div class="h-full flex flex-col gap-4">
    <!-- Toolbar -->
    <Card class="flex-none">
      <CardContent class="p-4 flex items-center justify-between gap-4">
        <div class="flex items-center gap-4 flex-1">
          <div class="space-y-1 flex-1">
            <div class="text-xs text-gray-500">用例名称</div>
            <Input v-model="caseName" placeholder="输入用例名称（如：Web登录功能验证）" />
          </div>
          <div class="space-y-1 w-40">
            <div class="text-xs text-gray-500">设备类型</div>
            <Select v-model="deviceType">
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="web">Web 浏览器</SelectItem>
                <SelectItem value="app">移动端 App</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-1 w-40">
            <div class="text-xs text-gray-500">自愈</div>
            <div class="flex items-center gap-2">
              <Switch v-model="selfHealingEnabled" />
              <span class="text-xs text-gray-600">{{ selfHealingEnabled ? '启用' : '关闭' }}</span>
            </div>
          </div>
          <div v-if="deviceType === 'web'" class="space-y-1 w-40">
            <div class="text-xs text-gray-500">可视化</div>
            <div class="flex items-center gap-2">
              <Switch v-model="headedEnabled" />
              <span class="text-xs text-gray-600">{{ headedEnabled ? '开启' : '关闭' }}</span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <Button v-if="isExecuting" variant="destructive" @click="handleStop">
            <Square class="w-4 h-4 mr-2" />
            停止
          </Button>
          <Button v-else @click="handleExecute">
            <Play class="w-4 h-4 mr-2" />
            执行
          </Button>
          <Button variant="outline" @click="handleSave">
            <Save class="w-4 h-4 mr-2" />
            保存
          </Button>
        </div>
      </CardContent>
    </Card>
    <div v-if="saveError" class="text-xs text-red-600 px-4 -mt-2">{{ saveError }}</div>

    <div class="flex-1 flex gap-4 min-h-0">
      <!-- Keywords Sidebar -->
      <Card class="w-64 flex-none overflow-y-auto">
        <CardContent class="p-4 space-y-6">
          <div v-for="group in keywordGroups" :key="group.category" class="space-y-2">
            <h3 class="font-semibold text-sm text-gray-900 flex items-center gap-2">
              <Code class="w-4 h-4 text-gray-500" />
              {{ group.category }}
            </h3>
            <div class="grid grid-cols-1 gap-1">
              <button
                v-for="item in group.items"
                :key="item.label"
                class="text-left text-sm px-2 py-1.5 rounded hover:bg-gray-100 text-gray-600 transition-colors"
                @click="scriptContent += (scriptContent ? '\n' : '') + item.value"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Main Editor Area -->
      <div class="flex-1 flex flex-col gap-4 min-w-0">
        <Card v-if="deviceType === 'app'" class="flex-none">
          <CardContent class="p-4 grid grid-cols-2 gap-4">
            <div class="space-y-1">
              <div class="text-xs text-gray-500">Appium 地址</div>
              <Input v-model="appiumConf.remoteUrl" />
            </div>
            <div class="space-y-1">
              <div class="text-xs text-gray-500">platformName</div>
              <Input v-model="appiumConf.platformName" />
            </div>
            <div class="space-y-1">
              <div class="text-xs text-gray-500">automationName</div>
              <Input v-model="appiumConf.automationName" />
            </div>
            <div class="space-y-1">
              <div class="text-xs text-gray-500">deviceName</div>
              <Input v-model="appiumConf.deviceName" />
            </div>
            <div class="space-y-1">
              <div class="text-xs text-gray-500">appPackage</div>
              <Input v-model="appiumConf.appPackage" />
            </div>
            <div class="space-y-1">
              <div class="text-xs text-gray-500">appActivity</div>
              <Input v-model="appiumConf.appActivity" />
            </div>
          </CardContent>
        </Card>
        <!-- Script Editor -->
        <Card class="flex-1 flex flex-col min-h-0">
          <div class="flex items-center justify-between px-4 py-2 border-b border-gray-100">
            <span class="font-semibold text-sm">脚本编辑器</span>
            <Button variant="ghost" size="sm" @click="handleCopyCode">
              <Copy class="w-4 h-4 mr-2" />
              复制代码
            </Button>
          </div>
          <div class="flex-1 relative">
            <textarea
              v-model="scriptContent"
              :placeholder="placeholderText"
              class="absolute inset-0 w-full h-full p-4 font-mono text-sm bg-gray-900 text-gray-100 resize-none focus:outline-none"
              style="caret-color:#fff;"
              placeholder-class="text-gray-500"
              spellcheck="false"
            ></textarea>
          </div>
        </Card>

        <!-- Execution Logs -->
        <Card class="h-48 flex-none flex flex-col">
          <div class="px-4 py-2 border-b border-gray-100 bg-gray-50">
            <span class="font-semibold text-sm">执行日志</span>
          </div>
          <div class="flex-1 p-4 overflow-y-auto font-mono text-xs bg-white">
            <div v-if="executionLogs.length === 0" class="text-gray-400 italic">
              等待执行...
            </div>
            <div v-for="(log, index) in executionLogs" :key="index" class="space-y-1">
              <div :class="{
                'text-green-600': log.includes('✓'),
                'text-blue-600': log.includes('开始') || log.includes('初始化'),
                'text-gray-600': !log.includes('✓') && !log.includes('开始')
              }">
                {{ log }}
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>
