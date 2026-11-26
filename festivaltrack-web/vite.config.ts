/// <reference types="vitest/config" />
import { readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { defineConfig, loadEnv, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';

/** Comodín de public/serve.json que se sustituye por el origen exacto del backend. */
const COMODIN_BACKEND = 'https://*.onrender.com';

/**
 * Ajusta la CSP de dist/serve.json al backend real: `connect-src`, `img-src` y `media-src` pasan
 * de permitir cualquier https://*.onrender.com a permitir solo el origen de VITE_API_URL.
 * Sin VITE_API_URL se deja el comodín (y la app mostrará el error de configuración).
 */
function cspConBackend(apiUrl: string | undefined): Plugin {
  let outDir = 'dist';
  return {
    name: 'festivaltrack:csp-backend',
    apply: 'build',
    configResolved(config) {
      outDir = resolve(config.root, config.build.outDir);
    },
    // closeBundle corre después de copiar public/ a dist/.
    async closeBundle() {
      const ruta = resolve(outDir, 'serve.json');
      const texto = await readFile(ruta, 'utf8');
      if (!texto.includes(COMODIN_BACKEND)) {
        throw new Error(`serve.json no contiene ${COMODIN_BACKEND}: revisa la CSP de public/serve.json`);
      }
      if (!apiUrl) {
        this.warn(`VITE_API_URL no está definida: la CSP de serve.json queda con ${COMODIN_BACKEND}.`);
        return;
      }
      const origen = new URL(apiUrl).origin;
      await writeFile(ruta, texto.replaceAll(COMODIN_BACKEND, origen));
    },
  };
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');
  const apiUrl = (process.env.VITE_API_URL ?? env.VITE_API_URL)?.trim() || undefined;

  return {
    plugins: [react(), cspConBackend(apiUrl)],
    // El frontend se sirve desde la raíz del dominio (serve en producción).
    base: '/',
    build: {
      outDir: 'dist',
      // Sin sourcemaps en producción: no se publica el código fuente original.
      sourcemap: false,
    },
    server: {
      port: 5173,
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      include: ['src/**/*.test.{ts,tsx}'],
      css: false,
      restoreMocks: true,
    },
    preview: {
      port: 4173,
    },
  };
});
