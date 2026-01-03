import React, { useState } from 'react';
import { Sidebar } from "./components/Sidebar";
import { Dashboard } from "./components/Dashboard";
import { ApiCaseEditor } from "./components/ApiCaseEditor";
import { ScriptEditor } from "./components/ScriptEditor";
import { ReportDetail } from "./components/ReportDetail";
import { TestPlanManager } from "./components/TestPlanManager";
import { Settings } from "./components/Settings";

export default function App() {
  const [currentPage, setCurrentPage] = useState('dashboard');

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return <Dashboard />;
      case 'api':
        return <ApiCaseEditor />;
      case 'web-app':
        return <ScriptEditor />;
      case 'report':
        return <ReportDetail />;
      case 'plans':
        return <TestPlanManager />;
      case 'settings':
        return <Settings />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Left Sidebar Navigation */}
      <Sidebar currentPage={currentPage} onPageChange={setCurrentPage} />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Header */}
        <header className="bg-white border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-xl font-semibold text-gray-900">
                {currentPage === 'dashboard' && '工作台'}
                {currentPage === 'api' && 'API 测试用例'}
                {currentPage === 'web-app' && 'Web/App 脚本编辑'}
                {currentPage === 'report' && '测试报告'}
                {currentPage === 'plans' && '测试计划'}
                {currentPage === 'settings' && '系统设置'}
              </h1>
              <p className="text-sm text-gray-500 mt-1">自动化测试平台</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 text-sm">
                <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                <span className="text-gray-600">在线</span>
              </div>
              <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white text-sm">
                U
              </div>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 overflow-auto">
          {renderPage()}
        </main>
      </div>
    </div>
  );
}
