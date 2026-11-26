import { mensajeDeError, type MensajesPorEstado } from '../../../lib/errores';

const SIN_PERMISOS =
  'Tu sesión no tiene permisos de administrador para hacer esto. Si te acaban de dar el rol, cierra sesión y vuelve a entrar.';
const SESION_TERMINADA = 'Tu sesión de administrador terminó. Vuelve a iniciar sesión.';

/** Mensajes comunes a las pantallas de administración de conciertos (401/403 como en el móvil, pero el 403 dice la verdad). */
export const MENSAJES_ADMIN: MensajesPorEstado = {
  401: SESION_TERMINADA,
  403: SIN_PERMISOS,
};

export function mensajeAlGuardarConcierto(error: unknown): string {
  return mensajeDeError(error, {
    ...MENSAJES_ADMIN,
    400: 'El servidor rechazó los datos. Revisa el formulario.',
    404: 'Este concierto ya no existe. Vuelve a la lista de conciertos.',
    // Cualquier otro error HTTP cae en el texto del móvil; los de red conservan su mensaje propio.
  });
}

export function mensajeAlCancelarConcierto(error: unknown): string {
  return mensajeDeError(error, {
    ...MENSAJES_ADMIN,
    404: 'Este concierto ya no existe. Actualiza la lista.',
  });
}

export function mensajeAlGuardarRepertorio(error: unknown): string {
  return mensajeDeError(error, {
    ...MENSAJES_ADMIN,
    400: 'Alguna canción ya no existe en el catálogo. Recarga la página e inténtalo de nuevo.',
    404: 'Este concierto ya no existe. Vuelve a la lista de conciertos.',
  });
}
