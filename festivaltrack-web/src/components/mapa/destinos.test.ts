import { describe, expect, it } from 'vitest';
import { crearEvento } from '../../test/datos';
import {
  coincideBusqueda,
  construirDestinos,
  DESTINOS_FIJOS,
  destinoDesdeParametro,
} from './destinos';

const sitio = { id: 'SITIO-CASA-MUSEO', nombre: 'Casa Museo José Alfredo Jiménez', ubicacion: 'Calle Guanajuato 13', descripcion: 'Museo', imagenUrl: null, latitud: 21.157341, longitud: -100.93277 };
const AHORA = Date.parse('2026-11-25T12:00:00Z');

describe('destinos del mapa', () => {
  it('trae los cuatro lugares fijos del móvil con sus coordenadas', () => {
    expect(DESTINOS_FIJOS.map((d) => [d.nombre, d.coords])).toEqual([
      ['Escenario Principal', [21.1566, -100.9341]],
      ['Plaza Principal', [21.1563, -100.9338]],
      ['Mausoleo de José Alfredo Jiménez', [21.1571, -100.9406]],
      ['Casa Museo', [21.1574, -100.933]],
    ]);
  });

  it('suma conciertos próximos (por fecha, con o sin coordenadas) y sitios, y descarta eventos terminados', () => {
    const destinos = construirDestinos(
      [sitio],
      [
        crearEvento({ id: 'tarde', nombre: 'Tarde', fechaHora: '2026-12-10T20:00:00Z' }),
        crearEvento({ id: 'sin', nombre: 'Sin mapa', fechaHora: '2026-11-30T20:00:00Z', latitud: null, longitud: null }),
        crearEvento({ id: 'viejo', nombre: 'Viejo', fechaHora: '2026-11-01T20:00:00Z' }),
        crearEvento({ id: 'cancelado', nombre: 'Cancelado', estado: 'CANCELADO', fechaHora: '2026-12-01T20:00:00Z' }),
      ],
      AHORA,
    );
    expect(destinos.slice(4).map((d) => [d.id, d.categoria, d.coords !== null])).toEqual([
      ['evento-sin', 'concierto', false],
      ['evento-tarde', 'concierto', true],
      ['sitio-SITIO-CASA-MUSEO', 'historico', true],
    ]);
    expect(destinos.find((d) => d.id === 'evento-tarde')?.evento).toEqual({ id: 'tarde', fechaHora: '2026-12-10T20:00:00Z', aLaVenta: true });
  });

  it('resuelve ?destino= por id, por id crudo y descarta valores inválidos', () => {
    const destinos = construirDestinos([sitio], [crearEvento({ id: 'ev9', fechaHora: '2026-12-10T20:00:00Z' })], AHORA);
    expect(destinoDesdeParametro('lugar-casa-museo', destinos)?.nombre).toBe('Casa Museo');
    expect(destinoDesdeParametro('ev9', destinos)?.id).toBe('evento-ev9');
    expect(destinoDesdeParametro('SITIO-CASA-MUSEO', destinos)?.id).toBe('sitio-SITIO-CASA-MUSEO');
    expect(destinoDesdeParametro('<img src=x onerror=alert(1)>', destinos)).toBeNull();
    expect(destinoDesdeParametro('no-existe', destinos)).toBeNull();
    expect(destinoDesdeParametro('a'.repeat(500), destinos)).toBeNull();
    expect(destinoDesdeParametro(null, destinos)).toBeNull();
  });

  it('busca por nombre o dirección sin distinguir mayúsculas ni espacios', () => {
    const [escenario] = DESTINOS_FIJOS;
    expect(coincideBusqueda(escenario, '  jardín ')).toBe(true);
    expect(coincideBusqueda(escenario, 'ESCENARIO')).toBe(true);
    expect(coincideBusqueda(escenario, 'mausoleo')).toBe(false);
    expect(coincideBusqueda(escenario, '')).toBe(true);
  });
});
