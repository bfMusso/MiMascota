import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Modo desarrollo: http://localhost:5180 con recarga automática.
// Las llamadas a /api van al backend que corre en VS Code (puerto 8090),
// así el navegador las ve como del mismo sitio y la cookie de sesión funciona.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5180,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8090',
    },
  },
})
