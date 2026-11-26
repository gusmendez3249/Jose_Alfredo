import { http } from '../lib/http';
import { RespuestaInvalidaError, type EstadoEvento, type Evento } from './tipos';
import { esEvento, normalizarEvento } from './validacion';

/** El backend solo vende boletos de eventos en estos estados (boletos.service.ts). */
const ESTADOS_A_LA_VENTA: readonly EstadoEvento[] = ['PUBLICADO', 'EN_CURSO'];

export function estaALaVenta(evento: Evento): boolean {
  return ESTADOS_A_LA_VENTA.includes(evento.estado);
}

export function tieneCoordenadas(evento: Evento): evento is Evento & { latitud: number; longitud: number } {
  return evento.latitud != null && evento.longitud != null;
}

/** Escenario si lo hay; si no, la ubicación; null si faltan ambos. */
export function lugarDe(evento: Evento): string | null {
  const lugar = (evento.escenario ?? '').trim() || (evento.ubicacion ?? '').trim();
  return lugar || null;
}

export const eventosService = {
  async listar(signal?: AbortSignal): Promise<Evento[]> {
    const { data } = await http.get<unknown>('/eventos', { signal });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('eventos');
    // Un elemento con forma rara se descarta en lugar de romper toda la cartelera.
    return data.filter(esEvento).map(normalizarEvento);
  },

  async porId(id: string, signal?: AbortSignal): Promise<Evento> {
    const { data } = await http.get<unknown>(`/eventos/${encodeURIComponent(id)}`, { signal });
    if (!esEvento(data)) throw new RespuestaInvalidaError('evento');
    return normalizarEvento(data);
  },
};
