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
    // Las páginas se cargan bajo demanda (#93). El bloque más grande es el de la página del mapa:
    // leaflet-routing-machine pesa ~550 kB por sí sola y solo se descarga al abrir /events/map.
    chunkSizeWarningLimit: 800,
  },
  server: {
    port: 5173,
  },
  preview: {
    port: 4173,
  },
})
