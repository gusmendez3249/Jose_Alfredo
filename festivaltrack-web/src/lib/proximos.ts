import { eventosService } from '../services/eventos.service';
import type { Evento } from '../services/tipos';

/** Un evento se muestra mientras no termine: hasta 4 h después de empezar (igual que en la app móvil). */
const DURACION_MS = 4 * 60 * 60 * 1000;
const NO_VISIBLES = new Set<Evento['estado']>(['FINALIZADO', 'CANCELADO', 'BORRADOR']);

/** Eventos que todavía no terminan, del más cercano al más lejano. */
export function proximos(eventos: Evento[], ahora = Date.now()): Evento[] {
  return eventos
    .filter((e) => !NO_VISIBLES.has(e.estado))
    .filter((e) => new Date(e.fechaHora).getTime() + DURACION_MS > ahora)
    .sort((a, b) => new Date(a.fechaHora).getTime() - new Date(b.fechaHora).getTime());
}

export async function cargarProximos(signal: AbortSignal): Promise<Evento[]> {
  return proximos(await eventosService.listar(signal));
}
