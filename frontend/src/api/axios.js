import axios from 'axios';

// The backend is accessible via /api because nginx proxies it.
// Vite proxy will intercept /api requests and forward to localhost:80
const api = axios.create({
  baseURL: '/api',
});

// Add a request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default api;
