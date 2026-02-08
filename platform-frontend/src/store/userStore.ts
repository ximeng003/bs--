import { reactive } from 'vue'

interface User {
  username: string
  role?: string
  token?: string
}

export const userStore = reactive({
  user: null as User | null,
  
  initialize() {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser)
        // Handle potential different response structures
        this.user = parsed.data || parsed
      } catch (e) {
        console.error('Failed to parse user from localStorage', e)
        localStorage.removeItem('user')
      }
    }
  },

  setUser(user: User) {
    this.user = user
    localStorage.setItem('user', JSON.stringify(user))
  },

  clearUser() {
    this.user = null
    localStorage.removeItem('user')
  }
})

// Initialize immediately
userStore.initialize()
