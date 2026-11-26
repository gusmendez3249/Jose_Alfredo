import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { obtenerToken } from '../auth/session';
import { crearEvento, crearJwt, USUARIO } from '../test/datos';
import { renderizarApp } from '../test/render';
import { simularRed } from '../test/red';
import { iniciarSesionDePrueba } from '../test/sesion';

vi.mock('swiper/react', async () => import('../test/swiper'));

const RESPUESTA_AUTH = { accessToken: crearJwt(Math.floor(Date.now() / 1000) + 3600), usuario: USUARIO };

describe('ruta protegida', () => {
  it('sin sesión manda a /login y, tras iniciar sesión, regresa a la ruta pedida', async () => {
    const red = simularRed({
      'POST /auth/login': { data: RESPUESTA_AUTH },
      'GET /boletos/mis-boletos': { data: [] },
    });
    const usuario = userEvent.setup();
    renderizarApp('/mis-boletos');

    // Redirigido al formulario de acceso en lugar de ver los boletos.
    expect(await screen.findByRole('heading', { level: 1, name: /José Alfredo Jiménez/ })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Mis boletos' })).not.toBeInTheDocument();

    await usuario.type(screen.getByLabelText('Correo electrónico'), '  ana@correo.mx ');
    await usuario.type(screen.getByLabelText('Contraseña'), 'Secreta123');
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Mis boletos' })).toBeInTheDocument();
    expect(screen.getByText('Hola, Ana Pérez', { selector: 'p' })).toBeInTheDocument();

    // El correo se envía sin espacios y la petición posterior ya lleva el token.
    expect(red.peticiones[0].cuerpo).toEqual({ correo: 'ana@correo.mx', contrasena: 'Secreta123' });
    expect(red.peticiones[0].autorizacion).toBeUndefined();
    const boletos = red.peticiones.find((p) => p.url === '/boletos/mis-boletos');
    expect(boletos?.autorizacion).toBe(`Bearer ${RESPUESTA_AUTH.accessToken}`);
  });

  it('avisa que la sesión expiró cuando el cierre fue por caducidad', async () => {
    simularRed({ 'GET /boletos/mis-boletos': { status: 401, data: {} } });
    iniciarSesionDePrueba();
    renderizarApp('/mis-boletos');

    // El backend rechaza el token: la web cierra la sesión y lleva al login con el aviso.
    expect(await screen.findByText('Tu sesión expiró. Inicia sesión de nuevo.')).toBeInTheDocument();
    expect(obtenerToken()).toBeNull();
  });
});

describe('login', () => {
  it('con credenciales malas muestra el error y no inicia sesión', async () => {
    simularRed({ 'POST /auth/login': { status: 401, data: { message: 'Unauthorized' } } });
    const usuario = userEvent.setup();
    renderizarApp('/login');

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'ana@correo.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'incorrecta');
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.');
    expect(obtenerToken()).toBeNull();
    // El formulario sigue disponible para reintentar.
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeEnabled();
  });

  it('muestra un mensaje claro cuando hay demasiados intentos (429)', async () => {
    simularRed({ 'POST /auth/login': { status: 429, data: { message: 'ThrottlerException: Too Many Requests' } } });
    const usuario = userEvent.setup();
    renderizarApp('/login');

    await usuario.type(await screen.findByLabelText('Correo electrónico'), 'ana@correo.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'x');
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/Demasiados intentos de inicio de sesión/);
  });

  it('el botón de ojo alterna entre ocultar y mostrar la contraseña', async () => {
    simularRed({});
    const usuario = userEvent.setup();
    renderizarApp('/login');

    const campo = await screen.findByLabelText('Contraseña');
    expect(campo).toHaveAttribute('type', 'password');
    await usuario.click(screen.getByRole('button', { name: 'Mostrar contraseña' }));
    expect(campo).toHaveAttribute('type', 'text');
    await usuario.click(screen.getByRole('button', { name: 'Ocultar contraseña' }));
    expect(campo).toHaveAttribute('type', 'password');
  });

  it('con sesión iniciada, /login redirige a mis boletos', async () => {
    simularRed({ 'GET /boletos/mis-boletos': { data: [] } });
    iniciarSesionDePrueba();
    renderizarApp('/login');
    expect(await screen.findByRole('heading', { level: 1, name: 'Mis boletos' })).toBeInTheDocument();
  });
});

describe('registro', () => {
  it('valida la contraseña antes de llamar al servidor', async () => {
    const red = simularRed({});
    const usuario = userEvent.setup();
    renderizarApp('/register');

    await usuario.type(await screen.findByLabelText('Nombre completo'), 'Ana Pérez');
    await usuario.type(screen.getByLabelText('Correo electrónico'), 'ana@correo.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'sinmayuscula1');
    await usuario.click(screen.getByRole('button', { name: 'Registrarse ahora' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La contraseña debe tener al menos 8 caracteres, una mayúscula y un número',
    );
    expect(red.peticiones).toHaveLength(0);
  });

  it('con datos válidos crea la cuenta y entra con la sesión iniciada', async () => {
    const red = simularRed({
      'POST /auth/register': { status: 201, data: RESPUESTA_AUTH },
      'GET /eventos': { data: [crearEvento({ nombre: 'Gran Gala Mariachi' })] },
    });
    const usuario = userEvent.setup();
    renderizarApp('/register');

    await usuario.type(await screen.findByLabelText('Nombre completo'), 'Ana Pérez');
    await usuario.type(screen.getByLabelText('Correo electrónico'), 'ana@correo.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'Secreta123');
    await usuario.click(screen.getByRole('button', { name: 'Registrarse ahora' }));

    // Destino por defecto: la cartelera, ya con la sesión iniciada.
    expect(await screen.findByRole('heading', { level: 1, name: 'Próximos eventos' })).toBeInTheDocument();
    expect(red.peticiones[0].cuerpo).toEqual({ nombre: 'Ana Pérez', correo: 'ana@correo.mx', contrasena: 'Secreta123' });
    expect(obtenerToken()).toBe(RESPUESTA_AUTH.accessToken);
    expect(screen.getByText('Hola, Ana Pérez')).toBeInTheDocument();
  });

  it('explica que el correo ya está registrado (409)', async () => {
    simularRed({ 'POST /auth/register': { status: 409, data: { message: 'Conflict' } } });
    const usuario = userEvent.setup();
    renderizarApp('/register');

    await usuario.type(await screen.findByLabelText('Nombre completo'), 'Ana');
    await usuario.type(screen.getByLabelText('Correo electrónico'), 'ana@correo.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'Secreta123');
    await usuario.click(screen.getByRole('button', { name: 'Registrarse ahora' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ese correo ya está registrado');
    await waitFor(() => expect(obtenerToken()).toBeNull());
  });
});

describe('cerrar sesión', () => {
  it('pide confirmación y, al confirmar, limpia la sesión y vuelve al inicio', async () => {
    simularRed({ 'GET /boletos/mis-boletos': { data: [] }, 'GET /eventos': { data: [] } });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/mis-boletos');

    await screen.findByRole('heading', { level: 1, name: 'Mis boletos' });
    await usuario.click(screen.getByRole('button', { name: /Cerrar sesión/ }));

    const dialogo = await screen.findByRole('dialog', { name: '¿Cerrar sesión?' });
    // Aún no se cerró: la confirmación es obligatoria.
    expect(obtenerToken()).not.toBeNull();
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'El Rey Vive en Su Tierra' })).toBeInTheDocument();
    expect(obtenerToken()).toBeNull();
    expect(localStorage.getItem('festivaltrack.sesion')).toBeNull();
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('cancelar el diálogo conserva la sesión', async () => {
    simularRed({ 'GET /boletos/mis-boletos': { data: [] } });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/mis-boletos');

    await screen.findByRole('heading', { level: 1, name: 'Mis boletos' });
    await usuario.click(screen.getByRole('button', { name: /Cerrar sesión/ }));
    await usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancelar' }));

    expect(obtenerToken()).not.toBeNull();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

describe('ruta desconocida', () => {
  it('muestra la página 404 con salidas hacia el inicio y la cartelera', async () => {
    simularRed({});
    renderizarApp('/ruta-que-no-existe');

    expect(await screen.findByRole('heading', { level: 1, name: 'Página no encontrada' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir al inicio' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Ver eventos' })).toHaveAttribute('href', '/events');
  });
});
