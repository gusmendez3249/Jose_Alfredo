import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Aplicacion } from '../App';
import type { MarcadorMapa, TrazoMapa } from '../components/mapa/MapaFestival';
import { crearEvento } from '../test/datos';
import { simularRed, simularSinConexion } from '../test/red';

vi.mock('swiper/react', async () => import('../test/swiper'));

// Leaflet no funciona en jsdom: el mapa se sustituye por una maqueta que expone lo que recibiría.
vi.mock('../components/mapa/MapaFestival', () => ({
  default: (props: {
    marcadores: MarcadorMapa[];
    seleccionId: string | null;
    onSeleccionar: (id: string) => void;
    origen: [number, number] | null;
    ruta: TrazoMapa | null;
  }) => (
    <div data-testid="mapa" data-seleccion={props.seleccionId ?? ''} data-origen={props.origen ? props.origen.join(',') : ''} data-ruta={props.ruta ? (props.ruta.enLinea ? 'recta' : 'osrm') : ''}>
      {props.marcadores.map((m) => (
        <button key={m.id} type="button" data-categoria={m.categoria} onClick={() => props.onSeleccionar(m.id)}>
          Marcador {m.nombre}
        </button>
      ))}
    </div>
  ),
}));

const EN_DIAS = (dias: number) => new Date(Date.now() + dias * 24 * 3600 * 1000).toISOString();

const SITIOS = [
  { id: 'SITIO-CASA-MUSEO', nombre: 'Casa Museo José Alfredo Jiménez', ubicacion: 'Calle Guanajuato 13, Centro', descripcion: 'La casa donde vivió el compositor.', pistasVisuales: '', imagenUrl: null, audioUrl: null, latitud: 21.157341, longitud: -100.93277, activo: true },
  { id: 'SITIO-SIN-GPS', nombre: 'Rincón sin coordenadas', ubicacion: 'Centro', descripcion: '', pistasVisuales: '', imagenUrl: null, audioUrl: null, latitud: null, longitud: null, activo: true },
];
const EVENTOS = [
  crearEvento({ id: 'ev1', nombre: 'Serenata en el Mausoleo', fechaHora: EN_DIAS(2), escenario: 'Mausoleo', latitud: 21.1571, longitud: -100.9406 }),
];

function renderizarMapa(ruta = '/mapa') {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <Aplicacion />
    </MemoryRouter>,
  );
}

const respuestaOsrm = (distancia = 650, duracion = 540) => ({
  ok: true,
  status: 200,
  json: async () => ({ code: 'Ok', routes: [{ distance: distancia, duration: duracion, geometry: { type: 'LineString', coordinates: [[-100.9341, 21.1566], [-100.9406, 21.1571]] } }] }),
});

let obtenerPosicion: ReturnType<typeof vi.fn>;
let fetchFalso: ReturnType<typeof vi.fn>;

function darUbicacion(lat = 21.1563, lon = -100.9338) {
  obtenerPosicion.mockImplementation((ok: PositionCallback) => ok({ coords: { latitude: lat, longitude: lon } } as GeolocationPosition));
}

beforeEach(() => {
  obtenerPosicion = vi.fn();
  Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition: obtenerPosicion }, configurable: true });
  fetchFalso = vi.fn(async () => respuestaOsrm());
  vi.stubGlobal('fetch', fetchFalso);
});

afterEach(() => {
  vi.unstubAllGlobals();
  Reflect.deleteProperty(navigator, 'geolocation');
});

describe('mapa del festival (/mapa)', () => {
  it('muestra destinos fijos, conciertos y sitios con marcadores, y no pide la ubicación al cargar', async () => {
    simularRed({ 'GET /ar/sitios': { data: SITIOS }, 'GET /eventos': { data: EVENTOS } });
    renderizarMapa();

    expect(await screen.findByRole('heading', { level: 1, name: 'Mapa del festival' })).toBeInTheDocument();
    const mapa = await screen.findByTestId('mapa');
    await screen.findByRole('button', { name: 'Marcador Serenata en el Mausoleo' });

    // 4 fijos + 1 concierto con coordenadas + 1 sitio con coordenadas (el otro sitio no tiene).
    const marcadores = within(mapa).getAllByRole('button');
    expect(marcadores.map((m) => m.dataset.categoria)).toEqual(['escenario', 'escenario', 'escenario', 'escenario', 'concierto', 'historico']);
    expect(within(mapa).queryByText(/Rincón sin coordenadas/)).not.toBeInTheDocument();

    const lista = screen.getByRole('list', { name: 'Destinos' });
    expect(within(lista).getAllByRole('listitem')).toHaveLength(7);
    expect(within(lista).getByText('Sin ubicación en el mapa')).toBeInTheDocument();
    expect(obtenerPosicion).not.toHaveBeenCalled();
    expect(fetchFalso).not.toHaveBeenCalled();
  });

  it('filtra por categoría', async () => {
    simularRed({ 'GET /ar/sitios': { data: SITIOS }, 'GET /eventos': { data: EVENTOS } });
    const usuario = userEvent.setup();
    renderizarMapa();
    await screen.findByRole('button', { name: 'Marcador Serenata en el Mausoleo' });

    await usuario.click(screen.getByRole('button', { name: 'Sitios históricos' }));
    const lista = screen.getByRole('list', { name: 'Destinos' });
    expect(within(lista).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      expect.stringContaining('Casa Museo José Alfredo Jiménez'),
      expect.stringContaining('Rincón sin coordenadas'),
    ]);
    expect(screen.getByRole('button', { name: 'Sitios históricos' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('un destino sin coordenadas no ofrece ruta y lo explica', async () => {
    simularRed({ 'GET /ar/sitios': { data: SITIOS }, 'GET /eventos': { data: [] } });
    const usuario = userEvent.setup();
    renderizarMapa();
    await usuario.click(await within(await screen.findByRole('list', { name: 'Destinos' })).findByRole('button', { name: /Rincón sin coordenadas/ }));

    expect(screen.getByText(/no tiene coordenadas registradas/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cómo llegar/i })).not.toBeInTheDocument();
  });

  it('elegir un concierto muestra su ficha con fecha y enlaces, y "Cómo llegar" traza la ruta a pie', async () => {
    simularRed({ 'GET /ar/sitios': { data: SITIOS }, 'GET /eventos': { data: EVENTOS } });
    darUbicacion();
    const usuario = userEvent.setup();
    renderizarMapa();

    await usuario.click(await screen.findByRole('button', { name: 'Marcador Serenata en el Mausoleo' }));
    const ficha = screen.getByRole('region', { name: 'Destino elegido' });
    expect(within(ficha).getByRole('heading', { level: 2, name: 'Serenata en el Mausoleo' })).toBeInTheDocument();
    expect(within(ficha).getByText(/hrs/)).toBeInTheDocument();
    expect(within(ficha).getByRole('link', { name: 'Ver concierto' })).toHaveAttribute('href', '/events/ev1');
    expect(within(ficha).getByRole('link', { name: /Comprar boletos/ })).toHaveAttribute('href', '/checkout/ev1');
    expect(screen.getByTestId('mapa')).toHaveAttribute('data-seleccion', 'evento-ev1');
    expect(obtenerPosicion).not.toHaveBeenCalled();

    await usuario.click(within(ficha).getByRole('button', { name: /Cómo llegar/i }));

    expect(await within(ficha).findByText(/A pie:/)).toHaveTextContent('A pie: 650 m · unos 9 min');
    expect(obtenerPosicion).toHaveBeenCalledTimes(1);
    expect(obtenerPosicion.mock.calls[0][2]).toMatchObject({ timeout: 10_000, maximumAge: 60_000 });
    expect(fetchFalso).toHaveBeenCalledTimes(1);
    const [url, opciones] = fetchFalso.mock.calls[0] as [string, RequestInit];
    expect(url).toContain('https://routing.openstreetmap.de/routed-foot/route/v1/foot/-100.9338,21.1563;-100.9406,21.1571');
    expect(opciones.credentials).toBe('omit');
    expect(screen.getByTestId('mapa')).toHaveAttribute('data-ruta', 'osrm');
    expect(screen.getByTestId('mapa')).toHaveAttribute('data-origen', '21.1563,-100.9338');
    expect(within(ficha).getByText(/solo se envía al servicio externo de rutas/)).toBeInTheDocument();
  });

  it('cambiar de destino cancela la ruta anterior y recalcula sin volver a pedir permiso', async () => {
    simularRed({ 'GET /ar/sitios': { data: [] }, 'GET /eventos': { data: [] } });
    darUbicacion();
    const señales: AbortSignal[] = [];
    fetchFalso.mockImplementation((_url: string, opciones: RequestInit) => {
      señales.push(opciones.signal as AbortSignal);
      // La primera petición queda colgada hasta que se cancela.
      return señales.length === 1
        ? new Promise((_, rechazar) => (opciones.signal as AbortSignal).addEventListener('abort', () => rechazar(new DOMException('x', 'AbortError'))))
        : Promise.resolve(respuestaOsrm(1400, 1000));
    });
    const usuario = userEvent.setup();
    renderizarMapa();

    await usuario.click(await screen.findByRole('button', { name: 'Marcador Plaza Principal' }));
    await usuario.click(screen.getByRole('button', { name: /Cómo llegar/i }));
    await waitFor(() => expect(señales).toHaveLength(1));

    await usuario.click(screen.getByRole('button', { name: 'Marcador Casa Museo' }));
    await waitFor(() => expect(señales).toHaveLength(2));
    expect(señales[0].aborted).toBe(true);
    expect(await screen.findByText(/A pie:/)).toHaveTextContent('1.4 km');
    expect(obtenerPosicion).toHaveBeenCalledTimes(1);
  });

  it('si se niega la ubicación avisa, el mapa sigue disponible y "Reintentar ubicación" vuelve a pedirla', async () => {
    simularRed({ 'GET /ar/sitios': { data: [] }, 'GET /eventos': { data: [] } });
    obtenerPosicion.mockImplementationOnce((_ok: PositionCallback, fallo: PositionErrorCallback) =>
      fallo({ code: 1, PERMISSION_DENIED: 1, TIMEOUT: 3, POSITION_UNAVAILABLE: 2, message: '' } as GeolocationPositionError),
    );
    const usuario = userEvent.setup();
    renderizarMapa();

    await usuario.click(await screen.findByRole('button', { name: 'Marcador Escenario Principal' }));
    await usuario.click(screen.getByRole('button', { name: /Cómo llegar/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/No se permitió el acceso a tu ubicación/);
    expect(screen.getByTestId('mapa')).toHaveAttribute('data-ruta', '');
    expect(screen.getByRole('button', { name: 'Marcador Plaza Principal' })).toBeInTheDocument();

    darUbicacion();
    await usuario.click(screen.getByRole('button', { name: 'Reintentar ubicación' }));
    expect(await screen.findByText(/A pie:/)).toBeInTheDocument();
    expect(obtenerPosicion).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('si el servicio de rutas falla, usa la línea recta y lo dice', async () => {
    simularRed({ 'GET /ar/sitios': { data: [] }, 'GET /eventos': { data: [] } });
    darUbicacion(21.1566, -100.9341);
    fetchFalso.mockRejectedValue(new TypeError('Failed to fetch'));
    const usuario = userEvent.setup();
    renderizarMapa();

    await usuario.click(await screen.findByRole('button', { name: 'Marcador Mausoleo de José Alfredo Jiménez' }));
    await usuario.click(screen.getByRole('button', { name: /Cómo llegar/i }));

    expect(await screen.findByText('Sin servicio de rutas: distancia en línea recta.')).toBeInTheDocument();
    expect(screen.getByText(/En línea recta:/)).toHaveTextContent(/\d+ m · unos \d+ min/);
    expect(screen.getByTestId('mapa')).toHaveAttribute('data-ruta', 'recta');
  });

  it('si fallan los sitios y los eventos, el mapa funciona con los destinos fijos y cada aviso permite reintentar', async () => {
    let intentosSitios = 0;
    simularRed({
      'GET /ar/sitios': () => {
        intentosSitios += 1;
        return intentosSitios === 1 ? { status: 500, data: {} } : { data: SITIOS };
      },
      'GET /eventos': { status: 500, data: {} },
    });
    const usuario = userEvent.setup();
    renderizarMapa();

    expect(await screen.findByText('No se pudieron cargar los sitios históricos.')).toBeInTheDocument();
    expect(screen.getByText('No se pudieron cargar los conciertos.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Marcador Escenario Principal' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    const aviso = screen.getByText('No se pudieron cargar los sitios históricos.').closest('p') as HTMLElement;
    await usuario.click(within(aviso).getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Marcador Casa Museo José Alfredo Jiménez' })).toBeInTheDocument();
    expect(screen.queryByText('No se pudieron cargar los sitios históricos.')).not.toBeInTheDocument();
    expect(screen.getByText('No se pudieron cargar los conciertos.')).toBeInTheDocument();
  });

  it('sin conexión sigue mostrando los destinos fijos', async () => {
    simularSinConexion();
    renderizarMapa();
    expect(await screen.findByText('No se pudieron cargar los conciertos.')).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Destinos' })).toHaveTextContent('Escenario Principal');
  });

  it('llega con un destino preseleccionado por ?destino= y lo mantiene en la URL al elegir otro', async () => {
    simularRed({ 'GET /ar/sitios': { data: SITIOS }, 'GET /eventos': { data: EVENTOS } });
    const usuario = userEvent.setup();
    renderizarMapa('/mapa?destino=ev1');

    const ficha = await screen.findByRole('region', { name: 'Destino elegido' });
    expect(await within(ficha).findByRole('heading', { level: 2, name: 'Serenata en el Mausoleo' })).toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Marcador Casa Museo' }));
    expect(screen.getByRole('heading', { level: 2, name: 'Casa Museo' })).toBeInTheDocument();
    expect(screen.getByTestId('mapa')).toHaveAttribute('data-seleccion', 'lugar-casa-museo');
  });

  it('ignora un ?destino= con forma inválida o inexistente', async () => {
    simularRed({ 'GET /ar/sitios': { data: SITIOS }, 'GET /eventos': { data: EVENTOS } });
    renderizarMapa('/mapa?destino=%3Cscript%3Ealert(1)%3C/script%3E');
    await screen.findByRole('button', { name: 'Marcador Serenata en el Mausoleo' });
    expect(screen.queryByRole('region', { name: 'Destino elegido' })).not.toBeInTheDocument();
    expect(screen.getByTestId('mapa')).toHaveAttribute('data-seleccion', '');
  });
});
