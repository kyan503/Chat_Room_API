import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // Ép kiểu biến global của Node.js sang globalThis của trình duyệt bảo đảm tương thích với SockJS
    global: 'globalThis',
  },
})
