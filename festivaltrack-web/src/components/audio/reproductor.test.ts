import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../../config/env';
import {
  avanzarA,
  crearCancion,
  desinstalarAudioFalso,
  fallarCarga,
  fijarDuracion,
  instalarAudioFalso,
  modoPlay,
  reiniciarContadores,
  sesionFalsa,
  vecesReproducido,
} from './audioDePrueba';
import {
  AVISO_COLA_FALLIDA,
  AVISO_SIN_CONEXION,
  duracionDemo,
  elementoAudio,
  formatearTiempo,
  mezclar,
  obtenerEstadoReproductor,
  pistaActual,
  reproductor,
} from './reproductor';

const CONCIERTO = { eventoId: 'ev-1', nombre: 'Noche de Bohemia' };
const TRES = [
  crearCancion({ id: 'a', titulo: 'El Rey', archivoUrl: 'https://www.soundhelix.com/a.mp3' }),
  crearCancion({ id: 'b', titulo: 'Caminos de Guanajuato', archivoUrl: '/uploads/cancion-1-2.mp3' }),
  crearCancion({ id: 'c', titulo: 'Ella', archivoUrl: 'https://www.soundhelix.com/c.mp3' }),
];

const estado = obtenerEstadoReproductor;
const audio = () => elementoAudio();
const esperarMicrotareas = () => new Promise((r) => setTimeout(r, 0));

beforeAll(instalarAudioFalso);
afterAll(desinstalarAudioFalso);
beforeEach(reiniciarContadores);
afterEach(() => {
  reproductor.cerrar();
  vi.restoreAllMocks();
});

describe('utilidades', () => {
  it('formatea m:ss', () => {
    expect(formatearTiempo(0)).toBe('0:00');
    expect(formatearTiempo(65.9)).toBe('1:05');
    expect(formatearTiempo(Number.NaN)).toBe('0:00');
  });

  it('la demo dura 30 s como máximo', () => {
    expect(duracionDemo(180)).toBe(30);
    expect(duracionDemo(12)).toBe(12);
    expect(duracionDemo(0)).toBe(30);
    expect(duracionDemo(180, 20.5)).toBe(20.5);
  });

  it('el aleatorio deja primero la pista elegida y conserva todas', () => {
    const mezcla = mezclar(['a', 'b', 'c', 'd'], 2, () => 0);
    expect(mezcla[0]).toBe('c');
    expect([...mezcla].sort()).toEqual(['a', 'b', 'c', 'd']);
  });
});

describe('reproductor', () => {
  it('carga la cola, resuelve /uploads contra el backend y publica los metadatos', () => {
    reproductor.reproducirCola(TRES, 1, CONCIERTO);

    expect(audio().getAttribute('src')).toBe(`${new URL(API_URL).origin}/uploads/cancion-1-2.mp3`);
    expect(estado().reproduciendo).toBe(true);
    expect(pistaActual(estado())?.titulo).toBe('Caminos de Guanajuato');
    expect(sesionFalsa.metadata?.title).toBe('Caminos de Guanajuato');
    expect(sesionFalsa.metadata?.album).toBe('Noche de Bohemia');
    expect(sesionFalsa.playbackState).toBe('playing');
  });

  it('corta cada demo a los 30 s y pasa a la siguiente; tras la última queda en pausa al inicio', () => {
    reproductor.reproducirCola(TRES, 1, CONCIERTO);
    avanzarA(audio(), 12);
    expect(estado().posicion).toBe(12);

    avanzarA(audio(), 30);
    expect(pistaActual(estado())?.id).toBe('c');
    expect(estado().reproduciendo).toBe(true);

    audio().dispatchEvent(new Event('ended'));
    expect(pistaActual(estado())?.id).toBe('a');
    expect(estado().reproduciendo).toBe(false);
    expect(audio().getAttribute('src')).toBe('https://www.soundhelix.com/a.mp3');
  });

  it('usa la duración real si el audio dura menos de 30 s', () => {
    reproductor.reproducirCola(TRES, 0, CONCIERTO);
    fijarDuracion(audio(), 18);
    expect(estado().duracion).toBe(18);
  });

  it('una pista que falla se marca, avisa y salta a la siguiente', () => {
    modoPlay('silencio');
    reproductor.reproducirCola(TRES, 0, CONCIERTO);
    fallarCarga(audio());

    expect(estado().noDisponibles).toEqual(['a']);
    expect(pistaActual(estado())?.id).toBe('b');
    expect(estado().aviso).toBe('No se pudo reproducir «El Rey». Pasamos a la siguiente.');
  });

  it('si falla la última vuelve al inicio en pausa', () => {
    reproductor.reproducirCola(TRES, 2, CONCIERTO);
    modoPlay('silencio');
    fallarCarga(audio());

    expect(pistaActual(estado())?.id).toBe('a');
    expect(estado().reproduciendo).toBe(false);
    expect(estado().aviso).toBe('No se pudo reproducir «Ella».');
  });

  it('si no funciona ninguna se detiene y avisa, sin entrar en bucle', () => {
    modoPlay('silencio');
    reproductor.reproducirCola(TRES, 0, CONCIERTO);
    fallarCarga(audio());
    fallarCarga(audio());
    fallarCarga(audio());
    fallarCarga(audio()); // un error de más no debe reanudar nada

    expect(estado().noDisponibles).toEqual(['a', 'b', 'c']);
    expect(estado().reproduciendo).toBe(false);
    expect(estado().aviso).toBe(AVISO_COLA_FALLIDA);
    expect(vecesReproducido()).toBe(3);
  });

  it('salta las pistas con URL no permitida sin intentar reproducirlas', () => {
    const conMala = [crearCancion({ id: 'x', archivoUrl: 'http://otro.example/x.mp3' }), ...TRES];
    reproductor.reproducirCola(conMala, 0, CONCIERTO);
    expect(pistaActual(estado())?.id).toBe('a');
    expect(estado().noDisponibles).toEqual(['x']);
  });

  it('una cola sin ninguna URL válida no reproduce nada y avisa', () => {
    reproductor.reproducirCola([crearCancion({ archivoUrl: 'javascript:alert(1)' })], 0, CONCIERTO);
    expect(vecesReproducido()).toBe(0);
    expect(estado().aviso).toBe(AVISO_COLA_FALLIDA);
  });

  it('si el navegador bloquea el autoplay queda en pausa sin errores en consola', async () => {
    const consola = vi.spyOn(console, 'error');
    modoPlay('rechaza');
    reproductor.reproducirCola(TRES, 0, CONCIERTO);
    await esperarMicrotareas();

    expect(estado().reproduciendo).toBe(false);
    expect(estado().esperando).toBe(false);
    expect(consola).not.toHaveBeenCalled();
  });

  it('sin conexión no marca la canción: pausa y pide reintentar', () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
    modoPlay('silencio');
    reproductor.reproducirCola(TRES, 0, CONCIERTO);
    fallarCarga(audio());

    expect(estado().noDisponibles).toEqual([]);
    expect(pistaActual(estado())?.id).toBe('a');
    expect(estado().aviso).toBe(AVISO_SIN_CONEXION);
  });

  it('"Anterior" reinicia si lleva más de 3 s y si no, va a la anterior', () => {
    reproductor.reproducirCola(TRES, 1, CONCIERTO);
    avanzarA(audio(), 10);
    reproductor.anterior();
    expect(audio().currentTime).toBe(0);
    expect(pistaActual(estado())?.id).toBe('b');

    reproductor.anterior();
    expect(pistaActual(estado())?.id).toBe('a');
  });

  it('"Siguiente" no hace nada en la última', () => {
    reproductor.reproducirCola(TRES, 2, CONCIERTO);
    reproductor.siguiente();
    expect(pistaActual(estado())?.id).toBe('c');
  });

  it('busca dentro de la demo sin pasar de su duración', () => {
    reproductor.reproducirCola(TRES, 0, CONCIERTO);
    reproductor.buscar(12);
    expect(audio().currentTime).toBe(12);
    reproductor.buscar(500);
    expect(audio().currentTime).toBe(30);
  });

  it('el aleatorio mantiene la pista actual y al quitarlo vuelve el orden del repertorio', () => {
    reproductor.reproducirCola(TRES, 1, CONCIERTO);
    reproductor.alternarAleatorio();
    expect(estado().aleatorio).toBe(true);
    expect(estado().indice).toBe(0);
    expect(pistaActual(estado())?.id).toBe('b');

    reproductor.alternarAleatorio();
    expect(estado().cola.map((p) => p.id)).toEqual(['a', 'b', 'c']);
    expect(estado().indice).toBe(1);
  });

  it('las teclas multimedia controlan el reproductor y cerrar lo vacía', () => {
    reproductor.reproducirCola(TRES, 0, CONCIERTO);
    sesionFalsa.acciones.get('pause')?.({ action: 'pause' });
    expect(estado().reproduciendo).toBe(false);
    sesionFalsa.acciones.get('nexttrack')?.({ action: 'nexttrack' });
    expect(pistaActual(estado())?.id).toBe('b');

    reproductor.cerrar();
    expect(estado().cola).toEqual([]);
    expect(audio().getAttribute('src')).toBeNull();
    expect(sesionFalsa.metadata).toBeNull();
  });
});
