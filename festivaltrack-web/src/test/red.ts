import { AxiosError, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios';
import { http } from '../lib/http';

/** Respuesta simulada: un cuerpo (200 por defecto) o un estado de error. */
export interface RespuestaFalsa {
  status?: number;
  data?: unknown;
}

export type Manejador = RespuestaFalsa | ((cuerpo: unknown, config: InternalAxiosRequestConfig) => RespuestaFalsa | Promise<RespuestaFalsa>);

export interface PeticionRegistrada {
  metodo: string;
  url: string;
  cuerpo: unknown;
  autorizacion: string | undefined;
}

/**
 * Sustituye el adaptador de axios del cliente real (`lib/http`) por uno en memoria:
 * así se prueban también los interceptores (token, 401) sin tocar la red.
 * Las claves son "METODO /ruta" (p. ej. "POST /auth/login"); una ruta sin manejador devuelve 404.
 */
export function simularRed(manejadores: Record<string, Manejador>) {
  const peticiones: PeticionRegistrada[] = [];

  const adaptador: AxiosAdapter = async (config) => {
    const metodo = (config.method ?? 'get').toUpperCase();
    const url = config.url ?? '';
    const cuerpo: unknown = typeof config.data === 'string' ? JSON.parse(config.data) : config.data;
    peticiones.push({ metodo, url, cuerpo, autorizacion: config.headers.get('Authorization')?.toString() });

    const manejador = manejadores[`${metodo} ${url}`];
    const { status = 200, data } =
      manejador === undefined
        ? { status: 404, data: { message: 'Not Found' } }
        : typeof manejador === 'function'
          ? await manejador(cuerpo, config)
          : manejador;

    const respuesta = { data, status, statusText: String(status), headers: {}, config };
    if (status >= 200 && status < 300) return respuesta;
    throw new AxiosError(`Request failed with status code ${status}`, 'ERR_BAD_REQUEST', config, null, respuesta);
  };

  http.defaults.adapter = adaptador;
  return { peticiones };
}

/** Simula que no hay conexión con el servidor (sin respuesta). */
export function simularSinConexion() {
  http.defaults.adapter = async (config) => {
    throw new AxiosError('Network Error', 'ERR_NETWORK', config, null, undefined);
  };
}
