import axios from 'axios'

const request = axios.create({
  baseURL: '/api', // Vite proxy will handle this
  timeout: 120000
})

// Request interceptor
request.interceptors.request.use(
  config => {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        const parsed = JSON.parse(userStr)
        const user = parsed && parsed.username ? parsed : (parsed.data || parsed)
        if (user && user.username) {
          if (!config.headers) {
            config.headers = {} as any
          }
          ;(config.headers as any)['X-User-Name'] = encodeURIComponent(user.username)
        }
      } catch {
      }
    }
    
    const projectId = localStorage.getItem('currentProjectId')
    if (projectId) {
       if (!config.headers) config.headers = {} as any
       (config.headers as any)['X-Project-Id'] = projectId
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
    
    // Auto-recovery for 404 caused by invalid project context
     if (error.response && error.response.status === 404) {
        const currentProjectId = localStorage.getItem('currentProjectId')
        if (currentProjectId) {
           // Prevent infinite reload loops
           const lastReload = sessionStorage.getItem('last_404_reload')
           const now = Date.now()
           if (lastReload && (now - parseInt(lastReload) < 5000)) {
              console.warn('Loop detected: 404 reload prevented.')
              return Promise.reject(error)
           }
           
           console.warn('Received 404 with active Project ID. Clearing invalid project context to recover.')
           localStorage.removeItem('currentProjectId')
           sessionStorage.setItem('last_404_reload', now.toString())
           
           // Reloading the page to reset state is the safest way to recover
           setTimeout(() => {
              window.location.reload()
           }, 500)
        }
     }
    
    return Promise.reject(error)
  }
)

export default request
