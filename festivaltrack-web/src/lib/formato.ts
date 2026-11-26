import type { CategoriaBoleto, EstadoBoleto } from '../services/tipos';

/** Los eventos ocurren en Dolores Hidalgo: se muestran en hora del centro de México sin importar dónde esté el visitante. */
const ZONA_FESTIVAL = 'America/Mexico_City';

const formatoFechaEvento = new Intl.DateTimeFormat('es-MX', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: ZONA_FESTIVAL,
});

const formatoPesos = new Intl.NumberFormat('es-MX', {
  style: 'currency',
  currency: 'MXN',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

const formatoKm = new Intl.NumberFormat('es-MX', { maximumFractionDigits: 1 });

/** "miércoles, 25 de noviembre, 20:00" (hora del festival); cadena vacía si la fecha no es válida. */
export function formatearFechaEvento(iso: string): string {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime()) ? '' : formatoFechaEvento.format(fecha);
}

/** "$4,500 MXN" */
export function formatearPrecio(monto: number): string {
  return `${formatoPesos.format(monto)} MXN`;
}

/** "850 m" o "1.2 km" */
export function formatearDistancia(metros: number): string {
  return metros < 1000 ? `${Math.round(metros)} m` : `${formatoKm.format(metros / 1000)} km`;
}

/** Minutos redondeados, mínimo 1. */
export function formatearDuracion(segundos: number): string {
  const minutos = Math.max(1, Math.round(segundos / 60));
  if (minutos < 60) return `${minutos} min`;
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return resto ? `${horas} h ${resto} min` : `${horas} h`;
}

/** Folio corto para mostrar: el id completo permite reconstruir el QR, así que no se enseña. */
export function folioCorto(id: string): string {
  return id.slice(-6).toUpperCase();
}

export const ETIQUETA_CATEGORIA: Record<CategoriaBoleto, string> = {
  VIP: 'VIP',
  GENERAL: 'General',
  ESTUDIANTE: 'Estudiante',
};

export const ETIQUETA_ESTADO_BOLETO: Record<EstadoBoleto, string> = {
  ACTIVO: 'Activo',
  USADO: 'Usado',
  CANCELADO: 'Cancelado',
  EXPIRADO: 'Expirado',
};
