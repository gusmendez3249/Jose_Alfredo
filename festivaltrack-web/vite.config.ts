import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // En Railway el frontend se sirve desde la raíz del dominio asignado
  base: '/',
  build: {
    outDir: 'dist',
    // Genera sourcemaps para facilitar debugging en producción
    sourcemap: false,
  },
  server: {
    port: 5173,
  },
  preview: {
    port: 4173,
  },
})
