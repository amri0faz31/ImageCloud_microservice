import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL || 'http://imagecloud.local/api/images'

const imageService = {
  async uploadImage(file, targetFormat, userId) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('targetFormat', targetFormat)

    const response = await axios.post(`${API_URL}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-User-Id': userId
      }
    })
    return response.data
  },

  async getHistory(userId) {
    const response = await axios.get(`${API_URL}/history`, {
      headers: {
        'X-User-Id': userId
      }
    })
    return response.data
  },

  async getImageStatus(imageId, userId) {
    const response = await axios.get(`${API_URL}/${imageId}/status`, {
      headers: {
        'X-User-Id': userId
      }
    })
    return response.data
  },

  async downloadImage(imageId, userId) {
    const response = await axios.get(`${API_URL}/${imageId}/download`, {
      headers: {
        'X-User-Id': userId
      },
      responseType: 'blob'
    })
    return response.data
  }
}

export default imageService
