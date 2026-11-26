/** Partes de la fecha de un evento para las tarjetas (en hora del festival, igual que lib/formato.ts). */
const ZONA_FESTIVAL = 'America/Mexico_City';

const formatoDia = new Intl.DateTimeFormat('es-MX', { day: 'numeric', timeZone: ZONA_FESTIVAL });
const formatoMes = new Intl.DateTimeFormat('es-MX', { month: 'short', timeZone: ZONA_FESTIVAL });
const formatoHora = new Intl.DateTimeFormat('es-MX', {
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: ZONA_FESTIVAL,
});

/** { dia: "25", mes: "NOV" }; null si la fecha no es válida. */
export function diaYMes(iso: string): { dia: string; mes: string } | null {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return null;
  return { dia: formatoDia.format(fecha), mes: formatoMes.format(fecha).replace('.', '').toUpperCase() };
}

/** "20:00"; cadena vacía si la fecha no es válida. */
export function horaDeEvento(iso: string): string {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime()) ? '' : formatoHora.format(fecha);
}
