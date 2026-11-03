import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, getInfo } from '../api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    async login(form) {
      const data = await apiLogin(form)
      this.token = data.token
      localStorage.setItem('token', data.token)
      this.user = data.user
    },
    async register(form) {
      const data = await apiRegister(form)
      this.token = data.token
      localStorage.setItem('token', data.token)
      this.user = data.user
    },
    async fetchInfo() {
      this.user = await getInfo()
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
    }
  }
})
