import React, { useState } from 'react';
import { Card, CardContent } from "./ui/card";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Textarea } from "./ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./ui/tabs";
import { Play, Save, Clock } from 'lucide-react';

export function ApiCaseEditor() {
  const [method, setMethod] = useState('GET');
  const [url, setUrl] = useState('https://api.example.com/users/login');
  const [responseData, setResponseData] = useState<any>(null);
  const [statusCode, setStatusCode] = useState<number | null>(null);
  const [responseTime, setResponseTime] = useState<number | null>(null);

  const handleSend = () => {
    // Simulate API call
    setStatusCode(200);
    setResponseTime(342);
    setResponseData({
      success: true,
      data: {
        userId: "12345",
        username: "testuser",
        token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        expiresIn: 3600
      },
      message: "登录成功"
    });
  };

  return (
    <div className="p-6 space-y-4">
      {/* URL Input Section */}
      <Card className="border-gray-200">
        <CardContent className="pt-6">
          <div className="flex gap-2">
            <Select value={method} onValueChange={setMethod}>
              <SelectTrigger className="w-32 border-gray-300">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="GET">GET</SelectItem>
                <SelectItem value="POST">POST</SelectItem>
                <SelectItem value="PUT">PUT</SelectItem>
                <SelectItem value="DELETE">DELETE</SelectItem>
                <SelectItem value="PATCH">PATCH</SelectItem>
              </SelectContent>
            </Select>
            <Input
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="输入请求URL"
              className="flex-1 border-gray-300"
            />
            <Button
              onClick={handleSend}
              className="bg-[#409EFF] hover:bg-[#3a8ee6] px-8"
            >
              <Play className="w-4 h-4 mr-2" />
              发送
            </Button>
            <Button variant="outline" className="border-gray-300">
              <Save className="w-4 h-4 mr-2" />
              保存
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Request Configuration */}
      <Card className="border-gray-200">
        <CardContent className="pt-6">
          <Tabs defaultValue="params" className="w-full">
            <TabsList className="grid w-full grid-cols-4 bg-gray-100">
              <TabsTrigger value="params">Params</TabsTrigger>
              <TabsTrigger value="headers">Headers</TabsTrigger>
              <TabsTrigger value="body">Body</TabsTrigger>
              <TabsTrigger value="assertions">Assertions</TabsTrigger>
            </TabsList>

            <TabsContent value="params" className="space-y-4">
              <div className="space-y-3">
                <div className="grid grid-cols-12 gap-2 text-sm font-semibold text-gray-700 px-2">
                  <div className="col-span-1"></div>
                  <div className="col-span-5">KEY</div>
                  <div className="col-span-5">VALUE</div>
                  <div className="col-span-1"></div>
                </div>
                {[1, 2, 3].map((i) => (
                  <div key={i} className="grid grid-cols-12 gap-2 items-center">
                    <div className="col-span-1 flex justify-center">
                      <input type="checkbox" defaultChecked className="rounded" />
                    </div>
                    <div className="col-span-5">
                      <Input placeholder="参数名" className="border-gray-300" />
                    </div>
                    <div className="col-span-5">
                      <Input placeholder="参数值" className="border-gray-300" />
                    </div>
                    <div className="col-span-1">
                      <Button variant="ghost" size="sm" className="text-gray-400 hover:text-red-600">
                        ×
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </TabsContent>

            <TabsContent value="headers" className="space-y-4">
              <div className="space-y-3">
                <div className="grid grid-cols-12 gap-2 text-sm font-semibold text-gray-700 px-2">
                  <div className="col-span-1"></div>
                  <div className="col-span-5">KEY</div>
                  <div className="col-span-5">VALUE</div>
                  <div className="col-span-1"></div>
                </div>
                <div className="grid grid-cols-12 gap-2 items-center">
                  <div className="col-span-1 flex justify-center">
                    <input type="checkbox" defaultChecked className="rounded" />
                  </div>
                  <div className="col-span-5">
                    <Input value="Content-Type" className="border-gray-300" />
                  </div>
                  <div className="col-span-5">
                    <Input value="application/json" className="border-gray-300" />
                  </div>
                  <div className="col-span-1">
                    <Button variant="ghost" size="sm" className="text-gray-400 hover:text-red-600">
                      ×
                    </Button>
                  </div>
                </div>
                <div className="grid grid-cols-12 gap-2 items-center">
                  <div className="col-span-1 flex justify-center">
                    <input type="checkbox" defaultChecked className="rounded" />
                  </div>
                  <div className="col-span-5">
                    <Input value="Authorization" className="border-gray-300" />
                  </div>
                  <div className="col-span-5">
                    <Input value="Bearer ${API_TOKEN}" className="border-gray-300" />
                  </div>
                  <div className="col-span-1">
                    <Button variant="ghost" size="sm" className="text-gray-400 hover:text-red-600">
                      ×
                    </Button>
                  </div>
                </div>
              </div>
            </TabsContent>

            <TabsContent value="body" className="space-y-4">
              <div>
                <Select defaultValue="json">
                  <SelectTrigger className="w-48 border-gray-300 mb-3">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="json">JSON</SelectItem>
                    <SelectItem value="form">Form Data</SelectItem>
                    <SelectItem value="raw">Raw Text</SelectItem>
                  </SelectContent>
                </Select>
                <Textarea
                  className="font-mono text-sm min-h-[300px] bg-gray-900 text-green-400 border-gray-700"
                  defaultValue={JSON.stringify({
                    username: "testuser",
                    password: "Test@123456"
                  }, null, 2)}
                />
              </div>
            </TabsContent>

            <TabsContent value="assertions" className="space-y-4">
              <div className="space-y-3">
                <div className="grid grid-cols-12 gap-2 text-sm font-semibold text-gray-700 px-2">
                  <div className="col-span-1"></div>
                  <div className="col-span-3">类型</div>
                  <div className="col-span-4">路径/字段</div>
                  <div className="col-span-3">期望值</div>
                  <div className="col-span-1"></div>
                </div>
                <div className="grid grid-cols-12 gap-2 items-center">
                  <div className="col-span-1 flex justify-center">
                    <input type="checkbox" defaultChecked className="rounded" />
                  </div>
                  <div className="col-span-3">
                    <Select defaultValue="status">
                      <SelectTrigger className="border-gray-300">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="status">状态码</SelectItem>
                        <SelectItem value="json">JSON路径</SelectItem>
                        <SelectItem value="time">响应时间</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="col-span-4">
                    <Input value="-" disabled className="border-gray-300 bg-gray-50" />
                  </div>
                  <div className="col-span-3">
                    <Input value="200" className="border-gray-300" />
                  </div>
                  <div className="col-span-1">
                    <Button variant="ghost" size="sm" className="text-gray-400 hover:text-red-600">
                      ×
                    </Button>
                  </div>
                </div>
                <div className="grid grid-cols-12 gap-2 items-center">
                  <div className="col-span-1 flex justify-center">
                    <input type="checkbox" defaultChecked className="rounded" />
                  </div>
                  <div className="col-span-3">
                    <Select defaultValue="json">
                      <SelectTrigger className="border-gray-300">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="status">状态码</SelectItem>
                        <SelectItem value="json">JSON路径</SelectItem>
                        <SelectItem value="time">响应时间</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="col-span-4">
                    <Input value="$.data.success" className="border-gray-300" />
                  </div>
                  <div className="col-span-3">
                    <Input value="true" className="border-gray-300" />
                  </div>
                  <div className="col-span-1">
                    <Button variant="ghost" size="sm" className="text-gray-400 hover:text-red-600">
                      ×
                    </Button>
                  </div>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>

      {/* Response Panel */}
      <Card className="border-gray-200">
        <CardContent className="pt-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-gray-900">响应结果</h3>
              {statusCode && (
                <div className="flex items-center gap-4">
                  <span className={`inline-flex items-center gap-1 px-3 py-1 rounded ${
                    statusCode === 200 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                  } text-sm font-medium`}>
                    Status: {statusCode} {statusCode === 200 ? 'OK' : 'Error'}
                  </span>
                  <span className="flex items-center gap-1 text-sm text-gray-600">
                    <Clock className="w-4 h-4" />
                    {responseTime}ms
                  </span>
                </div>
              )}
            </div>

            {responseData ? (
              <div className="bg-gray-900 text-green-400 p-4 rounded font-mono text-sm overflow-x-auto">
                <pre>{JSON.stringify(responseData, null, 2)}</pre>
              </div>
            ) : (
              <div className="bg-gray-50 border-2 border-dashed border-gray-300 rounded p-8 text-center text-gray-500">
                点击"发送"按钮执行请求查看响应结果
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
