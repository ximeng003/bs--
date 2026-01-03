import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Badge } from "./ui/badge";
import { Textarea } from "./ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { Plus, Play, Calendar, Clock, Pencil, Trash2, Copy } from 'lucide-react';
import { Switch } from "./ui/switch";

interface TestPlan {
  id: string;
  name: string;
  description: string;
  testCases: { id: string; name: string; type: string }[];
  schedule?: {
    enabled: boolean;
    cron: string;
    nextRun?: string;
  };
  environment: string;
  status: 'active' | 'inactive';
  lastRun?: string;
  successRate?: number;
}

const mockTestPlans: TestPlan[] = [
  {
    id: '1',
    name: '每日回归测试',
    description: '包含核心功能的全量回归测试',
    testCases: [
      { id: '1', name: '用户登录接口测试', type: 'API' },
      { id: '2', name: 'Web首页功能测试', type: 'WEB' },
      { id: '3', name: 'APP支付流程测试', type: 'APP' }
    ],
    schedule: {
      enabled: true,
      cron: '0 2 * * *',
      nextRun: '2026-01-03 02:00'
    },
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-02 02:00',
    successRate: 95
  },
  {
    id: '2',
    name: 'API接口测试套件',
    description: '所有API接口的完整测试',
    testCases: [
      { id: '1', name: '用户登录接口测试', type: 'API' },
      { id: '4', name: '用户注册接口测试', type: 'API' },
      { id: '5', name: '数据查询接口测试', type: 'API' }
    ],
    schedule: {
      enabled: true,
      cron: '0 */4 * * *',
      nextRun: '2026-01-02 16:00'
    },
    environment: 'staging',
    status: 'active',
    lastRun: '2026-01-02 12:00',
    successRate: 100
  },
  {
    id: '3',
    name: '移动端功能测试',
    description: 'APP端的关键业务流程测试',
    testCases: [
      { id: '3', name: 'APP支付流程测试', type: 'APP' },
      { id: '6', name: 'APP登录测试', type: 'APP' }
    ],
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-01 18:00',
    successRate: 85
  }
];

export function TestPlanManager() {
  const [testPlans, setTestPlans] = useState<TestPlan[]>(mockTestPlans);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);

  const getStatusBadge = (status: string) => {
    return status === 'active' 
      ? <Badge className="bg-green-500">启用</Badge>
      : <Badge className="bg-gray-500">禁用</Badge>;
  };

  const getSuccessRateColor = (rate?: number) => {
    if (!rate) return 'text-gray-500';
    if (rate >= 90) return 'text-green-600';
    if (rate >= 70) return 'text-yellow-600';
    return 'text-red-600';
  };

  return (
    <div className="space-y-6">
      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">总测试计划</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold">{testPlans.length}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">启用计划</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-green-600">
              {testPlans.filter(p => p.status === 'active').length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">定时任务</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-blue-600">
              {testPlans.filter(p => p.schedule?.enabled).length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">平均成功率</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-green-600">
              {Math.round(testPlans.reduce((acc, p) => acc + (p.successRate || 0), 0) / testPlans.length)}%
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Actions */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="w-4 h-4 mr-2" />
                  创建测试计划
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                  <DialogTitle>创建测试计划</DialogTitle>
                  <DialogDescription>
                    组合多个测试用例，配置定时任务和执行策略
                  </DialogDescription>
                </DialogHeader>
                <CreateTestPlanForm onClose={() => setIsCreateDialogOpen(false)} />
              </DialogContent>
            </Dialog>
          </div>
        </CardContent>
      </Card>

      {/* CI/CD Integration */}
      <Card className="border-purple-200 bg-purple-50">
        <CardHeader>
          <CardTitle className="text-purple-900">CI/CD 集成</CardTitle>
          <CardDescription className="text-purple-700">
            通过 OpenAPI 或 Webhook 集成到您的 CI/CD 流程
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="bg-white rounded-lg p-4 space-y-2">
            <h4 className="font-semibold text-sm">OpenAPI 触发</h4>
            <pre className="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">
{`curl -X POST https://platform.example.com/api/v1/plans/1/execute \\
  -H "Authorization: Bearer YOUR_API_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{"environment": "production"}'`}
            </pre>
          </div>
          <div className="bg-white rounded-lg p-4 space-y-2">
            <h4 className="font-semibold text-sm">Jenkins 集成示例</h4>
            <pre className="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">
{`pipeline {
  stage('Run Tests') {
    steps {
      sh 'curl -X POST https://platform.example.com/api/v1/plans/1/execute'
    }
  }
}`}
            </pre>
          </div>
        </CardContent>
      </Card>

      {/* Test Plans List */}
      <Card>
        <CardHeader>
          <CardTitle>测试计划列表</CardTitle>
          <CardDescription>管理和执行所有测试计划</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {testPlans.map((plan) => (
              <Card key={plan.id} className="hover:shadow-md transition-shadow">
                <CardContent className="pt-6">
                  <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
                    <div className="flex-1 space-y-3">
                      <div>
                        <div className="flex items-center gap-2 mb-2">
                          <h3 className="font-semibold">{plan.name}</h3>
                          {getStatusBadge(plan.status)}
                          {plan.schedule?.enabled && (
                            <Badge variant="outline" className="flex items-center gap-1">
                              <Clock className="w-3 h-3" />
                              定时任务
                            </Badge>
                          )}
                        </div>
                        <p className="text-sm text-gray-600">{plan.description}</p>
                      </div>
                      
                      <div className="space-y-2">
                        <div className="text-sm">
                          <span className="text-gray-600">包含用例: </span>
                          <span className="font-semibold">{plan.testCases.length} 个</span>
                          <span className="text-gray-500 ml-2">
                            (API: {plan.testCases.filter(tc => tc.type === 'API').length}, 
                            WEB: {plan.testCases.filter(tc => tc.type === 'WEB').length}, 
                            APP: {plan.testCases.filter(tc => tc.type === 'APP').length})
                          </span>
                        </div>
                        
                        {plan.schedule?.enabled && (
                          <div className="flex items-center gap-4 text-sm text-gray-600">
                            <span>Cron: {plan.schedule.cron}</span>
                            {plan.schedule.nextRun && (
                              <span className="flex items-center gap-1">
                                <Calendar className="w-3 h-3" />
                                下次运行: {plan.schedule.nextRun}
                              </span>
                            )}
                          </div>
                        )}
                        
                        <div className="flex items-center gap-4 text-sm">
                          <span className="text-gray-600">环境: {plan.environment}</span>
                          {plan.lastRun && <span className="text-gray-600">最后运行: {plan.lastRun}</span>}
                          {plan.successRate !== undefined && (
                            <span className={`font-semibold ${getSuccessRateColor(plan.successRate)}`}>
                              成功率: {plan.successRate}%
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex gap-2">
                      <Button size="sm" className="bg-green-600 hover:bg-green-700">
                        <Play className="w-4 h-4" />
                      </Button>
                      <Button variant="outline" size="sm">
                        <Pencil className="w-4 h-4" />
                      </Button>
                      <Button variant="outline" size="sm">
                        <Copy className="w-4 h-4" />
                      </Button>
                      <Button variant="outline" size="sm" className="text-red-600 hover:text-red-700">
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function CreateTestPlanForm({ onClose }: { onClose: () => void }) {
  const [scheduleEnabled, setScheduleEnabled] = useState(false);

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label>计划名称</Label>
        <Input placeholder="输入测试计划名称" />
      </div>

      <div className="space-y-2">
        <Label>计划描述</Label>
        <Textarea placeholder="描述测试计划的目的和范围" rows={3} />
      </div>

      <div className="space-y-2">
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

      <div className="space-y-2">
        <Label>选择测试用例</Label>
        <Card className="p-4">
          <div className="space-y-2 max-h-48 overflow-y-auto">
            <label className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
              <input type="checkbox" className="rounded" />
              <span className="text-sm">用户登录接口测试 (API)</span>
            </label>
            <label className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
              <input type="checkbox" className="rounded" />
              <span className="text-sm">Web首页功能测试 (WEB)</span>
            </label>
            <label className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
              <input type="checkbox" className="rounded" />
              <span className="text-sm">APP支付流程测试 (APP)</span>
            </label>
            <label className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
              <input type="checkbox" className="rounded" />
              <span className="text-sm">用户注册接口测试 (API)</span>
            </label>
            <label className="flex items-center gap-2 p-2 hover:bg-gray-50 rounded cursor-pointer">
              <input type="checkbox" className="rounded" />
              <span className="text-sm">数据查询接口测试 (API)</span>
            </label>
          </div>
        </Card>
      </div>

      <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
        <div className="flex items-center justify-between">
          <div>
            <Label>启用定时任务</Label>
            <p className="text-xs text-gray-500 mt-1">自动按计划执行测试</p>
          </div>
          <Switch checked={scheduleEnabled} onCheckedChange={setScheduleEnabled} />
        </div>

        {scheduleEnabled && (
          <>
            <div className="space-y-2">
              <Label>Cron 表达式</Label>
              <Input placeholder="0 2 * * * (每天凌晨2点)" />
              <p className="text-xs text-gray-500">
                格式: 分 时 日 月 周，例如 "0 2 * * *" 表示每天凌晨2点
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Button variant="outline" size="sm">每小时</Button>
              <Button variant="outline" size="sm">每天</Button>
              <Button variant="outline" size="sm">每周</Button>
              <Button variant="outline" size="sm">每月</Button>
            </div>
          </>
        )}
      </div>

      <div className="space-y-2">
        <Label>失败重试</Label>
        <Select defaultValue="0">
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

      <div className="flex justify-end gap-2 pt-4">
        <Button variant="outline" onClick={onClose}>
          取消
        </Button>
        <Button onClick={onClose}>
          创建计划
        </Button>
      </div>
    </div>
  );
}