import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./ui/tabs";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { Badge } from "./ui/badge";
import { Textarea } from "./ui/textarea";
import { Plus, Search, Pencil, Trash2, Play, Copy, Code } from 'lucide-react';

interface TestCase {
  id: string;
  name: string;
  type: 'API' | 'WEB' | 'APP';
  description: string;
  environment: string;
  status: 'active' | 'inactive';
  lastRun?: string;
  result?: 'success' | 'failed' | 'pending';
}

const mockTestCases: TestCase[] = [
  {
    id: '1',
    name: '用户登录接口测试',
    type: 'API',
    description: '测试用户登录接口的正常流程和异常情况',
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-02 10:30',
    result: 'success'
  },
  {
    id: '2',
    name: 'Web首页功能测试',
    type: 'WEB',
    description: '测试Web首页的关键功能和交互',
    environment: 'staging',
    status: 'active',
    lastRun: '2026-01-02 09:15',
    result: 'success'
  },
  {
    id: '3',
    name: 'APP支付流程测试',
    type: 'APP',
    description: '测试APP端的支付流程',
    environment: 'production',
    status: 'active',
    lastRun: '2026-01-01 18:20',
    result: 'failed'
  }
];

export function TestCaseManager() {
  const [testCases, setTestCases] = useState<TestCase[]>(mockTestCases);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<string>('all');
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [selectedTestCase, setSelectedTestCase] = useState<TestCase | null>(null);

  const filteredTestCases = testCases.filter(tc => {
    const matchesSearch = tc.name.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = filterType === 'all' || tc.type === filterType;
    return matchesSearch && matchesType;
  });

  const getResultBadge = (result?: string) => {
    switch (result) {
      case 'success':
        return <Badge className="bg-green-500">成功</Badge>;
      case 'failed':
        return <Badge className="bg-red-500">失败</Badge>;
      case 'pending':
        return <Badge className="bg-yellow-500">待执行</Badge>;
      default:
        return <Badge variant="outline">未运行</Badge>;
    }
  };

  const getTypeBadge = (type: string) => {
    const colors = {
      'API': 'bg-blue-100 text-blue-800',
      'WEB': 'bg-purple-100 text-purple-800',
      'APP': 'bg-green-100 text-green-800'
    };
    return <Badge className={colors[type as keyof typeof colors]}>{type}</Badge>;
  };

  return (
    <div className="space-y-6">
      {/* Search and Filter Bar */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <Input
                placeholder="搜索测试用例..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select value={filterType} onValueChange={setFilterType}>
              <SelectTrigger className="w-full md:w-[180px]">
                <SelectValue placeholder="用例类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部类型</SelectItem>
                <SelectItem value="API">API测试</SelectItem>
                <SelectItem value="WEB">WEB测试</SelectItem>
                <SelectItem value="APP">APP测试</SelectItem>
              </SelectContent>
            </Select>
            <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
              <DialogTrigger asChild>
                <Button className="w-full md:w-auto">
                  <Plus className="w-4 h-4 mr-2" />
                  新建用例
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-2xl">
                <DialogHeader>
                  <DialogTitle>创建测试用例</DialogTitle>
                  <DialogDescription>
                    填写测试用例的基本信息和测试步骤
                  </DialogDescription>
                </DialogHeader>
                <CreateTestCaseForm onClose={() => setIsCreateDialogOpen(false)} />
              </DialogContent>
            </Dialog>
          </div>
        </CardContent>
      </Card>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">总用例数</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold">{testCases.length}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">API用例</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-blue-600">
              {testCases.filter(tc => tc.type === 'API').length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">WEB用例</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-purple-600">
              {testCases.filter(tc => tc.type === 'WEB').length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">APP用例</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-green-600">
              {testCases.filter(tc => tc.type === 'APP').length}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Test Cases List */}
      <Card>
        <CardHeader>
          <CardTitle>测试用例列表</CardTitle>
          <CardDescription>管理和执行所有测试用例</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {filteredTestCases.map((testCase) => (
              <Card key={testCase.id} className="hover:shadow-md transition-shadow">
                <CardContent className="pt-6">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="font-semibold">{testCase.name}</h3>
                        {getTypeBadge(testCase.type)}
                        {testCase.result && getResultBadge(testCase.result)}
                      </div>
                      <p className="text-sm text-gray-600 mb-2">{testCase.description}</p>
                      <div className="flex items-center gap-4 text-xs text-gray-500">
                        <span>环境: {testCase.environment}</span>
                        {testCase.lastRun && <span>最后运行: {testCase.lastRun}</span>}
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" onClick={() => setSelectedTestCase(testCase)}>
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

function CreateTestCaseForm({ onClose }: { onClose: () => void }) {
  const [testType, setTestType] = useState<string>('API');

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>用例名称</Label>
          <Input placeholder="输入用例名称" />
        </div>
        <div className="space-y-2">
          <Label>用例类型</Label>
          <Select value={testType} onValueChange={setTestType}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="API">API测试</SelectItem>
              <SelectItem value="WEB">WEB测试</SelectItem>
              <SelectItem value="APP">APP测试</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="space-y-2">
        <Label>用例描述</Label>
        <Textarea placeholder="描述测试用例的目的和范围" rows={3} />
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

      <Tabs defaultValue="steps" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="steps">测试步骤</TabsTrigger>
          <TabsTrigger value="assertions">断言规则</TabsTrigger>
          <TabsTrigger value="params">参数配置</TabsTrigger>
        </TabsList>
        <TabsContent value="steps" className="space-y-4">
          {testType === 'API' && (
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>请求方法</Label>
                <Select>
                  <SelectTrigger>
                    <SelectValue placeholder="选择请求方法" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="GET">GET</SelectItem>
                    <SelectItem value="POST">POST</SelectItem>
                    <SelectItem value="PUT">PUT</SelectItem>
                    <SelectItem value="DELETE">DELETE</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>接口地址</Label>
                <Input placeholder="/api/users/login" />
              </div>
              <div className="space-y-2">
                <Label>请求体</Label>
                <Textarea placeholder='{"username": "test", "password": "123456"}' rows={4} className="font-mono text-sm" />
              </div>
            </div>
          )}
          {testType === 'WEB' && (
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>关键字驱动步骤</Label>
                <Textarea 
                  placeholder="输入URL: https://example.com&#10;点击元素: #login-btn&#10;输入文本: #username, testuser&#10;输入文本: #password, 123456&#10;点击元素: #submit" 
                  rows={6}
                />
              </div>
            </div>
          )}
          {testType === 'APP' && (
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>应用包名</Label>
                <Input placeholder="com.example.app" />
              </div>
              <div className="space-y-2">
                <Label>关键字驱动步骤</Label>
                <Textarea 
                  placeholder="启动应用: com.example.app&#10;点击元素: resource-id=login_btn&#10;输入文本: resource-id=username, testuser&#10;输入文本: resource-id=password, 123456&#10;点击元素: resource-id=submit" 
                  rows={6}
                />
              </div>
            </div>
          )}
        </TabsContent>
        <TabsContent value="assertions" className="space-y-4">
          <div className="space-y-2">
            <Label>断言规则</Label>
            <Textarea 
              placeholder="状态码等于: 200&#10;响应时间小于: 1000ms&#10;JSON路径存在: $.data.token&#10;JSON路径等于: $.data.status, success" 
              rows={6}
            />
          </div>
        </TabsContent>
        <TabsContent value="params" className="space-y-4">
          <div className="space-y-2">
            <Label>公共参数</Label>
            <Textarea 
              placeholder="变量名: 变量值&#10;base_url: https://api.example.com&#10;api_key: ${API_KEY}&#10;timestamp: ${random.timestamp()}" 
              rows={6}
            />
          </div>
        </TabsContent>
      </Tabs>

      <div className="flex justify-end gap-2 pt-4">
        <Button variant="outline" onClick={onClose}>
          取消
        </Button>
        <Button onClick={onClose}>
          创建用例
        </Button>
      </div>
    </div>
  );
}