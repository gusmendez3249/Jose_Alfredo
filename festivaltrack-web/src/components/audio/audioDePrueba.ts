/**
 * Solo para pruebas: jsdom no reproduce audio. Sustituye play/pause/load y las propiedades de
 * tiempo de HTMLMediaElement por versiones controlables, y simula `navigator.mediaSession`.
 */
import type { Cancion } from '../../services/canciones.service';

export type ModoPlay = 'suena' | 'silencio' | 'rechaza';

const tiempos = new WeakMap<HTMLMediaElement, number>();
const duraciones = new WeakMap<HTMLMediaElement, number>();
let modo: ModoPlay = 'suena';
let reproducciones = 0;

const originales = {
  play: Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'play'),
  pause: Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'pause'),
  load: Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'load'),
  currentTime: Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'currentTime'),
  duration: Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'duration'),
};

export interface SesionFalsa {
  metadata: MediaMetadata | null;
  playbackState: MediaSessionPlaybackState;
  acciones: Map<string, MediaSessionActionHandler | null>;
}

export const sesionFalsa: SesionFalsa = { metadata: null, playbackState: 'none', acciones: new Map() };

export function instalarAudioFalso(): void {
  Object.defineProperty(HTMLMediaElement.prototype, 'play', {
    configurable: true,
    value: function play(this: HTMLMediaElement) {
      reproducciones += 1;
      if (modo === 'rechaza') return Promise.reject(new DOMException('Autoplay bloqueado', 'NotAllowedError'));
      this.dispatchEvent(new Event('play'));
      if (modo === 'suena') this.dispatchEvent(new Event('playing'));
      return Promise.resolve();
    },
  });
  Object.defineProperty(HTMLMediaElement.prototype, 'pause', {
    configurable: true,
    value: function pause(this: HTMLMediaElement) {
      this.dispatchEvent(new Event('pause'));
    },
  });
  Object.defineProperty(HTMLMediaElement.prototype, 'load', { configurable: true, value: () => undefined });
  Object.defineProperty(HTMLMediaElement.prototype, 'currentTime', {
    configurable: true,
    get(this: HTMLMediaElement) {
      return tiempos.get(this) ?? 0;
    },
    set(this: HTMLMediaElement, valor: number) {
      tiempos.set(this, valor);
    },
  });
  Object.defineProperty(HTMLMediaElement.prototype, 'duration', {
    configurable: true,
    get(this: HTMLMediaElement) {
      return duraciones.get(this) ?? Number.NaN;
    },
  });

  class MetadatosFalsos {
    title: string;
    artist: string;
    album: string;
    constructor(datos: MediaMetadataInit) {
      this.title = datos.title ?? '';
      this.artist = datos.artist ?? '';
      this.album = datos.album ?? '';
    }
  }
  Object.defineProperty(globalThis, 'MediaMetadata', { configurable: true, writable: true, value: MetadatosFalsos });
  Object.defineProperty(navigator, 'mediaSession', {
    configurable: true,
    value: {
      get metadata() {
        return sesionFalsa.metadata;
      },
      set metadata(m: MediaMetadata | null) {
        sesionFalsa.metadata = m;
      },
      get playbackState() {
        return sesionFalsa.playbackState;
      },
      set playbackState(e: MediaSessionPlaybackState) {
        sesionFalsa.playbackState = e;
      },
      setActionHandler(accion: string, manejador: MediaSessionActionHandler | null) {
        sesionFalsa.acciones.set(accion, manejador);
      },
      setPositionState: () => undefined,
    },
  });
}

export function desinstalarAudioFalso(): void {
  for (const [nombre, descriptor] of Object.entries(originales)) {
    if (descriptor) Object.defineProperty(HTMLMediaElement.prototype, nombre, descriptor);
  }
}

export function modoPlay(nuevo: ModoPlay): void {
  modo = nuevo;
}

/** Cuántas veces se llamó a play() desde el último reinicio. */
export function vecesReproducido(): number {
  return reproducciones;
}

export function reiniciarContadores(): void {
  reproducciones = 0;
  modo = 'suena';
}

/** Mueve el tiempo del audio y avisa como lo haría el navegador. */
export function avanzarA(audio: HTMLMediaElement, segundos: number): void {
  tiempos.set(audio, segundos);
  audio.dispatchEvent(new Event('timeupdate'));
}

export function fijarDuracion(audio: HTMLMediaElement, segundos: number): void {
  duraciones.set(audio, segundos);
  audio.dispatchEvent(new Event('durationchange'));
}

export function fallarCarga(audio: HTMLMediaElement): void {
  audio.dispatchEvent(new Event('error'));
}

export function crearCancion(parcial: Partial<Cancion> = {}): Cancion {
  return {
    id: 'c-1',
    titulo: 'El Rey',
    artista: 'José Alfredo Jiménez',
    duracion: 180,
    archivoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
    portadaUrl: null,
    genero: 'Ranchera',
    estado: 'PUBLICADA',
    createdAt: '2026-01-01T00:00:00.000Z',
    ...parcial,
  };
}
