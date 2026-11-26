import { describe, expect, it } from 'vitest';
import {
  ETIQUETA_CATEGORIA,
  ETIQUETA_ESTADO_BOLETO,
  folioCorto,
  formatearDistancia,
  formatearDuracion,
  formatearFechaEvento,
  formatearPrecio,
} from './formato';

describe('formatearPrecio', () => {
  it('usa pesos mexicanos con separador de miles y sin decimales vacíos', () => {
    expect(formatearPrecio(4500)).toBe('$4,500 MXN');
    expect(formatearPrecio(1200)).toBe('$1,200 MXN');
    expect(formatearPrecio(0)).toBe('$0 MXN');
  });
});

describe('formatearDistancia', () => {
  it('muestra metros por debajo del kilómetro y kilómetros con un decimal', () => {
    expect(formatearDistancia(850.4)).toBe('850 m');
    expect(formatearDistancia(1234)).toBe('1.2 km');
  });
});

describe('formatearDuracion', () => {
  it('redondea a minutos con mínimo de 1', () => {
    expect(formatearDuracion(10)).toBe('1 min');
    expect(formatearDuracion(600)).toBe('10 min');
  });

  it('pasa a horas desde los 60 minutos', () => {
    expect(formatearDuracion(3600)).toBe('1 h');
    expect(formatearDuracion(5400)).toBe('1 h 30 min');
  });
});

describe('formatearFechaEvento', () => {
  it('muestra la hora del centro de México sin importar la zona del visitante', () => {
    // 02:00 UTC del 26 de noviembre = 20:00 del 25 (UTC-6, sin horario de verano en 2026).
    const texto = formatearFechaEvento('2026-11-26T02:00:00.000Z');
    expect(texto).toContain('25');
    expect(texto).toContain('noviembre');
    expect(texto).toContain('20:00');
  });

  it('devuelve cadena vacía con una fecha inválida', () => {
    expect(formatearFechaEvento('no es fecha')).toBe('');
  });
});

describe('folioCorto', () => {
  it('toma los últimos 6 caracteres en mayúsculas y nunca el id completo', () => {
    const id = 'cmwabcdef123456';
    expect(folioCorto(id)).toBe('123456');
    expect(folioCorto('cmwabcdefghijk')).toBe('FGHIJK');
    expect(folioCorto(id)).not.toBe(id);
  });
});

describe('etiquetas', () => {
  it('traduce categorías y estados', () => {
    expect(ETIQUETA_CATEGORIA.GENERAL).toBe('General');
    expect(ETIQUETA_ESTADO_BOLETO.CANCELADO).toBe('Cancelado');
  });
});
