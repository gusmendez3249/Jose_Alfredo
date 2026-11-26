import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { crearJwt, USUARIO } from '../test/datos';

const CLAVE = 'festivaltrack.sesion';

/** Importa session.ts de nuevo: su estado se calcula al cargar el módulo. */
async function cargarSesion() {
  vi.resetModules();
  return import('./session');
}

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('guardar y limpiar', () => {
  it('guardarSesion deja token y usuario en una sola clave y avisa a los oyentes', async () => {
    const { guardarSesion, obtenerToken, obtenerEstadoSesion, suscribirSesion } = await cargarSesion();
    const oyente = vi.fn();
    suscribirSesion(oyente);

    guardarSesion('tok-1', USUARIO);

    expect(obtenerToken()).toBe('tok-1');
    expect(obtenerEstadoSesion().sesion?.usuario).toEqual(USUARIO);
    expect(JSON.parse(localStorage.getItem(CLAVE) ?? '{}')).toEqual({ token: 'tok-1', usuario: USUARIO });
    expect(oyente).toHaveBeenCalledTimes(1);
  });

  it('limpiarSesion borra el almacenamiento y recuerda el motivo', async () => {
    const { guardarSesion, limpiarSesion, obtenerToken, obtenerEstadoSesion } = await cargarSesion();
    guardarSesion('tok-1', USUARIO);

    limpiarSesion('manual');

    expect(obtenerToken()).toBeNull();
    expect(localStorage.getItem(CLAVE)).toBeNull();
    expect(obtenerEstadoSesion().motivoCierre).toBe('manual');
  });

  it('el objeto de estado se reemplaza (no se muta) para que React detecte el cambio', async () => {
    const { guardarSesion, obtenerEstadoSesion } = await cargarSesion();
    const antes = obtenerEstadoSesion();
    guardarSesion('tok-1', USUARIO);
    expect(obtenerEstadoSesion()).not.toBe(antes);
  });
});

describe('carga inicial', () => {
  it('restaura una sesión guardada', async () => {
    localStorage.setItem(CLAVE, JSON.stringify({ token: 'tok-2', usuario: USUARIO }));
    const { obtenerEstadoSesion } = await cargarSesion();
    expect(obtenerEstadoSesion().sesion?.token).toBe('tok-2');
  });

  it('migra el token de las claves viejas y las borra', async () => {
    localStorage.setItem('accessToken', 'token-viejo');
    localStorage.setItem('token', 'otro-viejo');
    const { obtenerToken } = await cargarSesion();

    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(obtenerToken()).not.toBeNull();
    expect(JSON.parse(localStorage.getItem(CLAVE) ?? '{}').usuario).toBeNull();
  });

  it('no pisa una sesión nueva con una clave vieja, pero sí limpia la vieja', async () => {
    localStorage.setItem(CLAVE, JSON.stringify({ token: 'nuevo', usuario: USUARIO }));
    localStorage.setItem('token', 'viejo');
    const { obtenerToken } = await cargarSesion();
    expect(obtenerToken()).toBe('nuevo');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('una sesión caducada se descarta y se marca como expirada', async () => {
    const ayer = Math.floor(Date.now() / 1000) - 86_400;
    localStorage.setItem(CLAVE, JSON.stringify({ token: crearJwt(ayer), usuario: USUARIO }));
    const { obtenerEstadoSesion } = await cargarSesion();

    expect(obtenerEstadoSesion()).toEqual({ sesion: null, motivoCierre: 'expirada' });
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });

  it('ignora contenido corrupto o sin token', async () => {
    localStorage.setItem(CLAVE, '{no es json');
    expect((await cargarSesion()).obtenerToken()).toBeNull();
    localStorage.setItem(CLAVE, JSON.stringify({ usuario: USUARIO }));
    expect((await cargarSesion()).obtenerToken()).toBeNull();
  });
});

describe('caducidad en vivo', () => {
  it('cierra la sesión sola cuando vence el JWT', async () => {
    vi.useFakeTimers();
    const { guardarSesion, obtenerEstadoSesion } = await cargarSesion();
    const dentroDeUnMinuto = Math.floor(Date.now() / 1000) + 60;

    guardarSesion(crearJwt(dentroDeUnMinuto), USUARIO);
    expect(obtenerEstadoSesion().sesion).not.toBeNull();

    vi.advanceTimersByTime(61_000);
    expect(obtenerEstadoSesion()).toEqual({ sesion: null, motivoCierre: 'expirada' });
  });
});
