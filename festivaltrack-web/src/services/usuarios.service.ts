import { http } from '../lib/http';
import { obtenerToken } from '../auth/session';
import { esObjeto, RespuestaInvalidaError, type Rol, type Usuario } from './tipos';

/** Usuario tal como lo lista GET /auth/usuarios (sin contraseña). */
export type UsuarioAdmin = Usuario;

export interface DatosAdministrador {
  nombre: string;
  correo: string;
  contrasena: string;
}

/**
 * `lib/http` NO adjunta el token a rutas `/auth/*` (para que un 401 de login no cierre la sesión),
 * pero estos tres endpoints exigen ADMINISTRADOR: la cabecera se pone aquí a mano.
 */
function cabecerasAdmin(): Record<string, string> {
  const token = obtenerToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function esRol(v: unknown): v is Rol {
  return v === 'USUARIO' || v === 'ADMINISTRADOR';
}

function esUsuarioAdmin(v: unknown): v is UsuarioAdmin {
  return (
    esObjeto(v) &&
    typeof v.id === 'string' &&
    typeof v.nombre === 'string' &&
    typeof v.correo === 'string' &&
    esRol(v.rol)
  );
}

export const usuariosService = {
  /** Todos los usuarios; los elementos con forma inválida se descartan. Solo administrador. */
  async listar(signal?: AbortSignal): Promise<UsuarioAdmin[]> {
    const { data } = await http.get<unknown>('/auth/usuarios', { signal, headers: cabecerasAdmin() });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('usuarios');
    return data.filter(esUsuarioAdmin);
  },

  /**
   * Da de alta a un administrador. El backend responde con el token del NUEVO administrador:
   * se descarta a propósito para no sustituir la sesión de quien lo registra.
   */
  async registrarAdministrador(datos: DatosAdministrador): Promise<void> {
    await http.post('/auth/register-admin', datos, { headers: cabecerasAdmin() });
  },

  /** Cambia el rol. El backend rechaza (400) quitar el último administrador o el propio rol. */
  async cambiarRol(id: string, rol: Rol): Promise<UsuarioAdmin> {
    const { data } = await http.put<unknown>(
      `/auth/usuarios/${encodeURIComponent(id)}/rol`,
      { rol },
      { headers: cabecerasAdmin() },
    );
    if (!esUsuarioAdmin(data)) throw new RespuestaInvalidaError('usuario');
    return data;
  },
};
