import { reactive } from 'vue'

interface User {
  username: string
  role?: string
  token?: string
}

const normalizeUserPayload = (payload: any): User | null => {
  if (!payload) return null
  const data = payload?.data || payload
  if (data?.user && typeof data.user === 'object') {
    return {
      ...data.user,
      token: data.token ?? data.user.token ?? payload?.token ?? data?.accessToken ?? payload?.accessToken
    }
  }
  if (payload?.user && typeof payload.user === 'object') {
    return {
      ...payload.user,
      token: payload.token ?? payload.user.token ?? payload?.accessToken
    }
  }
  return data
}

export const userStore = reactive({
  user: null as User | null,
  
  initialize() {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser)
        this.user = normalizeUserPayload(parsed)
        if (!this.user) {
          localStorage.removeItem('user')
        } else {
          localStorage.setItem('user', JSON.stringify(this.user))
        }
      } catch (e) {
        console.error('Failed to parse user from localStorage', e)
        localStorage.removeItem('user')
      }
    }
  },

  setUser(user: User) {
    const normalized = normalizeUserPayload(user)
    this.user = normalized
    if (normalized) {
      localStorage.setItem('user', JSON.stringify(normalized))
    } else {
      localStorage.removeItem('user')
    }
    sessionStorage.removeItem('auth_redirecting')
  },

  clearUser() {
    this.user = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    localStorage.removeItem('accessToken')
    sessionStorage.removeItem('auth_redirecting')
  }
})

// Initialize immediately
userStore.initialize()
