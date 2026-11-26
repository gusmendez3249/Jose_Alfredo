import {
  esObjeto,
  type Boleto,
  type BoletoConEvento,
  type CategoriaBoleto,
  type EstadoBoleto,
  type EstadoEvento,
  type Evento,
  type RespuestaAuth,
  type Rol,
} from './tipos';

// Comprobaciones mínimas de forma: lo justo para que la interfaz no reviente si el backend
// cambia o responde algo inesperado (p. ej. una página HTML de error con status 200).

const ESTADOS_EVENTO: readonly EstadoEvento[] = ['BORRADOR', 'PUBLICADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'];
const CATEGORIAS: readonly CategoriaBoleto[] = ['VIP', 'GENERAL', 'ESTUDIANTE'];
const ESTADOS_BOLETO: readonly EstadoBoleto[] = ['ACTIVO', 'USADO', 'CANCELADO', 'EXPIRADO'];
const ROLES: readonly Rol[] = ['USUARIO', 'ADMINISTRADOR'];

function esUno<T extends string>(valor: unknown, opciones: readonly T[]): valor is T {
  return typeof valor === 'string' && (opciones as readonly string[]).includes(valor);
}

const esTexto = (v: unknown): v is string => typeof v === 'string';
const esTextoONulo = (v: unknown): v is string | null => v === null || v === undefined || typeof v === 'string';
const esNumeroONulo = (v: unknown): v is number | null =>
  v === null || v === undefined || (typeof v === 'number' && Number.isFinite(v));

export function esEvento(v: unknown): v is Evento {
  return (
    esObjeto(v) &&
    esTexto(v.id) &&
    esTexto(v.nombre) &&
    esTexto(v.fechaHora) &&
    esTextoONulo(v.ubicacion) &&
    esTextoONulo(v.escenario) &&
    esUno(v.estado, ESTADOS_EVENTO) &&
    esNumeroONulo(v.latitud) &&
    esNumeroONulo(v.longitud) &&
    (v.artista === undefined || v.artista === null || (esObjeto(v.artista) && esTexto(v.artista.nombre)))
  );
}

/** Normaliza campos opcionales a null para que el resto del código solo compare con null. */
export function normalizarEvento(e: Evento): Evento {
  return {
    ...e,
    ubicacion: e.ubicacion ?? '',
    escenario: e.escenario ?? null,
    descripcion: e.descripcion ?? null,
    bannerUrl: e.bannerUrl ?? null,
    artistaId: e.artistaId ?? null,
    latitud: e.latitud ?? null,
    longitud: e.longitud ?? null,
    artista: e.artista ?? null,
  };
}

export function esBoleto(v: unknown): v is Boleto {
  return (
    esObjeto(v) &&
    esTexto(v.id) &&
    esTexto(v.eventoId) &&
    esUno(v.categoria, CATEGORIAS) &&
    typeof v.precio === 'number' &&
    esTexto(v.codigoQR) &&
    esUno(v.estado, ESTADOS_BOLETO)
  );
}

export function esBoletoConEvento(v: unknown): v is BoletoConEvento {
  return esBoleto(v) && esObjeto(v) && (v.evento === null || v.evento === undefined || esEvento(v.evento));
}

export function esRespuestaAuth(v: unknown): v is RespuestaAuth {
  return (
    esObjeto(v) &&
    esTexto(v.accessToken) &&
    v.accessToken !== '' &&
    esObjeto(v.usuario) &&
    esTexto(v.usuario.id) &&
    esTexto(v.usuario.nombre) &&
    esTexto(v.usuario.correo) &&
    esUno(v.usuario.rol, ROLES)
  );
}
