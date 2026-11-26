import { describe, expect, it } from 'vitest';
import { simularRed } from '../test/red';
import { sitiosService } from './sitios.service';

describe('sitiosService.listar', () => {
  it('lee el catálogo, descarta lo inválido y los sitios inactivos', async () => {
    const { peticiones } = simularRed({
      'GET /ar/sitios': {
        data: [
          { id: 'S1', nombre: 'Casa Museo', ubicacion: 'Calle Guanajuato 13', descripcion: 'Museo', imagenUrl: null, audioUrl: null, latitud: 21.157341, longitud: -100.93277, activo: true },
          { id: 'S2', nombre: 'Sin coordenadas', ubicacion: 'Centro', descripcion: '', latitud: null, longitud: null, activo: true },
          { id: 'S3', nombre: 'Oculto', activo: false },
          { id: 4, nombre: 'Id numérico' },
          { id: 'S5', nombre: '   ' },
          'basura',
          { id: 'S6', nombre: 'Coordenada rara', latitud: '21', longitud: null, imagenUrl: '/uploads/a.jpg' },
        ],
      },
    });
    const sitios = await sitiosService.listar();
    expect(peticiones).toEqual([expect.objectContaining({ metodo: 'GET', url: '/ar/sitios' })]);
    expect(sitios.map((s) => s.id)).toEqual(['S1', 'S2', 'S6']);
    expect(sitios[0]).toMatchObject({ latitud: 21.157341, longitud: -100.93277, imagenUrl: null });
    expect(sitios[1]).toMatchObject({ latitud: null, longitud: null });
    expect(sitios[2]).toMatchObject({ latitud: null, imagenUrl: '/uploads/a.jpg' });
  });

  it('rechaza una respuesta que no es una lista', async () => {
    simularRed({ 'GET /ar/sitios': { data: { mensaje: 'hola' } } });
    await expect(sitiosService.listar()).rejects.toThrow(/Respuesta inesperada/);
  });
});
