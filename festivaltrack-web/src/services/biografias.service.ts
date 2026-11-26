import { http } from '../lib/http';
import { esObjeto, RespuestaInvalidaError } from './tipos';

export interface Hito {
  ano: string | null;
  titulo: string;
  descripcion: string | null;
}

export interface Biografia {
  id: string;
  artistaId: string;
  descripcion: string;
  citaCelebre: string | null;
  hitos: Hito[];
  /** Títulos de las grabaciones (texto plano). */
  discografia: string[];
  artista: { id: string; nombre: string; imagenUrl: string | null };
}

const MAX_ELEMENTOS = 100;

/** El campo llega como texto con JSON dentro (datos del seed) o como arreglo (altas nuevas). */
function comoArreglo(valor: unknown): unknown[] {
  if (Array.isArray(valor)) return valor;
  if (typeof valor !== 'string') return [];
  try {
    const analizado: unknown = JSON.parse(valor);
    return Array.isArray(analizado) ? analizado : [];
  } catch {
    return [];
  }
}

function texto(v: unknown): string | null {
  if (typeof v === 'string') return v.trim() || null;
  if (typeof v === 'number' && Number.isFinite(v)) return String(v);
  return null;
}

export function normalizarHitos(valor: unknown): Hito[] {
  const hitos: Hito[] = [];
  for (const el of comoArreglo(valor).slice(0, MAX_ELEMENTOS)) {
    if (typeof el === 'string') {
      const titulo = texto(el);
      if (titulo) hitos.push({ ano: null, titulo, descripcion: null });
    } else if (esObjeto(el)) {
      const titulo = texto(el.titulo);
      if (titulo) hitos.push({ ano: texto(el.ano), titulo, descripcion: texto(el.descripcion) });
    }
  }
  return hitos;
}

export function normalizarDiscografia(valor: unknown): string[] {
  const titulos: string[] = [];
  for (const el of comoArreglo(valor).slice(0, MAX_ELEMENTOS)) {
    const t = typeof el === 'string' ? texto(el) : esObjeto(el) ? (texto(el.titulo) ?? texto(el.nombre)) : null;
    if (t) titulos.push(t);
  }
  return titulos;
}

function aBiografia(v: unknown): Biografia | null {
  if (!esObjeto(v) || typeof v.id !== 'string' || typeof v.descripcion !== 'string') return null;
  const a = esObjeto(v.artista) ? v.artista : null;
  return {
    id: v.id,
    artistaId: typeof v.artistaId === 'string' ? v.artistaId : '',
    descripcion: v.descripcion,
    citaCelebre: texto(v.citaCelebre),
    hitos: normalizarHitos(v.hitos),
    discografia: normalizarDiscografia(v.discografia),
    artista: {
      id: a && typeof a.id === 'string' ? a.id : '',
      nombre: a && typeof a.nombre === 'string' ? a.nombre : 'José Alfredo Jiménez',
      imagenUrl: a ? texto(a.imagenUrl) : null,
    },
  };
}

export const biografiasService = {
  /** Biografía principal (la primera que devuelve la API, como en el móvil) o null si no hay. Público. */
  async principal(signal?: AbortSignal): Promise<Biografia | null> {
    const { data } = await http.get<unknown>('/biografias', { signal });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('biografías');
    for (const el of data) {
      const b = aBiografia(el);
      if (b) return b;
    }
    return null;
  },
};
