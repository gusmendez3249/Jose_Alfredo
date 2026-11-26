import { Badge } from '../../ui';
import type { EstadoEvento } from '../../../services/tipos';
import { ETIQUETA_ESTADO, TONO_ESTADO } from './estados';

export function EstadoConcierto({ estado }: { estado: EstadoEvento }) {
  return <Badge tono={TONO_ESTADO[estado]}>{ETIQUETA_ESTADO[estado]}</Badge>;
}
