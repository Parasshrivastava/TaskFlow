import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Dev only: forward /api/* to the Spring Boot backend so the browser
    // sees one origin (no CORS). In K3s an Ingress does this routing.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
