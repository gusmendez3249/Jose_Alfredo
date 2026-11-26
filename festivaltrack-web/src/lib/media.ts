import { API_URL } from '../config/env';

/**
 * Hosts externos desde los que se aceptan imágenes y audio. Debe coincidir con `img-src` y
 * `media-src` de la CSP (public/serve.json y render.yaml): un host fuera de la lista lo bloquearía
 * el navegador de todos modos, así que se sustituye por un marcador en lugar de pintarlo roto.
 */
const HOSTS_EXTERNOS: readonly string[] = ['images.unsplash.com', 'www.soundhelix.com'];

/** Rutas de archivos subidos tal como las guarda el backend. */
const RUTA_SUBIDA = /^\/uploads\/[\w.-]+$/;

function origenApi(): string | null {
  try {
    return new URL(API_URL).origin;
  } catch {
    return null;
  }
}

/**
 * Convierte una URL de imagen o audio que viene de la API en una URL segura para `src`.
 * El foro acepta cualquier https y los campos de administrador cualquier texto, así que aquí
 * solo pasan: archivos subidos (`/uploads/...`, que se sirven desde el origen del backend, sin
 * `/api/v1`) y URLs https de la lista permitida. Todo lo demás (http:, javascript:, data:, otros
 * hosts, vacío) devuelve null y quien llama muestra un marcador.
 */
export function urlMedia(valor: string | null | undefined): string | null {
  const texto = (valor ?? '').trim();
  if (!texto) return null;

  const origen = origenApi();
  if (RUTA_SUBIDA.test(texto)) return origen ? `${origen}${texto}` : null;

  let url: URL;
  try {
    url = new URL(texto);
  } catch {
    return null;
  }
  if (url.protocol !== 'https:' && url.origin !== origen) return null;
  if (url.origin === origen) return url.pathname.startsWith('/uploads/') ? url.href : null;
  return HOSTS_EXTERNOS.includes(url.hostname) ? url.href : null;
}
