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
          ;(config.headers as any)['X-User-Name'] = user.username
        }
      } catch {
      }
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
    return Promise.reject(error)
  }
)

export default request
