<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { LogOut, Lock, Settings, Copy, Eye, EyeOff, Trash2, RefreshCw } from 'lucide-vue-next'
import { userStore } from '@/store/userStore'
import { showToast } from '@/lib/notify'
import request from '@/api/request'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useProjectStore } from '@/store/projectStore'

const router = useRouter()
const projectStore = useProjectStore()

const confirmDialog = ref({
  open: false,
  title: '',
  description: '',
  variant: 'default' as 'default' | 'destructive',
  confirmAction: (() => {}) as () => void | Promise<void>
})

const NOTIFY_RULE_KEY = 'notify_rule'
const NOTIFY_THRESHOLD_KEY = 'notify_threshold'
const notifyRule = ref<string>(localStorage.getItem(NOTIFY_RULE_KEY) || 'on_fail')
const notifyThreshold = ref<number>(Number(localStorage.getItem(NOTIFY_THRESHOLD_KEY) || '80'))
const saveNotifyRule = (rule: string) => {
  notifyRule.value = rule
  localStorage.setItem(NOTIFY_RULE_KEY, rule)
  profileForm.value.notifyRule = rule
}
const saveNotifyThreshold = () => {
  localStorage.setItem(NOTIFY_THRESHOLD_KEY, String(notifyThreshold.value))
  profileForm.value.notifyThreshold = notifyThreshold.value
}
const openConfirm = (title: string, description: string, action: () => void | Promise<void>, variant: 'default' | 'destructive' = 'default') => {
  confirmDialog.value = {
    open: true,
    title,
    description,
    variant,
    confirmAction: action
  }
}

const handleConfirmAction = async () => {
  await confirmDialog.value.confirmAction()
  confirmDialog.value.open = false
}

// Avatar Logic
const avatarStyles = [
  { label: '卡通', value: 'avataaars' },
  { label: '机器人', value: 'bottts' },
  { label: '简笔画', value: 'open-peeps' },
  { label: '几何', value: 'shapes' },
  { label: '像素', value: 'pixel-art' },
  { label: '首字母', value: 'initials' }
]

const currentAvatarStyle = ref('avataaars')

const refreshAvatar = () => {
  const seed = Math.random().toString(36).substring(7)
  profileForm.value.avatar = `https://api.dicebear.com/7.x/${currentAvatarStyle.value}/svg?seed=${seed}`
}

const changeAvatarStyle = (style: string) => {
  currentAvatarStyle.value = style
  refreshAvatar()
}

// Profile Data
const profileForm = ref({
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  notificationWebhook: '',
  enableNotification: false,
  notifyRule: notifyRule.value,
  notifyThreshold: notifyThreshold.value
})

// Password Data
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// Logs Data
const loginLogs = ref<any[]>([])
const logsLoading = ref(false)

// Variables Data
const variables = ref<any[]>([])
const varsLoading = ref(false)
const showVarDialog = ref(false)
const currentVar = ref({
  id: null as number | null,
  keyName: '',
  value: '',
  description: ''
})

// Fetch Profile
const fetchProfile = async () => {
  try {
    const res: any = await request.get('/user/profile')
    if (res) {
      const serverRule = res.notificationRule || notifyRule.value
      const serverThreshold = typeof res.notificationThreshold === 'number' ? res.notificationThreshold : notifyThreshold.value
      notifyRule.value = serverRule
      notifyThreshold.value = serverThreshold
      localStorage.setItem(NOTIFY_RULE_KEY, serverRule)
      localStorage.setItem(NOTIFY_THRESHOLD_KEY, String(serverThreshold))
      profileForm.value = {
        nickname: res.nickname || '',
        email: res.email || '',
        phone: res.phone || '',
        avatar: res.avatar || '',
        notificationWebhook: res.notificationWebhook || '',
        enableNotification: res.enableNotification || false,
        notifyRule: serverRule,
        notifyThreshold: serverThreshold
      }
    }
  } catch (e) {
    console.error(e)
  }
}

// Update Profile
const handleUpdateProfile = async () => {
  try {
    await request.put('/user/profile', profileForm.value)
    showToast('个人资料已更新', 'success')
  } catch (e: any) {
    showToast(e.message || '更新失败', 'error')
  }
}

// Change Password
const handleChangePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showToast('两次输入的密码不一致', 'error')
    return
  }
  if (!passwordForm.value.oldPassword) {
    showToast('请输入当前密码', 'error')
    return
  }
  try {
    await request.put('/user/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    showToast('密码修改成功，请重新登录', 'success')
    // Clear data
    passwordForm.value.oldPassword = ''
    passwordForm.value.newPassword = ''
    passwordForm.value.confirmPassword = ''
    // Logout
    userStore.clearUser()
    router.push('/login')
  } catch (e: any) {
    showToast(e.message || '修改失败', 'error')
  }
}


// Fetch Logs
const fetchLogs = async () => {
  logsLoading.value = true
  try {
    const res: any = await request.get('/user/logs')
    loginLogs.value = res.records || []
  } catch (e) {
    console.error(e)
  } finally {
    logsLoading.value = false
  }
}

// Fetch Variables
const fetchVariables = async () => {
  varsLoading.value = true
  try {
    const res: any = await request.get('/user/variables')
    variables.value = res.records || []
  } catch (e) {
    console.error(e)
  } finally {
    varsLoading.value = false
  }
}

// Variable CRUD
const openCreateVarDialog = () => {
  resetVarForm()
  showVarDialog.value = true
}

const saveVariable = async () => {
  if (!currentVar.value.keyName || !currentVar.value.value) {
    showToast('请输入变量名和值', 'error')
    return
  }
  if (currentVar.value.id) {
    try {
      await request.put(`/user/variables/${currentVar.value.id}`, currentVar.value)
      showToast('更新成功', 'success')
      fetchVariables()
      showVarDialog.value = false
    } catch (e: any) {
      showToast(e.message || '更新失败', 'error')
    }
  } else {
    try {
      await request.post('/user/variables', currentVar.value)
      showToast('创建成功', 'success')
      fetchVariables()
      showVarDialog.value = false
    } catch (e: any) {
      showToast(e.message || '创建失败', 'error')
    }
  }
  resetVarForm()
}

const deleteVariable = (id: number) => {
  openConfirm('删除变量', '确定要删除该变量吗？', async () => {
    try {
      await request.delete(`/user/variables/${id}`)
      showToast('删除成功', 'success')
      fetchVariables()
    } catch (e: any) {
      showToast(e.message || '删除失败', 'error')
    }
  }, 'destructive')
}

const editVariable = (item: any) => {
  currentVar.value = { ...item }
  showVarDialog.value = true
}

const resetVarForm = () => {
  currentVar.value = { id: null, keyName: '', value: '', description: '' }
}

const handleLogout = () => {
  userStore.clearUser()
  router.push('/login')
}

// API Keys
interface ApiKey {
  id: string
  name: string
  accessKey: string
  createdAt: string
  lastUsedAt?: string
  visible?: boolean
}

const apiKeys = ref<ApiKey[]>([])

const loadApiKeys = async () => {
  try {
    const res: any = await request.get('/user/api-keys')
    apiKeys.value = res.map((k: any) => ({
      ...k,
      createdAt: new Date(k.createdAt).toLocaleString(),
      visible: false
    }))
  } catch (e) {
    console.error(e)
  }
}

const generateApiKey = async () => {
  try {
    await request.post('/user/api-keys', { name: `Personal Key ${apiKeys.value.length + 1}` })
    showToast('API Key 生成成功', 'success')
    loadApiKeys()
  } catch (e: any) {
    showToast(e.message || '生成失败', 'error')
  }
}

const revokeApiKey = (id: string) => {
  openConfirm('撤销密钥', '确定要撤销此密钥吗？撤销后将无法恢复。', async () => {
    try {
      await request.delete(`/user/api-keys/${id}`)
      showToast('API Key 已撤销', 'success')
      loadApiKeys()
    } catch (e: any) {
      showToast(e.message || '撤销失败', 'error')
    }
  }, 'destructive')
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    showToast('已复制到剪贴板', 'success')
  }).catch(() => {
    showToast('复制失败', 'error')
  })
}

onMounted(() => {
  fetchProfile()
  fetchLogs()
  fetchVariables()
  loadApiKeys()
})

// Permission Requests
const applyForm = ref({
  requestType: 'JOIN_PROJECT' as 'JOIN_PROJECT' | 'UPGRADE_ROLE',
  projectId: null as number | null,
  description: ''
})
const submitting = ref(false)
const submitPermissionRequest = async () => {
  if (applyForm.value.requestType === 'JOIN_PROJECT') {
    if (!applyForm.value.projectId) {
      const pid = projectStore.currentProject?.id
      if (!pid) {
        showToast('请选择项目或先在左侧选择当前项目', 'error')
        return
      }
      applyForm.value.projectId = pid
    }
  }
  try {
    submitting.value = true
    await request.post('/permission-requests', {
      requestType: applyForm.value.requestType,
      projectId: applyForm.value.projectId,
      description: applyForm.value.description
    })
    showToast('申请已提交，等待管理员审批', 'success')
    applyForm.value = { requestType: 'JOIN_PROJECT', projectId: null, description: '' }
  } catch (e: any) {
    showToast(e.message || '提交失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container mx-auto py-10 max-w-4xl">
    <Card class="mb-6">
      <CardHeader>
        <div class="flex items-center justify-between">
          <div>
            <CardTitle>个人中心</CardTitle>
            <CardDescription>管理您的个人资料和账户安全</CardDescription>
          </div>
          <div class="flex gap-2">
            <Button v-if="userStore.user?.role === 'admin'" variant="outline" @click="router.push('/settings')">
              <Settings class="w-4 h-4 mr-2" />
              后台管理
            </Button>
            <Button variant="ghost" size="icon" @click="handleLogout">
              <LogOut class="w-5 h-5" />
            </Button>
          </div>
        </div>
      </CardHeader>
    </Card>

    <Tabs default-value="profile" class="space-y-4">
      <TabsList class="grid w-full grid-cols-4">
        <TabsTrigger value="profile">个人资料</TabsTrigger>
        <TabsTrigger value="security">账户安全</TabsTrigger>
        <TabsTrigger value="assets">我的资产</TabsTrigger>
        <TabsTrigger value="notification">消息通知</TabsTrigger>
      </TabsList>
      
      <!-- Profile Tab -->
      <TabsContent value="profile">
        <Card>
          <CardHeader>
            <CardTitle>基本信息</CardTitle>
            <CardDescription>更新您的个人基本资料</CardDescription>
          </CardHeader>
          <CardContent class="space-y-6">
            <div class="flex flex-col md:flex-row items-center gap-6 pb-6 border-b">
              <div class="w-32 h-32 rounded-full bg-blue-100 flex items-center justify-center border-4 border-white shadow-lg overflow-hidden shrink-0">
                <img 
                  :src="profileForm.avatar || `https://api.dicebear.com/7.x/${currentAvatarStyle}/svg?seed=${userStore.user?.username || 'user'}`" 
                  alt="Avatar"
                  class="w-full h-full object-cover"
                />
              </div>
              <div class="space-y-4 flex-1 w-full">
                 <div class="space-y-2">
                   <Label>选择头像风格</Label>
                   <div class="flex flex-wrap gap-2">
                     <Button 
                       v-for="style in avatarStyles" 
                       :key="style.value"
                       variant="outline" 
                       size="sm"
                       :class="currentAvatarStyle === style.value ? 'bg-primary text-primary-foreground hover:bg-primary/90' : ''"
                       @click="changeAvatarStyle(style.value)"
                     >
                       {{ style.label }}
                     </Button>
                   </div>
                 </div>
                 <div class="flex items-center gap-2">
                   <Button variant="secondary" size="sm" @click="refreshAvatar">
                     <RefreshCw class="w-4 h-4 mr-2" /> 随机生成
                   </Button>
                   <div class="text-xs text-muted-foreground ml-2">
                     点击上方风格或随机生成按钮更换头像
                   </div>
                 </div>
              </div>
            </div>
            
            <div class="grid gap-4 md:grid-cols-2">
              <div class="space-y-2">
                <Label>昵称</Label>
                <Input v-model="profileForm.nickname" placeholder="请输入昵称" />
              </div>
              <div class="space-y-2">
                <Label>邮箱</Label>
                <Input v-model="profileForm.email" placeholder="请输入邮箱" />
              </div>
              <div class="space-y-2">
                <Label>手机号</Label>
                <Input v-model="profileForm.phone" placeholder="请输入手机号" />
              </div>
              <div class="space-y-2">
                <Label>角色</Label>
                <Input :model-value="userStore.user?.role === 'admin' ? '管理员' : '普通用户'" readonly disabled class="bg-gray-50" />
              </div>
            </div>
            
            <div class="flex justify-end">
              <Button @click="handleUpdateProfile">保存更改</Button>
            </div>
          </CardContent>
        </Card>

        <Card class="mt-6">
          <CardHeader>
            <CardTitle>申请权限</CardTitle>
            <CardDescription>当需要加入项目或升级为管理员时，提交申请由管理员审批</CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="space-y-2">
                <Label>申请类型</Label>
                <select v-model="applyForm.requestType" class="h-9 px-3 rounded-md border border-input bg-background text-sm">
                  <option value="JOIN_PROJECT">加入当前项目</option>
                  <option value="UPGRADE_ROLE">申请管理员</option>
                </select>
              </div>
              <div class="space-y-2" v-if="applyForm.requestType === 'JOIN_PROJECT'">
                <Label>项目</Label>
                <select v-model="applyForm.projectId" class="h-9 px-3 rounded-md border border-input bg-background text-sm">
                  <option :value="null">使用左侧当前项目</option>
                  <option v-for="p in projectStore.projectList" :key="p.id" :value="p.id">#{{ p.id }} - {{ p.name }}</option>
                </select>
              </div>
            </div>
            <div class="space-y-2">
              <Label>说明（可选）</Label>
              <Input v-model="applyForm.description" placeholder="说明申请原因或用途" />
            </div>
            <div class="flex justify-end">
              <Button :disabled="submitting" @click="submitPermissionRequest">
                提交申请
              </Button>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
      
      <!-- Security Tab -->
      <TabsContent value="security" class="space-y-4">
        <Card>
          <CardHeader>
            <CardTitle class="flex items-center gap-2"><Lock class="w-4 h-4"/> 修改密码</CardTitle>
            <CardDescription>定期修改密码可以保护您的账号安全</CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
             <div class="space-y-2">
               <Label>当前密码</Label>
               <Input type="password" v-model="passwordForm.oldPassword" placeholder="请输入当前密码" />
             </div>
             <div class="space-y-2">
               <Label>新密码</Label>
               <Input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码" />
             </div>
             <div class="space-y-2">
               <Label>确认新密码</Label>
               <Input type="password" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
             </div>
             <div class="flex justify-end">
               <Button @click="handleChangePassword">修改密码</Button>
             </div>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader>
            <CardTitle>登录日志</CardTitle>
            <CardDescription>最近 10 次登录记录</CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>时间</TableHead>
                  <TableHead>IP 地址</TableHead>
                  <TableHead>状态</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="log in loginLogs" :key="log.id">
                  <TableCell>{{ new Date(log.loginTime).toLocaleString() }}</TableCell>
                  <TableCell>{{ log.ipAddress }}</TableCell>
                  <TableCell>
                    <span :class="log.status === 'success' ? 'text-green-600' : 'text-red-600'">
                      {{ log.status === 'success' ? '成功' : '失败' }}
                    </span>
                  </TableCell>
                </TableRow>
                <TableRow v-if="loginLogs.length === 0">
                  <TableCell colspan="3" class="text-center text-gray-500">暂无登录记录</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </TabsContent>
      
      <!-- Assets Tab -->
      <TabsContent value="assets">
        <Tabs default-value="variables" class="w-full">
          <TabsList class="w-full justify-start border-b rounded-none bg-transparent p-0 mb-4 h-auto">
            <TabsTrigger 
              value="variables" 
              class="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-600 data-[state=active]:bg-transparent px-4 py-2"
            >
              个人变量
            </TabsTrigger>
            <TabsTrigger 
              value="apikeys" 
              class="rounded-none border-b-2 border-transparent data-[state=active]:border-blue-600 data-[state=active]:bg-transparent px-4 py-2"
            >
              API Keys
            </TabsTrigger>
          </TabsList>
          
          <TabsContent value="variables" class="mt-0">
            <Card>
              <CardHeader class="flex flex-row items-center justify-between">
                <div>
                  <CardTitle>个人变量 (Private Variables)</CardTitle>
                  <CardDescription>仅您可见的测试参数，优先级高于全局变量</CardDescription>
                </div>
                <Button size="sm" @click="openCreateVarDialog">新增变量</Button>
              </CardHeader>
              <CardContent>
                 <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>变量名</TableHead>
                      <TableHead>值</TableHead>
                      <TableHead>描述</TableHead>
                      <TableHead class="text-right">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    <TableRow v-for="v in variables" :key="v.id">
                      <TableCell class="font-medium">{{ v.keyName }}</TableCell>
                      <TableCell class="truncate max-w-[200px]">{{ v.value }}</TableCell>
                      <TableCell>{{ v.description }}</TableCell>
                      <TableCell class="text-right space-x-2">
                        <Button variant="ghost" size="sm" @click="editVariable(v)">编辑</Button>
                        <Button variant="ghost" size="sm" class="text-red-600" @click="deleteVariable(v.id)">删除</Button>
                      </TableCell>
                    </TableRow>
                    <TableRow v-if="variables.length === 0">
                      <TableCell colspan="4" class="text-center text-gray-500">暂无私有变量</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>
          
          <TabsContent value="apikeys" class="mt-0">
            <Card>
              <CardHeader class="flex flex-row items-center justify-between">
                <div>
                  <CardTitle>API Keys</CardTitle>
                  <CardDescription>用于个人脚本调试和第三方集成的访问密钥</CardDescription>
                </div>
                <Button size="sm" @click="generateApiKey">生成 Key</Button>
              </CardHeader>
              <CardContent>
                <div class="space-y-4">
                  <div v-for="key in apiKeys" :key="key.id" class="flex items-center justify-between p-4 border rounded-lg bg-gray-50">
                    <div class="space-y-1">
                      <div class="flex items-center gap-2">
                        <span class="font-medium">{{ key.name }}</span>
                        <span class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700">Active</span>
                      </div>
                      <div class="flex items-center gap-2 text-sm text-gray-500">
                        <span>创建于: {{ key.createdAt }}</span>
                        <span class="mx-1">|</span>
                        <span>最后使用: {{ key.lastUsedAt }}</span>
                      </div>
                      <div class="flex items-center gap-2 mt-2">
                        <div class="relative">
                          <code class="px-2 py-1 bg-gray-200 rounded text-sm font-mono">
                            {{ key.visible ? key.accessKey : '••••••••••••••••••••••••••••••••' }}
                          </code>
                        </div>
                        <Button variant="ghost" size="icon" class="h-6 w-6" @click="key.visible = !key.visible">
                          <Eye v-if="!key.visible" class="w-3 h-3" />
                          <EyeOff v-else class="w-3 h-3" />
                        </Button>
                        <Button variant="ghost" size="icon" class="h-6 w-6" @click="copyToClipboard(key.accessKey)">
                          <Copy class="w-3 h-3" />
                        </Button>
                      </div>
                    </div>
                    <Button variant="ghost" size="sm" class="text-red-600 hover:text-red-700 hover:bg-red-50" @click="revokeApiKey(key.id)">
                      <Trash2 class="w-4 h-4 mr-1" /> 撤销
                    </Button>
                  </div>
                  
                  <div v-if="apiKeys.length === 0" class="text-center py-8 text-gray-500">
                    暂无 API Key，请点击右上角生成
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
            
        <!-- Simple Dialog/Modal for Variable Editing -->
        <div v-if="showVarDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div class="bg-white rounded-lg p-6 w-full max-w-md shadow-xl">
            <h3 class="text-lg font-semibold mb-4">{{ currentVar.id ? '编辑变量' : '新增变量' }}</h3>
            <div class="space-y-4">
              <div class="space-y-2">
                <Label>变量名 (Key)</Label>
                <Input v-model="currentVar.keyName" placeholder="例如: api_key" />
              </div>
              <div class="space-y-2">
                <Label>变量值 (Value)</Label>
                <Input v-model="currentVar.value" placeholder="变量的具体值" />
              </div>
              <div class="space-y-2">
                <Label>描述</Label>
                <Input v-model="currentVar.description" placeholder="用途说明" />
              </div>
            </div>
            <div class="flex justify-end gap-2 mt-6">
              <Button variant="outline" @click="showVarDialog = false">取消</Button>
              <Button @click="saveVariable">保存</Button>
            </div>
          </div>
        </div>
      </TabsContent>
      <!-- Notification Tab -->
      <TabsContent value="notification">
        <Card>
          <CardHeader>
            <CardTitle>消息通知中心</CardTitle>
            <CardDescription>配置您的消息通知偏好，接收测试失败告警</CardDescription>
          </CardHeader>
          <CardContent class="space-y-6">
            <div class="flex items-center justify-between p-4 border rounded-lg bg-gray-50">
              <div class="space-y-0.5">
                <Label class="text-base">开启 Webhook 通知</Label>
                <p class="text-sm text-gray-500">当您负责的测试计划执行失败时，发送告警消息</p>
              </div>
              <div class="flex items-center space-x-2">
                <input 
                  type="checkbox" 
                  class="toggle toggle-primary" 
                  v-model="profileForm.enableNotification"
                  @change="handleUpdateProfile"
                />
                <span class="text-sm font-medium">{{ profileForm.enableNotification ? '已开启' : '已关闭' }}</span>
              </div>
            </div>
            
            <div v-if="profileForm.enableNotification" class="space-y-4 animate-in fade-in slide-in-from-top-2">
              <div class="space-y-2">
                <Label>通知事件触发配置</Label>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                  <label class="flex items-center gap-2 text-sm">
                    <input type="radio" name="notifyRule" value="on_fail" @change="saveNotifyRule('on_fail')" :checked="notifyRule === 'on_fail'" />
                    仅在任务失败时通知
                  </label>
                  <label class="flex items-center gap-2 text-sm">
                    <input type="radio" name="notifyRule" value="low_pass_rate" @change="saveNotifyRule('low_pass_rate')" :checked="notifyRule === 'low_pass_rate'" />
                    通过率低于阈值时通知
                  </label>
                  <label class="flex items-center gap-2 text-sm">
                    <input type="radio" name="notifyRule" value="all" @change="saveNotifyRule('all')" :checked="notifyRule === 'all'" />
                    所有任务通知
                  </label>
                </div>
                <div v-if="notifyRule === 'low_pass_rate'" class="flex items-center gap-2 mt-2">
                  <Label>阈值</Label>
                  <input type="number" min="0" max="100" step="1" v-model="notifyThreshold" @change="saveNotifyThreshold" class="h-9 px-3 rounded-md border border-input bg-background text-sm w-24" />
                  <span class="text-sm text-gray-500">%（例如 80）</span>
                </div>
                <p class="text-xs text-gray-400">提示：通知规则仅用于减少消息噪音，后续将配合后端执行引擎触发。</p>
              </div>
              <div class="space-y-2">
                <Label>Webhook 地址 (钉钉/飞书/企业微信)</Label>
                <Input 
                  v-model="profileForm.notificationWebhook" 
                  placeholder="https://oapi.dingtalk.com/robot/send?access_token=..." 
                />
                <p class="text-xs text-gray-500">请输入完整的 Webhook URL，系统将通过 POST 请求发送告警信息。</p>
              </div>
              
              <div class="flex justify-end">
                <Button @click="handleUpdateProfile">保存配置</Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
    <ConfirmDialog 
      :open="confirmDialog.open"
      :title="confirmDialog.title"
      :description="confirmDialog.description"
      :variant="confirmDialog.variant"
      @update:open="(val) => confirmDialog.open = val"
      @confirm="handleConfirmAction"
    />
  </div>
</template>
