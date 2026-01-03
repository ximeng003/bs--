<script setup lang="ts">
import { ref } from 'vue'
import { Card } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import Textarea from '@/components/ui/textarea/Textarea.vue'
import Switch from '@/components/ui/switch/Switch.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const emit = defineEmits(['close'])

const scheduleEnabled = ref(false)

const handleClose = () => {
  emit('close')
}
</script>

<template>
  <div class="space-y-4">
    <div class="space-y-2">
      <Label>计划名称</Label>
      <Input placeholder="输入测试计划名称" />
    </div>

    <div class="space-y-2">
      <Label>计划描述</Label>
      <Textarea placeholder="描述测试计划的目的和范围" :rows="3" />
    </div>

    <div class="space-y-2">
      <Label>运行环境</Label>
      <Select>
        <SelectTrigger>
          <SelectValue placeholder="选择运行环境" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="dev">开发环境</SelectItem>
          <SelectItem value="staging">测试环境</SelectItem>
          <SelectItem value="production">生产环境</SelectItem>
        </SelectContent>
      </Select>
    </div>

    <div class="space-y-2">
      <Label>选择测试用例</Label>
      <Card class="p-4">
        <div class="space-y-2 max-h-48 overflow-y-auto">
          <label class="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
            <input type="checkbox" class="rounded" />
            <span class="text-sm">用户登录接口测试 (API)</span>
          </label>
          <label class="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
            <input type="checkbox" class="rounded" />
            <span class="text-sm">Web首页功能测试 (WEB)</span>
          </label>
          <label class="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
            <input type="checkbox" class="rounded" />
            <span class="text-sm">APP支付流程测试 (APP)</span>
          </label>
          <label class="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
            <input type="checkbox" class="rounded" />
            <span class="text-sm">用户注册接口测试 (API)</span>
          </label>
          <label class="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
            <input type="checkbox" class="rounded" />
            <span class="text-sm">数据查询接口测试 (API)</span>
          </label>
        </div>
      </Card>
    </div>

    <div class="space-y-4 p-4 bg-gray-50 rounded-lg">
      <div class="flex items-center justify-between">
        <div>
          <Label>启用定时任务</Label>
          <p class="text-xs text-gray-500 mt-1">自动按计划执行测试</p>
        </div>
        <Switch v-model="scheduleEnabled" />
      </div>

      <div v-if="scheduleEnabled" class="space-y-4">
        <div class="space-y-2">
          <Label>Cron 表达式</Label>
          <Input placeholder="0 2 * * * (每天凌晨2点)" />
          <p class="text-xs text-gray-500">
            格式: 分 时 日 月 周，例如 "0 2 * * *" 表示每天凌晨2点
          </p>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <Button variant="outline" size="sm">每小时</Button>
          <Button variant="outline" size="sm">每天</Button>
          <Button variant="outline" size="sm">每周</Button>
          <Button variant="outline" size="sm">每月</Button>
        </div>
      </div>
    </div>

    <div class="space-y-2">
      <Label>失败重试</Label>
      <Select default-value="0">
        <SelectTrigger>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="0">不重试</SelectItem>
          <SelectItem value="1">失败重试1次</SelectItem>
          <SelectItem value="2">失败重试2次</SelectItem>
          <SelectItem value="3">失败重试3次</SelectItem>
        </SelectContent>
      </Select>
    </div>

    <div class="flex justify-end gap-2 pt-4">
      <Button variant="outline" @click="handleClose">
        取消
      </Button>
      <Button @click="handleClose">
        创建计划
      </Button>
    </div>
  </div>
</template>
