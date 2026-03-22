import axios from 'axios'

const request = axios.create({
  baseURL: '/api', // Vite proxy will handle this
  timeout: 120000
})

const LONG_RUNNING_TIMEOUT_MS = 15 * 60 * 1000
const AUTH_REDIRECT_KEY = 'auth_redirecting'

const resolveStoredUser = () => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return null
  try {
    const parsed = JSON.parse(userStr)
    const data = parsed?.data || parsed
    if (data?.user && typeof data.user === 'object') {
      return {
        ...data.user,
        token: data.token ?? data.user.token ?? parsed?.token ?? data?.accessToken ?? parsed?.accessToken
      }
    }
    if (parsed?.user && typeof parsed.user === 'object') {
      return {
        ...parsed.user,
        token: parsed.token ?? parsed.user.token ?? parsed?.accessToken
      }
    }
    return data
  } catch {
    return null
  }
}

const resolveToken = () => {
  const normalized = resolveStoredUser()
  if (normalized?.token) return normalized.token
  const plainToken = localStorage.getItem('token')
  if (plainToken) return plainToken
  const accessToken = localStorage.getItem('accessToken')
  if (accessToken) return accessToken
  return null
}

// Request interceptor
request.interceptors.request.use(
  config => {
    const token = resolveToken()
    if (token) {
      if (!config.headers) {
        config.headers = {} as any
      }
      ;(config.headers as any)['Authorization'] = `Bearer ${token}`
    }
    
    const url = config.url || ''
    const projectId = localStorage.getItem('currentProjectId')
    const isGlobalEndpoint =
      url.startsWith('/projects') ||
      url.startsWith('/teams') ||
      url.startsWith('/user') ||
      url.startsWith('/auth') ||
      url.startsWith('/permission-requests')

    if (url.includes('/execute')) {
      config.timeout = LONG_RUNNING_TIMEOUT_MS
    }

    if (projectId && !isGlobalEndpoint) {
      if (!config.headers) config.headers = {} as any
      ;(config.headers as any)['X-Project-Id'] = projectId
    }

    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  response => {
    const res = response.data
    // Assuming backend returns Result<T> with code, message, data
    if (res.code !== 200 && res.code !== 0) { // 0 or 200 as success
       // Handle error
       console.error(res.message)
       return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res.data
    }
  },
  error => {
    console.error('Request Error:', error)
    if (error.response && error.response.status === 401) {
      const isLoginPage = window.location.pathname === '/login'
      if (!isLoginPage && sessionStorage.getItem(AUTH_REDIRECT_KEY) !== '1') {
        sessionStorage.setItem(AUTH_REDIRECT_KEY, '1')
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('currentProjectId')
        setTimeout(() => {
          window.location.href = '/login'
        }, 100)
      }
      return Promise.reject(new Error('登录已失效，请重新登录'))
    }
    
    // Auto-recovery for 404/403 (Keep current project context unless explicitly needed)
    if (error.response && (error.response.status === 404 || error.response.status === 403)) {
       const msg = error.response.data?.message || ''
       if (msg.includes('project context invalid') || msg.includes('访问权限')) {
          console.warn('Project context might be invalid, but we will not clear it automatically to avoid blank pages.')
       }
    }
    
    return Promise.reject(error)
  }
)

export default request
