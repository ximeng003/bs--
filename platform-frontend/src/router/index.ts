import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import Dashboard from '@/components/Dashboard.vue'
import ApiCaseEditor from '@/views/ApiCaseEditor.vue'
import ScriptEditor from '@/views/ScriptEditor.vue'
import ReportDetail from '@/views/ReportDetail.vue'
import TestPlanManager from '@/views/TestPlanManager.vue'
import Settings from '@/views/Settings.vue'
import TestCaseManager from '@/views/TestCaseManager.vue'
import ExecutionHistory from '@/views/ExecutionHistory.vue'
import Login from '@/views/Login.vue'
import UserProfile from '@/views/UserProfile.vue'

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
    path: '/api-test',
    name: 'ApiTest',
    component: TestCaseManager,
    meta: { requiresAuth: true }
  },
  {
    path: '/api-test/edit/:id?',
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  void from
  const user = localStorage.getItem('user')
  if (to.meta.requiresAuth && !user) {
    next('/login')
  } else {
    next()
  }
})

export default router
