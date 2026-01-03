import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Badge } from "./ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { Server, Plus, RefreshCw, Trash2, Activity, Monitor, Smartphone, Globe } from 'lucide-react';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";

interface TestEngine {
  id: string;
  name: string;
  type: 'server' | 'local';
  status: 'online' | 'offline' | 'busy';
  capabilities: string[];
  ip: string;
  port: number;
  registeredAt: string;
  lastHeartbeat: string;
  runningTasks: number;
  totalExecutions: number;
}

const mockEngines: TestEngine[] = [
  {
    id: '1',
    name: '服务器引擎-01',
    type: 'server',
    status: 'online',
    capabilities: ['API', 'WEB', 'APP'],
    ip: '192.168.1.100',
    port: 8080,
    registeredAt: '2026-01-01 08:00',
    lastHeartbeat: '2026-01-02 10:35',
    runningTasks: 2,
    totalExecutions: 1523
  },
  {
    id: '2',
    name: '本地引擎-张三',
    type: 'local',
    status: 'online',
    capabilities: ['WEB', 'APP'],
    ip: '192.168.1.25',
    port: 8081,
    registeredAt: '2026-01-02 09:15',
    lastHeartbeat: '2026-01-02 10:34',
    runningTasks: 1,
    totalExecutions: 45
  },
  {
    id: '3',
    name: '服务器引擎-02',
    type: 'server',
    status: 'busy',
    capabilities: ['API', 'WEB'],
    ip: '192.168.1.101',
    port: 8080,
    registeredAt: '2026-01-01 08:00',
    lastHeartbeat: '2026-01-02 10:35',
    runningTasks: 5,
    totalExecutions: 2341
  },
  {
    id: '4',
    name: '本地引擎-李四',
    type: 'local',
    status: 'offline',
    capabilities: ['APP'],
    ip: '192.168.1.32',
    port: 8081,
    registeredAt: '2026-01-01 14:20',
    lastHeartbeat: '2026-01-02 09:10',
    runningTasks: 0,
    totalExecutions: 23
  }
];

export function TestEngineManager() {
  const [engines, setEngines] = useState<TestEngine[]>(mockEngines);
  const [isRegisterDialogOpen, setIsRegisterDialogOpen] = useState(false);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'online':
        return <Badge className="bg-green-500">在线</Badge>;
      case 'offline':
        return <Badge className="bg-gray-500">离线</Badge>;
      case 'busy':
        return <Badge className="bg-yellow-500">忙碌</Badge>;
      default:
        return <Badge variant="outline">未知</Badge>;
    }
  };

  const getTypeBadge = (type: string) => {
    return type === 'server' 
      ? <Badge className="bg-blue-100 text-blue-800">服务器</Badge>
      : <Badge className="bg-purple-100 text-purple-800">本地</Badge>;
  };

  const onlineEngines = engines.filter(e => e.status === 'online').length;
  const busyEngines = engines.filter(e => e.status === 'busy').length;
  const offlineEngines = engines.filter(e => e.status === 'offline').length;

  return (
    <div className="space-y-6">
      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">总引擎数</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold">{engines.length}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">在线引擎</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-green-600">{onlineEngines}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">忙碌引擎</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-yellow-600">{busyEngines}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-600">离线引擎</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-semibold text-gray-600">{offlineEngines}</div>
          </CardContent>
        </Card>
      </div>

      {/* Actions */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row gap-4">
            <Dialog open={isRegisterDialogOpen} onOpenChange={setIsRegisterDialogOpen}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="w-4 h-4 mr-2" />
                  注册新引擎
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>注册测试引擎</DialogTitle>
                  <DialogDescription>
                    添加新的测试引擎到平台，支持服务器部署和本地部署
                  </DialogDescription>
                </DialogHeader>
                <RegisterEngineForm onClose={() => setIsRegisterDialogOpen(false)} />
              </DialogContent>
            </Dialog>
            <Button variant="outline">
              <RefreshCw className="w-4 h-4 mr-2" />
              刷新状态
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Engine Registration Guide */}
      <Card className="border-blue-200 bg-blue-50">
        <CardHeader>
          <CardTitle className="text-blue-900">快速部署引擎</CardTitle>
          <CardDescription className="text-blue-700">
            在您的服务器或本地电脑上快速部署测试引擎
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="bg-white rounded-lg p-4 space-y-2">
            <h4 className="font-semibold text-sm">1. 下载测试引擎</h4>
            <div className="flex gap-2">
              <Button variant="outline" size="sm">
                <Monitor className="w-4 h-4 mr-2" />
                Windows
              </Button>
              <Button variant="outline" size="sm">
                <Monitor className="w-4 h-4 mr-2" />
                Linux
              </Button>
              <Button variant="outline" size="sm">
                <Monitor className="w-4 h-4 mr-2" />
                MacOS
              </Button>
            </div>
          </div>
          <div className="bg-white rounded-lg p-4 space-y-2">
            <h4 className="font-semibold text-sm">2. 启动引擎并注册</h4>
            <pre className="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">
              ./test-engine --server https://platform.example.com --port 8081 --name "我的引擎"
            </pre>
          </div>
          <div className="bg-white rounded-lg p-4 space-y-2">
            <h4 className="font-semibold text-sm">3. 配置能力</h4>
            <p className="text-sm text-gray-600">
              引擎会自动检测环境并上报支持的测试能力（API/WEB/APP）
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Engines List */}
      <Card>
        <CardHeader>
          <CardTitle>测试引擎列表</CardTitle>
          <CardDescription>管理所有注册的测试引擎</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {engines.map((engine) => (
              <Card key={engine.id} className="hover:shadow-md transition-shadow">
                <CardContent className="pt-6">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <Server className="w-5 h-5 text-gray-600" />
                        <h3 className="font-semibold">{engine.name}</h3>
                        {getTypeBadge(engine.type)}
                        {getStatusBadge(engine.status)}
                      </div>
                      <div className="space-y-1 text-sm text-gray-600">
                        <div className="flex items-center gap-4">
                          <span>地址: {engine.ip}:{engine.port}</span>
                          <span>正在运行: {engine.runningTasks} 个任务</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span>能力:</span>
                          {engine.capabilities.map(cap => {
                            const icons = {
                              'API': <Globe className="w-3 h-3" />,
                              'WEB': <Monitor className="w-3 h-3" />,
                              'APP': <Smartphone className="w-3 h-3" />
                            };
                            return (
                              <Badge key={cap} variant="outline" className="flex items-center gap-1">
                                {icons[cap as keyof typeof icons]}
                                {cap}
                              </Badge>
                            );
                          })}
                        </div>
                        <div className="flex items-center gap-4 text-xs text-gray-500">
                          <span>注册时间: {engine.registeredAt}</span>
                          <span>最后心跳: {engine.lastHeartbeat}</span>
                          <span>累计执行: {engine.totalExecutions} 次</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm">
                        <Activity className="w-4 h-4" />
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

function RegisterEngineForm({ onClose }: { onClose: () => void }) {
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label>引擎名称</Label>
        <Input placeholder="例如: 本地引擎-张三" />
      </div>

      <div className="space-y-2">
        <Label>引擎类型</Label>
        <Select defaultValue="local">
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="server">服务器引擎</SelectItem>
            <SelectItem value="local">本地引擎</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>IP地址</Label>
          <Input placeholder="192.168.1.100" />
        </div>
        <div className="space-y-2">
          <Label>端口</Label>
          <Input placeholder="8081" type="number" />
        </div>
      </div>

      <div className="space-y-2">
        <Label>测试能力</Label>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" className="flex-1">
            <Globe className="w-4 h-4 mr-2" />
            API
          </Button>
          <Button variant="outline" size="sm" className="flex-1">
            <Monitor className="w-4 h-4 mr-2" />
            WEB
          </Button>
          <Button variant="outline" size="sm" className="flex-1">
            <Smartphone className="w-4 h-4 mr-2" />
            APP
          </Button>
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-4">
        <Button variant="outline" onClick={onClose}>
          取消
        </Button>
        <Button onClick={onClose}>
          注册引擎
        </Button>
      </div>
    </div>
  );
}
