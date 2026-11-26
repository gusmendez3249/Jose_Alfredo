import { http } from '../lib/http';
import { obtenerToken } from '../auth/session';
import { RespuestaInvalidaError, type RespuestaAuth } from './tipos';
import { esRespuestaAuth } from './validacion';

export interface DatosRegistro {
  nombre: string;
  correo: string;
  contrasena: string;
}

function comoRespuestaAuth(datos: unknown): RespuestaAuth {
  if (!esRespuestaAuth(datos)) throw new RespuestaInvalidaError('autenticación');
  return { accessToken: datos.accessToken, usuario: datos.usuario };
}

export const authService = {
  async login(correo: string, contrasena: string): Promise<RespuestaAuth> {
    const { data } = await http.post<unknown>('/auth/login', { correo, contrasena });
    return comoRespuestaAuth(data);
  },

  /** RegisterDto solo admite nombre, correo y contraseña: el rol lo asigna el servidor. */
  async registrar(datos: DatosRegistro): Promise<RespuestaAuth> {
    const { data } = await http.post<unknown>('/auth/register', datos);
    return comoRespuestaAuth(data);
  },

  /**
   * Aprueba el código que muestra la TV: la TV entra con la cuenta de esta sesión en unos
   * segundos. El usuario lo saca el servidor del token, nunca del cuerpo.
   *
   * `lib/http` no adjunta el token a rutas `/auth/*` (un 401 del login no debe cerrar la sesión),
   * pero esta sí exige sesión: la cabecera se pone aquí, como en `usuarios.service`. Si el token
   * ya venció, el 401 cierra la sesión y RequireAuth lleva al login, que regresa aquí.
   */
  async vincularTv(codigo: string): Promise<void> {
    const token = obtenerToken();
    await http.post<unknown>('/auth/tv-sync', { codigo }, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  },
};

/**
 * Deja el código como lo muestra la TV mientras se escribe: mayúsculas, solo letras y números,
 * máximo 8 y con guion a la mitad ("kx7m3q" → "KX7M-3Q").
 */
export function limpiarCodigoTv(texto: string): string {
  const limpio = texto.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8);
  return limpio.length > 4 ? `${limpio.slice(0, 4)}-${limpio.slice(4)}` : limpio;
}
