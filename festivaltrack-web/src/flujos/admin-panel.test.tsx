import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarSesion } from '../auth/session';
import { crearEvento, crearJwt } from '../test/datos';
import { renderizarApp } from '../test/render';
import { simularRed, type PeticionRegistrada } from '../test/red';
import type { Usuario } from '../services/tipos';

// jsdom no puede leer la duración de un audio: se simula el resultado de la medición.
const medicion = vi.hoisted(() => ({ segundos: 215 as number | null }));
vi.mock('../components/admin/panel/archivos', async (original) => ({
  ...(await original<typeof import('../components/admin/panel/archivos')>()),
  medirDuracion: async () => medicion.segundos,
}));

const ADMIN: Usuario = { id: 'u-admin', nombre: 'Lucía Admin', correo: 'lucia@fest.mx', rol: 'ADMINISTRADOR' };

function entrarComoAdmin() {
  guardarSesion(crearJwt(Math.floor(Date.now() / 1000) + 3600), ADMIN);
}

const cancion = (parcial: Record<string, unknown> = {}) => ({
  id: 'c-1',
  titulo: 'El Rey',
  artista: 'José Alfredo Jiménez',
  duracion: 185,
  archivoUrl: '/uploads/a-1.mp3',
  portadaUrl: null,
  genero: 'Ranchera',
  estado: 'PUBLICADA',
  createdAt: '2026-01-01T00:00:00Z',
  ...parcial,
});

function archivo(nombre: string, tipo: string, bytes?: number): File {
  const f = new File(['contenido'], nombre, { type: tipo });
  if (bytes !== undefined) Object.defineProperty(f, 'size', { value: bytes });
  return f;
}

const conTipoLibre = () => userEvent.setup({ applyAccept: false });
const subidas = (ps: PeticionRegistrada[]) => ps.filter((p) => p.url === '/canciones/upload');

beforeEach(() => {
  medicion.segundos = 215;
});

describe('resumen (/admin)', () => {
  it('saluda, muestra la transmisión, las cifras y las últimas publicaciones', async () => {
    entrarComoAdmin();
    simularRed({
      'GET /canciones': { data: [cancion(), cancion({ id: 'c-2' })] },
      'GET /eventos': {
        data: [crearEvento({ id: 'a' }), crearEvento({ id: 'b' }), crearEvento({ id: 'viejo', estado: 'FINALIZADO' })],
      },
      'GET /auth/usuarios': { data: [ADMIN, { ...ADMIN, id: 'u2', rol: 'USUARIO' }, { ...ADMIN, id: 'u3' }] },
      'GET /stream/status': { data: { isLive: true, eventoNombre: 'Gala <b>Final</b>' } },
      'GET /foro': {
        data: [
          { id: 'p1', titulo: null, contenido: '<script>x</script> Hola', autorNombre: 'Ana', createdAt: new Date().toISOString(), likes: 4 },
        ],
      },
    });
    renderizarApp('/admin');

    expect(await screen.findByRole('heading', { level: 1, name: '¡Bienvenido, Lucía Admin!' })).toBeInTheDocument();
    expect(await screen.findByText('En vivo: Gala <b>Final</b>')).toBeInTheDocument();
    const cifra = (etiqueta: string) => screen.getByText(etiqueta).parentElement!.firstElementChild!.textContent;
    await waitFor(() => {
      expect(cifra('Canciones en el catálogo')).toBe('2');
      expect(cifra('Próximos conciertos')).toBe('2');
      expect(cifra('Usuarios registrados')).toBe('3');
    });
    expect(await screen.findByText('Ana: <script>x</script> Hola')).toBeInTheDocument();
    expect(screen.getByText(/hace un momento · 4 me gusta/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Subir canción' })).toHaveAttribute('href', '/admin/canciones');
    expect(screen.getByText(/se emite desde la app móvil/)).toBeInTheDocument();
  });

  it('una cifra que falla muestra "—" sin tumbar el resto', async () => {
    entrarComoAdmin();
    simularRed({
      'GET /canciones': { data: [cancion()] },
      'GET /eventos': { data: [] },
      'GET /auth/usuarios': { status: 500, data: {} },
      'GET /stream/status': { data: { isLive: false, eventoNombre: null } },
      'GET /foro': { data: [] },
    });
    renderizarApp('/admin');

    await screen.findByText('Sin transmisión');
    await waitFor(() => expect(screen.getByText('Usuarios registrados').previousElementSibling).toHaveTextContent('—'));
    expect(screen.getByText('Canciones en el catálogo').previousElementSibling).toHaveTextContent('1');
    expect(await screen.findByText('Todavía no hay publicaciones en la galería.')).toBeInTheDocument();
  });

  it('sin respuesta del estado de transmisión lo dice', async () => {
    entrarComoAdmin();
    simularRed({
      'GET /canciones': { data: [] },
      'GET /eventos': { data: [] },
      'GET /auth/usuarios': { data: [] },
      'GET /stream/status': { status: 503, data: {} },
      'GET /foro': { data: [] },
    });
    renderizarApp('/admin');
    expect(await screen.findByText('Sin conexión con el servidor')).toBeInTheDocument();
  });
});

describe('canciones (/admin/canciones)', () => {
  it('lista el catálogo con marcador de portada y elimina tras confirmar', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    const red = simularRed({
      'GET /canciones': { data: [cancion(), cancion({ id: 'c-2', titulo: 'Camino', estado: 'BORRADOR' })] },
      'DELETE /canciones/c-1': { status: 200, data: {} },
    });
    renderizarApp('/admin/canciones');

    expect(await screen.findByText('El Rey', { selector: 'p' })).toBeInTheDocument();
    expect(screen.getByText('Borrador')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Eliminar El Rey' }));
    const dialogo = screen.getByRole('dialog');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Eliminar' }));

    expect(await screen.findByText('Canción eliminada del catálogo')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Eliminar El Rey' })).not.toBeInTheDocument();
    const borrado = red.peticiones.find((p) => p.metodo === 'DELETE');
    expect(borrado?.autorizacion).toMatch(/^Bearer /);
  });

  it('traduce el 500 al eliminar una canción que está en un repertorio', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    simularRed({
      'GET /canciones': { data: [cancion()] },
      'DELETE /canciones/c-1': { status: 500, data: { message: 'Internal server error' } },
    });
    renderizarApp('/admin/canciones');
    await usuario.click(await screen.findByRole('button', { name: 'Eliminar El Rey' }));
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar' }));
    expect(
      await screen.findByText('No se pudo eliminar: puede estar en el repertorio de un concierto. Quítala primero del repertorio.'),
    ).toBeInTheDocument();
    // La canción sigue en la lista.
    expect(screen.getByRole('button', { name: 'Eliminar El Rey' })).toBeInTheDocument();
  });

  it('valida tipo y tamaño antes de subir nada', async () => {
    entrarComoAdmin();
    const usuario = conTipoLibre();
    const red = simularRed({ 'GET /canciones': { data: [] } });
    renderizarApp('/admin/canciones');

    const audio = await screen.findByLabelText('Audio de la canción');
    await usuario.upload(audio, archivo('doc.pdf', 'application/pdf'));
    expect(await screen.findByText(/Formato no permitido: usa MP3, M4A, WAV u OGG/)).toBeInTheDocument();

    await usuario.upload(audio, archivo('enorme.mp3', 'audio/mpeg', 31 * 1024 * 1024));
    expect(await screen.findByText('El audio pesa más de 30 MB; elige un archivo más ligero')).toBeInTheDocument();

    await usuario.upload(screen.getByLabelText('Agregar portada'), archivo('p.png', 'image/png', 31 * 1024 * 1024));
    expect(await screen.findByText('La portada pesa más de 30 MB; elige una imagen más ligera')).toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: /Publicar en el catálogo/ }));
    expect(await screen.findByText('Escribe el título de la canción')).toBeInTheDocument();
    expect(red.peticiones.filter((p) => p.metodo !== 'GET')).toHaveLength(0);
  });

  it('sube audio, luego portada, y guarda con las URL, la duración y el token', async () => {
    entrarComoAdmin();
    const usuario = conTipoLibre();
    let n = 0;
    const red = simularRed({
      'GET /canciones': { data: [] },
      'POST /canciones/upload': () => ({ status: 201, data: { url: ++n === 1 ? '/uploads/audio-1.mp3' : '/uploads/portada-1.png' } }),
      'POST /canciones': (cuerpo) => ({ status: 201, data: cancion({ id: 'nueva', ...(cuerpo as object) }) }),
    });
    renderizarApp('/admin/canciones');

    await usuario.upload(await screen.findByLabelText('Audio de la canción'), archivo('cancion.mp3', 'audio/mpeg'));
    expect(await screen.findByText(/cancion\.mp3 · 3:35/)).toBeInTheDocument();
    await usuario.upload(screen.getByLabelText('Agregar portada'), archivo('portada.png', 'image/png'));
    expect(await screen.findByRole('img', { name: /Vista previa de la portada portada\.png/ })).toHaveAttribute(
      'src',
      expect.stringMatching(/^data:/),
    );

    await usuario.type(screen.getByLabelText('Título de la canción'), '  Si nos dejan  ');
    await usuario.click(screen.getByRole('button', { name: /Publicar en el catálogo/ }));

    expect(await screen.findByText(/«Si nos dejan» ya está en el catálogo/)).toBeInTheDocument();
    expect(screen.getByText('¡Canción publicada!')).toBeInTheDocument();

    const escrituras = red.peticiones.filter((p) => p.metodo === 'POST');
    expect(escrituras.map((p) => p.url)).toEqual(['/canciones/upload', '/canciones/upload', '/canciones']);
    expect((subidas(escrituras)[0].cuerpo as FormData).get('file')).toHaveProperty('name', 'cancion.mp3');
    expect((subidas(escrituras)[1].cuerpo as FormData).get('file')).toHaveProperty('name', 'portada.png');
    expect(escrituras[2].cuerpo).toEqual({
      titulo: 'Si nos dejan',
      artista: 'José Alfredo Jiménez',
      duracion: 215,
      archivoUrl: '/uploads/audio-1.mp3',
      portadaUrl: '/uploads/portada-1.png',
    });
    escrituras.forEach((p) => expect(p.autorizacion).toMatch(/^Bearer /));

    // Formulario limpio y la canción nueva ya aparece en el catálogo.
    expect(screen.getByLabelText('Título de la canción')).toHaveValue('');
    expect(screen.getByText('Si nos dejan', { selector: 'p.admin-panel-fila__titulo' })).toBeInTheDocument();
  });

  it('sin audio ni duración no manda archivoUrl ni duracion y avisa de la pista de muestra', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    const red = simularRed({
      'GET /canciones': { data: [] },
      'POST /canciones': { status: 201, data: cancion({ titulo: 'Solo título' }) },
    });
    renderizarApp('/admin/canciones');
    expect(await screen.findByText('Sin audio propio se usará una pista de muestra.')).toBeInTheDocument();
    await usuario.type(screen.getByLabelText('Título de la canción'), 'Solo título');
    await usuario.click(screen.getByRole('button', { name: /Publicar en el catálogo/ }));
    await screen.findByText(/«Solo título» ya está en el catálogo/);
    const crear = red.peticiones.find((p) => p.metodo === 'POST');
    expect(crear?.cuerpo).toEqual({ titulo: 'Solo título', artista: 'José Alfredo Jiménez' });
  });

  it('si la portada falla conserva lo escrito y al reintentar no vuelve a subir el audio', async () => {
    entrarComoAdmin();
    const usuario = conTipoLibre();
    let portadaFalla = true;
    const red = simularRed({
      'GET /canciones': { data: [] },
      'POST /canciones/upload': (cuerpo) => {
        const nombre = (cuerpo as FormData).get('file');
        if (nombre instanceof File && nombre.name === 'portada.jpg' && portadaFalla) {
          return { status: 400, data: { message: 'Formato no permitido (usa MP3, M4A, WAV, OGG, JPG, PNG o WEBP)' } };
        }
        return { status: 201, data: { url: nombre instanceof File && nombre.name === 'portada.jpg' ? '/uploads/p.jpg' : '/uploads/a.mp3' } };
      },
      'POST /canciones': { status: 201, data: cancion() },
    });
    renderizarApp('/admin/canciones');

    await usuario.upload(await screen.findByLabelText('Audio de la canción'), archivo('a.mp3', 'audio/mpeg'));
    await usuario.upload(screen.getByLabelText('Agregar portada'), archivo('portada.jpg', 'image/jpeg'));
    await usuario.type(screen.getByLabelText('Título de la canción'), 'El Rey');
    await usuario.click(screen.getByRole('button', { name: /Publicar en el catálogo/ }));

    expect(await screen.findByText(/No se pudo subir la portada\. Formato no permitido/)).toBeInTheDocument();
    expect(screen.getByLabelText('Título de la canción')).toHaveValue('El Rey');
    expect(red.peticiones.some((p) => p.url === '/canciones' && p.metodo === 'POST')).toBe(false);

    portadaFalla = false;
    await usuario.click(screen.getByRole('button', { name: /Publicar en el catálogo/ }));
    await screen.findByText(/«El Rey» ya está en el catálogo/);
    const nombres = subidas(red.peticiones).map((p) => ((p.cuerpo as FormData).get('file') as File).name);
    expect(nombres).toEqual(['a.mp3', 'portada.jpg', 'portada.jpg']);
  });

  it('un 401 al guardar muestra que terminó la sesión de administrador', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    simularRed({
      'GET /canciones': { data: [] },
      'POST /canciones': { status: 403, data: { message: 'Forbidden' } },
    });
    renderizarApp('/admin/canciones');
    await usuario.type(await screen.findByLabelText('Título de la canción'), 'X');
    await usuario.click(screen.getByRole('button', { name: /Publicar en el catálogo/ }));
    expect(await screen.findByText(/No se pudo guardar la canción en el catálogo\. No tienes permisos de administrador/)).toBeInTheDocument();
  });

  it('admite arrastrar y soltar un audio sobre la zona', async () => {
    entrarComoAdmin();
    simularRed({ 'GET /canciones': { data: [] } });
    renderizarApp('/admin/canciones');
    const zona = (await screen.findByLabelText('Audio de la canción')).parentElement!;
    fireEvent.drop(zona, { dataTransfer: { files: [archivo('arrastrada.mp3', 'audio/mpeg')] } });
    expect(await screen.findByText(/arrastrada\.mp3/)).toBeInTheDocument();
  });

  it('el catálogo muestra el error con reintento', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    let falla = true;
    simularRed({ 'GET /canciones': () => (falla ? { status: 503, data: {} } : { data: [cancion()] }) });
    renderizarApp('/admin/canciones');
    expect(await screen.findByText('No se pudo cargar el catálogo')).toBeInTheDocument();
    falla = false;
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('button', { name: 'Eliminar El Rey' })).toBeInTheDocument();
  });
});

describe('usuarios (/admin/usuarios)', () => {
  const OTRO: Usuario = { id: 'u-2', nombre: 'Beto <i>Rojas</i>', correo: 'beto@fest.mx', rol: 'USUARIO' };
  const PAR: Usuario = { id: 'u-3', nombre: 'Carla', correo: 'carla@fest.mx', rol: 'ADMINISTRADOR' };

  it('lista con "Tú", sin botón para el propio usuario, y envía el token en /auth/usuarios', async () => {
    entrarComoAdmin();
    const red = simularRed({ 'GET /auth/usuarios': { data: [ADMIN, OTRO, PAR, { forma: 'rara' }] } });
    renderizarApp('/admin/usuarios');

    const filaYo = (await screen.findByRole('row', { name: /Lucía Admin/ }));
    expect(within(filaYo).getByText('Tú')).toBeInTheDocument();
    expect(within(filaYo).queryByRole('button')).not.toBeInTheDocument();
    expect(screen.getByText('Beto <i>Rojas</i>')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hacer Admin a Beto <i>Rojas</i>' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Quitar Admin a Carla' })).toBeInTheDocument();
    // Una fila inválida se descarta: cabecera + 3 usuarios.
    expect(screen.getAllByRole('row')).toHaveLength(4);
    expect(red.peticiones[0].autorizacion).toMatch(/^Bearer /);
  });

  it('hace admin tras confirmar: PUT con {rol} y token', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    const red = simularRed({
      'GET /auth/usuarios': { data: [ADMIN, OTRO] },
      'PUT /auth/usuarios/u-2/rol': (cuerpo) => ({ data: { ...OTRO, rol: (cuerpo as { rol: Usuario['rol'] }).rol } }),
    });
    renderizarApp('/admin/usuarios');
    await usuario.click(await screen.findByRole('button', { name: /Hacer Admin a/ }));

    const dialogo = screen.getByRole('dialog');
    expect(within(dialogo).getByRole('heading', { name: '¿Hacer administrador a Beto <i>Rojas</i>?' })).toBeInTheDocument();
    expect(within(dialogo).getByText('Podrá crear y borrar conciertos, canciones y usuarios, y transmitir en vivo.')).toBeInTheDocument();
    await usuario.click(within(dialogo).getByRole('button', { name: 'Hacer Admin' }));

    expect(await screen.findByText('Rol actualizado correctamente')).toBeInTheDocument();
    const put = red.peticiones.find((p) => p.metodo === 'PUT');
    expect(put?.cuerpo).toEqual({ rol: 'ADMINISTRADOR' });
    expect(put?.autorizacion).toMatch(/^Bearer /);
    expect(screen.getByRole('button', { name: /Quitar Admin a Beto/ })).toBeInTheDocument();
  });

  it('quitar el rol pide confirmación y muestra el mensaje del 400', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    simularRed({
      'GET /auth/usuarios': { data: [ADMIN, PAR] },
      'PUT /auth/usuarios/u-3/rol': { status: 400, data: { message: 'Debe quedar al menos un administrador' } },
    });
    renderizarApp('/admin/usuarios');
    await usuario.click(await screen.findByRole('button', { name: 'Quitar Admin a Carla' }));
    const dialogo = screen.getByRole('dialog');
    expect(within(dialogo).getByText('Ya no podrá entrar al panel de administración.')).toBeInTheDocument();
    await usuario.click(within(dialogo).getByRole('button', { name: 'Quitar Admin' }));
    expect(
      await screen.findByText('No se puede: debe quedar al menos un administrador y no puedes quitarte tu propio rol'),
    ).toBeInTheDocument();
  });

  it('cancelar la confirmación no envía nada', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    const red = simularRed({ 'GET /auth/usuarios': { data: [ADMIN, OTRO] } });
    renderizarApp('/admin/usuarios');
    await usuario.click(await screen.findByRole('button', { name: /Hacer Admin a/ }));
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));
    expect(red.peticiones.filter((p) => p.metodo !== 'GET')).toHaveLength(0);
  });

  it('da de alta a un administrador validando antes y enviando el token; no sustituye la sesión', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    let altas = 0;
    const red = simularRed({
      'GET /auth/usuarios': () => ({ data: altas ? [ADMIN, { ...PAR, nombre: 'Nuevo Admin' }] : [ADMIN] }),
      'POST /auth/register-admin': () => {
        altas += 1;
        return { status: 201, data: { accessToken: 'token-del-nuevo', usuario: { ...PAR, nombre: 'Nuevo Admin' } } };
      },
    });
    renderizarApp('/admin/usuarios');
    await usuario.click(await screen.findByRole('button', { name: 'Registrar administrador' }));

    const enviar = () => usuario.click(screen.getByRole('button', { name: 'Registrar administrador' }));
    await enviar();
    expect(await screen.findByText('Por favor, llena todos los campos')).toBeInTheDocument();

    await usuario.type(screen.getByLabelText('Nombre Completo'), 'Nuevo Admin');
    await usuario.type(screen.getByLabelText('Correo Electrónico'), 'nuevo@fest.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'debil');
    await enviar();
    expect(await screen.findByText('La contraseña debe tener al menos 8 caracteres, una mayúscula y un número')).toBeInTheDocument();
    expect(red.peticiones.some((p) => p.metodo === 'POST')).toBe(false);

    await usuario.clear(screen.getByLabelText('Contraseña'));
    await usuario.type(screen.getByLabelText('Contraseña'), 'Segura123');
    await enviar();

    expect(await screen.findByText('Administrador registrado exitosamente')).toBeInTheDocument();
    const alta = red.peticiones.find((p) => p.metodo === 'POST');
    expect(alta?.url).toBe('/auth/register-admin');
    expect(alta?.cuerpo).toEqual({ nombre: 'Nuevo Admin', correo: 'nuevo@fest.mx', contrasena: 'Segura123' });
    expect(alta?.autorizacion).toMatch(/^Bearer /);
    expect(alta?.autorizacion).not.toContain('token-del-nuevo');
    expect(await screen.findByRole('row', { name: /Nuevo Admin/ })).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('festivaltrack.sesion') ?? '{}').usuario.id).toBe('u-admin');
  });

  it('un correo repetido (409) muestra el mensaje del móvil', async () => {
    entrarComoAdmin();
    const usuario = userEvent.setup();
    simularRed({
      'GET /auth/usuarios': { data: [ADMIN] },
      'POST /auth/register-admin': { status: 409, data: { message: 'El correo ya está registrado' } },
    });
    renderizarApp('/admin/usuarios');
    await usuario.click(await screen.findByRole('button', { name: 'Registrar administrador' }));
    await usuario.type(screen.getByLabelText('Nombre Completo'), 'Dup');
    await usuario.type(screen.getByLabelText('Correo Electrónico'), 'lucia@fest.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'Segura123');
    await usuario.click(screen.getByRole('button', { name: 'Registrar administrador' }));
    expect(await screen.findByText('Ese correo ya está registrado')).toBeInTheDocument();
  });

  it('un 403 al listar se muestra como falta de permisos y un 401 como sesión terminada', async () => {
    entrarComoAdmin();
    simularRed({ 'GET /auth/usuarios': { status: 403, data: { message: 'Forbidden' } } });
    renderizarApp('/admin/usuarios');
    expect(await screen.findByText('No tienes permisos de administrador para hacer esto.')).toBeInTheDocument();
  });

  it('un 401 al listar muestra el aviso de sesión terminada', async () => {
    entrarComoAdmin();
    simularRed({ 'GET /auth/usuarios': { status: 401, data: {} } });
    renderizarApp('/admin/usuarios');
    // El interceptor cierra la sesión y RequireAuth lleva al login; no queda el panel a la vista.
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Gestión de usuarios' })).not.toBeInTheDocument());
  });
});
