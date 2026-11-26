import { afterEach, describe, expect, it, vi } from 'vitest';
import { formatearFechaLista, instanteDeMexico, isoAMexico, mexicoAIso } from './fechaMexico';

describe('mexicoAIso', () => {
  it('convierte la hora de México a UTC (hoy sin horario de verano: +6 h)', () => {
    expect(mexicoAIso('2026-11-25', '20:00')).toBe('2026-11-26T02:00:00.000Z');
    expect(mexicoAIso('2026-07-04', '09:30')).toBe('2026-07-04T15:30:00.000Z');
    expect(mexicoAIso('2026-12-31', '23:59')).toBe('2027-01-01T05:59:00.000Z');
  });

  it('no asume un desfase fijo: en verano de 2021 (horario de verano vigente) son +5 h', () => {
    expect(mexicoAIso('2021-07-15', '20:00')).toBe('2021-07-16T01:00:00.000Z');
    expect(mexicoAIso('2022-01-15', '20:00')).toBe('2022-01-16T02:00:00.000Z');
  });

  it('cruza correctamente un cambio de horario (4 de abril de 2021: 02:00 pasó a 03:00)', () => {
    expect(mexicoAIso('2021-04-04', '01:30')).toBe('2021-04-04T07:30:00.000Z'); // -6 h
    expect(mexicoAIso('2021-04-04', '03:30')).toBe('2021-04-04T08:30:00.000Z'); // -5 h
    expect(mexicoAIso('2021-04-04', '02:30')).toBeNull(); // esa hora no existió
  });

  it('rechaza fechas y horas que no existen o con formato incorrecto', () => {
    expect(mexicoAIso('2026-02-30', '10:00')).toBeNull();
    expect(mexicoAIso('2026-13-01', '10:00')).toBeNull();
    expect(mexicoAIso('2026-11-25', '24:00')).toBeNull();
    expect(mexicoAIso('2026-11-25', '10:60')).toBeNull();
    expect(mexicoAIso('', '10:00')).toBeNull();
    expect(mexicoAIso('2026-11-25', '')).toBeNull();
    expect(mexicoAIso('25/11/2026', '10:00')).toBeNull();
  });

  it('devuelve milisegundos con instanteDeMexico', () => {
    expect(instanteDeMexico('2026-11-25', '20:00')).toBe(Date.UTC(2026, 10, 26, 2, 0));
  });
});

describe('isoAMexico', () => {
  it('muestra el instante guardado en hora de México', () => {
    expect(isoAMexico('2026-11-26T02:00:00.000Z')).toEqual({ fecha: '2026-11-25', hora: '20:00' });
    expect(isoAMexico('2026-07-04T15:30:00Z')).toEqual({ fecha: '2026-07-04', hora: '09:30' });
    // Medianoche en México.
    expect(isoAMexico('2026-11-25T06:00:00Z')).toEqual({ fecha: '2026-11-25', hora: '00:00' });
  });

  it('es la inversa de mexicoAIso', () => {
    for (const [fecha, hora] of [
      ['2026-11-25', '20:00'],
      ['2026-01-01', '00:00'],
      ['2026-12-31', '23:59'],
      ['2021-07-15', '20:00'],
    ]) {
      const iso = mexicoAIso(fecha, hora);
      expect(iso).not.toBeNull();
      expect(isoAMexico(iso as string)).toEqual({ fecha, hora });
    }
  });

  it('devuelve null con una fecha ilegible', () => {
    expect(isoAMexico('no es fecha')).toBeNull();
  });
});

describe('con el navegador en otra zona horaria', () => {
  afterEach(() => vi.unstubAllEnvs());

  it.each(['Asia/Tokyo', 'America/Los_Angeles', 'Pacific/Kiritimati', 'UTC'])(
    'da el mismo resultado con TZ=%s',
    (zona) => {
      vi.stubEnv('TZ', zona);
      expect(mexicoAIso('2026-11-25', '20:00')).toBe('2026-11-26T02:00:00.000Z');
      expect(isoAMexico('2026-11-26T02:00:00.000Z')).toEqual({ fecha: '2026-11-25', hora: '20:00' });
      expect(formatearFechaLista('2026-11-26T02:00:00.000Z')).toContain('20:00');
    },
  );
});

describe('formatearFechaLista', () => {
  it('formatea en hora de México', () => {
    const texto = formatearFechaLista('2026-11-26T02:00:00.000Z');
    expect(texto).toContain('25');
    expect(texto).toContain('2026');
    expect(texto).toContain('20:00');
  });

  it('devuelve vacío con una fecha inválida', () => {
    expect(formatearFechaLista('x')).toBe('');
  });
});
