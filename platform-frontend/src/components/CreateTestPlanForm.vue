<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { Card } from '@/components/ui/card'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import Textarea from '@/components/ui/textarea/Textarea.vue'
import Switch from '@/components/ui/switch/Switch.vue'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import request from '@/api/request'
import { showToast } from '@/lib/notify'

const props = defineProps<{
  mode?: 'create' | 'edit'
  plan?: any | null
}>()

const emit = defineEmits(['close', 'success'])

const form = ref({
  name: '',
  description: '',
  environment: 'dev',
  testCaseIds: [] as string[],
  scheduleEnabled: false,
  cronExpression: ''
})

const testCases = ref<any[]>([])

const handleClose = () => {
  emit('close')
}

const fetchTestCases = async () => {
  try {
    const res: any = await request.get('/testcases?size=100')
    if (res && res.records) {
      testCases.value = res.records
    }
  } catch (e) {
    console.error(e)
  }
}

const resetFormFromPlan = () => {
  if (props.mode === 'edit' && props.plan) {
    form.value.name = props.plan.name || ''
    form.value.description = props.plan.description || ''
    form.value.environment = props.plan.environment || 'dev'
    form.value.scheduleEnabled = !!props.plan.cronExpression
    form.value.cronExpression = props.plan.cronExpression || ''
    const rawIds = props.plan.testCaseIds || props.plan.test_case_ids || ''
    if (rawIds) {
      form.value.testCaseIds = String(rawIds)
        .split(',')
        .map((s: string) => s.trim())
        .filter((s: string) => s.length > 0)
    } else {
      form.value.testCaseIds = []
    }
  } else {
    form.value.name = ''
    form.value.description = ''
    form.value.environment = 'dev'
    form.value.testCaseIds = []
    form.value.scheduleEnabled = false
    form.value.cronExpression = ''
  }
}

watch(
  () => props.plan,
  () => {
    resetFormFromPlan()
  },
  { immediate: true }
)

onMounted(() => {
  fetchTestCases()
})

const submitLabel = computed(() => (props.mode === 'edit' ? '保存' : '创建计划'))

const handleSubmit = async () => {
  if (!form.value.name) {
    showToast('请输入计划名称', 'warning')
    return
  }

  const payload: any = {
    name: form.value.name,
    description: form.value.description,
    environment: form.value.environment,
    testCaseIds: form.value.testCaseIds.join(','),
    cronExpression: form.value.scheduleEnabled ? form.value.cronExpression : null,
    status: props.plan?.status || 'active'
  }

  try {
    if (props.mode === 'edit' && props.plan && props.plan.id) {
      await request.put('/plans', {
        ...payload,
        id: props.plan.id
      })
    } else {
      await request.post('/plans', payload)
    }
    emit('success')
    showToast(props.mode === 'edit' ? '保存成功' : '创建成功', 'success')
  } catch (e) {
    showToast(props.mode === 'edit' ? '保存失败' : '创建失败', 'error')
  }
}
</script>

<template>
  <div class="space-y-4">
    <div class="space-y-2">
      <Label>计划名称</Label>
      <Input v-model="form.name" placeholder="输入测试计划名称" />
    </div>

    <div class="space-y-2">
      <Label>计划描述</Label>
      <Textarea v-model="form.description" placeholder="描述测试计划的目的和范围" :rows="3" />
    </div>

    <div class="space-y-2">
      <Label>运行环境</Label>
      <Select v-model="form.environment">
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
          <label v-for="tc in testCases" :key="tc.id" class="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
            <input type="checkbox" :value="tc.id" v-model="form.testCaseIds" class="rounded" />
            <span class="text-sm">{{ tc.name }} ({{ tc.type }})</span>
          </label>
          <div v-if="testCases.length === 0" class="text-sm text-gray-400 text-center">
            暂无测试用例，请先创建用例
          </div>
        </div>
      </Card>
    </div>

    <div class="space-y-4 p-4 bg-gray-50 rounded-lg">
      <div class="flex items-center justify-between">
        <div>
          <Label>启用定时任务</Label>
          <p class="text-xs text-gray-500 mt-1">自动按计划执行测试</p>
        </div>
        <Switch v-model="form.scheduleEnabled" />
      </div>

      <div v-if="form.scheduleEnabled" class="space-y-4">
        <div class="space-y-2">
          <Label>Cron 表达式</Label>
          <Input v-model="form.cronExpression" placeholder="0 2 * * * (每天凌晨2点)" />
          <p class="text-xs text-gray-500">
            格式: 分 时 日 月 周，例如 "0 2 * * *" 表示每天凌晨2点
          </p>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <Button type="button" variant="outline" size="sm" @click="form.cronExpression = '0 * * * *'">每小时</Button>
          <Button type="button" variant="outline" size="sm" @click="form.cronExpression = '0 0 * * *'">每天</Button>
          <Button type="button" variant="outline" size="sm" @click="form.cronExpression = '0 0 * * 0'">每周</Button>
          <Button type="button" variant="outline" size="sm" @click="form.cronExpression = '0 0 1 * *'">每月</Button>
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
      <Button @click="handleSubmit">
        {{ submitLabel }}
      </Button>
    </div>
  </div>
</template>
