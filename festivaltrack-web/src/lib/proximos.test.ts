import { describe, expect, it } from 'vitest';
import { crearEvento } from '../test/datos';
import { proximos } from './proximos';

const AHORA = Date.parse('2026-11-25T18:00:00.000Z');
const HORA = 3600 * 1000;
const en = (horas: number) => new Date(AHORA + horas * HORA).toISOString();

describe('proximos', () => {
  it('descarta borradores, cancelados y finalizados', () => {
    const lista = [
      crearEvento({ id: 'b', estado: 'BORRADOR', fechaHora: en(5) }),
      crearEvento({ id: 'c', estado: 'CANCELADO', fechaHora: en(5) }),
      crearEvento({ id: 'f', estado: 'FINALIZADO', fechaHora: en(5) }),
      crearEvento({ id: 'ok', estado: 'PUBLICADO', fechaHora: en(5) }),
    ];
    expect(proximos(lista, AHORA).map((e) => e.id)).toEqual(['ok']);
  });

  it('un evento sigue vigente hasta 4 horas después de empezar', () => {
    const lista = [
      crearEvento({ id: 'en-curso', estado: 'EN_CURSO', fechaHora: en(-3) }),
      crearEvento({ id: 'terminado', estado: 'EN_CURSO', fechaHora: en(-4.5) }),
    ];
    expect(proximos(lista, AHORA).map((e) => e.id)).toEqual(['en-curso']);
  });

  it('ordena del más cercano al más lejano sin modificar la lista original', () => {
    const lista = [
      crearEvento({ id: 'lejos', fechaHora: en(48) }),
      crearEvento({ id: 'cerca', fechaHora: en(2) }),
      crearEvento({ id: 'medio', fechaHora: en(10) }),
    ];
    expect(proximos(lista, AHORA).map((e) => e.id)).toEqual(['cerca', 'medio', 'lejos']);
    expect(lista.map((e) => e.id)).toEqual(['lejos', 'cerca', 'medio']);
  });
});
