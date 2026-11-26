import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { esperaSondeo, useSondeo } from './useSondeo';

function ocultar(oculta: boolean) {
  Object.defineProperty(document, 'hidden', { configurable: true, get: () => oculta });
  document.dispatchEvent(new Event('visibilitychange'));
}

const avanzar = (ms: number) => act(async () => void (await vi.advanceTimersByTimeAsync(ms)));

describe('esperaSondeo', () => {
  it('sin fallos usa el intervalo; con fallos se duplica hasta el tope', () => {
    expect(esperaSondeo(8000, 0, 60000)).toBe(8000);
    expect(esperaSondeo(8000, 1, 60000)).toBe(16000);
    expect(esperaSondeo(8000, 2, 60000)).toBe(32000);
    expect(esperaSondeo(8000, 5, 60000)).toBe(60000);
  });
});

describe('useSondeo', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    ocultar(false);
    vi.useRealTimers();
  });

  it('consulta al montar y luego cada intervalo, y se detiene al desmontar', async () => {
    const tarea = vi.fn().mockResolvedValue(undefined);
    const { unmount } = renderHook(() => useSondeo(tarea, { intervaloMs: 1000 }));
    await avanzar(0);
    expect(tarea).toHaveBeenCalledTimes(1);
    await avanzar(1000);
    expect(tarea).toHaveBeenCalledTimes(2);
    unmount();
    await avanzar(5000);
    expect(tarea).toHaveBeenCalledTimes(2);
  });

  it('no solapa consultas: espera a que termine la lenta antes de agendar la siguiente', async () => {
    let enVuelo = 0;
    let maximo = 0;
    const tarea = vi.fn(async () => {
      enVuelo += 1;
      maximo = Math.max(maximo, enVuelo);
      await new Promise((r) => setTimeout(r, 3500));
      enVuelo -= 1;
    });
    renderHook(() => useSondeo(tarea, { intervaloMs: 1000 }));
    await avanzar(12_000);
    expect(maximo).toBe(1);
    expect(tarea.mock.calls.length).toBeGreaterThanOrEqual(2);
    expect(tarea.mock.calls.length).toBeLessThanOrEqual(3);
  });

  it('con la pestaña oculta no consulta y al volver consulta de inmediato', async () => {
    const tarea = vi.fn().mockResolvedValue(undefined);
    renderHook(() => useSondeo(tarea, { intervaloMs: 1000 }));
    await avanzar(0);
    ocultar(true);
    await avanzar(10_000);
    expect(tarea).toHaveBeenCalledTimes(1);
    ocultar(false);
    await avanzar(0);
    expect(tarea).toHaveBeenCalledTimes(2);
  });

  it('retrocede de forma exponencial ante errores y se recupera', async () => {
    const tarea = vi.fn().mockRejectedValueOnce(new Error('429')).mockRejectedValueOnce(new Error('429')).mockResolvedValue(undefined);
    const { result } = renderHook(() => useSondeo(tarea, { intervaloMs: 1000, maxEsperaMs: 60_000 }));
    await avanzar(0);
    expect(result.current.fallos).toBe(1);
    await avanzar(1999);
    expect(tarea).toHaveBeenCalledTimes(1); // tras 1 fallo espera 2 s
    await avanzar(1);
    expect(tarea).toHaveBeenCalledTimes(2);
    expect(result.current.fallos).toBe(2);
    await avanzar(4000); // tras 2 fallos espera 4 s
    expect(tarea).toHaveBeenCalledTimes(3);
    expect(result.current.fallos).toBe(0);
  });
});
