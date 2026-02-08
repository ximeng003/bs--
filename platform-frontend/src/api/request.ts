import axios from 'axios'

const request = axios.create({
  baseURL: '/api', // Vite proxy will handle this
  timeout: 5000
})

// Request interceptor
request.interceptors.request.use(
  config => {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      // If using JWT, add token here. For now, we just proceed.
      // const user = JSON.parse(userStr)
      // config.headers['Authorization'] = `Bearer ${user.token}`
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
