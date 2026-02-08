<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { User, LogOut, Shield } from 'lucide-vue-next'
import { userStore } from '@/store/userStore'

const router = useRouter()

const handleLogout = () => {
  userStore.clearUser()
  router.push('/login')
}
</script>

<template>
  <div class="container mx-auto py-10 max-w-4xl">
    <Card>
      <CardHeader>
        <CardTitle class="text-2xl">个人中心</CardTitle>
        <CardDescription>管理您的个人信息和账户设置</CardDescription>
      </CardHeader>
      <CardContent class="space-y-8">
        
        <!-- Profile Header -->
        <div class="flex items-center gap-6 pb-8 border-b">
          <div class="w-24 h-24 rounded-full bg-blue-100 flex items-center justify-center border-4 border-white shadow-lg overflow-hidden">
            <img 
              :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${userStore.user?.username || 'user'}`" 
              alt="Avatar"
              class="w-full h-full object-cover"
            />
          </div>
          <div>
            <h2 class="text-2xl font-bold text-gray-900">{{ userStore.user?.username || 'User' }}</h2>
            <p class="text-gray-500">{{ userStore.user?.role === 'admin' ? '管理员' : '普通用户' }}</p>
          </div>
        </div>

        <!-- Account Info -->
        <div class="grid gap-6 md:grid-cols-2">
          <div class="space-y-2">
            <Label>登录账号</Label>
            <div class="relative">
              <User class="absolute left-3 top-3 h-4 w-4 text-gray-500" />
              <Input :model-value="userStore.user?.username || ''" readonly class="pl-9 bg-gray-50" />
            </div>
          </div>
          
          <div class="space-y-2">
            <Label>用户角色</Label>
            <div class="relative">
              <Shield class="absolute left-3 top-3 h-4 w-4 text-gray-500" />
              <Input :model-value="userStore.user?.role || 'user'" readonly class="pl-9 bg-gray-50" />
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end pt-4 border-t">
          <Button variant="destructive" @click="handleLogout" class="gap-2">
            <LogOut class="w-4 h-4" />
            退出登录
          </Button>
        </div>

      </CardContent>
    </Card>
  </div>
</template>
