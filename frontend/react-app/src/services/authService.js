import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/auth'

const authService = {
  async signup(email, password) {
    const response = await axios.post(`${API_URL}/signup`, { email, password })
    return response.data
  },

  async login(email, password) {
    const response = await axios.post(`${API_URL}/login`, { email, password })
    return response.data
  },

  async getCurrentUser() {
    const token = localStorage.getItem('token')
    const response = await axios.get(`${API_URL}/me`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    return response.data
  }
}

export default authService
