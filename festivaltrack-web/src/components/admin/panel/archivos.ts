/** Reglas de archivo de POST /canciones/upload (canciones.controller.ts). */
export const MAX_BYTES = 30 * 1024 * 1024;

export type TipoArchivo = 'audio' | 'imagen';

const MIME_AUDIO = ['audio/mpeg', 'audio/mp3', 'audio/mp4', 'audio/x-m4a', 'audio/aac', 'audio/wav', 'audio/x-wav', 'audio/ogg'];
const MIME_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];

/** Algunos navegadores no informan el tipo de .m4a u .ogg: se completa por extensión. */
const POR_EXTENSION: Record<string, string> = {
  mp3: 'audio/mpeg',
  m4a: 'audio/x-m4a',
  aac: 'audio/aac',
  wav: 'audio/wav',
  ogg: 'audio/ogg',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
};

export const ACEPTA_AUDIO = '.mp3,.m4a,.aac,.wav,.ogg,audio/mpeg,audio/mp4,audio/x-m4a,audio/aac,audio/wav,audio/ogg';
export const ACEPTA_IMAGEN = '.jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp';

function tipoEfectivo(archivo: File): string {
  if (archivo.type) return archivo.type;
  const ext = archivo.name.split('.').pop()?.toLowerCase() ?? '';
  return POR_EXTENSION[ext] ?? '';
}

/** Devuelve el mensaje de error, o null si el archivo es válido. Se llama ANTES de subir. */
export function validarArchivo(archivo: File, tipo: TipoArchivo): string | null {
  const permitidos = tipo === 'audio' ? MIME_AUDIO : MIME_IMAGEN;
  if (!permitidos.includes(tipoEfectivo(archivo))) {
    return tipo === 'audio'
      ? 'Formato no permitido: usa MP3, M4A, WAV u OGG.'
      : 'Formato no permitido: usa JPG, PNG o WEBP.';
  }
  if (archivo.size > MAX_BYTES) {
    return tipo === 'audio'
      ? 'El audio pesa más de 30 MB; elige un archivo más ligero'
      : 'La portada pesa más de 30 MB; elige una imagen más ligera';
  }
  return null;
}

/** Vista previa local con data: (la CSP de producción no permite blob: en img-src). */
export function leerComoDataUrl(archivo: File): Promise<string | null> {
  return new Promise((resolver) => {
    const lector = new FileReader();
    lector.onload = () => resolver(typeof lector.result === 'string' ? lector.result : null);
    lector.onerror = () => resolver(null);
    lector.readAsDataURL(archivo);
  });
}

const TIEMPO_MAX_MEDICION_MS = 8000;
/** Por encima de este tamaño no se decodifica en memoria (podría ocupar cientos de MB). */
const MAX_BYTES_DECODIFICAR = 20 * 1024 * 1024;

function normalizar(segundos: number): number | null {
  return Number.isFinite(segundos) && segundos > 0 ? Math.max(1, Math.round(segundos)) : null;
}

/** Con un elemento de audio temporal y su `loadedmetadata`. Requiere `blob:` en `media-src`. */
function conElementoAudio(archivo: File): Promise<number | null> {
  return new Promise((resolver) => {
    let url: string;
    try {
      url = URL.createObjectURL(archivo);
    } catch {
      resolver(null);
      return;
    }
    const audio = new Audio();
    let terminado = false;
    const terminar = (valor: number | null) => {
      if (terminado) return;
      terminado = true;
      clearTimeout(temporizador);
      audio.removeAttribute('src');
      audio.load();
      URL.revokeObjectURL(url);
      resolver(valor);
    };
    const temporizador = setTimeout(() => terminar(null), TIEMPO_MAX_MEDICION_MS);
    audio.preload = 'metadata';
    audio.addEventListener('loadedmetadata', () => terminar(normalizar(audio.duration)));
    // Con la CSP de producción (sin blob: en media-src) llega un `error` enseguida.
    audio.addEventListener('error', () => terminar(null));
    audio.src = url;
  });
}

/** Sin URL alguna: decodifica los bytes en memoria. Funciona con cualquier CSP. */
async function conDecodificador(archivo: File): Promise<number | null> {
  if (archivo.size > MAX_BYTES_DECODIFICAR) return null;
  const Contexto = typeof OfflineAudioContext === 'undefined' ? undefined : OfflineAudioContext;
  if (!Contexto) return null;
  try {
    const buffer = await new Contexto(1, 1, 44100).decodeAudioData(await archivo.arrayBuffer());
    return normalizar(buffer.duration);
  } catch {
    return null;
  }
}

/**
 * Duración en segundos enteros leída del propio archivo, o null si el navegador no puede
 * (el servidor pondrá entonces su valor por defecto).
 */
export async function medirDuracion(archivo: File): Promise<number | null> {
  return (await conElementoAudio(archivo)) ?? (await conDecodificador(archivo));
}

/** "3:05" */
export function formatearMinSeg(segundos: number): string {
  const total = Math.max(0, Math.round(segundos));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}
