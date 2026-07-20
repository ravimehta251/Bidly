import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'window',
  },
  server: {
    proxy: {
      '/api': 'http://localhost:80',
      '/ws': {
        target: 'http://localhost:80',
        ws: true,
      },
    },
  },
})
