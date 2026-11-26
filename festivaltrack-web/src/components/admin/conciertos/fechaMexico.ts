/**
 * Conversión entre la fecha y hora que escribe el administrador (siempre hora del centro de
 * México) y el instante UTC que espera el servidor. No usa ninguna zona del navegador ni un
 * desfase fijo: el desfase de cada instante se deriva con Intl, así que también es correcto
 * para fechas con horario de verano (México lo abolió en 2022).
 */
export const ZONA_MEXICO = 'America/Mexico_City';

const formatoPartes = new Intl.DateTimeFormat('en-US', {
  timeZone: ZONA_MEXICO,
  hourCycle: 'h23',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
});

interface Partes {
  anio: number;
  mes: number;
  dia: number;
  hora: number;
  minuto: number;
  segundo: number;
}

function partesEnMexico(instante: number): Partes {
  const valores: Record<string, number> = {};
  for (const { type, value } of formatoPartes.formatToParts(new Date(instante))) {
    if (type !== 'literal') valores[type] = Number(value);
  }
  return {
    anio: valores.year,
    mes: valores.month,
    dia: valores.day,
    // Algunos motores devuelven 24 a medianoche aun con h23.
    hora: valores.hour === 24 ? 0 : valores.hour,
    minuto: valores.minute,
    segundo: valores.second,
  };
}

/** Milisegundos que la hora de México va por delante de UTC en ese instante (negativo hoy: -6 h). */
function desfaseEn(instante: number): number {
  const p = partesEnMexico(instante);
  return Date.UTC(p.anio, p.mes - 1, p.dia, p.hora, p.minuto, p.segundo) - Math.floor(instante / 1000) * 1000;
}

const PATRON_FECHA = /^(\d{4})-(\d{2})-(\d{2})$/;
const PATRON_HORA = /^(\d{2}):(\d{2})$/;

/**
 * "2026-11-25" + "20:00" (hora de México) → instante en milisegundos UTC.
 * null si el texto no es una fecha/hora real o si esa hora no existió en México (salto de horario).
 */
export function instanteDeMexico(fecha: string, hora: string): number | null {
  const f = PATRON_FECHA.exec(fecha);
  const h = PATRON_HORA.exec(hora);
  if (!f || !h) return null;
  const [anio, mes, dia, horas, minutos] = [f[1], f[2], f[3], h[1], h[2]].map(Number);
  const local = Date.UTC(anio, mes - 1, dia, horas, minutos);
  const real = new Date(local);
  // Rechaza 31 de febrero, hora 25, etc. (Date.UTC los desborda sin avisar).
  if (
    real.getUTCFullYear() !== anio ||
    real.getUTCMonth() !== mes - 1 ||
    real.getUTCDate() !== dia ||
    real.getUTCHours() !== horas ||
    real.getUTCMinutes() !== minutos
  ) {
    return null;
  }
  // Dos pasadas: la segunda corrige el desfase cuando el primer cálculo cae al otro lado de un cambio de horario.
  const primero = local - desfaseEn(local);
  const instante = local - desfaseEn(primero);
  const p = partesEnMexico(instante);
  const coincide =
    p.anio === anio && p.mes === mes && p.dia === dia && p.hora === horas && p.minuto === minutos;
  return coincide ? instante : null;
}

/** "2026-11-25" + "20:00" (hora de México) → "2026-11-26T02:00:00.000Z"; null si no es válida. */
export function mexicoAIso(fecha: string, hora: string): string | null {
  const instante = instanteDeMexico(fecha, hora);
  return instante === null ? null : new Date(instante).toISOString();
}

const dos = (n: number) => String(n).padStart(2, '0');

/** Instante ISO del servidor → { fecha: "2026-11-25", hora: "20:00" } en hora de México; null si no se puede leer. */
export function isoAMexico(iso: string): { fecha: string; hora: string } | null {
  const instante = new Date(iso).getTime();
  if (Number.isNaN(instante)) return null;
  const p = partesEnMexico(instante);
  return { fecha: `${p.anio}-${dos(p.mes)}-${dos(p.dia)}`, hora: `${dos(p.hora)}:${dos(p.minuto)}` };
}

const formatoLista = new Intl.DateTimeFormat('es-MX', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: ZONA_MEXICO,
});

/** "mié, 25 nov 2026, 20:00" en hora de México; cadena vacía si la fecha no es válida. */
export function formatearFechaLista(iso: string): string {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime()) ? '' : formatoLista.format(fecha);
}
