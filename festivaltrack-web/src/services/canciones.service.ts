import { http } from '../lib/http';
import { esObjeto, RespuestaInvalidaError } from './tipos';

export type EstadoCancion = 'BORRADOR' | 'PUBLICADA' | 'ARCHIVADA';

export interface Cancion {
  id: string;
  titulo: string;
  artista: string;
  /** Segundos. */
  duracion: number;
  /** Relativa ("/uploads/...") o absoluta: pásala por `urlMedia()` antes de usarla. */
  archivoUrl: string;
  portadaUrl: string | null;
  genero: string | null;
  estado: EstadoCancion;
  createdAt: string;
}

export interface NuevaCancion {
  titulo: string;
  artista?: string;
  /** Segundos, entero >= 1. */
  duracion?: number;
  archivoUrl?: string;
  portadaUrl?: string;
  genero?: string;
}

function esCancion(v: unknown): v is Cancion {
  return (
    esObjeto(v) &&
    typeof v.id === 'string' &&
    typeof v.titulo === 'string' &&
    typeof v.artista === 'string' &&
    typeof v.duracion === 'number' &&
    typeof v.archivoUrl === 'string'
  );
}

function listaDeCanciones(data: unknown): Cancion[] {
  if (!Array.isArray(data)) throw new RespuestaInvalidaError('canciones');
  return data.filter(esCancion);
}

export const cancionesService = {
  /** Catálogo completo (todos los estados), más recientes primero. Público. */
  async listar(signal?: AbortSignal): Promise<Cancion[]> {
    const { data } = await http.get<unknown>('/canciones', { signal });
    return listaDeCanciones(data);
  },

  /** Repertorio publicado de un concierto, en orden de reproducción. Público; [] si no tiene. */
  async deEvento(eventoId: string, signal?: AbortSignal): Promise<Cancion[]> {
    const { data } = await http.get<unknown>(`/canciones/evento/${encodeURIComponent(eventoId)}`, { signal });
    return listaDeCanciones(data);
  },

  /** Reemplaza el repertorio; el orden del arreglo es el orden de reproducción. Solo administrador. */
  async guardarRepertorio(eventoId: string, cancionIds: string[]): Promise<Cancion[]> {
    const { data } = await http.put<unknown>(`/canciones/evento/${encodeURIComponent(eventoId)}`, { cancionIds });
    return listaDeCanciones(data);
  },

  /**
   * Sube un audio o una portada (campo `file`, máx. 30 MB) y devuelve su ruta "/uploads/...".
   * Solo administrador. El Content-Type se anula porque el cliente fija JSON por defecto y
   * con FormData debe ponerlo el navegador (con su boundary).
   */
  async subirArchivo(archivo: File, alProgresar?: (fraccion: number) => void): Promise<string> {
    const cuerpo = new FormData();
    cuerpo.append('file', archivo);
    const { data } = await http.post<unknown>('/canciones/upload', cuerpo, {
      headers: { 'Content-Type': undefined },
      timeout: 0,
      onUploadProgress: (e) => {
        if (alProgresar && e.total) alProgresar(e.loaded / e.total);
      },
    });
    if (!esObjeto(data) || typeof data.url !== 'string') throw new RespuestaInvalidaError('subida');
    return data.url;
  },

  /** Da de alta la canción en el catálogo (queda PUBLICADA). Solo administrador. */
  async crear(datos: NuevaCancion): Promise<Cancion> {
    const { data } = await http.post<unknown>('/canciones', datos);
    if (!esCancion(data)) throw new RespuestaInvalidaError('canción');
    return data;
  },

  /** Solo administrador. El backend responde 500 si la canción está en algún repertorio. */
  async eliminar(id: string): Promise<void> {
    await http.delete(`/canciones/${encodeURIComponent(id)}`);
  },
};
