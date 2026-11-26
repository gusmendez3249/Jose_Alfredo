import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  crearCancion,
  desinstalarAudioFalso,
  fallarCarga,
  instalarAudioFalso,
  modoPlay,
  reiniciarContadores,
} from '../components/audio/audioDePrueba';
import { olvidarDemosEnMemoria } from '../components/audio/demosEvento';
import { elementoAudio, reproductor } from '../components/audio/reproductor';
import { crearEvento } from '../test/datos';
import { simularRed } from '../test/red';
import { renderizarApp } from '../test/render';

vi.mock('swiper/react', async () => import('../test/swiper'));

const EN_DIAS = (dias: number) => new Date(Date.now() + dias * 24 * 3600 * 1000).toISOString();

const REPERTORIO = [
  crearCancion({ id: 'c1', titulo: 'El Rey', archivoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3' }),
  crearCancion({ id: 'c2', titulo: 'Caminos de Guanajuato', duracion: 125, archivoUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3' }),
];

const reproductorEnPantalla = () => screen.queryByRole('region', { name: 'Reproductor de demos' });

beforeAll(instalarAudioFalso);
afterAll(desinstalarAudioFalso);
beforeEach(() => {
  reiniciarContadores();
  olvidarDemosEnMemoria();
});
afterEach(() => reproductor.cerrar());

describe('detalle de concierto (/events/:id)', () => {
  it('muestra el concierto con sus acciones y sus demos; al tocar una suena y sigue al navegar', async () => {
    simularRed({
      'GET /eventos/ev-1': { data: crearEvento({ descripcion: 'Una noche de canciones.\nCon mariachi.' }) },
      'GET /canciones/evento/ev-1': { data: REPERTORIO },
      'GET /eventos': { data: [crearEvento()] },
    });
    const usuario = userEvent.setup();
    renderizarApp('/events/ev-1');

    expect(await screen.findByRole('heading', { level: 1, name: 'Noche de Bohemia' })).toBeInTheDocument();
    expect(screen.getByText('José Alfredo Jiménez', { selector: '.concierto-cabecera__artista' })).toBeInTheDocument();
    expect(screen.getByText('Plaza Principal')).toBeInTheDocument();
    expect(screen.getByText('Boletos a la venta')).toBeInTheDocument();
    expect(screen.getByText(/Una noche de canciones/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Comprar boletos del concierto/i })).toHaveAttribute('href', '/checkout/ev-1');
    expect(screen.getByRole('link', { name: /Ver ruta/ })).toHaveAttribute('href', '/events/ev-1/map');

    expect(await screen.findByText('2 canciones · demos de hasta 30 s cada una')).toBeInTheDocument();
    expect(reproductorEnPantalla()).not.toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Reproducir «Caminos de Guanajuato» de José Alfredo Jiménez' }));

    const barra = await screen.findByRole('region', { name: 'Reproductor de demos' });
    expect(within(barra).getByText('Caminos de Guanajuato')).toBeInTheDocument();
    expect(within(barra).getByRole('button', { name: 'Pausar «Caminos de Guanajuato»' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Pausar «Caminos de Guanajuato» de José Alfredo Jiménez' })).toHaveAttribute(
      'aria-current',
      'true',
    );
    const audio = elementoAudio();
    expect(audio.getAttribute('src')).toBe('https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3');

    // Navegar a la cartelera no corta el audio: mismo elemento, misma pista, sigue sonando.
    await usuario.click(screen.getByRole('link', { name: 'Cartelera' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Próximos eventos' })).toBeInTheDocument();
    expect(elementoAudio()).toBe(audio);
    expect(audio.getAttribute('src')).toBe('https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3');
    expect(within(reproductorEnPantalla()!).getByRole('button', { name: 'Pausar «Caminos de Guanajuato»' })).toBeInTheDocument();
    // La tarjeta del concierto activo lo indica.
    expect(await screen.findByText('Sonando ahora')).toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Cerrar reproductor' }));
    expect(reproductorEnPantalla()).not.toBeInTheDocument();
    expect(audio.getAttribute('src')).toBeNull();
  });

  it('el panel tiene la búsqueda accesible, la cola y el aleatorio', async () => {
    simularRed({
      'GET /eventos/ev-1': { data: crearEvento() },
      'GET /canciones/evento/ev-1': { data: REPERTORIO },
    });
    const usuario = userEvent.setup();
    renderizarApp('/events/ev-1');

    await usuario.click(await screen.findByRole('button', { name: /Reproducir todo/i }));
    const barra = await screen.findByRole('region', { name: 'Reproductor de demos' });
    const abrir = within(barra).getByRole('button', { name: /Abrir el reproductor: El Rey/ });
    expect(abrir).toHaveAttribute('aria-expanded', 'false');
    await usuario.click(abrir);
    expect(abrir).toHaveAttribute('aria-expanded', 'true');

    const rango = within(barra).getByRole('slider', { name: 'Posición de la demo' });
    expect(rango).toHaveAttribute('max', '30');
    expect(rango).toHaveAttribute('aria-valuetext', '0:00 de 0:30');
    fireEvent.change(rango, { target: { value: '12' } });
    expect(elementoAudio().currentTime).toBe(12);
    expect(rango).toHaveAttribute('aria-valuetext', '0:12 de 0:30');

    expect(within(barra).getByText('1 de 2')).toBeInTheDocument();
    const aleatorio = within(barra).getByRole('button', { name: 'Orden aleatorio' });
    expect(aleatorio).toHaveAttribute('aria-pressed', 'false');
    await usuario.click(aleatorio);
    expect(aleatorio).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /^Aleatorio$/ })).toHaveAttribute('aria-pressed', 'true');

    await usuario.click(within(barra).getByRole('button', { name: /Caminos de Guanajuato/ }));
    expect(elementoAudio().getAttribute('src')).toContain('SoundHelix-Song-2.mp3');

    await usuario.keyboard('{Escape}');
    expect(abrir).toHaveAttribute('aria-expanded', 'false');
    expect(abrir).toHaveFocus();
  });

  it('si una demo no carga lo avisa y pasa a la siguiente', async () => {
    simularRed({
      'GET /eventos/ev-1': { data: crearEvento() },
      'GET /canciones/evento/ev-1': { data: REPERTORIO },
    });
    modoPlay('silencio');
    const usuario = userEvent.setup();
    renderizarApp('/events/ev-1');

    await usuario.click(await screen.findByRole('button', { name: /Reproducir todo/i }));
    fallarCarga(elementoAudio());

    const barra = await screen.findByRole('region', { name: 'Reproductor de demos' });
    expect(await within(barra).findByText('No se pudo reproducir «El Rey». Pasamos a la siguiente.')).toBeInTheDocument();
    expect(within(barra).getByText('Caminos de Guanajuato')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /«El Rey»/ })).toBeDisabled();
  });

  it('sin repertorio muestra el vacío y ofrece el catálogo general (solo lo publicado)', async () => {
    const red = simularRed({
      'GET /eventos/ev-1': { data: crearEvento() },
      'GET /canciones/evento/ev-1': { data: [] },
      'GET /canciones': {
        data: [
          crearCancion({ id: 'borrador', titulo: 'Borrador secreto', estado: 'BORRADOR' }),
          crearCancion({ id: 'p1', titulo: 'Ella' }),
          { id: 'roto' },
        ],
      },
    });
    const usuario = userEvent.setup();
    renderizarApp('/events/ev-1');

    expect(await screen.findByRole('heading', { name: 'Aún no hay demos para este concierto' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Comprar boletos del concierto/i })).toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Escuchar el catálogo general' }));

    const barra = await screen.findByRole('region', { name: 'Reproductor de demos' });
    expect(within(barra).getByText('Ella')).toBeInTheDocument();
    expect(within(barra).getByText(/Discografía de José Alfredo/)).toBeInTheDocument();
    expect(red.peticiones.filter((p) => p.url === '/canciones')).toHaveLength(1);

    await usuario.click(within(barra).getByRole('button', { name: /Abrir el reproductor/ }));
    expect(within(barra).getByText('1 de 1')).toBeInTheDocument();
    expect(within(barra).queryByText('Borrador secreto')).not.toBeInTheDocument();
  });

  it('un evento inexistente muestra el 404 con enlace a la cartelera', async () => {
    simularRed({ 'GET /canciones/evento/no-existe': { data: [] } });
    renderizarApp('/events/no-existe');

    expect(await screen.findByRole('heading', { level: 1, name: 'No encontramos este concierto' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver la cartelera' })).toHaveAttribute('href', '/events');
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('si fallan las canciones avisa con el texto del móvil y "Reintentar" las vuelve a pedir', async () => {
    let intentos = 0;
    simularRed({
      'GET /eventos/ev-1': { data: crearEvento() },
      'GET /canciones/evento/ev-1': () => {
        intentos += 1;
        return intentos === 1 ? { status: 500, data: {} } : { data: REPERTORIO };
      },
    });
    const usuario = userEvent.setup();
    renderizarApp('/events/ev-1');

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('No pudimos cargar las canciones');
    expect(alerta).toHaveTextContent('Revisa tu conexión a internet e inténtalo de nuevo.');
    // El resto del concierto sigue a la vista.
    expect(screen.getByRole('heading', { level: 1, name: 'Noche de Bohemia' })).toBeInTheDocument();

    await usuario.click(within(alerta).getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('button', { name: /Reproducir «El Rey»/ })).toBeInTheDocument();
    expect(intentos).toBe(2);
  });

  it('un evento que no está a la venta ni tiene coordenadas no ofrece comprar ni ruta', async () => {
    simularRed({
      'GET /eventos/ev-1': { data: crearEvento({ estado: 'FINALIZADO', latitud: null, longitud: null }) },
      'GET /canciones/evento/ev-1': { data: [] },
    });
    renderizarApp('/events/ev-1');

    expect(await screen.findByText('Finalizado')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Comprar boletos/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Ver ruta/ })).not.toBeInTheDocument();
  });
});

describe('cartelera con demos', () => {
  it('cada tarjeta enlaza al detalle y muestra la insignia solo si hay demos', async () => {
    simularRed({
      'GET /eventos': {
        data: [
          crearEvento({ id: 'con', nombre: 'Con demos', fechaHora: EN_DIAS(1) }),
          crearEvento({ id: 'sin', nombre: 'Sin demos', fechaHora: EN_DIAS(2) }),
          crearEvento({ id: 'falla', nombre: 'Falla', fechaHora: EN_DIAS(3) }),
        ],
      },
      'GET /canciones/evento/con': { data: REPERTORIO },
      'GET /canciones/evento/sin': { data: [] },
      'GET /canciones/evento/falla': { status: 500, data: {} },
    });
    renderizarApp('/events');

    const con = await screen.findByRole('article', { name: 'Con demos' });
    expect(within(con).getByRole('link', { name: 'Con demos' })).toHaveAttribute('href', '/events/con');
    expect(within(con).getByRole('link', { name: /Comprar boleto/ })).toHaveAttribute('href', '/checkout/con');
    expect(await within(con).findByText('2 demos para escuchar')).toBeInTheDocument();

    const falla = screen.getByRole('article', { name: 'Falla' });
    await waitFor(() => expect(within(falla).getByRole('link', { name: 'Falla' })).toBeInTheDocument());
    expect(within(screen.getByRole('article', { name: 'Sin demos' })).queryByText(/demos? para escuchar/)).not.toBeInTheDocument();
    expect(within(falla).queryByText(/para escuchar/)).not.toBeInTheDocument();
  });

  it('pide los repertorios como mucho de dos en dos y no repite al volver', async () => {
    let enCurso = 0;
    let maximo = 0;
    let total = 0;
    const manejadores: Record<string, () => Promise<{ data: unknown }>> = {};
    const eventos = Array.from({ length: 6 }, (_, i) => crearEvento({ id: `e${i}`, nombre: `Evento ${i}`, fechaHora: EN_DIAS(i + 1) }));
    for (const ev of eventos) {
      manejadores[`GET /canciones/evento/${ev.id}`] = async () => {
        enCurso += 1;
        total += 1;
        maximo = Math.max(maximo, enCurso);
        await new Promise((r) => setTimeout(r, 5));
        enCurso -= 1;
        return { data: [REPERTORIO[0]] };
      };
    }
    simularRed({ 'GET /eventos': { data: eventos }, ...manejadores });
    const vista = renderizarApp('/events');

    await waitFor(() => expect(screen.getAllByText('1 demo para escuchar')).toHaveLength(6));
    expect(maximo).toBeLessThanOrEqual(2);
    expect(total).toBe(6);

    vista.unmount();
    renderizarApp('/events');
    await waitFor(() => expect(screen.getAllByText('1 demo para escuchar')).toHaveLength(6));
    expect(total).toBe(6);
  });
});
