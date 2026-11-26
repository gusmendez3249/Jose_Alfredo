import type { Evento } from '../../../services/tipos';

/** Lugar del festival donde puede haber un concierto, con sus coordenadas reales. */
export interface LugarFestival {
  escenario: string;
  ubicacion: string;
  latitud: number;
  longitud: number;
}

/**
 * Los mismos cuatro lugares que ofrece la app móvil (`FormularioEvento.LUGARES`) y que usa la
 * cartelera (`seed-cartelera.ts`). El escenario hace de identificador.
 */
export const LUGARES: readonly LugarFestival[] = [
  { escenario: 'Escenario Principal', ubicacion: 'Jardín Principal, Dolores Hidalgo', latitud: 21.1566, longitud: -100.9341 },
  { escenario: 'Plaza Principal', ubicacion: 'Jardín Principal, Dolores Hidalgo', latitud: 21.1563, longitud: -100.9338 },
  {
    escenario: 'Mausoleo de José Alfredo Jiménez',
    ubicacion: 'Panteón Municipal, Dolores Hidalgo',
    latitud: 21.1571,
    longitud: -100.9406,
  },
  {
    escenario: 'Casa Museo',
    ubicacion: 'Calle Guanajuato 13, Centro, Dolores Hidalgo',
    latitud: 21.1574,
    longitud: -100.933,
  },
];

/** Texto de una opción del menú: "{escenario} · {ubicación}". */
export function etiquetaDeLugar(lugar: LugarFestival): string {
  return `${lugar.escenario} · ${lugar.ubicacion}`;
}

export function lugarPorEscenario(escenario: string): LugarFestival | undefined {
  return LUGARES.find((l) => l.escenario === escenario);
}

/** Lugar conocido de un evento: por escenario o, si no coincide, por coordenadas cercanas (como el móvil). */
export function lugarDeEvento(evento: Pick<Evento, 'escenario' | 'latitud' | 'longitud'>): LugarFestival | undefined {
  const escenario = (evento.escenario ?? '').trim().toLowerCase();
  const porNombre = LUGARES.find((l) => l.escenario.toLowerCase() === escenario);
  if (porNombre) return porNombre;
  const { latitud, longitud } = evento;
  if (latitud == null || longitud == null) return undefined;
  return LUGARES.find((l) => Math.abs(l.latitud - latitud) < 0.0005 && Math.abs(l.longitud - longitud) < 0.0005);
}
