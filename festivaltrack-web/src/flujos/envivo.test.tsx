import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { crearEvento } from '../test/datos';
import { renderizarApp } from '../test/render';
import { simularRed, simularSinConexion, type RespuestaFalsa } from '../test/red';
import { iniciarSesionDePrueba } from '../test/sesion';
import type { MensajeChat } from '../services/stream.service';

vi.mock('swiper/react', async () => import('../test/swiper'));

const VIVO = { streamUrl: 'rtsp://192.168.1.50:1935', emulatorUrl: 'rtsp://10.0.2.2:1935', isLive: true, port: 1935, eventoId: 'EVT-001', eventoNombre: 'Gran Noche de Mariachi' };
const SIN_VIVO = { streamUrl: '', emulatorUrl: '', isLive: false, port: 1935, eventoId: null, eventoNombre: null };

const mensaje = (id: string, minuto: number, parcial: Partial<MensajeChat> = {}): MensajeChat => ({
  id,
  eventoId: 'EVT-001',
  usuarioNombre: 'Luis',
  mensaje: `texto ${id}`,
  esAdmin: false,
  fechaEnvio: new Date(Date.UTC(2026, 10, 25, 20, minuto)).toISOString(),
  ...parcial,
});

/** El servidor manda el más nuevo primero. */
const delServidor = (...cronologicos: MensajeChat[]) => [...cronologicos].reverse();

/** Simula cambiar de pestaña y volver: dispara una consulta inmediata sin esperar el intervalo. */
function ocultarYVolver() {
  const poner = (oculta: boolean) => {
    Object.defineProperty(document, 'hidden', { configurable: true, get: () => oculta });
    document.dispatchEvent(new Event('visibilitychange'));
  };
  poner(true);
  poner(false);
}

afterEach(() => {
  Object.defineProperty(document, 'hidden', { configurable: true, get: () => false });
});

describe('En vivo (/en-vivo)', () => {
  it('sin transmisión: estado vacío, próximo concierto, cartelera y ningún chat', async () => {
    const { peticiones } = simularRed({
      'GET /stream/status': { data: SIN_VIVO },
      'GET /eventos': { data: [crearEvento({ id: 'ev-9', nombre: 'Serenata del Jardín' })] },
    });
    renderizarApp('/en-vivo');

    expect(await screen.findByRole('heading', { name: 'No hay transmisión en este momento' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'En vivo' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Serenata del Jardín' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver concierto' })).toHaveAttribute('href', '/events/ev-9');
    expect(screen.getByRole('link', { name: /Ver cartelera/ })).toHaveAttribute('href', '/events');
    expect(screen.queryByRole('log')).not.toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(peticiones.some((p) => p.url.startsWith('/stream/chat'))).toBe(false);
  });

  it('con transmisión: insignia, concierto, aviso del video, detalle y chat en orden cronológico', async () => {
    simularRed({
      'GET /stream/status': { data: VIVO },
      'GET /stream/chat/EVT-001': {
        data: delServidor(mensaje('1', 1, { usuarioNombre: 'Ana' }), mensaje('2', 2, { usuarioNombre: 'Organización', esAdmin: true }), mensaje('3', 3)),
      },
    });
    renderizarApp('/en-vivo');

    expect(await screen.findByRole('heading', { name: 'Gran Noche de Mariachi' })).toBeInTheDocument();
    expect(screen.getByText('EN VIVO')).toBeInTheDocument();
    expect(screen.getByText(/Mira el video en la app FestivalTrack o en la Smart TV/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver detalle del concierto' })).toHaveAttribute('href', '/events/EVT-001');
    // El video no se intenta reproducir: ninguna URL RTSP llega al DOM.
    expect(document.body.innerHTML).not.toContain('rtsp://');
    expect(document.querySelector('video, iframe, source')).toBeNull();

    const registro = await screen.findByRole('log');
    const items = within(registro).getAllByRole('listitem');
    expect(items.map((li) => within(li).getByText(/^texto/).textContent)).toEqual(['texto 1', 'texto 2', 'texto 3']);
    expect(within(items[1]).getByText('Administrador')).toBeInTheDocument();
    expect(within(items[0]).queryByText('Administrador')).not.toBeInTheDocument();
    expect(registro).toHaveAttribute('aria-live', 'polite');
  });

  it('los mensajes de usuarios se pintan como texto, sin HTML ni enlaces', async () => {
    simularRed({
      'GET /stream/status': { data: VIVO },
      'GET /stream/chat/EVT-001': {
        data: [mensaje('1', 1, { usuarioNombre: '<b>Hacker</b>', mensaje: '<img src=x onerror=alert(1)> http://malo.example/robo' })],
      },
    });
    renderizarApp('/en-vivo');

    const registro = await screen.findByRole('log');
    expect(within(registro).getByText('<b>Hacker</b>')).toBeInTheDocument();
    expect(within(registro).getByText(/<img src=x onerror=alert\(1\)> http:\/\/malo\.example\/robo/)).toBeInTheDocument();
    expect(registro.querySelector('img, b, a')).toBeNull();
  });

  it('sin sesión se lee, no hay caja de envío y se invita a iniciar sesión', async () => {
    simularRed({ 'GET /stream/status': { data: VIVO }, 'GET /stream/chat/EVT-001': { data: [] } });
    renderizarApp('/en-vivo');

    expect(await screen.findByText('Aún no hay mensajes. ¡Saluda a los demás asistentes!')).toBeInTheDocument();
    expect(screen.getByText('Inicia sesión para escribir en el chat')).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    const chat = screen.getByRole('region', { name: 'Chat en vivo' });
    expect(within(chat).getByRole('link', { name: 'Iniciar sesión' })).toHaveAttribute('href', '/login');
  });

  it('con sesión: envía el mensaje, aparece al instante, se limpia el campo y se cuenta hasta 300', async () => {
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    const { peticiones } = simularRed({
      'GET /stream/status': { data: VIVO },
      'GET /stream/chat/EVT-001': { data: delServidor(mensaje('1', 1)) },
      'POST /stream/chat': (cuerpo) => ({
        status: 201,
        data: mensaje('nuevo', 5, { usuarioNombre: 'Ana Pérez', mensaje: (cuerpo as { mensaje: string }).mensaje }),
      }),
    });
    renderizarApp('/en-vivo');

    const campo = await screen.findByRole('textbox', { name: 'Escribe un mensaje' });
    const enviar = screen.getByRole('button', { name: 'Enviar mensaje' });
    expect(enviar).toBeDisabled();
    expect(campo).toHaveAttribute('maxlength', '300');

    await usuario.type(campo, '  ¡Viva México!  ');
    expect(screen.getByText('17/300')).toBeInTheDocument();
    expect(enviar).toBeEnabled();
    await usuario.click(enviar);

    expect(await within(screen.getByRole('log')).findByText('¡Viva México!')).toBeInTheDocument();
    expect(peticiones.find((p) => p.metodo === 'POST')?.cuerpo).toEqual({ eventoId: 'EVT-001', mensaje: '¡Viva México!' });
    expect(campo).toHaveValue('');
    expect(screen.getByText('0/300')).toBeInTheDocument();
  });

  it('mientras envía deshabilita el botón y no manda dos veces', async () => {
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    let liberar: (r: RespuestaFalsa) => void = () => undefined;
    const { peticiones } = simularRed({
      'GET /stream/status': { data: VIVO },
      'GET /stream/chat/EVT-001': { data: [] },
      'POST /stream/chat': () => new Promise<RespuestaFalsa>((resolver) => (liberar = resolver)),
    });
    renderizarApp('/en-vivo');

    await usuario.type(await screen.findByRole('textbox'), 'hola');
    const enviar = screen.getByRole('button', { name: 'Enviar mensaje' });
    await usuario.click(enviar);
    await waitFor(() => expect(enviar).toBeDisabled());
    await usuario.click(enviar);
    expect(peticiones.filter((p) => p.metodo === 'POST')).toHaveLength(1);

    await act(async () => liberar({ status: 201, data: mensaje('x', 9, { mensaje: 'hola' }) }));
    expect(await screen.findByText('hola', { selector: 'p' })).toBeInTheDocument();
  });

  it('un error al enviar muestra un aviso claro y conserva el texto escrito', async () => {
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    simularRed({
      'GET /stream/status': { data: VIVO },
      'GET /stream/chat/EVT-001': { data: [] },
      'POST /stream/chat': { status: 404, data: { message: 'Evento no encontrado' } },
    });
    renderizarApp('/en-vivo');

    const campo = await screen.findByRole('textbox');
    await usuario.type(campo, 'hola a todos');
    await usuario.click(screen.getByRole('button', { name: 'Enviar mensaje' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Este concierto ya no está disponible para el chat.');
    expect(campo).toHaveValue('hola a todos');
  });

  it('cada sondeo fusiona sin duplicar y, si el usuario subió a leer, avisa sin mover la vista', async () => {
    let ronda = 0;
    simularRed({
      'GET /stream/status': { data: VIVO },
      'GET /stream/chat/EVT-001': () => {
        ronda += 1;
        return { data: ronda === 1 ? delServidor(mensaje('1', 1), mensaje('2', 2)) : delServidor(mensaje('1', 1), mensaje('2', 2), mensaje('3', 3)) };
      },
    });
    renderizarApp('/en-vivo');
    const registro = await screen.findByRole('log');

    // jsdom no calcula layout: se simula un chat largo y que el usuario subió a leer.
    Object.defineProperty(registro, 'scrollHeight', { configurable: true, value: 1000 });
    Object.defineProperty(registro, 'clientHeight', { configurable: true, value: 200 });
    registro.scrollTop = 100;
    fireEvent.scroll(registro);

    ocultarYVolver();
    expect(await screen.findByRole('button', { name: 'Mensajes nuevos' })).toBeInTheDocument();
    expect(within(registro).getAllByRole('listitem')).toHaveLength(3);
    expect(registro.scrollTop).toBe(100);

    // Al volver al final el aviso desaparece.
    registro.scrollTop = 800;
    fireEvent.scroll(registro);
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Mensajes nuevos' })).not.toBeInTheDocument());
  });

  it('si el usuario ya estaba al final, la vista baja sola con los mensajes nuevos', async () => {
    let ronda = 0;
    simularRed({
      'GET /stream/status': { data: VIVO },
      'GET /stream/chat/EVT-001': () => {
        ronda += 1;
        return { data: ronda === 1 ? delServidor(mensaje('1', 1)) : delServidor(mensaje('1', 1), mensaje('2', 2)) };
      },
    });
    renderizarApp('/en-vivo');
    const registro = await screen.findByRole('log');
    Object.defineProperty(registro, 'scrollHeight', { configurable: true, value: 600 });
    Object.defineProperty(registro, 'clientHeight', { configurable: true, value: 200 });

    ocultarYVolver();
    await waitFor(() => expect(within(registro).getAllByRole('listitem')).toHaveLength(2));
    expect(registro.scrollTop).toBe(600);
    expect(screen.queryByRole('button', { name: 'Mensajes nuevos' })).not.toBeInTheDocument();
  });

  it('error al consultar el estado: mensaje con reintento que recupera la página', async () => {
    simularSinConexion();
    const usuario = userEvent.setup();
    renderizarApp('/en-vivo');
    expect(await screen.findByRole('alert')).toHaveTextContent(/No se pudo conectar con el servidor/);

    simularRed({ 'GET /stream/status': { data: SIN_VIVO }, 'GET /eventos': { data: [] } });
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('heading', { name: 'No hay transmisión en este momento' })).toBeInTheDocument();
  });

  it('mientras el backend despierta muestra "Consultando…" y no un error', async () => {
    simularRed({ 'GET /stream/status': () => new Promise<RespuestaFalsa>(() => undefined) });
    renderizarApp('/en-vivo');
    expect(await screen.findByText('Consultando…')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('Inicio ampliado (/)', () => {
  const evento = { 'GET /eventos': { data: [crearEvento({ id: 'ev-1', nombre: 'Noche de Bohemia' })] } };

  it('con transmisión muestra el aviso EN VIVO AHORA que lleva a /en-vivo', async () => {
    simularRed({ ...evento, 'GET /stream/status': { data: VIVO } });
    renderizarApp('/');
    const aviso = await screen.findByRole('link', { name: /EN VIVO AHORA · Gran Noche de Mariachi/ });
    expect(aviso).toHaveAttribute('href', '/en-vivo');
  });

  it('sin transmisión no hay aviso', async () => {
    simularRed({ ...evento, 'GET /stream/status': { data: SIN_VIVO } });
    renderizarApp('/');
    expect(await screen.findByRole('heading', { name: 'Noche de Bohemia' })).toBeInTheDocument();
    expect(screen.queryByText(/EN VIVO AHORA/)).not.toBeInTheDocument();
  });

  it('si falla la consulta del estado el resto del Inicio sigue y no hay errores visibles', async () => {
    simularRed({ ...evento, 'GET /stream/status': { status: 500, data: {} } });
    renderizarApp('/');
    expect(await screen.findByRole('heading', { name: 'Noche de Bohemia' })).toBeInTheDocument();
    expect(screen.queryByText(/EN VIVO AHORA/)).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('ofrece tarjetas de acceso a En vivo, Biografía, Comunidad, Mapa, Eventos y Mis boletos', async () => {
    simularRed({ ...evento, 'GET /stream/status': { data: SIN_VIVO } });
    renderizarApp('/');
    const nav = await screen.findByRole('navigation', { name: 'Accesos rápidos' });
    const destinos = within(nav)
      .getAllByRole('link')
      .map((a) => a.getAttribute('href'));
    expect(destinos).toEqual(['/en-vivo', '/biografia', '/comunidad', '/mapa', '/events', '/mis-boletos']);
    expect(within(nav).getByRole('link', { name: /Biografía.*Conoce al Maestro/ })).toBeInTheDocument();
    expect(within(nav).getByRole('link', { name: /Mapa del festival/ })).toBeInTheDocument();
  });
});
