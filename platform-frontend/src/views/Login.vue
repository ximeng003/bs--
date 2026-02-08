<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import request from '@/api/request'
import { userStore } from '@/store/userStore'

const router = useRouter()
const isRegister = ref(false)
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

const toggleMode = () => {
  isRegister.value = !isRegister.value
  error.value = ''
  username.value = ''
  password.value = ''
  confirmPassword.value = ''
}

const handleSubmit = async () => {
  error.value = ''
  
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }

  if (isRegister.value) {
    if (password.value !== confirmPassword.value) {
      error.value = '两次输入的密码不一致'
      return
    }
  }
  
  loading.value = true
  
  try {
    if (isRegister.value) {
      await request.post('/auth/register', {
        username: username.value,
        password: password.value
      })
      alert('注册成功，请登录')
      toggleMode()
    } else {
      const res: any = await request.post('/auth/login', {
        username: username.value,
        password: password.value
      })
      // Login success
      userStore.setUser(res)
      router.push('/')
    }
  } catch (e: any) {
    error.value = (isRegister.value ? '注册失败: ' : '登录失败: ') + (e.message || '未知错误')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex items-center justify-center min-h-screen bg-gray-100">
    <div class="w-full max-w-md p-8 bg-white rounded-lg shadow-md transition-all">
      <div class="text-center mb-8">
        <h1 class="text-2xl font-bold text-gray-900">自动化测试平台</h1>
        <p class="text-gray-500 mt-2">
          {{ isRegister ? '创建新账户' : '请登录您的账户' }}
        </p>
      </div>
      
      <div class="space-y-4">
        <div class="space-y-2">
          <Label>用户名</Label>
          <Input v-model="username" placeholder="请输入用户名" />
        </div>
        
        <div class="space-y-2">
          <Label>密码</Label>
          <Input v-model="password" type="password" placeholder="请输入密码" @keyup.enter="handleSubmit" />
        </div>

        <div v-if="isRegister" class="space-y-2">
          <Label>确认密码</Label>
          <Input v-model="confirmPassword" type="password" placeholder="请再次输入密码" @keyup.enter="handleSubmit" />
        </div>
        
        <div v-if="error" class="text-red-500 text-sm">
          {{ error }}
        </div>
        
        <Button class="w-full" :disabled="loading" @click="handleSubmit">
          {{ loading ? (isRegister ? '注册中...' : '登录中...') : (isRegister ? '注册' : '登录') }}
        </Button>
        
        <div class="text-center text-sm mt-4">
          <span class="text-gray-500">
            {{ isRegister ? '已有账号?' : '没有账号?' }}
          </span>
          <button 
            class="ml-2 text-blue-600 hover:underline font-medium focus:outline-none"
            @click="toggleMode"
          >
            {{ isRegister ? '去登录' : '去注册' }}
          </button>
        </div>

        <div v-if="!isRegister" class="text-center text-xs text-gray-400 mt-6 pt-4 border-t">
            默认账号: admin / 123456
        </div>
      </div>
    </div>
  </div>
</template>
