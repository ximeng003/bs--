import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import Dashboard from '@/components/Dashboard.vue'
import ApiCaseEditor from '@/views/ApiCaseEditor.vue'
import ScriptEditor from '@/views/ScriptEditor.vue'
import ReportDetail from '@/views/ReportDetail.vue'
import TestPlanManager from '@/views/TestPlanManager.vue'
import PlanReportView from '@/views/PlanReportView.vue'
import Settings from '@/views/Settings.vue'
import TestCaseManager from '@/views/TestCaseManager.vue'
import ExecutionHistory from '@/views/ExecutionHistory.vue'
import Login from '@/views/Login.vue'
import UserProfile from '@/views/UserProfile.vue'
import ProjectSettings from '@/views/ProjectSettings.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { layout: 'blank' }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: UserProfile,
    meta: { requiresAuth: true }
  },
  {
    path: '/api-cases',
    name: 'ApiTest',
    component: TestCaseManager,
    meta: { requiresAuth: true }
  },
  {
    path: '/api-cases/edit/:id?',
    name: 'ApiCaseEditor',
    component: ApiCaseEditor,
    meta: { requiresAuth: true }
  },
  {
    path: '/web-app',
    name: 'WebApp',
    component: ScriptEditor,
    meta: { requiresAuth: true }
  },
  {
    path: '/plans',
    name: 'TestPlans',
    component: TestPlanManager,
    meta: { requiresAuth: true }
  },
  {
    path: '/plan-reports',
    name: 'PlanReports',
    component: PlanReportView,
    meta: { requiresAuth: true }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: ExecutionHistory,
    meta: { requiresAuth: true }
  },
  {
    path: '/reports/:id',
    name: 'ReportDetail',
    component: ReportDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { requiresAuth: true }
  },
  {
    path: '/project-settings',
    name: 'ProjectSettings',
    component: ProjectSettings,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const resolveTokenFromStorage = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const parsed = JSON.parse(userStr)
      const data = parsed?.data || parsed
      if (data?.token) return data.token
      if (data?.user?.token) return data.user.token
      if (parsed?.token) return parsed.token
      if (parsed?.user?.token) return parsed.user.token
      if (data?.accessToken) return data.accessToken
      if (parsed?.accessToken) return parsed.accessToken
    } catch {
      localStorage.removeItem('user')
    }
  }
  return localStorage.getItem('token') || localStorage.getItem('accessToken')
}

router.beforeEach((to, from, next) => {
  void from
  const user = localStorage.getItem('user')
  const token = resolveTokenFromStorage()
  
  if (to.meta.requiresAuth && (!user || !token)) {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    localStorage.removeItem('accessToken')
    next('/login')
  } else {
    sessionStorage.removeItem('auth_redirecting')
    next()
  }
})

export default router
