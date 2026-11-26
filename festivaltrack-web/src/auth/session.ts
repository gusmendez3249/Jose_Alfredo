import { esObjeto, type Rol, type Usuario } from '../services/tipos';

/**
 * Sesión del usuario guardada en localStorage bajo UNA sola clave.
 * Es el único módulo que toca localStorage para la sesión; las páginas usan `useAuth()`.
 */

const CLAVE = 'festivaltrack.sesion';
/** Claves que usaban versiones anteriores de la web (se migran y se borran). */
const CLAVES_VIEJAS = ['token', 'accessToken'] as const;

export interface Sesion {
  token: string;
  /** null solo para sesiones migradas de la versión vieja, que no guardaba el usuario. */
  usuario: Usuario | null;
  /** Momento (ms) en que caduca el JWT según su `exp`; null si no se pudo leer. */
  expiraEn: number | null;
}

/** Por qué terminó la última sesión: lo usa el login para avisar "tu sesión expiró". */
export type MotivoCierre = 'manual' | 'expirada' | null;

export interface EstadoSesion {
  sesion: Sesion | null;
  motivoCierre: MotivoCierre;
}

type Oyente = () => void;

const ROLES: readonly Rol[] = ['USUARIO', 'ADMINISTRADOR'];

function leerStorage(clave: string): string | null {
  try {
    return localStorage.getItem(clave);
  } catch {
    return null;
  }
}

function escribirStorage(clave: string, valor: string | null): void {
  try {
    if (valor === null) localStorage.removeItem(clave);
    else localStorage.setItem(clave, valor);
  } catch {
    // Almacenamiento bloqueado (modo privado estricto): la sesión vive solo en memoria.
  }
}

/** Lee `exp` del JWT. Solo sirve para la interfaz: la validez real la decide el backend. */
function leerExpiracion(token: string): number | null {
  const partes = token.split('.');
  if (partes.length !== 3) return null;
  try {
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const relleno = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const payload: unknown = JSON.parse(atob(relleno));
    if (esObjeto(payload) && typeof payload.exp === 'number') return payload.exp * 1000;
  } catch {
    // Token ilegible: se trata como sin caducidad conocida.
  }
  return null;
}

function esUsuario(valor: unknown): valor is Usuario {
  return (
    esObjeto(valor) &&
    typeof valor.id === 'string' &&
    typeof valor.nombre === 'string' &&
    typeof valor.correo === 'string' &&
    typeof valor.rol === 'string' &&
    (ROLES as readonly string[]).includes(valor.rol)
  );
}

function parsearSesion(texto: string | null): Sesion | null {
  if (!texto) return null;
  try {
    const datos: unknown = JSON.parse(texto);
    if (!esObjeto(datos) || typeof datos.token !== 'string' || datos.token === '') return null;
    return {
      token: datos.token,
      usuario: esUsuario(datos.usuario) ? datos.usuario : null,
      expiraEn: leerExpiracion(datos.token),
    };
  } catch {
    return null;
  }
}

function caducada(sesion: Sesion, ahora = Date.now()): boolean {
  return sesion.expiraEn !== null && sesion.expiraEn <= ahora;
}

/** Migra el token de las claves viejas (si lo hay) y las borra siempre. */
function migrarClavesViejas(): void {
  const tokenViejo = CLAVES_VIEJAS.map(leerStorage).find((t) => t !== null && t !== '');
  if (tokenViejo && !leerStorage(CLAVE)) {
    escribirStorage(CLAVE, JSON.stringify({ token: tokenViejo, usuario: null }));
  }
  CLAVES_VIEJAS.forEach((clave) => escribirStorage(clave, null));
}

function cargarInicial(): EstadoSesion {
  migrarClavesViejas();
  const sesion = parsearSesion(leerStorage(CLAVE));
  if (sesion && caducada(sesion)) {
    escribirStorage(CLAVE, null);
    return { sesion: null, motivoCierre: 'expirada' };
  }
  return { sesion, motivoCierre: null };
}

// El objeto se reemplaza (nunca se muta) para que useSyncExternalStore detecte cambios.
let estado: EstadoSesion = cargarInicial();
const oyentes = new Set<Oyente>();
let temporizador: ReturnType<typeof setTimeout> | undefined;

function emitir(nuevo: EstadoSesion): void {
  estado = nuevo;
  programarCaducidad();
  oyentes.forEach((oyente) => oyente());
}

/** setTimeout no admite más de ~24,8 días; si el token dura más, se revisa al recargar. */
const MAX_TIMEOUT = 2_147_483_647;

function programarCaducidad(): void {
  clearTimeout(temporizador);
  const expiraEn = estado.sesion?.expiraEn;
  if (expiraEn == null) return;
  const restante = expiraEn - Date.now();
  if (restante > MAX_TIMEOUT) return;
  temporizador = setTimeout(() => limpiarSesion('expirada'), Math.max(0, restante));
}

export function obtenerEstadoSesion(): EstadoSesion {
  return estado;
}

export function obtenerToken(): string | null {
  return estado.sesion?.token ?? null;
}

export function guardarSesion(token: string, usuario: Usuario): void {
  escribirStorage(CLAVE, JSON.stringify({ token, usuario }));
  emitir({ sesion: { token, usuario, expiraEn: leerExpiracion(token) }, motivoCierre: null });
}

export function limpiarSesion(motivo: Exclude<MotivoCierre, null>): void {
  escribirStorage(CLAVE, null);
  if (estado.sesion === null && estado.motivoCierre === motivo) return;
  emitir({ sesion: null, motivoCierre: motivo });
}

export function suscribirSesion(oyente: Oyente): () => void {
  oyentes.add(oyente);
  return () => {
    oyentes.delete(oyente);
  };
}

// Otra pestaña inició o cerró sesión: se refleja aquí sin recargar.
window.addEventListener('storage', (evento) => {
  if (evento.key !== CLAVE && evento.key !== null) return;
  const sesion = parsearSesion(leerStorage(CLAVE));
  if (sesion && !caducada(sesion)) {
    emitir({ sesion, motivoCierre: null });
  } else if (estado.sesion !== null) {
    emitir({ sesion: null, motivoCierre: sesion ? 'expirada' : 'manual' });
  }
});

programarCaducidad();
