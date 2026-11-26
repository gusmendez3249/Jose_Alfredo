// Tipos que reflejan las respuestas reales del backend (Prisma + NestJS).
// Fechas: ISO 8601 en texto, tal como llegan en el JSON.

export type Rol = 'USUARIO' | 'ADMINISTRADOR';

export interface Usuario {
  id: string;
  nombre: string;
  correo: string;
  rol: Rol;
}

/** Respuesta de POST /auth/login y POST /auth/register. */
export interface RespuestaAuth {
  accessToken: string;
  usuario: Usuario;
}

export type EstadoEvento = 'BORRADOR' | 'PUBLICADO' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO';

export interface Artista {
  id: string;
  nombre: string;
  imagenUrl: string | null;
}

export interface Evento {
  id: string;
  nombre: string;
  fechaHora: string;
  ubicacion: string;
  escenario: string | null;
  descripcion: string | null;
  capacidad: number;
  bannerUrl: string | null;
  estado: EstadoEvento;
  artistaId: string | null;
  latitud: number | null;
  longitud: number | null;
  /** GET /eventos y GET /eventos/:id lo incluyen; en /boletos/mis-boletos no viene. */
  artista?: Artista | null;
}

export type CategoriaBoleto = 'VIP' | 'GENERAL' | 'ESTUDIANTE';
export type EstadoBoleto = 'ACTIVO' | 'USADO' | 'CANCELADO' | 'EXPIRADO';
export type MetodoPago = 'TARJETA_CREDITO' | 'TARJETA_DEBITO' | 'TRANSFERENCIA' | 'PAYPAL';

export interface Boleto {
  id: string;
  eventoId: string;
  usuarioId: string;
  categoria: CategoriaBoleto;
  /** Precio total cobrado por este registro (precio oficial × cantidad pedida). */
  precio: number;
  fechaCompra: string;
  codigoQR: string;
  estado: EstadoBoleto;
}

/** Elemento de GET /boletos/mis-boletos. */
export interface BoletoConEvento extends Boleto {
  evento: Evento | null;
}

/** Cuerpo de POST /boletos/comprar (ComprarBoletoDto). */
export interface CompraBoleto {
  eventoId: string;
  categoria: CategoriaBoleto;
  cantidad: number;
  metodoPago: MetodoPago;
}

/** El servidor respondió 2xx pero con una forma que la web no sabe usar. */
export class RespuestaInvalidaError extends Error {
  constructor(recurso: string) {
    super(`Respuesta inesperada del servidor (${recurso})`);
    this.name = 'RespuestaInvalidaError';
  }
}

export function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null && !Array.isArray(valor);
}
