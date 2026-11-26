import { describe, expect, it } from 'vitest';
import { distanciaEnLinea, rutaEnLinea } from './rutas.service';

describe('respaldo en línea recta', () => {
  it('calcula la distancia de Haversine entre dos puntos de Dolores Hidalgo', () => {
    // Escenario Principal ↔ Mausoleo (coordenadas del móvil): unos 650 m.
    const metros = distanciaEnLinea([21.1566, -100.9341], [21.1571, -100.9406]);
    expect(metros).toBeGreaterThan(640);
    expect(metros).toBeLessThan(700);
    expect(distanciaEnLinea([21.1, -100.9], [21.1, -100.9])).toBe(0);
  });

  it('estima los minutos a pie como el móvil: ceil(metros / 80), mínimo 1', () => {
    const ruta = rutaEnLinea([21.1566, -100.9341], [21.1571, -100.9406]);
    expect(ruta.puntos).toEqual([[21.1566, -100.9341], [21.1571, -100.9406]]);
    expect(ruta.duracionSegundos).toBe(Math.ceil(ruta.distanciaMetros / 80) * 60);
    expect(rutaEnLinea([21.1, -100.9], [21.1, -100.9]).duracionSegundos).toBe(60);
  });
});
