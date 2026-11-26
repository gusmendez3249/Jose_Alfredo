import { describe, expect, it } from 'vitest';
import { simularRed } from '../test/red';
import { biografiasService, normalizarDiscografia, normalizarHitos } from './biografias.service';

describe('normalizarHitos', () => {
  it('acepta texto con JSON de textos (datos del seed)', () => {
    expect(normalizarHitos('["Nació en Dolores Hidalgo","Escribió más de mil canciones"]')).toEqual([
      { ano: null, titulo: 'Nació en Dolores Hidalgo', descripcion: null },
      { ano: null, titulo: 'Escribió más de mil canciones', descripcion: null },
    ]);
  });

  it('acepta arreglos de objetos, mezclados con textos, y descarta lo ilegible', () => {
    const hitos = normalizarHitos([
      { ano: 1926, titulo: 'Nace', descripcion: 'En Dolores', icono: 'cuna' },
      'Gran gala',
      { ano: '1950' },
      42,
      null,
      '   ',
    ]);
    expect(hitos).toEqual([
      { ano: '1926', titulo: 'Nace', descripcion: 'En Dolores' },
      { ano: null, titulo: 'Gran gala', descripcion: null },
    ]);
  });

  it('un JSON roto o que no es arreglo da una lista vacía', () => {
    expect(normalizarHitos('{no es json')).toEqual([]);
    expect(normalizarHitos('{"a":1}')).toEqual([]);
    expect(normalizarHitos(undefined)).toEqual([]);
  });
});

describe('normalizarDiscografia', () => {
  it('lee títulos de textos y de objetos', () => {
    expect(normalizarDiscografia('["El Rey","Caminos de Guanajuato"]')).toEqual(['El Rey', 'Caminos de Guanajuato']);
    expect(normalizarDiscografia([{ titulo: 'La Enorme Distancia' }, { nombre: 'Otra' }, {}])).toEqual([
      'La Enorme Distancia',
      'Otra',
    ]);
  });
});

describe('biografiasService.principal', () => {
  it('usa la primera biografía válida y normaliza sus campos', async () => {
    simularRed({
      'GET /biografias': {
        data: [
          { id: 'roto' },
          {
            id: 'b1',
            artistaId: 'a1',
            descripcion: 'Texto',
            citaCelebre: '"Cita"',
            hitos: '["Uno"]',
            discografia: '["El Rey"]',
            artista: { id: 'a1', nombre: 'José Alfredo Jiménez', imagenUrl: 'https://images.unsplash.com/x.jpg' },
          },
        ],
      },
    });
    const bio = await biografiasService.principal();
    expect(bio?.id).toBe('b1');
    expect(bio?.citaCelebre).toBe('"Cita"');
    expect(bio?.hitos).toHaveLength(1);
    expect(bio?.discografia).toEqual(['El Rey']);
  });

  it('sin biografías devuelve null y una respuesta que no es lista falla', async () => {
    simularRed({ 'GET /biografias': { data: [] } });
    expect(await biografiasService.principal()).toBeNull();
    simularRed({ 'GET /biografias': { data: '<html>' } });
    await expect(biografiasService.principal()).rejects.toThrow();
  });
});
