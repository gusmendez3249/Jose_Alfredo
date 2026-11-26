import { http } from '../../../lib/http';
import { esObjeto, RespuestaInvalidaError } from '../../../services/tipos';

// Lecturas públicas del resumen. Se mantienen aquí, sin depender de los servicios de otras secciones.

export interface PublicacionResumen {
  id: string;
  titulo: string | null;
  contenido: string;
  autorNombre: string;
  createdAt: string;
  likes: number;
}

export interface EstadoTransmision {
  isLive: boolean;
  eventoNombre: string | null;
}

function esPublicacion(v: unknown): v is PublicacionResumen {
  return (
    esObjeto(v) &&
    typeof v.id === 'string' &&
    (v.titulo === null || v.titulo === undefined || typeof v.titulo === 'string') &&
    typeof v.contenido === 'string' &&
    typeof v.autorNombre === 'string' &&
    typeof v.createdAt === 'string' &&
    typeof v.likes === 'number'
  );
}

export async function ultimasPublicaciones(signal?: AbortSignal): Promise<PublicacionResumen[]> {
  const { data } = await http.get<unknown>('/foro', { params: { limit: 3 }, signal });
  if (!Array.isArray(data)) throw new RespuestaInvalidaError('foro');
  return data.filter(esPublicacion).slice(0, 3);
}

export async function estadoTransmision(signal?: AbortSignal): Promise<EstadoTransmision> {
  const { data } = await http.get<unknown>('/stream/status', { signal });
  if (!esObjeto(data) || typeof data.isLive !== 'boolean') throw new RespuestaInvalidaError('transmisión');
  return { isLive: data.isLive, eventoNombre: typeof data.eventoNombre === 'string' ? data.eventoNombre : null };
}

/** "hace 5 min", "hace 2 h", "hace 3 d". */
export function haceTiempo(iso: string, ahora = Date.now()): string {
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return '';
  const min = Math.max(0, Math.floor((ahora - t) / 60_000));
  if (min < 1) return 'hace un momento';
  if (min < 60) return `hace ${min} min`;
  const horas = Math.floor(min / 60);
  if (horas < 24) return `hace ${horas} h`;
  return `hace ${Math.floor(horas / 24)} d`;
}
