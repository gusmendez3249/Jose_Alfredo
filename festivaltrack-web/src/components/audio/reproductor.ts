import { useSyncExternalStore } from 'react';
import { urlMedia } from '../../lib/media';
import type { Cancion } from '../../services/canciones.service';

/**
 * Reproductor global de demos: un único <audio> para toda la aplicación, creado aquí (fuera de
 * React) para que siga sonando al navegar. El estado es un store externo, como `auth/session.ts`:
 * cualquier componente lo lee con `useReproductor()` y lo controla con `reproductor.*`.
 *
 * Reglas tomadas del móvil (playback/PlaybackManager.kt y PlaybackLogic.kt): cada demo se corta a
 * 30 s, "Anterior" reinicia si lleva más de 3 s, al terminar la última la cola queda en pausa al
 * inicio y una canción que falla se salta sin entrar en bucle.
 */

/** Igual que `DEMO_MS` del móvil. */
export const DEMO_SEGUNDOS = 30;
const UMBRAL_REINICIO = 3;
const DURACION_AVISO_MS = 8000;

export const AVISO_SIN_CONEXION = 'Sin conexión. Cuando vuelva el internet toca reproducir para continuar.';
export const AVISO_COLA_FALLIDA = 'No se pudo reproducir la demo. Revisa tu conexión e intenta de nuevo.';

export interface Pista {
  id: string;
  titulo: string;
  artista: string;
  /** Segundos según el catálogo. */
  duracion: number;
  /** URL ya validada con `urlMedia()`; null si no se puede reproducir. */
  src: string | null;
  portada: string | null;
}

export interface ContextoCola {
  /** Concierto al que pertenece la cola (para enlazar a su detalle); null en el catálogo general. */
  eventoId: string | null;
  nombre: string;
}

export interface EstadoReproductor {
  /** Orden en que suena (aleatorio o el del repertorio). */
  cola: readonly Pista[];
  /** Orden del repertorio, para restaurarlo al quitar el aleatorio. */
  original: readonly Pista[];
  indice: number;
  reproduciendo: boolean;
  /** Cargando o esperando datos (buffering). */
  esperando: boolean;
  /** Segundos. */
  posicion: number;
  /** Duración de la demo actual en segundos (30 o menos). */
  duracion: number;
  aleatorio: boolean;
  contexto: ContextoCola | null;
  aviso: string | null;
  /** Ids de las pistas que fallaron o no tienen URL válida. */
  noDisponibles: readonly string[];
}

const INICIAL: EstadoReproductor = {
  cola: [],
  original: [],
  indice: 0,
  reproduciendo: false,
  esperando: false,
  posicion: 0,
  duracion: DEMO_SEGUNDOS,
  aleatorio: false,
  contexto: null,
  aviso: null,
  noDisponibles: [],
};

let estado: EstadoReproductor = INICIAL;
const oyentes = new Set<() => void>();

let audio: HTMLAudioElement | null = null;
/** Sube con cada carga: las promesas y eventos de una carga anterior se ignoran. */
let generacion = 0;
/** Si la carga actual debe sonar (para decidir cómo seguir si falla). */
let intencion = false;
let erroresSeguidos = 0;
let temporizadorAviso: ReturnType<typeof setTimeout> | undefined;

function emitir(cambios: Partial<EstadoReproductor>): void {
  estado = { ...estado, ...cambios };
  oyentes.forEach((oyente) => oyente());
}

function avisar(texto: string | null, temporal = false): void {
  clearTimeout(temporizadorAviso);
  emitir({ aviso: texto });
  if (texto && temporal) {
    temporizadorAviso = setTimeout(() => {
      if (estado.aviso === texto) emitir({ aviso: null });
    }, DURACION_AVISO_MS);
  }
}

// ---------- Utilidades puras (exportadas para las pruebas) ----------

export function pistaDe(cancion: Cancion): Pista {
  return {
    id: cancion.id,
    titulo: cancion.titulo,
    artista: cancion.artista,
    duracion: cancion.duracion,
    src: urlMedia(cancion.archivoUrl),
    portada: urlMedia(cancion.portadaUrl),
  };
}

/** Duración estimada de la demo mientras el navegador no informa la real. */
export function duracionDemo(segundosCatalogo: number, segundosAudio?: number): number {
  if (segundosAudio !== undefined && Number.isFinite(segundosAudio) && segundosAudio > 0) {
    return Math.min(segundosAudio, DEMO_SEGUNDOS);
  }
  return segundosCatalogo > 0 ? Math.min(segundosCatalogo, DEMO_SEGUNDOS) : DEMO_SEGUNDOS;
}

/** 65 -> "1:05" */
export function formatearTiempo(segundos: number): string {
  const s = Math.max(0, Math.floor(Number.isFinite(segundos) ? segundos : 0));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

/** La pista `inicio` queda primero y el resto se mezcla detrás (como `PlaybackLogic.shuffled`). */
export function mezclar<T>(elementos: readonly T[], inicio: number, azar: () => number = Math.random): T[] {
  const resto = elementos.filter((_, i) => i !== inicio);
  for (let i = resto.length - 1; i > 0; i -= 1) {
    const j = Math.floor(azar() * (i + 1));
    [resto[i], resto[j]] = [resto[j], resto[i]];
  }
  const primero = elementos[inicio];
  return primero === undefined ? resto : [primero, ...resto];
}

export function estaDisponible(e: EstadoReproductor, pista: Pista): boolean {
  return pista.src !== null && !e.noDisponibles.includes(pista.id);
}

/** Índice de la primera pista disponible desde `desde` (incluido) en la dirección dada; -1 si no hay. */
function buscarDisponible(desde: number, paso: 1 | -1): number {
  for (let i = desde; i >= 0 && i < estado.cola.length; i += paso) {
    if (estaDisponible(estado, estado.cola[i])) return i;
  }
  return -1;
}

export function haySiguiente(e: EstadoReproductor): boolean {
  return e.cola.slice(e.indice + 1).some((p) => estaDisponible(e, p));
}

// ---------- Elemento de audio y Media Session ----------

function sesionMedios(): MediaSession | null {
  return typeof navigator !== 'undefined' && 'mediaSession' in navigator ? navigator.mediaSession : null;
}

function accionMedios(accion: MediaSessionAction, manejador: MediaSessionActionHandler | null): void {
  try {
    sesionMedios()?.setActionHandler(accion, manejador);
  } catch {
    // El navegador no admite esta acción.
  }
}

function actualizarMetadatos(): void {
  const sesion = sesionMedios();
  if (!sesion) return;
  const pista = estado.cola[estado.indice];
  if (!pista || typeof MediaMetadata === 'undefined') {
    sesion.metadata = null;
    return;
  }
  sesion.metadata = new MediaMetadata({
    title: pista.titulo,
    artist: pista.artista,
    album: estado.contexto?.nombre ?? '',
    artwork: pista.portada ? [{ src: pista.portada, sizes: '512x512' }] : [],
  });
}

function actualizarPosicionMedios(): void {
  const sesion = sesionMedios();
  if (!sesion?.setPositionState || estado.cola.length === 0) return;
  try {
    sesion.setPositionState({
      duration: estado.duracion,
      position: Math.min(estado.posicion, estado.duracion),
      playbackRate: 1,
    });
  } catch {
    // Valores fuera de rango mientras carga: se ignora.
  }
}

function elemento(): HTMLAudioElement {
  if (audio) return audio;
  const a = document.createElement('audio');
  a.preload = 'none';

  a.addEventListener('play', () => {
    emitir({ reproduciendo: true });
    const sesion = sesionMedios();
    if (sesion) sesion.playbackState = 'playing';
  });
  a.addEventListener('pause', () => {
    emitir({ reproduciendo: false, esperando: false });
    const sesion = sesionMedios();
    if (sesion) sesion.playbackState = 'paused';
  });
  a.addEventListener('playing', () => {
    erroresSeguidos = 0;
    emitir({ esperando: false });
  });
  a.addEventListener('waiting', () => emitir({ esperando: true }));
  const alCambiarDuracion = () => {
    const pista = estado.cola[estado.indice];
    if (!pista) return;
    emitir({ duracion: duracionDemo(pista.duracion, a.duration) });
    actualizarPosicionMedios();
  };
  a.addEventListener('loadedmetadata', alCambiarDuracion);
  a.addEventListener('durationchange', alCambiarDuracion);
  a.addEventListener('timeupdate', () => {
    if (estado.cola.length === 0) return;
    // El corte a 30 s del móvil: al llegar al límite se pasa a la siguiente como si hubiera terminado.
    if (a.currentTime >= DEMO_SEGUNDOS) {
      terminarDemo();
      return;
    }
    emitir({ posicion: a.currentTime });
  });
  a.addEventListener('seeked', actualizarPosicionMedios);
  a.addEventListener('ended', terminarDemo);
  a.addEventListener('error', () => {
    // Sin src (al cerrar) también dispara error: solo cuenta si hay una pista cargada.
    if (estado.cola.length === 0 || !a.getAttribute('src')) return;
    fallo();
  });

  accionMedios('play', () => reproductor.reanudar());
  accionMedios('pause', () => reproductor.pausar());
  accionMedios('previoustrack', () => reproductor.anterior());
  accionMedios('nexttrack', () => reproductor.siguiente());
  accionMedios('stop', () => reproductor.cerrar());
  accionMedios('seekto', (detalles) => {
    if (typeof detalles.seekTime === 'number') reproductor.buscar(detalles.seekTime);
  });

  audio = a;
  return a;
}

function intentarReproducir(): void {
  const a = elemento();
  const g = generacion;
  intencion = true;
  emitir({ reproduciendo: true, esperando: true });
  let promesa: Promise<void> | undefined;
  try {
    promesa = a.play();
  } catch {
    promesa = undefined;
  }
  // La política de autoplay puede rechazar play(): se queda en pausa sin error en consola.
  // AbortError y NotSupportedError llegan cuando la carga cambia o falla; de eso se encarga `error`.
  promesa?.catch((error: unknown) => {
    if (g !== generacion) return;
    const nombre = error instanceof DOMException ? error.name : '';
    if (nombre === 'AbortError' || nombre === 'NotSupportedError') return;
    intencion = false;
    emitir({ reproduciendo: false, esperando: false });
  });
}

function cargarIndice(indice: number, reproducir: boolean): void {
  const pista = estado.cola[indice];
  if (!pista?.src) return;
  generacion += 1;
  intencion = reproducir;
  const a = elemento();
  emitir({ indice, posicion: 0, duracion: duracionDemo(pista.duracion), esperando: false });
  a.src = pista.src;
  actualizarMetadatos();
  if (reproducir) intentarReproducir();
  else {
    a.pause();
    emitir({ reproduciendo: false });
  }
}

/** Fin de la demo (o corte a 30 s): sigue la próxima disponible o vuelve al inicio en pausa. */
function terminarDemo(): void {
  const siguiente = buscarDisponible(estado.indice + 1, 1);
  if (siguiente !== -1) {
    cargarIndice(siguiente, true);
    return;
  }
  const inicio = buscarDisponible(0, 1);
  if (inicio === -1) detenerPorFallo();
  else cargarIndice(inicio, false);
}

function detenerPorFallo(): void {
  generacion += 1;
  intencion = false;
  audio?.pause();
  emitir({ reproduciendo: false, esperando: false, posicion: 0 });
  avisar(AVISO_COLA_FALLIDA);
}

function fallo(): void {
  const pista = estado.cola[estado.indice];
  // Un error repetido de una pista ya descartada (p. ej. tras detener la cola) no cuenta otra vez.
  if (!pista || estado.noDisponibles.includes(pista.id)) return;

  // Sin red el problema no es la canción: se queda en pausa para reintentar al volver internet.
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    generacion += 1;
    intencion = false;
    emitir({ reproduciendo: false, esperando: false });
    avisar(AVISO_SIN_CONEXION);
    return;
  }

  const seguir = intencion;
  erroresSeguidos += 1;
  emitir({ noDisponibles: [...estado.noDisponibles, pista.id] });

  if (erroresSeguidos >= estado.cola.length || buscarDisponible(0, 1) === -1) {
    detenerPorFallo();
    return;
  }
  const siguiente = buscarDisponible(estado.indice + 1, 1);
  if (siguiente !== -1) {
    avisar(`No se pudo reproducir «${pista.titulo}». Pasamos a la siguiente.`, true);
    cargarIndice(siguiente, seguir);
  } else {
    avisar(`No se pudo reproducir «${pista.titulo}».`, true);
    cargarIndice(buscarDisponible(0, 1), false);
  }
}

// ---------- API pública ----------

export const reproductor = {
  /**
   * Carga una cola y empieza a sonar por `indice` (o por la siguiente disponible).
   * Con `aleatorio`, la pista inicial queda primero y el resto se mezcla.
   */
  reproducirCola(canciones: readonly Cancion[], indice: number, contexto: ContextoCola, opciones: { aleatorio?: boolean } = {}): void {
    const original = canciones.map(pistaDe);
    const aleatorio = Boolean(opciones.aleatorio) && original.length > 1;
    const inicio = Math.min(Math.max(0, indice), Math.max(0, original.length - 1));
    const cola = aleatorio ? mezclar(original, inicio) : original;
    erroresSeguidos = 0;
    clearTimeout(temporizadorAviso);
    estado = {
      ...INICIAL,
      cola,
      original,
      aleatorio,
      contexto,
      indice: aleatorio ? 0 : inicio,
      noDisponibles: original.filter((p) => p.src === null).map((p) => p.id),
    };
    const primera = buscarDisponible(estado.indice, 1);
    const elegida = primera !== -1 ? primera : buscarDisponible(0, 1);
    if (elegida === -1) {
      detenerPorFallo();
      return;
    }
    cargarIndice(elegida, true);
    actualizarPosicionMedios();
  },

  /** Salta a una pista de la cola actual. */
  seleccionar(indice: number): void {
    const pista = estado.cola[indice];
    if (!pista || !estaDisponible(estado, pista)) return;
    avisar(null);
    cargarIndice(indice, true);
  },

  reanudar(): void {
    const pista = estado.cola[estado.indice];
    if (!pista || !estaDisponible(estado, pista)) return;
    avisar(null);
    intentarReproducir();
  },

  pausar(): void {
    intencion = false;
    audio?.pause();
  },

  alternar(): void {
    if (estado.reproduciendo) reproductor.pausar();
    else reproductor.reanudar();
  },

  /** Si lleva más de 3 s reinicia la demo; si no, va a la anterior disponible. */
  anterior(): void {
    if (estado.cola.length === 0) return;
    const a = elemento();
    const previa = buscarDisponible(estado.indice - 1, -1);
    if (a.currentTime > UMBRAL_REINICIO || previa === -1) {
      a.currentTime = 0;
      emitir({ posicion: 0 });
      return;
    }
    avisar(null);
    cargarIndice(previa, estado.reproduciendo);
  },

  siguiente(): void {
    const siguiente = buscarDisponible(estado.indice + 1, 1);
    if (siguiente === -1) return;
    avisar(null);
    cargarIndice(siguiente, estado.reproduciendo || intencion);
  },

  buscar(segundos: number): void {
    if (estado.cola.length === 0 || !Number.isFinite(segundos)) return;
    const destino = Math.min(Math.max(0, segundos), estado.duracion);
    elemento().currentTime = destino;
    emitir({ posicion: destino });
  },

  /** Orden aleatorio: la actual queda primero; al quitarlo vuelve el orden del repertorio. */
  alternarAleatorio(): void {
    const actual = estado.cola[estado.indice];
    if (!actual || estado.original.length < 2) return;
    if (estado.aleatorio) {
      emitir({ aleatorio: false, cola: estado.original, indice: Math.max(0, estado.original.indexOf(actual)) });
    } else {
      const indiceOriginal = estado.original.indexOf(actual);
      emitir({ aleatorio: true, cola: mezclar(estado.original, indiceOriginal), indice: 0 });
    }
  },

  /** Detiene, vacía la cola y oculta el reproductor. */
  cerrar(): void {
    generacion += 1;
    intencion = false;
    erroresSeguidos = 0;
    clearTimeout(temporizadorAviso);
    if (audio) {
      audio.pause();
      audio.removeAttribute('src');
      audio.load();
    }
    estado = INICIAL;
    oyentes.forEach((oyente) => oyente());
    const sesion = sesionMedios();
    if (sesion) {
      sesion.metadata = null;
      sesion.playbackState = 'none';
    }
  },

  descartarAviso(): void {
    avisar(null);
  },
};

export function obtenerEstadoReproductor(): EstadoReproductor {
  return estado;
}

export function suscribirReproductor(oyente: () => void): () => void {
  oyentes.add(oyente);
  return () => {
    oyentes.delete(oyente);
  };
}

/** Solo para pruebas: el elemento <audio> único (se crea si no existe). */
export function elementoAudio(): HTMLAudioElement {
  return elemento();
}

export function useReproductor(): EstadoReproductor {
  return useSyncExternalStore(suscribirReproductor, obtenerEstadoReproductor, obtenerEstadoReproductor);
}

/** Pista que suena (o está en pausa) ahora; null si no hay cola. */
export function pistaActual(e: EstadoReproductor): Pista | null {
  return e.cola[e.indice] ?? null;
}
