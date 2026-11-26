import { http } from '../lib/http';
import type { Usuario } from './tipos';
import { esObjeto, RespuestaInvalidaError } from './tipos';

// Foro de la comunidad (la "Galería interactiva" del móvil). Contrato: festivaltrack-backend/src/foro.

export type TipoPublicacion = 'EXPERIENCIA' | 'RECOMENDACION' | 'FOTO';

export const TIPOS_PUBLICACION: readonly TipoPublicacion[] = ['EXPERIENCIA', 'RECOMENDACION', 'FOTO'];

export interface Publicacion {
  id: string;
  tipo: TipoPublicacion;
  titulo: string | null;
  /** '' si la publicación es solo una foto. */
  contenido: string;
  /** "/uploads/..." o cualquier "https://...": pásala SIEMPRE por `urlMedia()`. */
  imagenUrl: string | null;
  lugar: string | null;
  createdAt: string;
  autorId: string;
  autorNombre: string;
  likes: number;
  comentarios: number;
  /** Siempre false si la petición no llevó un token válido. */
  likedByMe: boolean;
}

export interface ComentarioForo {
  id: string;
  texto: string;
  createdAt: string;
  autorId: string;
  autorNombre: string;
}

export interface NuevaPublicacion {
  tipo: TipoPublicacion;
  titulo?: string;
  contenido?: string;
  /** Solo rutas "/uploads/..." devueltas por `subirImagen`: la web no publica URLs externas. */
  imagenUrl?: string;
  lugar?: string;
}

export interface EstadoLike {
  liked: boolean;
  likes: number;
}

export interface PaginaForo {
  publicaciones: Publicacion[];
  /** `createdAt` con el que pedir la página siguiente; null si ya no hay más. */
  siguienteCursor: string | null;
}

/** Los mismos límites que aplica el servidor (foro.service.ts del backend). */
export const LIMITES_FORO = {
  contenido: 1000,
  comentario: 400,
  titulo: 80,
  lugar: 60,
} as const;

export const TAMANO_PAGINA = 20;

/** Lo que acepta `POST /upload`. */
export const TAMANO_MAX_IMAGEN = 8 * 1024 * 1024;
export const TIPOS_IMAGEN: readonly string[] = ['image/jpeg', 'image/png', 'image/webp'];

const RUTA_SUBIDA = /^\/uploads\/[\w.-]+$/;

const esTexto = (v: unknown): v is string => typeof v === 'string';
const esTextoONulo = (v: unknown): boolean => v === null || v === undefined || typeof v === 'string';
const esConteo = (v: unknown): v is number => typeof v === 'number' && Number.isFinite(v) && v >= 0;
const esFecha = (v: unknown): v is string => esTexto(v) && !Number.isNaN(new Date(v).getTime());

function esTipo(v: unknown): v is TipoPublicacion {
  return esTexto(v) && (TIPOS_PUBLICACION as readonly string[]).includes(v);
}

/** Valida y normaliza una publicación; null si la forma no sirve (se descarta, no rompe la lista). */
export function aPublicacion(v: unknown): Publicacion | null {
  if (
    !esObjeto(v) ||
    !esTexto(v.id) ||
    !esTipo(v.tipo) ||
    !esTextoONulo(v.titulo) ||
    !esTextoONulo(v.contenido) ||
    !esTextoONulo(v.imagenUrl) ||
    !esTextoONulo(v.lugar) ||
    !esFecha(v.createdAt) ||
    !esTexto(v.autorId) ||
    !esTexto(v.autorNombre) ||
    !esConteo(v.likes) ||
    !esConteo(v.comentarios)
  ) {
    return null;
  }
  return {
    id: v.id,
    tipo: v.tipo,
    titulo: esTexto(v.titulo) && v.titulo.trim() ? v.titulo : null,
    contenido: esTexto(v.contenido) ? v.contenido : '',
    imagenUrl: esTexto(v.imagenUrl) && v.imagenUrl.trim() ? v.imagenUrl : null,
    lugar: esTexto(v.lugar) && v.lugar.trim() ? v.lugar : null,
    createdAt: v.createdAt,
    autorId: v.autorId,
    autorNombre: v.autorNombre,
    likes: v.likes,
    comentarios: v.comentarios,
    likedByMe: v.likedByMe === true,
  };
}

export function aComentario(v: unknown): ComentarioForo | null {
  if (!esObjeto(v) || !esTexto(v.id) || !esTexto(v.texto) || !esFecha(v.createdAt) || !esTexto(v.autorId) || !esTexto(v.autorNombre)) {
    return null;
  }
  return { id: v.id, texto: v.texto, createdAt: v.createdAt, autorId: v.autorId, autorNombre: v.autorNombre };
}

function obligatorio<T>(valor: T | null, recurso: string): T {
  if (valor === null) throw new RespuestaInvalidaError(recurso);
  return valor;
}

/** Une una página nueva a la lista sin repetir ids (el orden es el de llegada). */
export function fusionarPaginas(actuales: readonly Publicacion[], nuevas: readonly Publicacion[]): Publicacion[] {
  const vistos = new Set(actuales.map((p) => p.id));
  const resultado = [...actuales];
  for (const p of nuevas) {
    if (!vistos.has(p.id)) {
      vistos.add(p.id);
      resultado.push(p);
    }
  }
  return resultado;
}

/** El servidor deja borrar al autor y a cualquier administrador. */
export function puedeBorrar(usuario: Usuario | null, autorId: string): boolean {
  return usuario !== null && (usuario.id === autorId || usuario.rol === 'ADMINISTRADOR');
}

/** Mensaje de error si el archivo no se puede subir; null si es válido. */
export function validarImagen(archivo: File): string | null {
  if (!TIPOS_IMAGEN.includes(archivo.type)) return 'Solo se permiten imágenes JPG, PNG o WEBP.';
  if (archivo.size > TAMANO_MAX_IMAGEN) return 'La foto pesa más de 8 MB. Elige una más ligera.';
  if (archivo.size === 0) return 'El archivo está vacío.';
  return null;
}

/** "ahora", "hace 5 min", "hace 3 h", "hace 2 d", "hace 1 sem", "hace 4 meses" (como el móvil). */
export function tiempoRelativo(iso: string, ahora: number = Date.now()): string {
  const momento = new Date(iso).getTime();
  if (Number.isNaN(momento)) return '';
  const segundos = Math.floor((ahora - momento) / 1000);
  if (segundos < 60) return 'ahora';
  const minutos = Math.floor(segundos / 60);
  if (minutos < 60) return `hace ${minutos} min`;
  const horas = Math.floor(minutos / 60);
  if (horas < 24) return `hace ${horas} h`;
  const dias = Math.floor(horas / 24);
  if (dias < 7) return `hace ${dias} d`;
  if (dias < 30) return `hace ${Math.floor(dias / 7)} sem`;
  if (dias < 365) {
    const meses = Math.floor(dias / 30);
    return `hace ${meses} ${meses === 1 ? 'mes' : 'meses'}`;
  }
  const anios = Math.floor(dias / 365);
  return `hace ${anios} ${anios === 1 ? 'año' : 'años'}`;
}

export interface OpcionesListado {
  tipo?: TipoPublicacion | null;
  before?: string | null;
  limit?: number;
}

export const foroService = {
  /** Público; con sesión trae `likedByMe`. Más recientes primero, paginado por cursor. */
  async listar({ tipo, before, limit = TAMANO_PAGINA }: OpcionesListado, signal?: AbortSignal): Promise<PaginaForo> {
    const params: Record<string, string> = { limit: String(limit) };
    if (tipo) params.tipo = tipo;
    if (before) params.before = before;
    const { data } = await http.get<unknown>('/foro', { params, signal });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('foro');

    const publicaciones: Publicacion[] = [];
    for (const elemento of data) {
      const p = aPublicacion(elemento);
      if (p) publicaciones.push(p);
    }
    // El cursor sale del último elemento recibido aunque se haya descartado por su forma,
    // para no volver a pedirlo; si ni su fecha sirve, del último válido.
    const ultimo: unknown = data[data.length - 1];
    const fechaUltimo = esObjeto(ultimo) && esFecha(ultimo.createdAt) ? ultimo.createdAt : null;
    const cursor = fechaUltimo ?? publicaciones[publicaciones.length - 1]?.createdAt ?? null;
    return { publicaciones, siguienteCursor: data.length < limit ? null : cursor };
  },

  async crear(datos: NuevaPublicacion): Promise<Publicacion> {
    // El servidor no valida tipos: se mandan siempre textos y se omiten los vacíos.
    const cuerpo: Record<string, string> = { tipo: datos.tipo, contenido: (datos.contenido ?? '').trim() };
    const titulo = datos.titulo?.trim();
    const lugar = datos.lugar?.trim();
    if (titulo) cuerpo.titulo = titulo.slice(0, LIMITES_FORO.titulo);
    if (lugar) cuerpo.lugar = lugar.slice(0, LIMITES_FORO.lugar);
    if (datos.imagenUrl) cuerpo.imagenUrl = datos.imagenUrl;
    const { data } = await http.post<unknown>('/foro', cuerpo);
    return obligatorio(aPublicacion(data), 'publicación');
  },

  async eliminar(id: string): Promise<void> {
    await http.delete(`/foro/${encodeURIComponent(id)}`);
  },

  /** Alterna el me gusta y devuelve el estado final según el servidor. */
  async alternarLike(id: string): Promise<EstadoLike> {
    const { data } = await http.post<unknown>(`/foro/${encodeURIComponent(id)}/like`);
    if (!esObjeto(data) || typeof data.liked !== 'boolean' || !esConteo(data.likes)) {
      throw new RespuestaInvalidaError('me gusta');
    }
    return { liked: data.liked, likes: data.likes };
  },

  /** Público. Orden cronológico, máximo 200. */
  async comentarios(publicacionId: string, signal?: AbortSignal): Promise<ComentarioForo[]> {
    const { data } = await http.get<unknown>(`/foro/${encodeURIComponent(publicacionId)}/comentarios`, { signal });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('comentarios');
    const lista: ComentarioForo[] = [];
    for (const elemento of data) {
      const c = aComentario(elemento);
      if (c) lista.push(c);
    }
    return lista;
  },

  async comentar(publicacionId: string, texto: string): Promise<ComentarioForo> {
    const { data } = await http.post<unknown>(`/foro/${encodeURIComponent(publicacionId)}/comentarios`, {
      texto: texto.trim(),
    });
    return obligatorio(aComentario(data), 'comentario');
  },

  async eliminarComentario(id: string): Promise<void> {
    await http.delete(`/foro/comentarios/${encodeURIComponent(id)}`);
  },

  /**
   * Sube una foto (campo `file`) y devuelve su ruta "/uploads/...". El Content-Type se anula
   * para que el navegador ponga el boundary del multipart; sin timeout porque una foto grande
   * con mala señal puede tardar más de 60 s.
   */
  async subirImagen(archivo: File, alProgresar?: (fraccion: number) => void, signal?: AbortSignal): Promise<string> {
    const cuerpo = new FormData();
    cuerpo.append('file', archivo);
    const { data } = await http.post<unknown>('/upload', cuerpo, {
      headers: { 'Content-Type': undefined },
      timeout: 0,
      signal,
      onUploadProgress: (e) => {
        if (alProgresar && e.total) alProgresar(Math.min(1, e.loaded / e.total));
      },
    });
    if (!esObjeto(data) || typeof data.url !== 'string' || !RUTA_SUBIDA.test(data.url)) {
      throw new RespuestaInvalidaError('subida');
    }
    return data.url;
  },
};
