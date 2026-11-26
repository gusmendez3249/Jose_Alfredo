import axios, { AxiosHeaders } from 'axios';
import { API_URL } from '../config/env';
import { limpiarSesion, obtenerToken } from '../auth/session';

/** Render (plan gratuito) tarda en despertar; más de 20 s se trata como "servidor despertando". */
const TIEMPO_LIMITE_MS = 20_000;

export const http = axios.create({
  baseURL: API_URL,
  timeout: TIEMPO_LIMITE_MS,
  headers: { 'Content-Type': 'application/json' },
});

/** Login y registro no llevan token: un 401 ahí son credenciales malas, no sesión caducada. */
function esRutaDeAuth(url: string | undefined): boolean {
  return (url ?? '').startsWith('/auth/');
}

http.interceptors.request.use((config) => {
  const token = obtenerToken();
  if (token && !esRutaDeAuth(config.url)) {
    const cabeceras = AxiosHeaders.from(config.headers);
    cabeceras.set('Authorization', `Bearer ${token}`);
    config.headers = cabeceras;
  }
  return config;
});

http.interceptors.response.use(
  (respuesta) => respuesta,
  (error: unknown) => {
    // Token rechazado por el backend: se cierra la sesión y RequireAuth lleva al login con el aviso.
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const cabeceras = AxiosHeaders.from(error.config?.headers);
      if (cabeceras.has('Authorization')) limpiarSesion('expirada');
    }
    return Promise.reject(error);
  },
);
