import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderizarApp } from '../test/render';
import { simularRed, simularSinConexion } from '../test/red';

vi.mock('swiper/react', async () => import('../test/swiper'));

const BIOGRAFIA = {
  id: 'b1',
  artistaId: 'a1',
  descripcion: 'Compositor de Dolores Hidalgo que cantó al pueblo.',
  citaCelebre: '"La vida no vale nada."',
  hitos: '["Nació en Dolores Hidalgo","Escribió más de mil canciones"]',
  discografia: '["Serenata del Rey","Camino Real"]',
  artista: { id: 'a1', nombre: 'José Alfredo Jiménez', imagenUrl: 'https://images.unsplash.com/foto.jpg' },
};

describe('biografía (/biografia)', () => {
  it('pinta la biografía de la API, sin duplicar las comillas de la cita, y la cronología fija', async () => {
    simularRed({ 'GET /biografias': { data: [BIOGRAFIA] } });
    renderizarApp('/biografia');

    expect(await screen.findByRole('heading', { level: 1, name: /Vida y Obra/ })).toBeInTheDocument();
    expect(screen.getByText('Compositor de Dolores Hidalgo que cantó al pueblo.')).toBeInTheDocument();
    expect(screen.getByText('"La vida no vale nada."')).toBeInTheDocument();

    const hitos = within(screen.getByRole('region', { name: 'Hitos' })).getAllByRole('listitem');
    expect(hitos.map((h) => h.textContent)).toEqual(['Nació en Dolores Hidalgo', 'Escribió más de mil canciones']);

    expect(screen.getByText('Serenata del Rey')).toBeInTheDocument();
    expect(screen.getByText('La Enorme Distancia')).toBeInTheDocument();

    const archivo = within(screen.getByRole('region', { name: 'Archivo histórico' }));
    expect(archivo.getAllByRole('article')).toHaveLength(9);
    expect(archivo.getByText('El Mausoleo de la Inmortalidad: El Sarape de Azulejos')).toBeInTheDocument();

    expect(screen.getByRole('img', { name: /Retrato de José Alfredo/ })).toHaveAttribute(
      'src',
      'https://images.unsplash.com/foto.jpg',
    );
    expect(screen.getByRole('link', { name: /Escuchar demos/ })).toHaveAttribute('href', '/events');
  });

  it('si la imagen del artista no es de un host permitido, usa el retrato propio', async () => {
    simularRed({
      'GET /biografias': { data: [{ ...BIOGRAFIA, artista: { ...BIOGRAFIA.artista, imagenUrl: 'https://malo.example/x.jpg' } }] },
    });
    renderizarApp('/biografia');
    const retrato = await screen.findByRole('img', { name: /Retrato de José Alfredo/ });
    expect(retrato).toHaveAttribute('src', '/images/biografia/jose_alfredo_portrait.webp');
  });

  it('sin biografías en la API muestra los textos fijos del móvil y la cronología', async () => {
    simularRed({ 'GET /biografias': { data: [] } });
    renderizarApp('/biografia');

    expect(await screen.findByText(/es el compositor más trascendental de la música vernácula mexicana/)).toBeInTheDocument();
    expect(screen.getByText('“No tengo trono ni reina, ni nadie que me comprenda, pero sigo siendo el Rey.”')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Hitos' })).not.toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Archivo histórico' })).getAllByRole('article')).toHaveLength(9);
  });

  it('ante un error muestra aviso con reintento y conserva la cronología; reintentar recupera', async () => {
    simularSinConexion();
    renderizarApp('/biografia');

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar la biografía');
    expect(screen.getByRole('region', { name: 'Archivo histórico' })).toBeInTheDocument();

    simularRed({ 'GET /biografias': { data: [BIOGRAFIA] } });
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('Compositor de Dolores Hidalgo que cantó al pueblo.')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
