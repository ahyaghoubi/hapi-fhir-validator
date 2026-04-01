import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
  server: {
    port: 5173,
    proxy: {
      '/v1': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/validate': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
})
