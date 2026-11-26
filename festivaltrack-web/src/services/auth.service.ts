import { http } from '../lib/http';
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
};
