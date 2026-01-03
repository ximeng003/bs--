import React, { useState } from 'react';
import { Card, CardContent } from "./ui/card";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Textarea } from "./ui/textarea";
import { Play, Save, Copy, Code } from 'lucide-react';

export function ScriptEditor() {
  const [caseName, setCaseName] = useState('Web首页功能测试');
  const [deviceType, setDeviceType] = useState('web');
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionLogs, setExecutionLogs] = useState<string[]>([]);

  const defaultScript = `// Web自动化测试脚本 - 关键字驱动
// 支持自然语言编写测试步骤

// 打开浏览器并访问URL
打开URL: https://example.com

// 等待页面加载完成
等待元素: #login-form, 5000

// 输入用户名和密码
输入文本: #username, testuser
输入文本: #password, Test@123456

// 点击登录按钮
点击元素: #login-button

// 等待跳转
等待元素: .dashboard, 5000

// 验证登录成功
断言文本: .welcome-message, 欢迎回来
断言URL包含: /dashboard

// 截图保存
截图: login_success.png

// 关闭浏览器
关闭浏览器`;

  const appScript = `// APP自动化测试脚本 - 基于UIAutomator2/WDA
// 支持Android和iOS设备

// 启动应用
启动应用: com.example.app

// 等待首页加载
等待元素: resource-id=home_page, 5000

// 点击登录按钮
点击元素: resource-id=login_btn

// 输入账号密码
输入文本: resource-id=username, 13800138000
输入文本: resource-id=password, 123456

// 提交登录
点击元素: resource-id=submit_btn

// 验证登录成功
等待元素: text=我的, 5000
断言元素存在: resource-id=user_profile

// 截图
截图: app_login_success.png`;

  const keywords = [
    { category: '导航操作', items: ['打开URL', '刷新页面', '后退', '前进', '关闭浏览器'] },
    { category: '元素操作', items: ['点击元素', '输入文本', '清空文本', '选择下拉框', '上传文件'] },
    { category: '等待操作', items: ['等待元素', '等待时间', '等待页面加载'] },
    { category: '断言验证', items: ['断言文本', '断言元素存在', '断言URL包含', '断言属性值'] },
    { category: '其他操作', items: ['截图', '滚动到元素', '执行JS脚本', '切换窗口'] }
  ];

  const handleExecute = () => {
    setIsExecuting(true);
    setExecutionLogs([]);
    
    const logs = [
      '[10:30:15] 开始执行测试用例: ' + caseName,
      '[10:30:15] 初始化测试引擎...',
      '[10:30:16] ✓ 打开URL: https://example.com',
      '[10:30:17] ✓ 等待元素: #login-form',
      '[10:30:18] ✓ 输入文本: #username',
      '[10:30:18] ✓ 输入文本: #password',
      '[10:30:19] ✓ 点击元素: #login-button',
      '[10:30:21] ✓ 等待元素: .dashboard',
      '[10:30:21] ✓ 断言文本: .welcome-message',
      '[10:30:22] ✓ 断言URL包含: /dashboard',
      '[10:30:22] ✓ 截图: login_success.png',
      '[10:30:23] ✓ 关闭浏览器',
      '[10:30:23] 测试执行完成 - 全部通过'
    ];

    logs.forEach((log, index) => {
      setTimeout(() => {
        setExecutionLogs(prev => [...prev, log]);
        if (index === logs.length - 1) {
          setIsExecuting(false);
        }
      }, index * 500);
    });
  };

  return (
    <div className="p-6 space-y-4">
      {/* Header Controls */}
      <Card className="border-gray-200">
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-2">用例名称</label>
              <Input
                value={caseName}
                onChange={(e) => setCaseName(e.target.value)}
                placeholder="输入测试用例名称"
                className="border-gray-300"
              />
            </div>
            <div className="w-64">
              <label className="block text-sm font-medium text-gray-700 mb-2">设备类型</label>
              <Select value={deviceType} onValueChange={setDeviceType}>
                <SelectTrigger className="border-gray-300">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="web">Web浏览器</SelectItem>
                  <SelectItem value="android">Android设备</SelectItem>
                  <SelectItem value="ios">iOS设备</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex items-end gap-2">
              <Button
                onClick={handleExecute}
                disabled={isExecuting}
                className="bg-[#409EFF] hover:bg-[#3a8ee6]"
              >
                <Play className="w-4 h-4 mr-2" />
                {isExecuting ? '执行中...' : '执行'}
              </Button>
              <Button variant="outline" className="border-gray-300">
                <Save className="w-4 h-4 mr-2" />
                保存
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Main Editor Area */}
      <div className="grid grid-cols-12 gap-4">
        {/* Keywords Sidebar */}
        <div className="col-span-3">
          <Card className="border-gray-200 h-full">
            <CardContent className="pt-6">
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Code className="w-4 h-4" />
                关键字列表
              </h3>
              <div className="space-y-4">
                {keywords.map((category, index) => (
                  <div key={index}>
                    <h4 className="text-sm font-medium text-gray-700 mb-2">{category.category}</h4>
                    <div className="space-y-1">
                      {category.items.map((item, idx) => (
                        <button
                          key={idx}
                          className="w-full text-left px-3 py-2 text-sm text-gray-600 hover:bg-blue-50 hover:text-[#409EFF] rounded transition-colors"
                          onClick={() => {
                            // Copy to clipboard
                            navigator.clipboard.writeText(item);
                          }}
                        >
                          {item}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Code Editor */}
        <div className="col-span-9">
          <Card className="border-gray-200 h-full">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-gray-900">脚本编辑器</h3>
                <Button variant="outline" size="sm" className="border-gray-300">
                  <Copy className="w-4 h-4 mr-2" />
                  复制代码
                </Button>
              </div>
              <div className="relative">
                <Textarea
                  defaultValue={deviceType === 'web' ? defaultScript : appScript}
                  className="font-mono text-sm min-h-[500px] bg-[#1e1e1e] text-[#d4d4d4] border-gray-700 resize-none"
                  style={{
                    lineHeight: '1.6',
                    tabSize: 2
                  }}
                />
                {/* Line numbers overlay simulation */}
                <div className="absolute left-3 top-3 text-xs text-gray-500 font-mono pointer-events-none select-none">
                  {Array.from({ length: 35 }, (_, i) => (
                    <div key={i} style={{ lineHeight: '1.6', height: '21px' }}>
                      {i + 1}
                    </div>
                  ))}
                </div>
              </div>
              <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded text-sm text-blue-800">
                💡 提示: 使用自然语言关键字编写测试步骤，支持参数化和变量引用 ${'{变量名}'}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Execution Console */}
      {executionLogs.length > 0 && (
        <Card className="border-gray-200">
          <CardContent className="pt-6">
            <h3 className="font-semibold text-gray-900 mb-4">执行日志</h3>
            <div className="bg-gray-900 text-green-400 p-4 rounded font-mono text-sm h-64 overflow-y-auto">
              {executionLogs.map((log, index) => (
                <div key={index} className="mb-1">
                  {log}
                </div>
              ))}
              {isExecuting && (
                <div className="mt-2 animate-pulse">
                  <span className="inline-block w-2 h-2 bg-green-400 rounded-full mr-2"></span>
                  执行中...
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
