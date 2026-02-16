import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import imageService from '../services/imageService'
import {
  Container,
  Box,
  Typography,
  Button,
  Paper,
  AppBar,
  Toolbar,
  Card,
  CardContent,
  CardActions,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Grid,
  Alert,
  Chip
} from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import CloudUploadIcon from '@mui/icons-material/CloudUpload'
import DownloadIcon from '@mui/icons-material/Download'
import HistoryIcon from '@mui/icons-material/History'

const Dashboard = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  
  const [selectedFile, setSelectedFile] = useState(null)
  const [targetFormat, setTargetFormat] = useState('')
  const [uploading, setUploading] = useState(false)
  const [currentConversion, setCurrentConversion] = useState(null)
  const [history, setHistory] = useState([])
  const [error, setError] = useState('')
  const [showHistory, setShowHistory] = useState(false)

  const supportedFormats = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']

  useEffect(() => {
    if (showHistory) {
      loadHistory()
    }
  }, [showHistory])

  useEffect(() => {
    let interval
    if (currentConversion && currentConversion.status === 'PROCESSING') {
      interval = setInterval(() => {
        checkConversionStatus(currentConversion.imageId)
      }, 2000)
    }
    return () => {
      if (interval) clearInterval(interval)
    }
  }, [currentConversion])

  const loadHistory = async () => {
    try {
      const data = await imageService.getHistory(user.userId)
      setHistory(data)
    } catch (err) {
      setError('Failed to load history')
    }
  }

  const checkConversionStatus = async (imageId) => {
    try {
      const status = await imageService.getImageStatus(imageId, user.userId)
      if (status.status === 'COMPLETED' || status.status === 'FAILED') {
        setCurrentConversion(status)
        loadHistory()
      }
    } catch (err) {
      console.error('Error checking status:', err)
    }
  }

  const handleFileSelect = (event) => {
    const file = event.target.files[0]
    if (file) {
      setSelectedFile(file)
      const ext = file.name.split('.').pop().toLowerCase()
      setTargetFormat('')
      setError('')
    }
  }

  const handleUploadAndConvert = async () => {
    if (!selectedFile || !targetFormat) {
      setError('Please select a file and target format')
      return
    }

    setUploading(true)
    setError('')
    
    try {
      const response = await imageService.uploadImage(selectedFile, targetFormat, user.userId)
      setCurrentConversion({
        imageId: response.imageId,
        status: response.status,
        originalFileName: selectedFile.name
      })
      setSelectedFile(null)
      setTargetFormat('')
    } catch (err) {
      setError('Failed to upload image: ' + (err.response?.data?.message || err.message))
    } finally {
      setUploading(false)
    }
  }

  const handleDownload = async (imageId, fileName) => {
    try {
      const blob = await imageService.downloadImage(imageId, user.userId)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (err) {
      setError('Failed to download image')
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED': return 'success'
      case 'FAILED': return 'error'
      case 'PROCESSING': return 'warning'
      case 'PENDING': return 'info'
      default: return 'default'
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <Box>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            ImageCloud Dashboard
          </Typography>
          <Button 
            color="inherit" 
            onClick={() => setShowHistory(!showHistory)}
            startIcon={<HistoryIcon />}
            sx={{ mr: 2 }}
          >
            {showHistory ? 'Upload' : 'History'}
          </Button>
          <Button color="inherit" onClick={handleLogout} startIcon={<LogoutIcon />}>
            Logout
          </Button>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg">
        <Box sx={{ mt: 4 }}>
          <Typography variant="h4" gutterBottom>
            Welcome, {user?.email}
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
              {error}
            </Alert>
          )}

          {!showHistory ? (
            <>
              {/* Upload Card */}
              <Paper elevation={3} sx={{ padding: 4, mb: 4 }}>
                <Typography variant="h5" gutterBottom>
                  Upload & Convert Image
                </Typography>
                
                <Box sx={{ mt: 3 }}>
                  <Button
                    variant="contained"
                    component="label"
                    startIcon={<CloudUploadIcon />}
                    disabled={uploading}
                  >
                    Select Image
                    <input
                      type="file"
                      hidden
                      accept="image/*"
                      onChange={handleFileSelect}
                    />
                  </Button>
                  
                  {selectedFile && (
                    <Typography variant="body2" sx={{ mt: 2 }}>
                      Selected: {selectedFile.name}
                    </Typography>
                  )}

                  <FormControl fullWidth sx={{ mt: 3 }}>
                    <InputLabel>Target Format</InputLabel>
                    <Select
                      value={targetFormat}
                      onChange={(e) => setTargetFormat(e.target.value)}
                      label="Target Format"
                      disabled={!selectedFile || uploading}
                    >
                      {supportedFormats.map((format) => (
                        <MenuItem key={format} value={format}>
                          {format.toUpperCase()}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <Button
                    variant="contained"
                    color="primary"
                    fullWidth
                    sx={{ mt: 3 }}
                    onClick={handleUploadAndConvert}
                    disabled={!selectedFile || !targetFormat || uploading}
                  >
                    {uploading ? 'Uploading...' : 'Convert Image'}
                  </Button>
                </Box>
              </Paper>

              {/* Current Conversion Status */}
              {currentConversion && (
                <Paper elevation={3} sx={{ padding: 4 }}>
                  <Typography variant="h5" gutterBottom>
                    Conversion Status
                  </Typography>
                  
                  <Card sx={{ mt: 2 }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="h6">
                          {currentConversion.originalFileName}
                        </Typography>
                        <Chip 
                          label={currentConversion.status} 
                          color={getStatusColor(currentConversion.status)}
                        />
                      </Box>
                      
                      {currentConversion.status === 'PROCESSING' && (
                        <Box sx={{ display: 'flex', alignItems: 'center', mt: 2 }}>
                          <CircularProgress size={24} sx={{ mr: 2 }} />
                          <Typography variant="body2">
                            Converting your image, please wait...
                          </Typography>
                        </Box>
                      )}

                      {currentConversion.status === 'COMPLETED' && (
                        <Button
                          variant="contained"
                          color="success"
                          startIcon={<DownloadIcon />}
                          sx={{ mt: 2 }}
                          onClick={() => handleDownload(
                            currentConversion.imageId,
                            currentConversion.originalFileName
                          )}
                        >
                          Download Converted Image
                        </Button>
                      )}

                      {currentConversion.status === 'FAILED' && (
                        <Alert severity="error" sx={{ mt: 2 }}>
                          Conversion failed: {currentConversion.errorMessage}
                        </Alert>
                      )}
                    </CardContent>
                  </Card>
                </Paper>
              )}
            </>
          ) : (
            /* History View */
            <Paper elevation={3} sx={{ padding: 4 }}>
              <Typography variant="h5" gutterBottom>
                Conversion History
              </Typography>
              
              {history.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  No conversions yet
                </Typography>
              ) : (
                <Grid container spacing={2} sx={{ mt: 2 }}>
                  {history.map((item) => (
                    <Grid item xs={12} md={6} key={item.id}>
                      <Card>
                        <CardContent>
                          <Typography variant="h6" noWrap>
                            {item.originalFileName}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {item.originalFormat} → {item.targetFormat}
                          </Typography>
                          <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                            {new Date(item.uploadedAt).toLocaleString()}
                          </Typography>
                          <Chip 
                            label={item.status} 
                            color={getStatusColor(item.status)}
                            size="small"
                            sx={{ mt: 1 }}
                          />
                        </CardContent>
                        <CardActions>
                          {item.status === 'COMPLETED' && (
                            <Button
                              size="small"
                              startIcon={<DownloadIcon />}
                              onClick={() => handleDownload(item.id, item.originalFileName)}
                            >
                              Download
                            </Button>
                          )}
                        </CardActions>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </Paper>
          )}
        </Box>
      </Container>
    </Box>
  )
}

export default Dashboard
