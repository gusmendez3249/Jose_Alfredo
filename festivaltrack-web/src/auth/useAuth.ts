import { useCallback, useSyncExternalStore } from 'react';
import { authService, type DatosRegistro } from '../services/auth.service';
import type { Usuario } from '../services/tipos';
import {
  guardarSesion,
  limpiarSesion,
  obtenerEstadoSesion,
  suscribirSesion,
  type MotivoCierre,
} from './session';

export interface Auth {
  usuario: Usuario | null;
  autenticado: boolean;
  /** Motivo del último cierre de sesión ('expirada' si el backend rechazó el token). */
  motivoCierre: MotivoCierre;
  iniciarSesion: (correo: string, contrasena: string) => Promise<Usuario>;
  registrarse: (datos: DatosRegistro) => Promise<Usuario>;
  cerrarSesion: () => void;
}

/**
 * Estado de autenticación. La sesión es un store externo (session.ts), así que no hace
 * falta un Provider: cualquier componente que llame a useAuth() se actualiza al iniciar
 * o cerrar sesión, también si ocurre en otra pestaña.
 */
export function useAuth(): Auth {
  const { sesion, motivoCierre } = useSyncExternalStore(suscribirSesion, obtenerEstadoSesion);

  const iniciarSesion = useCallback(async (correo: string, contrasena: string) => {
    const { accessToken, usuario } = await authService.login(correo, contrasena);
    guardarSesion(accessToken, usuario);
    return usuario;
  }, []);

  const registrarse = useCallback(async (datos: DatosRegistro) => {
    const { accessToken, usuario } = await authService.registrar(datos);
    guardarSesion(accessToken, usuario);
    return usuario;
  }, []);

  const cerrarSesion = useCallback(() => limpiarSesion('manual'), []);

  return {
    usuario: sesion?.usuario ?? null,
    autenticado: sesion !== null,
    motivoCierre,
    iniciarSesion,
    registrarse,
    cerrarSesion,
  };
}
