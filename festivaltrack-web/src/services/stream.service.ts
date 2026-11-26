import { http } from '../lib/http';
import { RespuestaInvalidaError, esObjeto } from './tipos';

/**
 * Estado de la transmisión. El backend también manda `streamUrl` y `emulatorUrl` (RTSP en una IP
 * privada): ningún navegador puede reproducirlos, así que se descartan a propósito.
 */
export interface EstadoTransmision {
  isLive: boolean;
  eventoId: string | null;
  eventoNombre: string | null;
}

export interface MensajeChat {
  id: string;
  eventoId: string;
  usuarioNombre: string;
  mensaje: string;
  esAdmin: boolean;
  fechaEnvio: string;
}

export const MAX_CARACTERES_CHAT = 300;
/** Tope de mensajes en memoria: el servidor solo manda los últimos 50 en cada consulta. */
const MAX_MENSAJES_EN_MEMORIA = 200;

const textoONulo = (v: unknown): string | null => (typeof v === 'string' && v.trim() ? v : null);

export function normalizarEstado(v: unknown): EstadoTransmision | null {
  if (!esObjeto(v) || typeof v.isLive !== 'boolean') return null;
  return { isLive: v.isLive, eventoId: textoONulo(v.eventoId), eventoNombre: textoONulo(v.eventoNombre) };
}

export function esMensajeChat(v: unknown): v is MensajeChat {
  return (
    esObjeto(v) &&
    typeof v.id === 'string' &&
    v.id !== '' &&
    typeof v.eventoId === 'string' &&
    typeof v.usuarioNombre === 'string' &&
    typeof v.mensaje === 'string' &&
    typeof v.esAdmin === 'boolean' &&
    typeof v.fechaEnvio === 'string' &&
    !Number.isNaN(Date.parse(v.fechaEnvio))
  );
}

const marca = (m: MensajeChat) => Date.parse(m.fechaEnvio);

/**
 * Une los mensajes ya mostrados con los recién llegados: sin repetidos (por `id`; gana el más
 * reciente de la lista) y en orden cronológico. Devuelve la misma referencia si no cambió nada,
 * para no volver a pintar el chat en cada sondeo.
 */
export function fusionarMensajes(actuales: MensajeChat[], nuevos: MensajeChat[]): MensajeChat[] {
  const conocidos = new Set(actuales.map((m) => m.id));
  const añadidos = nuevos.filter((m, i) => !conocidos.has(m.id) && nuevos.findIndex((o) => o.id === m.id) === i);
  if (añadidos.length === 0) return actuales;
  // `sort` es estable: con la misma fecha se conserva el orden de llegada.
  const todos = [...actuales, ...añadidos].sort((a, b) => marca(a) - marca(b));
  return todos.slice(-MAX_MENSAJES_EN_MEMORIA);
}

export const streamService = {
  async estado(signal?: AbortSignal): Promise<EstadoTransmision> {
    const { data } = await http.get<unknown>('/stream/status', { signal });
    const estado = normalizarEstado(data);
    if (!estado) throw new RespuestaInvalidaError('estado de la transmisión');
    return estado;
  },

  /** Mensajes en orden cronológico (el servidor los manda del más nuevo al más viejo). */
  async chat(eventoId: string, signal?: AbortSignal): Promise<MensajeChat[]> {
    const { data } = await http.get<unknown>(`/stream/chat/${encodeURIComponent(eventoId)}`, { signal });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('chat');
    return data.filter(esMensajeChat).reverse();
  },

  async enviar(eventoId: string, mensaje: string): Promise<MensajeChat> {
    const { data } = await http.post<unknown>('/stream/chat', { eventoId, mensaje });
    if (!esMensajeChat(data)) throw new RespuestaInvalidaError('mensaje de chat');
    return data;
  },
};
