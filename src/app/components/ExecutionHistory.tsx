import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Badge } from "./ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { Search, FileText, CircleCheck, CircleX, Clock, CirclePlay } from 'lucide-react';
import { Progress } from "./ui/progress";

interface ExecutionRecord {
  id: string;
  planName: string;
  type: 'plan' | 'single';
  status: 'success' | 'failed' | 'running' | 'pending';
  startTime: string;
  endTime?: string;
  duration?: string;
  totalCases: number;
  passedCases: number;
  failedCases: number;
  engine: string;
  environment: string;
  executor: string;
}

const mockExecutions: ExecutionRecord[] = [
  {
    id: '1',
    planName: '每日回归测试',
    type: 'plan',
    status: 'success',
    startTime: '2026-01-02 02:00:00',
    endTime: '2026-01-02 02:15:23',
    duration: '15分23秒',
    totalCases: 15,
    passedCases: 15,
    failedCases: 0,
    engine: '服务器引擎-01',
    environment: 'production',
    executor: '定时任务'
  },
  {
    id: '2',
    planName: 'API接口测试套件',
    type: 'plan',
    status: 'success',
    startTime: '2026-01-02 12:00:00',
    endTime: '2026-01-02 12:08:45',
    duration: '8分45秒',
    totalCases: 12,
    passedCases: 12,
    failedCases: 0,
    engine: '服务器引擎-02',
    environment: 'staging',
    executor: '定时任务'
  },
  {
    id: '3',
    planName: 'Web首页功能测试',
    type: 'single',
    status: 'running',
    startTime: '2026-01-02 10:35:12',
    totalCases: 8,
    passedCases: 5,
    failedCases: 0,
    engine: '本地引擎-张三',
    environment: 'staging',
    executor: '张三'
  },
  {
    id: '4',
    planName: '移动端功能测试',
    type: 'plan',
    status: 'failed',
    startTime: '2026-01-01 18:00:00',
    endTime: '2026-01-01 18:12:34',
    duration: '12分34秒',
    totalCases: 10,
    passedCases: 8,
    failedCases: 2,
    engine: '服务器引擎-01',
    environment: 'production',
    executor: '定时任务'
  },
  {
    id: '5',
    planName: '用户登录接口测试',
    type: 'single',
    status: 'success',
    startTime: '2026-01-02 09:20:15',
    endTime: '2026-01-02 09:20:18',
    duration: '3秒',
    totalCases: 1,
    passedCases: 1,
    failedCases: 0,
    engine: '本地引擎-李四',
    environment: 'dev',
    executor: '李四'
  }
];

export function ExecutionHistory() {
  const [executions, setExecutions] = useState<ExecutionRecord[]>(mockExecutions);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [selectedExecution, setSelectedExecution] = useState<ExecutionRecord | null>(null);

  const filteredExecutions = executions.filter(exec => {
    const matchesSearch = exec.planName.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = filterStatus === 'all' || exec.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'success':
        return (
          <Badge className="bg-green-500 flex items-center gap-1">
            <CircleCheck className="w-3 h-3" />
            成功
          </Badge>
        );
      case 'failed':
        return (
          <Badge className="bg-red-500 flex items-center gap-1">
            <CircleX className="w-3 h-3" />
            失败
          </Badge>
        );
      case 'running':
        return (
          <Badge className="bg-blue-500 flex items-center gap-1 animate-pulse">
            <CirclePlay className="w-3 h-3" />
            运行中
          </Badge>
        );
      case 'pending':
        return (
          <Badge className="bg-gray-500 flex items-center gap-1">
            <Clock className="w-3 h-3" />
            等待中
          </Badge>
        );
      default:
        return <Badge variant="outline">未知</Badge>;
    }
  };

  const getSuccessRate = (passed: number, total: number) => {
    return Math.round((passed / total) * 100);
  };

  const totalExecutions = executions.length;
  const successExecutions = executions.filter(e => e.status === 'success').length;
  const failedExecutions = executions.filter(e => e.status === 'failed').length;
  const runningExecutions = executions.filter(e => e.status === 'running').length;

  return (
    <div className="space-y-6">
      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">总执行次数</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold">{totalExecutions}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">成功次数</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-green-600">{successExecutions}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">失败次数</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-red-600">{failedExecutions}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">运行中</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-blue-600">{runningExecutions}</div>
          </CardContent>
        </Card>
      </div>

      {/* Search and Filter */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <Input
                placeholder="搜索执行记录..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger className="w-full md:w-[180px]">
                <SelectValue placeholder="执行状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部状态</SelectItem>
                <SelectItem value="success">成功</SelectItem>
                <SelectItem value="failed">失败</SelectItem>
                <SelectItem value="running">运行中</SelectItem>
                <SelectItem value="pending">等待中</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Execution Records */}
      <Card>
        <CardHeader>
          <CardTitle>执行历史</CardTitle>
          <CardDescription>查看所有测试执行记录和详细报告</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {filteredExecutions.map((execution) => (
              <Card key={execution.id} className="hover:shadow-md transition-shadow">
                <CardContent className="pt-6">
                  <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
                    <div className="flex-1 space-y-3">
                      <div>
                        <div className="flex items-center gap-2 mb-2">
                          <h3 className="font-semibold">{execution.planName}</h3>
                          {getStatusBadge(execution.status)}
                          <Badge variant="outline">
                            {execution.type === 'plan' ? '测试计划' : '单用例'}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-4 text-sm text-gray-600">
                          <span>执行人: {execution.executor}</span>
                          <span>引擎: {execution.engine}</span>
                          <span>环境: {execution.environment}</span>
                        </div>
                      </div>

                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-sm">
                          <span className="text-gray-600">
                            测试进度: {execution.passedCases + execution.failedCases} / {execution.totalCases}
                          </span>
                          <span className="font-semibold">
                            通过率: {getSuccessRate(execution.passedCases, execution.totalCases)}%
                          </span>
                        </div>
                        <Progress 
                          value={getSuccessRate(execution.passedCases + execution.failedCases, execution.totalCases)} 
                          className="h-2"
                        />
                        <div className="flex gap-4 text-sm">
                          <span className="text-green-600">✓ {execution.passedCases} 通过</span>
                          <span className="text-red-600">✗ {execution.failedCases} 失败</span>
                          {execution.status === 'running' && (
                            <span className="text-blue-600">
                              ⏳ {execution.totalCases - execution.passedCases - execution.failedCases} 待执行
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-4 text-xs text-gray-500">
                        <span>开始时间: {execution.startTime}</span>
                        {execution.endTime && <span>结束时间: {execution.endTime}</span>}
                        {execution.duration && <span>耗时: {execution.duration}</span>}
                      </div>
                    </div>

                    <div className="flex gap-2">
                      <Dialog>
                        <DialogTrigger asChild>
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => setSelectedExecution(execution)}
                          >
                            <FileText className="w-4 h-4 mr-2" />
                            查看报告
                          </Button>
                        </DialogTrigger>
                        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
                          <DialogHeader>
                            <DialogTitle>测试报告 - {execution.planName}</DialogTitle>
                            <DialogDescription>
                              执行ID: {execution.id}
                            </DialogDescription>
                          </DialogHeader>
                          <ExecutionReportDetail execution={execution} />
                        </DialogContent>
                      </Dialog>
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

function ExecutionReportDetail({ execution }: { execution: ExecutionRecord }) {
  const mockCaseResults = [
    {
      name: '用户登录接口测试',
      status: 'success',
      duration: '2.3秒',
      assertions: 5,
      passedAssertions: 5,
      error: null
    },
    {
      name: 'Web首页功能测试',
      status: 'success',
      duration: '5.8秒',
      assertions: 8,
      passedAssertions: 8,
      error: null
    },
    {
      name: 'APP支付流程测试',
      status: execution.status === 'failed' ? 'failed' : 'success',
      duration: execution.status === 'failed' ? '3.2秒' : '4.5秒',
      assertions: 10,
      passedAssertions: execution.status === 'failed' ? 8 : 10,
      error: execution.status === 'failed' ? '断言失败: 支付金额不匹配，期望 100.00，实际 99.99' : null
    }
  ];

  return (
    <div className="space-y-6">
      {/* Summary */}
      <Card>
        <CardHeader>
          <CardTitle>执行概要</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div>
              <div className="text-sm text-gray-600">执行状态</div>
              <div className="mt-1">{execution.status === 'success' ? '✓ 成功' : '✗ 失败'}</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">总用例数</div>
              <div className="mt-1">{execution.totalCases}</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">通过率</div>
              <div className="mt-1 text-green-600 font-semibold">
                {Math.round((execution.passedCases / execution.totalCases) * 100)}%
              </div>
            </div>
            <div>
              <div className="text-sm text-gray-600">执行引擎</div>
              <div className="mt-1">{execution.engine}</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">运行环境</div>
              <div className="mt-1">{execution.environment}</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">执行耗时</div>
              <div className="mt-1">{execution.duration || '-'}</div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Case Results */}
      <Card>
        <CardHeader>
          <CardTitle>用例执行详情</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {mockCaseResults.map((caseResult, index) => (
              <Card key={index} className={caseResult.status === 'failed' ? 'border-red-200 bg-red-50' : ''}>
                <CardContent className="pt-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        {caseResult.status === 'success' ? (
                          <CircleCheck className="w-4 h-4 text-green-600" />
                        ) : (
                          <CircleX className="w-4 h-4 text-red-600" />
                        )}
                        <span className="font-semibold">{caseResult.name}</span>
                      </div>
                      <div className="flex items-center gap-4 text-sm text-gray-600">
                        <span>耗时: {caseResult.duration}</span>
                        <span>
                          断言: {caseResult.passedAssertions}/{caseResult.assertions} 通过
                        </span>
                      </div>
                      {caseResult.error && (
                        <div className="mt-2 p-2 bg-red-100 rounded text-sm text-red-800">
                          {caseResult.error}
                        </div>
                      )}
                    </div>
                    <Badge className={caseResult.status === 'success' ? 'bg-green-500' : 'bg-red-500'}>
                      {caseResult.status === 'success' ? '成功' : '失败'}
                    </Badge>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Logs */}
      <Card>
        <CardHeader>
          <CardTitle>执行日志</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="bg-gray-900 text-green-400 p-4 rounded text-xs font-mono max-h-64 overflow-y-auto">
            <div>[{execution.startTime}] 开始执行测试计划: {execution.planName}</div>
            <div>[{execution.startTime}] 分配测试引擎: {execution.engine}</div>
            <div>[{execution.startTime}] 加载测试用例: {execution.totalCases} 个</div>
            <div className="text-blue-400">[{execution.startTime}] 执行用例: 用户登录接口测试</div>
            <div className="text-green-400">[{execution.startTime}] ✓ 用户登录接口测试 - 通过</div>
            <div className="text-blue-400">[{execution.startTime}] 执行用例: Web首页功能测试</div>
            <div className="text-green-400">[{execution.startTime}] ✓ Web首页功能测试 - 通过</div>
            <div className="text-blue-400">[{execution.startTime}] 执行用例: APP支付流程测试</div>
            {execution.status === 'failed' ? (
              <>
                <div className="text-red-400">[{execution.endTime}] ✗ APP支付流程测试 - 失败</div>
                <div className="text-red-400">[{execution.endTime}] 错误: 断言失败: 支付金额不匹配</div>
              </>
            ) : (
              <div className="text-green-400">[{execution.endTime}] ✓ APP支付流程测试 - 通过</div>
            )}
            <div>[{execution.endTime}] 测试执行完成</div>
            <div>[{execution.endTime}] 生成测试报告</div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}