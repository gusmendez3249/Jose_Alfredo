import type { TonoBadge } from '../../ui/Badge';
import type { EstadoEvento } from '../../../services/tipos';

export const ETIQUETA_ESTADO: Record<EstadoEvento, string> = {
  BORRADOR: 'Borrador',
  PUBLICADO: 'Publicado',
  EN_CURSO: 'En curso',
  FINALIZADO: 'Finalizado',
  CANCELADO: 'Cancelado',
};

export const TONO_ESTADO: Record<EstadoEvento, TonoBadge> = {
  BORRADOR: 'neutra',
  PUBLICADO: 'exito',
  EN_CURSO: 'dorada',
  FINALIZADO: 'neutra',
  CANCELADO: 'error',
};

export const ESTADOS_EN_ORDEN: readonly EstadoEvento[] = ['BORRADOR', 'PUBLICADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'];

/** Un concierto ya cancelado o finalizado no se puede cancelar. */
export function sePuedeCancelar(estado: EstadoEvento): boolean {
  return estado !== 'CANCELADO' && estado !== 'FINALIZADO';
}
