import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { crearEvento } from '../test/datos';
import { renderizarApp } from '../test/render';
import { simularRed, simularSinConexion } from '../test/red';

vi.mock('swiper/react', async () => import('../test/swiper'));

const EN_DIAS = (dias: number) => new Date(Date.now() + dias * 24 * 3600 * 1000).toISOString();

describe('cartelera (/events)', () => {
  it('pinta los próximos eventos en orden, con sus acciones y sin los finalizados', async () => {
    simularRed({
      'GET /eventos': {
        data: [
          crearEvento({ id: 'lejos', nombre: 'Gran Gala Mariachi', fechaHora: EN_DIAS(5) }),
          crearEvento({ id: 'pasado', nombre: 'Evento Finalizado', estado: 'FINALIZADO', fechaHora: EN_DIAS(-9) }),
          crearEvento({ id: 'cerca', nombre: 'Serenata en el Mausoleo', fechaHora: EN_DIAS(1) }),
          crearEvento({ id: 'sin-mapa', nombre: 'Recorrido Casa Museo', fechaHora: EN_DIAS(3), latitud: null, longitud: null }),
        ],
      },
    });
    renderizarApp('/events');

    const tarjetas = await screen.findAllByRole('article');
    expect(tarjetas.map((t) => within(t).getByRole('heading', { level: 2 }).textContent)).toEqual([
      'Serenata en el Mausoleo',
      'Recorrido Casa Museo',
      'Gran Gala Mariachi',
    ]);
    expect(screen.queryByText('Evento Finalizado')).not.toBeInTheDocument();

    const primera = within(tarjetas[0]);
    expect(primera.getByText('José Alfredo Jiménez')).toBeInTheDocument();
    expect(primera.getByText('Plaza Principal')).toBeInTheDocument();
    expect(primera.getByRole('link', { name: /Ver ruta/ })).toHaveAttribute('href', '/events/cerca/map');
    expect(primera.getByRole('link', { name: /Comprar boleto/ })).toHaveAttribute('href', '/checkout/cerca');

    // Sin coordenadas no hay "Ver ruta", pero sí se puede comprar.
    const sinMapa = within(tarjetas[1]);
    expect(sinMapa.queryByRole('link', { name: /Ver ruta/ })).not.toBeInTheDocument();
    expect(sinMapa.getByRole('link', { name: /Comprar boleto/ })).toBeInTheDocument();
  });

  it('un evento EN_CURSO se puede comprar; uno no a la venta solo se consulta', async () => {
    simularRed({
      'GET /eventos': {
        data: [
          crearEvento({ id: 'ya', nombre: 'En curso', estado: 'EN_CURSO', fechaHora: EN_DIAS(0.01) }),
        ],
      },
    });
    renderizarApp('/events');
    expect(await screen.findByRole('link', { name: /Comprar boleto para En curso/ })).toBeInTheDocument();
  });

  it('sin eventos próximos muestra el estado vacío', async () => {
    simularRed({ 'GET /eventos': { data: [crearEvento({ estado: 'CANCELADO' })] } });
    renderizarApp('/events');

    expect(await screen.findByRole('heading', { name: 'No hay eventos próximos' })).toBeInTheDocument();
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  it('ante un error muestra el motivo y "Reintentar" vuelve a pedir los eventos', async () => {
    let intentos = 0;
    simularRed({
      'GET /eventos': () => {
        intentos += 1;
        return intentos === 1 ? { status: 500, data: {} } : { data: [crearEvento({ nombre: 'Gran Gala Mariachi' })] };
      },
    });
    const usuario = userEvent.setup();
    renderizarApp('/events');

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('No se pudieron cargar los eventos');
    expect(alerta).toHaveTextContent(/El servidor tuvo un problema/);

    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('heading', { level: 2, name: 'Gran Gala Mariachi' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(intentos).toBe(2);
  });

  it('sin conexión explica el problema de red', async () => {
    simularSinConexion();
    renderizarApp('/events');
    expect(await screen.findByRole('alert')).toHaveTextContent(/No se pudo conectar con el servidor/);
  });
});

describe('inicio (/)', () => {
  it('destaca el primer evento próximo con enlaces a su ruta y a comprar', async () => {
    simularRed({
      'GET /eventos': {
        data: [
          crearEvento({ id: 'b', nombre: 'Segundo evento', fechaHora: EN_DIAS(4) }),
          crearEvento({ id: 'a', nombre: 'Primer evento', fechaHora: EN_DIAS(2) }),
        ],
      },
    });
    renderizarApp('/');

    expect(await screen.findByRole('heading', { level: 1, name: 'El Rey Vive en Su Tierra' })).toBeInTheDocument();
    const destacado = await screen.findByRole('article', { name: 'Primer evento' });
    expect(within(destacado).getByRole('link', { name: /Comprar boletos/ })).toHaveAttribute('href', '/checkout/a');
    expect(within(destacado).getByRole('link', { name: /Ver ruta/ })).toHaveAttribute('href', '/events/a/map');
    expect(screen.queryByText('Segundo evento')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /VER EVENTOS|Ver eventos/ })).toHaveAttribute('href', '/events');
  });

  it('la navegación marca la página actual con aria-current', async () => {
    simularRed({ 'GET /eventos': { data: [] } });
    renderizarApp('/events');
    const nav = await screen.findByRole('navigation', { name: 'Principal' });
    expect(within(nav).getByRole('link', { name: 'Eventos' })).toHaveAttribute('aria-current', 'page');
    expect(within(nav).getByRole('link', { name: 'Inicio' })).not.toHaveAttribute('aria-current');
  });
});
