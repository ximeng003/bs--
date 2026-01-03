import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { CheckCircle, XCircle, Clock, TrendingUp } from 'lucide-react';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

export function Dashboard() {
  // Statistics data
  const stats = [
    {
      title: '总用例数',
      value: '1,246',
      change: '+12.5%',
      icon: TrendingUp,
      color: 'bg-blue-500',
      textColor: 'text-blue-600'
    },
    {
      title: '通过用例',
      value: '1,108',
      change: '+8.2%',
      icon: CheckCircle,
      color: 'bg-green-500',
      textColor: 'text-green-600'
    },
    {
      title: '失败用例',
      value: '138',
      change: '-3.1%',
      icon: XCircle,
      color: 'bg-red-500',
      textColor: 'text-red-600'
    },
    {
      title: '平均耗时',
      value: '2.8s',
      change: '-15.3%',
      icon: Clock,
      color: 'bg-yellow-500',
      textColor: 'text-yellow-600'
    }
  ];

  // Donut chart data
  const donutData = [
    { name: '通过', value: 1108, color: '#67C23A' },
    { name: '失败', value: 138, color: '#F56C6C' }
  ];

  // Bar chart data
  const barData = [
    { date: '01-27', passed: 145, failed: 12 },
    { date: '01-28', passed: 158, failed: 15 },
    { date: '01-29', passed: 162, failed: 10 },
    { date: '01-30', passed: 148, failed: 18 },
    { date: '01-31', passed: 172, failed: 14 },
    { date: '02-01', passed: 165, failed: 11 },
    { date: '02-02', passed: 158, failed: 16 }
  ];

  // Recent activities
  const recentActivities = [
    { id: 1, type: 'success', case: '用户登录API测试', time: '5分钟前', executor: '张三' },
    { id: 2, type: 'failed', case: 'Web支付流程测试', time: '15分钟前', executor: '李四' },
    { id: 3, type: 'success', case: 'APP首页加载测试', time: '30分钟前', executor: '王五' },
    { id: 4, type: 'success', case: '数据查询接口测试', time: '1小时前', executor: '赵六' },
    { id: 5, type: 'success', case: 'Web表单提交测试', time: '2小时前', executor: '张三' },
    { id: 6, type: 'failed', case: 'APP推送功能测试', time: '3小时前', executor: '李四' }
  ];

  return (
    <div className="p-6 space-y-6">
      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <Card key={index} className="border-gray-200 hover:shadow-md transition-shadow">
              <CardContent className="pt-6">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm text-gray-600 mb-1">{stat.title}</p>
                    <h3 className="text-3xl font-semibold text-gray-900">{stat.value}</h3>
                    <p className={`text-sm mt-2 ${stat.change.startsWith('+') ? 'text-green-600' : 'text-red-600'}`}>
                      {stat.change} vs 上周
                    </p>
                  </div>
                  <div className={`${stat.color} w-12 h-12 rounded-lg flex items-center justify-center`}>
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Donut Chart */}
        <Card className="border-gray-200">
          <CardHeader>
            <CardTitle>测试通过率分布</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-80 flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={donutData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={5}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  >
                    {donutData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="flex justify-center gap-8 mt-4">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-[#67C23A]"></div>
                <span className="text-sm text-gray-600">通过: 1,108</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-[#F56C6C]"></div>
                <span className="text-sm text-gray-600">失败: 138</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Bar Chart */}
        <Card className="border-gray-200">
          <CardHeader>
            <CardTitle>每日执行趋势</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={barData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="passed" fill="#67C23A" name="通过" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="failed" fill="#F56C6C" name="失败" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Activities Table */}
      <Card className="border-gray-200">
        <CardHeader>
          <CardTitle>最近活动</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">状态</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">测试用例</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">执行人</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">时间</th>
                </tr>
              </thead>
              <tbody>
                {recentActivities.map((activity) => (
                  <tr key={activity.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4">
                      {activity.type === 'success' ? (
                        <span className="inline-flex items-center gap-1 px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-medium">
                          <CheckCircle className="w-3 h-3" />
                          成功
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2 py-1 bg-red-100 text-red-700 rounded text-xs font-medium">
                          <XCircle className="w-3 h-3" />
                          失败
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-sm text-gray-900">{activity.case}</td>
                    <td className="py-3 px-4 text-sm text-gray-600">{activity.executor}</td>
                    <td className="py-3 px-4 text-sm text-gray-500">{activity.time}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
