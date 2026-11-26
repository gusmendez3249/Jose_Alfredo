import axios from 'axios';
import { RespuestaInvalidaError, esObjeto } from '../services/tipos';

/** Mensajes propios de una pantalla para ciertos códigos HTTP (p. ej. 401 en el login). */
export type MensajesPorEstado = Partial<Record<number, string>>;

const SERVIDOR_DESPERTANDO =
  'El servidor está despertando o tarda en responder. Espera unos segundos y vuelve a intentarlo.';
const SIN_CONEXION = 'No se pudo conectar con el servidor. Revisa tu conexión a internet e inténtalo de nuevo.';

/** true si la petición se canceló a propósito (p. ej. al salir de la página): no es un error que mostrar. */
export function esCancelacion(error: unknown): boolean {
  return axios.isCancel(error) || (error instanceof DOMException && error.name === 'AbortError');
}

/** Saca el texto de `message` de NestJS: class-validator manda un arreglo y se usa el primero. */
function mensajeDelBackend(datos: unknown): string | null {
  if (!esObjeto(datos)) return null;
  const { message } = datos;
  if (Array.isArray(message)) {
    const primero: unknown = message[0];
    return typeof primero === 'string' && primero.trim() ? primero : null;
  }
  return typeof message === 'string' && message.trim() ? message : null;
}

/** Convierte cualquier error en un mensaje en español apto para mostrar al usuario. */
export function mensajeDeError(error: unknown, propios: MensajesPorEstado = {}): string {
  if (error instanceof RespuestaInvalidaError) {
    return 'El servidor respondió con datos inesperados. Inténtalo de nuevo más tarde.';
  }
  if (!axios.isAxiosError(error)) {
    return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return SERVIDOR_DESPERTANDO;
  // Sin respuesta: red caída, CORS o servidor apagado (el navegador no distingue entre ellos).
  if (!error.response) return SIN_CONEXION;

  const { status, data } = error.response;
  const propio = propios[status];
  if (propio) return propio;

  const delBackend = mensajeDelBackend(data);
  switch (status) {
    case 400:
      return delBackend ?? 'Revisa los datos e inténtalo de nuevo.';
    case 401:
      return 'Tu sesión expiró. Inicia sesión de nuevo.';
    case 403:
      return 'No tienes permiso para hacer esto.';
    case 404:
      return delBackend ?? 'No se encontró lo que buscabas.';
    case 409:
      return delBackend ?? 'Ya existe un registro con esos datos.';
    case 429:
      // El backend responde "ThrottlerException: Too Many Requests": nunca se muestra tal cual.
      return 'Demasiados intentos seguidos. Espera un minuto y vuelve a intentarlo.';
    case 502:
    case 503:
    case 504:
      return SERVIDOR_DESPERTANDO;
    default:
      return status >= 500
        ? 'El servidor tuvo un problema. Inténtalo de nuevo en unos minutos.'
        : 'No se pudo completar la operación. Inténtalo de nuevo.';
  }
}
