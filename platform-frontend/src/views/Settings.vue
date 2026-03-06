<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import Switch from '@/components/ui/switch/Switch.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Plus, Trash2, Copy, Key, Database, Zap, Settings, BookOpen, UserCheck, Users, FolderCog } from 'lucide-vue-next'
import { showToast, openConfirm } from '@/lib/notify'
import request from '@/api/request'
import { Badge } from '@/components/ui/badge'
import { userStore } from '@/store/userStore'

const route = useRoute()
const router = useRouter()
const isSystemAdmin = computed(() => userStore.user?.role === 'admin')
const activeTab = ref(userStore.user?.role === 'admin' ? 'variables' : 'teams')

// Sync tab with URL query param
onMounted(() => {
  const queryTab = route.query.tab as string
  if (queryTab && (isSystemAdmin.value || queryTab === 'teams')) {
    activeTab.value = queryTab
  } else {
    // Default fallback: admin -> variables, user -> teams
    activeTab.value = isSystemAdmin.value ? 'variables' : 'teams'
  }
})

// Watch user role change (in case of re-login or hydration)
watch(() => userStore.user?.role, (role) => {
  if (role !== 'admin' && activeTab.value !== 'teams') {
    activeTab.value = 'teams'
  }
}, { immediate: true })

watch(activeTab, (val) => {
  router.replace({ query: { ...route.query, tab: val } })
})

// Global Variables & Parameters Persistence
const STORAGE_VARS_KEY = 'global_variables'
const STORAGE_PARAMS_KEY = 'global_parameters'

// Variables Settings
const variables = ref(JSON.parse(localStorage.getItem(STORAGE_VARS_KEY) || JSON.stringify([
  { 
    key: 'API_TOKEN', 
    value: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...', 
    type: 'variable', 
    scope: 'global', 
    description: 'API认证令牌（动态变量）', 
    isAuto: true,
    sourceType: 'api',
    sourceExpression: 'data.token'
  },
])))

const parameters = ref(JSON.parse(localStorage.getItem(STORAGE_PARAMS_KEY) || JSON.stringify([
  { key: 'DEFAULT_USER', value: 'testuser', type: 'parameter', scope: 'global', description: '默认测试用户（静态参数）' },
  { key: 'DEFAULT_PASSWORD', value: 'Test@123456', type: 'parameter', scope: 'global', description: '默认测试密码' },
  { key: 'TIMEOUT', value: '30000', type: 'parameter', scope: 'global', description: '请求超时时间（毫秒）' }
])))

watch(variables, (newVal) => {
  localStorage.setItem(STORAGE_VARS_KEY, JSON.stringify(newVal))
}, { deep: true })

watch(parameters, (newVal) => {
  localStorage.setItem(STORAGE_PARAMS_KEY, JSON.stringify(newVal))
}, { deep: true })

// Functions Settings
const functions = [
  {
    name: 'random.uuid()',
    description: '生成随机UUID',
    example: 'a1b2c3d4-e5f6-7890-1234-567890abcdef'
  },
  {
    name: 'random.timestamp()',
    description: '生成当前时间戳',
    example: '1704193200000'
  },
  {
    name: 'random.date(format)',
    description: '生成格式化日期',
    example: '2026-01-02'
  },
  {
    name: 'random.string(length)',
    description: '生成随机字符串',
    example: 'aBcDeF123'
  },
  {
    name: 'random.number(min, max)',
    description: '生成随机数字',
    example: '42'
  },
  {
    name: 'random.phone()',
    description: '生成随机手机号',
    example: '13800138000'
  },
  {
    name: 'random.email()',
    description: '生成随机邮箱',
    example: 'test123@example.com'
  },
  {
    name: 'random.username()',
    description: '生成随机用户名',
    example: 'user_a1b2c3'
  }
]

// API Settings
interface ApiKey {
  id: string
  name: string
  key: string
  permissions: string[]
  createdAt: string
  lastUsed: string
}

const savedApiKeys = localStorage.getItem('api_keys')
const apiKeys = ref<ApiKey[]>(savedApiKeys ? JSON.parse(savedApiKeys) : [
  {
    id: '1',
    name: 'Jenkins CI',
    key: 'sk_test_abcdef1234567890',
    permissions: ['read', 'execute'],
    createdAt: '2026-01-01',
    lastUsed: '2026-01-02 10:30'
  }
])

watch(apiKeys, (newKeys) => {
  localStorage.setItem('api_keys', JSON.stringify(newKeys))
}, { deep: true })

const handleRevokeKey = (id: string) => {
  apiKeys.value = apiKeys.value.filter(key => key.id !== id)
}

const copyToClipboard = async (text: string) => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      showToast('复制成功', 'success')
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      showToast('复制成功', 'success')
    }
  } catch (e) {
    const msg = (e as Error)?.message || '复制失败'
    showToast(msg, 'error')
  }
}

const handleGenerateKey = () => {
  const randomPart = Math.random().toString(36).slice(2, 12)
  const id = Date.now().toString()
  apiKeys.value.unshift({
    id,
    name: `New Key ${apiKeys.value.length + 1}`,
    key: `sk_${randomPart}`,
    permissions: ['read', 'execute'],
    createdAt: new Date().toISOString().slice(0, 10),
    lastUsed: '未使用'
  })
}

const isUsageDialogOpen = ref(false)

const curlExample = `curl -X POST "http://your-platform.com/api/openapi/v1/run-plan" \\
  -H "X-API-KEY: sk_test_..." \\
  -H "Content-Type: application/json" \\
  -d '{ 
        "planId": "PLAN_1001", 
        "environment": "Test", 
        "callbackUrl": "http://jenkins.com/webhook/job-123" 
      }'`

const handleAddVariable = () => {
    variables.value.push({
        key: 'NEW_VAR',
        value: '',
        scope: 'global',
        type: 'variable',
        description: 'New Variable'
    })
}

const handleAddParameter = () => {
    parameters.value.push({
        key: 'NEW_PARAM',
        value: '',
        scope: 'global',
        type: 'parameter',
        description: 'New Parameter'
    })
}

const handleDeleteVariable = (index: number) => {
    // Because the list is displayed in reverse order, we need to calculate the actual index
    const realIndex = variables.value.length - 1 - index
    variables.value.splice(realIndex, 1)
}

const handleDeleteParameter = (index: number) => {
    // Because the list is displayed in reverse order, we need to calculate the actual index
    const realIndex = parameters.value.length - 1 - index
    parameters.value.splice(realIndex, 1)
}

// --- Teams Management (Admin) ---
interface Team { id: number; name: string; description?: string }
interface TeamMember { id: number; username: string; nickname?: string; avatar?: string; role: 'member' | 'admin'; joinedAt: string }
interface Project { id: number; name: string; description?: string; teamId?: number; createdAt?: string }

const teams = ref<Team[]>([])
const showTeamDialog = ref(false)
const teamForm = ref({
  name: '',
  description: ''
})

const getTeamName = (teamId?: number) => {
  if (!teamId) return '-'
  const team = teams.value.find(t => t.id === teamId)
  return team ? team.name : '-'
}

const showTeamManageDialog = ref(false)
const currentTeam = ref<Team | null>(null)
const teamMembers = ref<TeamMember[]>([])
const newMemberForm = ref({
  username: '',
  role: 'member' as 'member' | 'admin'
})

const fetchTeams = async () => {
  try {
    const res: unknown = await request.get('/teams')
    teams.value = Array.isArray(res) ? (res as Team[]) : []
  } catch (e) {
    const msg = (e as Error)?.message || '获取团队失败'
    showToast(msg, 'error')
  }
}

const handleCreateTeam = async () => {
  if (!teamForm.value.name) {
    showToast('请输入团队名称', 'error')
    return
  }
  try {
    const payload = {
      name: teamForm.value.name,
      description: teamForm.value.description
    }
    await request.post('/teams', payload)
    showToast('创建成功', 'success')
    showTeamDialog.value = false
    teamForm.value = { name: '', description: '' }
    fetchTeams()
  } catch (e) {
    const msg = (e as Error)?.message || '创建失败'
    showToast(msg, 'error')
  }
}

const handleDeleteTeam = async (id: number) => {
  const ok = await openConfirm({
    title: '删除团队',
    message: '确定要删除该团队吗？将同时清空成员关系。',
    confirmText: '删除',
  })
  if (!ok) return
  try {
    await request.delete(`/teams/${id}`)
    showToast('团队删除成功', 'success')
    fetchTeams()
  } catch (e) {
    const msg = (e as Error)?.message || '删除失败'
    showToast(msg, 'error')
  }
}

const openTeamSettings = async (team: Team) => {
  currentTeam.value = team
  showTeamManageDialog.value = true
  await fetchTeamMembers(team.id)
}

const fetchTeamMembers = async (teamId: number) => {
  try {
    const res: unknown = await request.get(`/teams/${teamId}/members`)
    teamMembers.value = Array.isArray(res) ? (res as TeamMember[]) : []
  } catch (e) {
    const msg = (e as Error)?.message || '获取成员失败'
    showToast(msg, 'error')
  }
}

const handleAddMember = async () => {
  if (!currentTeam.value?.id) return
  if (!newMemberForm.value.username) {
    showToast('请输入用户名', 'error')
    return
  }
  try {
    await request.post(`/teams/${currentTeam.value.id}/members`, {
      username: newMemberForm.value.username,
      role: newMemberForm.value.role
    })
    showToast('成员添加成功', 'success')
    newMemberForm.value = { username: '', role: 'member' }
    fetchTeamMembers(currentTeam.value.id)
  } catch (e) {
    const msg = (e as Error)?.message || '添加失败'
    showToast(msg, 'error')
  }
}

const handleRemoveMember = async (memberId: number) => {
  if (!currentTeam.value?.id) return
  const ok = await openConfirm({
    title: '移除成员',
    message: '确定要移除该成员吗？',
    confirmText: '移除',
  })
  if (!ok) return
  try {
    await request.delete(`/teams/${currentTeam.value.id}/members/${memberId}`)
    showToast('成员已移除', 'success')
    fetchTeamMembers(currentTeam.value.id)
  } catch (e) {
    const msg = (e as Error)?.message || '移除失败'
    showToast(msg, 'error')
  }
}

const handleUpdateMemberRole = async (member: TeamMember, role: 'member' | 'admin') => {
  if (!currentTeam.value?.id) return
  try {
    await request.put(`/teams/${currentTeam.value.id}/members/${member.id}`, { role })
    showToast('角色更新成功', 'success')
    fetchTeamMembers(currentTeam.value.id)
  } catch (e) {
    const msg = (e as Error)?.message || '更新失败'
    showToast(msg, 'error')
  }
}

fetchTeams()

// --- Projects Management (Admin) ---
const projects = ref<Project[]>([])
const showProjectDialog = ref(false)
const projectForm = ref({
  name: '',
  description: '',
  teamId: '' as string
})

const fetchProjects = async () => {
  try {
    const res: unknown = await request.get('/projects')
    projects.value = Array.isArray(res) ? (res as Project[]) : []
  } catch (e) {
    const msg = (e as Error)?.message || '获取项目失败'
    showToast(msg, 'error')
  }
}

const archivedProjects = ref<Project[]>([])
const fetchArchivedProjects = async () => {
  try {
    const res: unknown = await request.get('/projects/archived')
    archivedProjects.value = Array.isArray(res) ? (res as Project[]) : []
  } catch (e) {
    const msg = (e as Error)?.message || '获取归档项目失败'
    showToast(msg, 'error')
  }
}

const handleCreateProject = async () => {
  if (!projectForm.value.name) {
    showToast('请输入项目名称', 'error')
    return
  }
  if (!projectForm.value.teamId) {
    showToast('请选择归属团队', 'error')
    return
  }
  try {
    await request.post('/projects', {
      name: projectForm.value.name,
      description: projectForm.value.description,
      teamId: Number(projectForm.value.teamId)
    })
    showToast('项目创建成功', 'success')
    showProjectDialog.value = false
    projectForm.value = { name: '', description: '', teamId: '' }
    await fetchProjects()
    // Find the created project and navigate
    const createdProject = projects.value.find((p) => p.name === projectForm.value.name && String(p.teamId ?? '') === projectForm.value.teamId)
    if (createdProject) {
        localStorage.setItem('currentProjectId', createdProject.id.toString());
        window.location.href = '/project-settings';
    }
  } catch (e) {
    const msg = (e as Error)?.message || '创建失败'
    showToast(msg, 'error')
  }
}

const handleDeleteProject = async (id: number) => {
  const ok = await openConfirm({
    title: '删除项目',
    message: '确定要删除该项目吗？此操作不可恢复。',
    confirmText: '删除',
  })
  if (!ok) return
  try {
    await request.delete(`/projects/${id}`)
    showToast('项目删除成功', 'success')
    fetchProjects()
    const currentProjectId = localStorage.getItem('currentProjectId')
    if (currentProjectId && Number(currentProjectId) === Number(id)) {
      localStorage.removeItem('currentProjectId')
      window.location.reload()
    }
  } catch (e) {
    const msg = (e as Error)?.message || '删除失败'
    showToast(msg, 'error')
  }
}

const handleRestoreProject = async (id: number) => {
  const ok = await openConfirm({
    title: '恢复项目',
    message: '确定要恢复该项目吗？恢复后项目重新可用。',
    confirmText: '恢复',
  })
  if (!ok) return
  try {
    await request.put(`/projects/${id}/restore`)
    showToast('项目恢复成功', 'success')
    fetchProjects()
    fetchArchivedProjects()
  } catch (e) {
    const msg = (e as Error)?.message || '恢复失败'
    showToast(msg, 'error')
  }
}
// --- User Approval (Admin) ---
 
 

// Permission Requests (Admin)
const permissionRequests = ref<any[]>([])
const fetchPermissionRequests = async () => {
  try {
    const res: any = await request.get('/permission-requests', { params: { page: 1, size: 50, status: 'pending' } })
    permissionRequests.value = res.records || []
  } catch (_e) {
    permissionRequests.value = []
  }
}
const approvePermission = async (id: number) => {
  try {
    await request.put(`/permission-requests/${id}/approve`)
    showToast('已通过', 'success')
    fetchPermissionRequests()
  } catch (e: any) {
    showToast(e.message || '操作失败', 'error')
  }
}
const rejectPermission = async (id: number) => {
  try {
    await request.put(`/permission-requests/${id}/reject`)
    showToast('已拒绝', 'success')
    fetchPermissionRequests()
  } catch (e: any) {
    showToast(e.message || '操作失败', 'error')
  }
}
 

 

fetchPermissionRequests()
fetchProjects()
fetchArchivedProjects()
</script>

<template>
  <div class="space-y-6 p-6">
    <Tabs v-model="activeTab" class="space-y-4">
      <TabsList>
        <TabsTrigger value="variables" v-if="isSystemAdmin">全局基准参数</TabsTrigger>
        <TabsTrigger value="functions" v-if="isSystemAdmin">随机函数</TabsTrigger>
        <TabsTrigger value="openapi" v-if="isSystemAdmin">OpenAPI</TabsTrigger>
        <TabsTrigger value="teams">用户与团队管理</TabsTrigger>
      </TabsList>

      <!-- Variables Tab -->
      <TabsContent value="variables" class="space-y-4" v-if="isSystemAdmin">
        <div class="space-y-4 h-full flex flex-col">
          <!-- Global Variables Section -->
          <Card class="flex flex-col flex-1">
          <CardHeader class="flex flex-row items-center justify-between flex-shrink-0">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <Database class="w-5 h-5 text-blue-500" />
                全局基准参数
                </CardTitle>
                <CardDescription>
                设置全平台的默认规则；项目参数可覆盖本页配置
                </CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="handleAddVariable">
                <Plus class="w-4 h-4 mr-2" />
                添加变量
              </Button>
            </CardHeader>
            <CardContent class="flex-1 overflow-auto">
              <div class="space-y-4 variable-list">
                <div v-for="(variable, index) in variables.slice().reverse()" :key="index" class="flex items-start gap-4 p-4 border border-gray-100 rounded-lg hover:bg-gray-50">
                  <div class="flex-1 grid grid-cols-1 md:grid-cols-12 gap-4">
                    <div class="col-span-3 space-y-1">
                      <div class="flex items-center justify-between mb-1">
                        <Label class="text-xs text-gray-500">变量名</Label>
                        <div class="flex items-center gap-1.5">
                          <Switch v-model="variable.isAuto" class="scale-75 origin-right" />
                          <span class="text-[10px] text-gray-400 font-medium">{{ variable.isAuto ? '自动回写' : '手动' }}</span>
                        </div>
                      </div>
                      <Input v-model="variable.key" class="font-mono text-blue-600" />
                    </div>
                    <div class="col-span-5 space-y-1">
                      <Label class="text-xs text-gray-500">变量值</Label>
                      <Input v-model="variable.value" :disabled="variable.isAuto" :class="variable.isAuto ? 'bg-gray-50 text-gray-500' : ''" />
                    </div>
                    <div class="col-span-4 space-y-1">
                      <Label class="text-xs text-gray-500">描述</Label>
                      <Input v-model="variable.description" />
                    </div>
                    
                    <!-- Auto Update Configuration -->
                    <div v-if="variable.isAuto" class="col-span-12 grid grid-cols-12 gap-4 mt-1 p-3 bg-blue-50/50 rounded-md border border-blue-100 animate-in fade-in slide-in-from-top-1">
                      <div class="col-span-3 space-y-1">
                        <Label class="text-[10px] font-medium text-blue-600">提取来源</Label>
                        <Select v-model="variable.sourceType">
                          <SelectTrigger class="h-8 text-xs bg-white border-blue-200 focus:ring-blue-200">
                            <SelectValue placeholder="选择来源" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="api">API 响应提取</SelectItem>
                            <SelectItem value="db">数据库查询</SelectItem>
                            <SelectItem value="script">自定义脚本</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div class="col-span-9 space-y-1">
                        <Label class="text-[10px] font-medium text-blue-600">提取规则 (JSONPath / SQL / Script)</Label>
                        <div class="flex gap-2">
                          <Input 
                            v-model="variable.sourceExpression" 
                            class="h-8 text-xs font-mono bg-white border-blue-200 focus-visible:ring-blue-200" 
                            :placeholder="variable.sourceType === 'db' ? 'SELECT value FROM table WHERE key=...' : variable.sourceType === 'script' ? 'return customized_value' : 'data.token'" 
                          />
                          <Button size="sm" variant="outline" class="h-8 px-3 text-xs text-blue-600 border-blue-200 hover:bg-blue-100 hover:text-blue-700">
                            测试
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <Button variant="ghost" size="icon" class="mt-5 text-gray-400 hover:text-red-500" @click="handleDeleteVariable(index)">
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <!-- Global Parameters Section -->
          <Card class="flex flex-col flex-1">
            <CardHeader class="flex flex-row items-center justify-between flex-shrink-0">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <Settings class="w-5 h-5 text-green-500" />
                  全局参数
                </CardTitle>
                <CardDescription>
                  管理系统或环境的静态配置（如超时时间、默认用户）
                </CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="handleAddParameter">
                <Plus class="w-4 h-4 mr-2" />
                添加参数
              </Button>
            </CardHeader>
            <CardContent class="flex-1 overflow-auto">
              <div class="space-y-4 variable-list">
                <div v-for="(param, index) in parameters.slice().reverse()" :key="index" class="flex items-start gap-4 p-4 border border-gray-100 rounded-lg hover:bg-gray-50">
                  <div class="flex-1 grid grid-cols-1 md:grid-cols-12 gap-4">
                    <div class="col-span-3 space-y-1">
                      <Label class="text-xs text-gray-500">参数名</Label>
                      <Input v-model="param.key" class="font-mono text-green-600" />
                    </div>
                    <div class="col-span-5 space-y-1">
                      <Label class="text-xs text-gray-500">参数值</Label>
                      <Input v-model="param.value" />
                    </div>
                    <div class="col-span-4 space-y-1">
                      <Label class="text-xs text-gray-500">描述</Label>
                      <Input v-model="param.description" />
                    </div>
                  </div>
                  <Button variant="ghost" size="icon" class="mt-5 text-gray-400 hover:text-red-500" @click="handleDeleteParameter(index)">
                    <Trash2 class="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

      <!-- Functions Tab -->
      <TabsContent value="functions" class="space-y-4" v-if="isSystemAdmin">
        <Card>
            <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Zap class="w-5 h-5 text-yellow-500" />
              随机函数
            </CardTitle>
            <CardDescription>
                公共手册：内置随机函数说明，支持在参数值中直接调用
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div v-for="func in functions" :key="func.name" class="p-4 border border-gray-100 rounded-lg space-y-2">
                <div class="flex items-center justify-between">
                  <code class="text-sm font-mono bg-gray-100 px-2 py-1 rounded text-purple-600">{{ func.name }}</code>
                  <Button
                    variant="ghost"
                    size="icon"
                    class="h-6 w-6"
                    @click="copyToClipboard(func.name)"
                  >
                    <Copy class="w-3 h-3" />
                  </Button>
                </div>
                <p class="text-sm text-gray-600">{{ func.description }}</p>
                <div class="text-xs text-gray-400">示例: {{ func.example }}</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <!-- OpenAPI Tab -->
      <TabsContent value="openapi" class="space-y-4" v-if="isSystemAdmin">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2">
              <Key class="w-5 h-5 text-green-500" />
              OpenAPI 密钥
            </CardTitle>
            <CardDescription>
              管理用于 CI/CD 集成的 API 访问密钥
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div class="space-y-4">
              <div v-for="key in apiKeys" :key="key.id" class="flex items-center justify-between p-4 border border-gray-100 rounded-lg">
                <div class="space-y-1">
                  <div class="font-medium">{{ key.name }}</div>
                  <div class="text-sm text-gray-500 font-mono">
                    {{ key.key.substring(0, 8) }}...{{ key.key.substring(key.key.length - 4) }}
                  </div>
                </div>
                <div class="flex items-center gap-4 text-sm text-gray-500">
                  <div>创建于: {{ key.createdAt }}</div>
                  <div>最后使用: {{ key.lastUsed }}</div>
                  <Button variant="outline" size="sm" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="handleRevokeKey(key.id)">
                    撤销
                  </Button>
                </div>
              </div>
              <div class="flex gap-4">
                <Button class="flex-1" variant="outline" @click="handleGenerateKey">
                  <Plus class="w-4 h-4 mr-2" />
                  生成新密钥
                </Button>
                <Button class="flex-1" variant="secondary" @click="isUsageDialogOpen = true">
                  <BookOpen class="w-4 h-4 mr-2" />
                  查看使用文档
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
      <!-- Users & Teams Tab -->
      <TabsContent value="teams" class="space-y-4">
        <div class="space-y-6">
          <Card class="flex flex-col border-l-4 border-indigo-500">
            <CardHeader class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <span class="inline-flex h-8 w-8 items-center justify-center rounded-md bg-indigo-50 text-indigo-600 ring-1 ring-indigo-100">
                    <UserCheck class="h-4 w-4" />
                  </span>
                  权限申请记录
                </CardTitle>
                <CardDescription>处理加入项目与角色升级的申请</CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="fetchPermissionRequests">刷新</Button>
            </CardHeader>
            <CardContent>
              <div class="rounded-lg border border-gray-100 bg-white overflow-hidden shadow-sm">
                <Table>
                  <TableHeader class="bg-gray-50">
                    <TableRow class="bg-gray-50 hover:bg-gray-50">
                      <TableHead class="px-6">申请人</TableHead>
                      <TableHead class="px-6">类型</TableHead>
                      <TableHead class="px-6">项目ID</TableHead>
                      <TableHead class="px-6">状态</TableHead>
                      <TableHead class="px-6">申请时间</TableHead>
                      <TableHead class="px-6 text-right">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    <TableRow v-for="r in permissionRequests" :key="r.id">
                      <TableCell class="px-6 py-4">@{{ r.userId }}</TableCell>
                      <TableCell class="px-6 py-4">{{ r.requestType }}</TableCell>
                      <TableCell class="px-6 py-4">{{ r.projectId ?? '-' }}</TableCell>
                      <TableCell class="px-6 py-4">{{ r.status }}</TableCell>
                      <TableCell class="px-6 py-4 text-gray-500 text-sm">{{ r.createdAt }}</TableCell>
                      <TableCell class="text-right px-6 py-4 whitespace-nowrap">
                        <div class="flex justify-end gap-3">
                          <Button variant="secondary" size="sm" @click="approvePermission(r.id)" :disabled="r.status !== 'pending'">通过</Button>
                          <Button variant="outline" size="sm" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="rejectPermission(r.id)" :disabled="r.status !== 'pending'">拒绝</Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>

          <Card class="flex flex-col border-l-4 border-purple-500">
            <CardHeader class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <span class="inline-flex h-8 w-8 items-center justify-center rounded-md bg-purple-50 text-purple-600 ring-1 ring-purple-100">
                    <Users class="h-4 w-4" />
                  </span>
                  团队管理
                  <Badge variant="outline" class="border-purple-100 bg-purple-50 text-purple-700">总数 {{ teams.length }}</Badge>
                </CardTitle>
                <CardDescription>创建团队、管理成员与角色</CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="showTeamDialog = true">
                <Plus class="w-4 h-4 mr-2" />
                创建团队
              </Button>
            </CardHeader>
            <CardContent class="flex-1 space-y-4">
              <div v-if="teams.length === 0" class="flex flex-col items-center justify-center py-12 text-sm text-muted-foreground">
                暂无团队
                <Button variant="outline" size="sm" class="mt-3" @click="showTeamDialog = true">创建团队</Button>
              </div>
                <div class="relative w-full overflow-auto rounded-lg border border-gray-100 bg-white shadow-sm">
                  <table class="w-full caption-bottom text-sm border-collapse">
                    <thead class="bg-gray-50 border-b">
                      <tr class="transition-colors hover:bg-muted/50">
                        <th class="h-12 px-6 text-left align-middle font-medium text-muted-foreground w-[200px]">团队名称</th>
                        <th class="h-12 px-6 text-left align-middle font-medium text-muted-foreground">描述</th>
                        <th class="h-12 px-6 text-right align-middle font-medium text-muted-foreground w-[200px]">操作</th>
                      </tr>
                    </thead>
                    <tbody class="[&_tr:last-child]:border-0">
                      <tr v-for="team in teams" :key="team.id" class="border-b transition-colors hover:bg-muted/50">
                        <td class="p-6 align-middle font-medium whitespace-nowrap truncate max-w-[200px]" :title="team.name">{{ team.name }}</td>
                        <td class="p-6 align-middle text-gray-700 break-words whitespace-normal" :title="team.description || ''">{{ team.description || '-' }}</td>
                        <td class="p-6 align-middle text-right whitespace-nowrap">
                          <div class="flex justify-end gap-3">
                            <Button variant="outline" size="sm" @click="openTeamSettings(team)">成员管理</Button>
                            <Button variant="ghost" size="sm" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="handleDeleteTeam(team.id)">
                              删除
                            </Button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
            </CardContent>
          </Card>

          <Card class="flex flex-col border-l-4 border-teal-500">
            <CardHeader class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <span class="inline-flex h-8 w-8 items-center justify-center rounded-md bg-teal-50 text-teal-700 ring-1 ring-teal-100">
                    <FolderCog class="h-4 w-4" />
                  </span>
                  项目管理
                  <Badge variant="outline" class="border-teal-100 bg-teal-50 text-teal-800">总数 {{ projects.length }}</Badge>
                </CardTitle>
                <CardDescription>创建项目、删除项目，项目隶属团队</CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="showProjectDialog = true">
                <Plus class="w-4 h-4 mr-2" />
                创建项目
              </Button>
            </CardHeader>
            <CardContent class="flex-1 space-y-4">
              <div v-if="projects.length === 0" class="flex flex-col items-center justify-center py-12 text-sm text-muted-foreground">
                暂无项目
                <Button variant="outline" size="sm" class="mt-3" @click="showProjectDialog = true">创建项目</Button>
              </div>
                <div class="relative w-full overflow-auto rounded-lg border border-gray-100 bg-white shadow-sm">
                  <table class="w-full caption-bottom text-sm border-collapse">
                    <thead class="bg-gray-50 border-b">
                      <tr class="transition-colors hover:bg-muted/50">
                        <th class="h-12 px-6 text-left align-middle font-medium text-muted-foreground w-[150px]">项目名称</th>
                        <th class="h-12 px-6 text-left align-middle font-medium text-muted-foreground">描述</th>
                        <th class="h-12 px-6 text-left align-middle font-medium text-muted-foreground w-[150px]">归属团队</th>
                        <th class="h-12 px-6 text-left align-middle font-medium text-muted-foreground w-[150px]">创建时间</th>
                        <th class="h-12 px-6 text-right align-middle font-medium text-muted-foreground w-[100px]">操作</th>
                      </tr>
                    </thead>
                    <tbody class="[&_tr:last-child]:border-0">
                      <tr v-for="p in projects" :key="p.id" class="border-b transition-colors hover:bg-muted/50">
                        <td class="p-6 align-middle font-medium whitespace-nowrap truncate max-w-[150px]" :title="p.name">{{ p.name }}</td>
                        <td class="p-6 align-middle text-gray-700 break-words whitespace-normal" :title="p.description || ''">{{ p.description || '-' }}</td>
                        <td class="p-6 align-middle whitespace-nowrap truncate max-w-[150px]" :title="getTeamName(p.teamId)">{{ getTeamName(p.teamId) }}</td>
                        <td class="p-6 align-middle text-gray-500 text-sm whitespace-nowrap">{{ p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '-' }}</td>
                        <td class="p-6 align-middle text-right whitespace-nowrap">
                          <Button variant="ghost" size="sm" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="handleDeleteProject(p.id)">
                            删除
                          </Button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
            </CardContent>
          </Card>

          <Card class="flex flex-col border-l-4 border-amber-500">
            <CardHeader class="flex flex-row items-center justify-between">
              <div class="space-y-1.5">
                <CardTitle class="flex items-center gap-2">
                  <span class="inline-flex h-8 w-8 items-center justify-center rounded-md bg-amber-50 text-amber-600 ring-1 ring-amber-100">
                    <ArchiveRestore class="h-4 w-4" />
                  </span>
                  已归档项目
                </CardTitle>
                <CardDescription>查看与恢复已归档项目（软删除）</CardDescription>
              </div>
              <Button variant="outline" size="sm" @click="fetchArchivedProjects">刷新</Button>
            </CardHeader>
            <CardContent>
              <div class="rounded-lg border border-gray-100 bg-white overflow-hidden shadow-sm">
                <Table>
                  <TableHeader class="bg-gray-50">
                    <TableRow class="bg-gray-50 hover:bg-gray-50">
                      <TableHead class="px-6">项目名称</TableHead>
                      <TableHead class="px-6">归属团队</TableHead>
                      <TableHead class="px-6">状态</TableHead>
                      <TableHead class="px-6 text-right">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    <TableRow v-for="p in archivedProjects" :key="p.id">
                      <TableCell class="px-6 py-4 font-medium whitespace-nowrap">{{ p.name }}</TableCell>
                      <TableCell class="px-6 py-4 whitespace-nowrap">{{ getTeamName(p.teamId) }}</TableCell>
                      <TableCell class="px-6 py-4 text-muted-foreground">已归档</TableCell>
                      <TableCell class="text-right px-6 py-4 whitespace-nowrap">
                        <div class="flex justify-end gap-3">
                          <Button variant="secondary" size="sm" @click="handleRestoreProject(p.id!)">恢复</Button>
                        </div>
                      </TableCell>
                    </TableRow>
                    <TableRow v-if="archivedProjects.length === 0">
                      <TableCell colspan="4" class="text-center text-gray-500 py-8">暂无归档项目</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

    </Tabs>



    <Dialog :open="isUsageDialogOpen" @update:open="isUsageDialogOpen = $event">
      <DialogContent class="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>OpenAPI 调用指南</DialogTitle>
          <DialogDescription>
            通过 HTTP 接口触发测试计划
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div class="space-y-2">
            <Label>1. 鉴权方式</Label>
            <p class="text-sm text-gray-500">
              在请求头中添加 <code class="bg-gray-100 px-1 rounded text-purple-600 font-mono">X-API-KEY</code>
            </p>
          </div>
          <div class="space-y-2">
            <Label>2. 调用示例 (cURL)</Label>
            <div class="relative bg-gray-900 rounded-lg p-4 font-mono text-xs text-green-400 overflow-x-auto">
              <pre>{{ curlExample }}</pre>
              <Button variant="ghost" size="icon" class="absolute top-2 right-2 text-gray-400 hover:text-white" @click="copyToClipboard(curlExample)">
                <Copy class="w-4 h-4" />
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <!-- Create Team Dialog -->
    <div v-if="showTeamDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md shadow-xl">
        <h3 class="text-lg font-semibold mb-4">创建新团队</h3>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>团队名称</Label>
            <Input v-model="teamForm.name" placeholder="输入团队名称" />
          </div>
          <div class="space-y-2">
            <Label>描述</Label>
            <Input v-model="teamForm.description" placeholder="可选描述" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <Button variant="outline" @click="showTeamDialog = false">取消</Button>
          <Button @click="handleCreateTeam">创建</Button>
        </div>
      </div>
    </div>

    <!-- Create Project Dialog -->
    <Dialog :open="showProjectDialog" @update:open="showProjectDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>创建新项目</DialogTitle>
          <DialogDescription>项目是测试用例、环境和报告的集合，必须归属于一个团队。</DialogDescription>
        </DialogHeader>
        <div class="grid gap-4 py-4">
          <div class="grid grid-cols-4 items-center gap-4">
            <Label for="projectName" class="text-right">项目名称</Label>
            <Input id="projectName" v-model="projectForm.name" class="col-span-3" />
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label for="projectDesc" class="text-right">项目描述</Label>
            <Input id="projectDesc" v-model="projectForm.description" class="col-span-3" />
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label for="projectTeam" class="text-right">归属团队</Label>
            <Select v-model="projectForm.teamId">
              <SelectTrigger class="col-span-3">
                <span class="block truncate text-sm">
                  {{ teams.find(t => String(t.id) === projectForm.teamId)?.name || '请选择团队' }}
                </span>
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="team in teams" :key="team.id" :value="String(team.id)">{{ team.name }}</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showProjectDialog = false">取消</Button>
          <Button @click="handleCreateProject">创建</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Team Members Dialog -->
    <div v-if="showTeamManageDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-lg w-full max-w-3xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 border-b">
          <div class="text-lg font-semibold">成员管理 - {{ currentTeam?.name }}</div>
          <div class="text-sm text-muted-foreground mt-1">添加成员、调整角色、移除成员</div>
        </div>

        <div class="p-6 space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-12 gap-4 items-end">
            <div class="md:col-span-6 space-y-2">
              <Label>用户名</Label>
              <Input v-model="newMemberForm.username" placeholder="输入要添加的用户名" />
            </div>
            <div class="md:col-span-3 space-y-2">
              <Label>角色</Label>
              <Select v-model="newMemberForm.role">
                <SelectTrigger class="w-full">
                  <span class="block truncate text-sm">
                    {{ newMemberForm.role === 'admin' ? '管理员' : newMemberForm.role === 'member' ? '成员' : '请选择角色' }}
                  </span>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="member">成员</SelectItem>
                  <SelectItem value="admin">管理员</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="md:col-span-3 flex justify-end">
              <Button variant="outline" class="w-full md:w-auto" @click="handleAddMember">
                <Plus class="w-4 h-4 mr-2" /> 添加成员
              </Button>
            </div>
          </div>

          <div class="rounded-lg border border-gray-100 bg-white overflow-hidden shadow-sm max-h-[420px] overflow-auto">
            <Table>
              <TableHeader class="sticky top-0 z-10 bg-gray-50">
                <TableRow class="bg-gray-50 hover:bg-gray-50">
                  <TableHead>用户</TableHead>
                  <TableHead>角色</TableHead>
                  <TableHead>加入时间</TableHead>
                  <TableHead class="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="member in teamMembers" :key="member.id">
                  <TableCell>
                    <div class="flex items-center gap-3">
                      <div class="w-9 h-9 rounded-full bg-gray-100 ring-1 ring-gray-200 flex items-center justify-center overflow-hidden">
                        <img v-if="member.avatar" :src="member.avatar" class="w-full h-full object-cover" />
                        <span v-else class="text-xs text-gray-600">{{ member.username?.substring(0, 2).toUpperCase() }}</span>
                      </div>
                      <div class="min-w-0">
                        <div class="font-medium truncate">{{ member.nickname || member.username }}</div>
                        <div class="text-xs text-gray-500 truncate">@{{ member.username }}</div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge :variant="member.role === 'admin' ? 'default' : 'secondary'">
                      {{ member.role === 'admin' ? '管理员' : '成员' }}
                    </Badge>
                  </TableCell>
                  <TableCell class="text-gray-500 text-sm">
                    {{ member.joinedAt ? new Date(member.joinedAt).toLocaleDateString() : '刚刚' }}
                  </TableCell>
                  <TableCell class="text-right">
                    <div class="flex justify-end gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        v-if="member.role === 'member'"
                        @click="handleUpdateMemberRole(member, 'admin')"
                      >
                        设为管理
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        v-if="member.role === 'admin'"
                        @click="handleUpdateMemberRole(member, 'member')"
                      >
                        降为成员
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        class="text-red-600 hover:text-red-700 hover:bg-red-50"
                        @click="handleRemoveMember(member.id)"
                      >
                        移除
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
                <TableRow v-if="teamMembers.length === 0">
                  <TableCell colspan="4" class="text-center text-gray-500 py-10">暂无成员</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
        </div>

        <div class="px-6 py-4 border-t flex justify-end">
          <Button variant="outline" @click="showTeamManageDialog = false">关闭</Button>
        </div>
      </div>
    </div>
  </div>
</template>
