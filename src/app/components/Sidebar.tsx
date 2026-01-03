import React from 'react';
import { Home, Globe, Monitor, FileText, Settings, CirclePlay } from 'lucide-react';

interface SidebarProps {
  currentPage: string;
  onPageChange: (page: string) => void;
}

export function Sidebar({ currentPage, onPageChange }: SidebarProps) {
  const menuItems = [
    { id: 'dashboard', icon: Home, label: '工作台' },
    { id: 'api', icon: Globe, label: 'API 测试' },
    { id: 'web-app', icon: Monitor, label: 'Web/App' },
    { id: 'plans', icon: CirclePlay, label: '测试计划' },
    { id: 'report', icon: FileText, label: '测试报告' },
    { id: 'settings', icon: Settings, label: '系统设置' }
  ];

  return (
    <aside className="w-64 bg-[#304156] text-white flex flex-col">
      {/* Logo */}
      <div className="px-6 py-6 border-b border-gray-700">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-[#409EFF] rounded-lg flex items-center justify-center">
            <Monitor className="w-6 h-6 text-white" />
          </div>
          <div>
            <div className="font-semibold text-lg">测试平台</div>
            <div className="text-xs text-gray-400">Automation Hub</div>
          </div>
        </div>
      </div>

      {/* Navigation Menu */}
      <nav className="flex-1 px-3 py-4">
        <ul className="space-y-1">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentPage === item.id;
            return (
              <li key={item.id}>
                <button
                  onClick={() => onPageChange(item.id)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-[#409EFF] text-white'
                      : 'text-gray-300 hover:bg-[#263445] hover:text-white'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span>{item.label}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* Footer */}
      <div className="px-6 py-4 border-t border-gray-700">
        <div className="text-xs text-gray-400">
          <div>版本 v2.0.1</div>
          <div className="mt-1">© 2026 测试平台</div>
        </div>
      </div>
    </aside>
  );
}
