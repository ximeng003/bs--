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
import { User, LogOut, Shield, Lock, Database, Settings, Copy, Eye, EyeOff, Bell, Trash2, Plus } from 'lucide-vue-next'
import { userStore } from '@/store/userStore'
import { useProjectStore } from '@/store/projectStore'
import { showToast } from '@/lib/notify'
import request from '@/api/request'
import { Badge } from '@/components/ui/badge'

const router = useRouter()
const projectStore = useProjectStore()

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
  enableNotification: false
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

// Team Data
const teams = ref<any[]>([])
const showTeamDialog = ref(false)
const teamForm = ref({
  name: '',
  description: ''
})

// Project Data
const showProjectDialog = ref(false)
const projectForm = ref({
  name: '',
  description: '',
  teamId: null as number | null
})

const switchToProject = (project: any) => {
  projectStore.setCurrentProject(project)
  showToast(`已切换到项目: ${project.name}`, 'success')
  window.location.reload()
}

const fetchTeams = async () => {
  try {
    const res: any = await request.get('/teams')
    if (res) {
      teams.value = res
    }
  } catch (e) {
    console.error(e)
  }
}

const handleCreateTeam = async () => {
  if (!teamForm.value.name) return showToast('请输入团队名称', 'error')
  try {
    await request.post('/teams', teamForm.value)
    showToast('团队创建成功', 'success')
    showTeamDialog.value = false
    teamForm.value = { name: '', description: '' }
    fetchTeams()
  } catch (e: any) {
    showToast(e.message || '创建失败', 'error')
  }
}

const handleCreateProject = async () => {
  if (!projectForm.value.name) return showToast('请输入项目名称', 'error')
  if (!projectForm.value.teamId) return showToast('请选择归属团队', 'error')
  
  try {
    await request.post('/projects', projectForm.value)
    showToast('项目创建成功', 'success')
    showProjectDialog.value = false
    projectForm.value = { name: '', description: '', teamId: null }
    // Refresh project list
    await projectStore.fetchProjects()
  } catch (e: any) {
    showToast(e.message || '创建失败', 'error')
  }
}

// Team Management
const showTeamManageDialog = ref(false)
const currentTeam = ref<any>(null)
const teamMembers = ref<any[]>([])
const newMemberForm = ref({
  username: '',
  role: 'member'
})

const fetchTeamMembers = async (teamId: number) => {
  try {
    const res: any = await request.get(`/teams/${teamId}/members`)
    if (res) {
      teamMembers.value = res
    }
  } catch (e: any) {
    showToast(e.message || '获取成员失败', 'error')
  }
}

const openTeamSettings = async (team: any) => {
  currentTeam.value = team
  await fetchTeamMembers(team.id)
  showTeamManageDialog.value = true
}

const handleAddMember = async () => {
  if (!newMemberForm.value.username) return showToast('请输入用户名', 'error')
  try {
    await request.post(`/teams/${currentTeam.value.id}/members`, newMemberForm.value)
    showToast('成员添加成功', 'success')
    newMemberForm.value.username = ''
    fetchTeamMembers(currentTeam.value.id)
  } catch (e: any) {
    showToast(e.message || '添加失败', 'error')
  }
}

const handleRemoveMember = async (userId: number) => {
  if (!confirm('确定要移除该成员吗？')) return
  try {
    await request.delete(`/teams/${currentTeam.value.id}/members/${userId}`)
    showToast('成员已移除', 'success')
    fetchTeamMembers(currentTeam.value.id)
  } catch (e: any) {
    showToast(e.message || '移除失败', 'error')
  }
}

const showRoleConfirmDialog = ref(false)
const roleConfirmData = ref<{ member: any, newRole: string } | null>(null)

const handleUpdateMemberRole = (member: any, newRole: string) => {
  roleConfirmData.value = { member, newRole }
  showRoleConfirmDialog.value = true
}

const confirmUpdateRole = async () => {
  if (!roleConfirmData.value) return
  const { member, newRole } = roleConfirmData.value
  
  try {
    await request.put(`/teams/${currentTeam.value.id}/members/${member.id}`, { role: newRole })
    showToast('角色更新成功', 'success')
    fetchTeamMembers(currentTeam.value.id)
    showRoleConfirmDialog.value = false
  } catch (e: any) {
    showToast(e.message || '更新失败', 'error')
  }
}

// Fetch Profile
const fetchProfile = async () => {
  try {
    const res: any = await request.get('/user/profile')
    if (res) {
      profileForm.value = {
        nickname: res.nickname || '',
        email: res.email || '',
        phone: res.phone || '',
        avatar: res.avatar || '',
        notificationWebhook: res.notificationWebhook || '',
        enableNotification: res.enableNotification || false
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

const deleteVariable = async (id: number) => {
  if (!confirm('确定要删除该变量吗？')) return
  try {
    await request.delete(`/user/variables/${id}`)
    showToast('删除成功', 'success')
    fetchVariables()
  } catch (e: any) {
    showToast(e.message || '删除失败', 'error')
  }
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

const goToAdmin = () => {
  router.push('/settings')
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
    const res: any = await request.post('/user/api-keys', { name: `Personal Key ${apiKeys.value.length + 1}` })
    showToast('API Key 生成成功', 'success')
    loadApiKeys()
  } catch (e: any) {
    showToast(e.message || '生成失败', 'error')
  }
}

const revokeApiKey = async (id: string) => {
  if (!confirm('确定要撤销此密钥吗？撤销后将无法恢复。')) return
  try {
    await request.delete(`/user/api-keys/${id}`)
    showToast('API Key 已撤销', 'success')
    loadApiKeys()
  } catch (e: any) {
    showToast(e.message || '撤销失败', 'error')
  }
}

const saveApiKeys = () => {
  // Deprecated: using backend
}

const handleDeleteTeam = async (id: number) => {
  if (!confirm('确定要删除该团队吗？这将同时删除所有团队成员，但必须先删除团队下的项目。')) return
  try {
    await request.delete(`/teams/${id}`)
    showToast('团队删除成功', 'success')
    fetchTeams()
  } catch (e: any) {
    showToast(e.message || '删除失败', 'error')
  }
}

const handleDeleteProject = async (id: number) => {
  if (!confirm('确定要删除该项目吗？此操作不可恢复。')) return
  try {
    await request.delete(`/projects/${id}`)
    showToast('项目删除成功', 'success')
    projectStore.fetchProjects()
    if (projectStore.currentProject?.id === id) {
      projectStore.setCurrentProject(null)
      window.location.reload()
    }
  } catch (e: any) {
    showToast(e.message || '删除失败', 'error')
  }
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    showToast('已复制到剪贴板', 'success')
  }).catch(() => {
    showToast('复制失败', 'error')
  })
}

const getTeamName = (teamId: number) => {
  if (!teamId) return '-'
  const team = teams.value.find(t => t.id === teamId)
  return team ? team.name : '-'
}

onMounted(() => {
  fetchProfile()
  fetchLogs()
  fetchVariables()
  loadApiKeys()
  fetchTeams()
  projectStore.fetchProjects()
})
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
      <TabsList class="grid w-full grid-cols-5">
        <TabsTrigger value="profile">个人资料</TabsTrigger>
        <TabsTrigger value="security">账户安全</TabsTrigger>
        <TabsTrigger value="assets">我的资产</TabsTrigger>
        <TabsTrigger value="notification">消息通知</TabsTrigger>
        <TabsTrigger value="projects">我的项目</TabsTrigger>
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
                       :class="{'bg-primary text-primary-foreground hover:bg-primary/90': currentAvatarStyle === style.value}"
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

      <!-- My Projects Tab -->
      <TabsContent value="projects">
        <div class="space-y-6">
          <Card>
            <CardHeader class="flex flex-row items-center justify-between">
              <div>
                <CardTitle>我的团队</CardTitle>
                <CardDescription>您所属的团队列表</CardDescription>
              </div>
              <Button @click="showTeamDialog = true">
                <Plus class="w-4 h-4 mr-2" /> 创建团队
              </Button>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>团队名称</TableHead>
                    <TableHead>描述</TableHead>
                    <TableHead>创建时间</TableHead>
                    <TableHead class="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="team in teams" :key="team.id">
                    <TableCell class="font-medium">{{ team.name }}</TableCell>
                    <TableCell>{{ team.description || '-' }}</TableCell>
                    <TableCell>{{ new Date(team.createdAt).toLocaleDateString() }}</TableCell>
                    <TableCell class="text-right">
                      <Button variant="ghost" size="sm" @click="openTeamSettings(team)">管理</Button>
                      <Button variant="ghost" size="sm" class="text-red-600 ml-2" @click="handleDeleteTeam(team.id)">删除</Button>
                    </TableCell>
                  </TableRow>
                  <TableRow v-if="teams.length === 0">
                    <TableCell colspan="4" class="text-center text-gray-500 py-8">暂无团队</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card>
            <CardHeader class="flex flex-row items-center justify-between">
              <div>
                <CardTitle>我的项目</CardTitle>
                <CardDescription>您参与的所有项目列表</CardDescription>
              </div>
              <Button @click="showProjectDialog = true">
                <Plus class="w-4 h-4 mr-2" /> 创建项目
              </Button>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>项目名称</TableHead>
                    <TableHead>描述</TableHead>
                    <TableHead>角色</TableHead>
                    <TableHead>归属团队</TableHead>
                    <TableHead class="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="project in projectStore.projectList" :key="project.id">
                    <TableCell class="font-medium">
                      <div class="flex items-center gap-2">
                        <div class="w-2 h-2 rounded-full bg-primary" v-if="projectStore.currentProject?.id === project.id"></div>
                        {{ project.name }}
                      </div>
                    </TableCell>
                    <TableCell>{{ project.description || '-' }}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{{ project.role || 'Member' }}</Badge>
                    </TableCell>
                    <TableCell>{{ getTeamName(project.teamId) }}</TableCell>
                    <TableCell class="text-right">
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        :disabled="projectStore.currentProject?.id === project.id"
                        @click="switchToProject(project)"
                      >
                        切换
                      </Button>
                      <Button 
                        variant="ghost" 
                        size="sm"
                        class="text-red-600 ml-2" 
                        @click="handleDeleteProject(project.id)"
                      >
                        删除
                      </Button>
                    </TableCell>
                  </TableRow>
                  <TableRow v-if="projectStore.projectList.length === 0">
                    <TableCell colspan="4" class="text-center text-gray-500 py-8">暂无项目</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

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
    <div v-if="showProjectDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md shadow-xl">
        <h3 class="text-lg font-semibold mb-4">创建新项目</h3>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>项目名称</Label>
            <Input v-model="projectForm.name" placeholder="输入项目名称" />
          </div>
          <div class="space-y-2">
            <Label>描述</Label>
            <Input v-model="projectForm.description" placeholder="可选描述" />
          </div>
          <div class="space-y-2">
            <Label>归属团队</Label>
            <select v-model="projectForm.teamId" class="w-full bg-white border border-gray-300 rounded px-3 py-2 text-sm">
              <option :value="null" disabled>请选择团队</option>
              <option v-for="team in teams" :key="team.id" :value="team.id">{{ team.name }}</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <Button variant="outline" @click="showProjectDialog = false">取消</Button>
          <Button @click="handleCreateProject">创建</Button>
        </div>
      </div>
    </div>

    <!-- Team Management Dialog -->
    <div v-if="showTeamManageDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-2xl shadow-xl max-h-[90vh] overflow-y-auto">
        <div class="flex justify-between items-center mb-6">
          <div>
            <h3 class="text-lg font-semibold">{{ currentTeam?.name }} - 成员管理</h3>
            <p class="text-sm text-gray-500">{{ currentTeam?.description }}</p>
          </div>
          <Button variant="ghost" size="icon" @click="showTeamManageDialog = false">
            <span class="text-2xl">&times;</span>
          </Button>
        </div>
        
        <div class="space-y-6">
          <!-- Add Member -->
          <div class="flex gap-4 items-end bg-gray-50 p-4 rounded-lg">
            <div class="flex-1 space-y-2">
              <Label>用户名</Label>
              <Input v-model="newMemberForm.username" placeholder="输入用户名邀请加入" />
            </div>
            <div class="w-32 space-y-2">
              <Label>角色</Label>
              <select v-model="newMemberForm.role" class="w-full h-10 px-3 rounded-md border border-input bg-background text-sm">
                <option value="member">成员</option>
                <option value="admin">管理员</option>
              </select>
            </div>
            <Button @click="handleAddMember">添加</Button>
          </div>

          <!-- Member List -->
          <div>
            <h4 class="font-medium mb-4">成员列表 ({{ teamMembers.length }})</h4>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>用户</TableHead>
                  <TableHead>角色</TableHead>
                  <TableHead>加入时间</TableHead>
                  <TableHead class="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="member in teamMembers" :key="member.id">
                  <TableCell>
                    <div class="flex items-center gap-2">
                      <div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden">
                        <img v-if="member.avatar" :src="member.avatar" class="w-full h-full object-cover" />
                        <span v-else class="text-xs">{{ member.username?.substring(0, 2).toUpperCase() }}</span>
                      </div>
                      <div>
                        <div class="font-medium">{{ member.nickname || member.username }}</div>
                        <div class="text-xs text-gray-500">@{{ member.username }}</div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge :variant="member.role === 'admin' ? 'default' : 'secondary'">
                      {{ member.role === 'admin' ? '管理员' : '成员' }}
                    </Badge>
                  </TableCell>
                  <TableCell class="text-gray-500 text-sm">
                    {{ new Date(member.joinedAt).toLocaleDateString() }}
                  </TableCell>
                  <TableCell class="text-right">
                    <div class="flex justify-end gap-2" v-if="member.userId !== userStore.user?.id">
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
              </TableBody>
            </Table>
          </div>
        </div>
      </div>
    </div>


    <!-- Role Change Confirmation Dialog -->
    <div v-if="showRoleConfirmDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-[60]">
      <div class="bg-white rounded-lg p-6 w-full max-w-sm shadow-xl">
        <h3 class="text-lg font-semibold mb-2">确认角色变更</h3>
        <p class="text-gray-600 mb-6">
          确定要将用户 
          <span class="font-bold text-gray-900">{{ roleConfirmData?.member?.username }}</span> 
          的角色设置为 
          <span class="font-bold text-blue-600">{{ roleConfirmData?.newRole === 'admin' ? '管理员' : '普通成员' }}</span> 
          吗？
        </p>
        <div class="flex justify-end gap-2">
          <Button variant="outline" @click="showRoleConfirmDialog = false">取消</Button>
          <Button @click="confirmUpdateRole" :variant="roleConfirmData?.newRole === 'admin' ? 'default' : 'secondary'">
            确认变更
          </Button>
        </div>
      </div>
    </div>

    </Tabs>
  </div>
</template>
