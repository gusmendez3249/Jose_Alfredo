import { esObjeto } from '../services/tipos';

/** `location.state` con el que se llega a /login o /register. */
export interface EstadoNavegacionLogin {
  /** Ruta interna a la que volver tras iniciar sesión. */
  from?: string;
  /** true si se llegó porque el backend rechazó el token. */
  sesionExpirada?: boolean;
}

/** Solo rutas internas: "/algo" sí; "//dominio", "/\dominio" o URLs absolutas no (open redirect). */
export function esRutaInterna(valor: unknown): valor is string {
  return (
    typeof valor === 'string' &&
    valor.startsWith('/') &&
    !valor.startsWith('//') &&
    !valor.startsWith('/\\') &&
    !valor.startsWith('/login') &&
    !valor.startsWith('/register')
  );
}

export function leerEstadoLogin(estado: unknown): EstadoNavegacionLogin {
  if (!esObjeto(estado)) return {};
  return {
    from: esRutaInterna(estado.from) ? estado.from : undefined,
    sesionExpirada: estado.sesionExpirada === true,
  };
}
