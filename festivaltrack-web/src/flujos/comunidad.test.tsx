import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { InternalAxiosRequestConfig } from 'axios';
import type { ComentarioForo, Publicacion } from '../services/foro.service';
import { USUARIO } from '../test/datos';
import { renderizarApp } from '../test/render';
import { simularRed, type RespuestaFalsa } from '../test/red';
import { iniciarSesionDePrueba } from '../test/sesion';

vi.mock('swiper/react', async () => import('../test/swiper'));

const BASE = Date.parse('2026-11-25T12:00:00.000Z');

function pub(n: number, parcial: Partial<Publicacion> = {}): Publicacion {
  return {
    id: `p${n}`,
    tipo: 'EXPERIENCIA',
    titulo: `Título ${n}`,
    contenido: `Contenido ${n}`,
    imagenUrl: null,
    lugar: null,
    createdAt: new Date(BASE - n * 60_000).toISOString(),
    autorId: 'u-2',
    autorNombre: 'Luis Gómez',
    likes: 2,
    comentarios: 0,
    likedByMe: false,
    ...parcial,
  };
}

function comentario(n: number, parcial: Partial<ComentarioForo> = {}): ComentarioForo {
  return {
    id: `c${n}`,
    texto: `Comentario ${n}`,
    createdAt: new Date(BASE).toISOString(),
    autorId: 'u-2',
    autorNombre: 'Luis Gómez',
    ...parcial,
  };
}

/** Lista simulada: responde según `tipo` y `before` como el backend (desc, `lt`). */
function foroSimulado(todas: Publicacion[], fallos: { primero?: boolean; mas?: boolean } = {}) {
  return (_c: unknown, config: InternalAxiosRequestConfig): RespuestaFalsa => {
    const params = (config.params ?? {}) as Record<string, string>;
    if (fallos.primero && !params.before) return { status: 500, data: {} };
    if (fallos.mas && params.before) return { status: 500, data: {} };
    const limit = Number(params.limit);
    const lista = todas
      .filter((p) => !params.tipo || p.tipo === params.tipo)
      .filter((p) => !params.before || Date.parse(p.createdAt) < Date.parse(params.before))
      .slice(0, limit);
    return { data: lista };
  };
}

const tarjetas = () => within(screen.getByRole('list', { name: 'Publicaciones' })).getAllByRole('article');

beforeEach(() => {
  URL.createObjectURL = vi.fn(() => 'blob:vista-previa');
  URL.revokeObjectURL = vi.fn();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('comunidad: lectura sin sesión', () => {
  it('muestra la lista, filtra por tipo y pagina con "Cargar más" sin duplicados', async () => {
    const todas = Array.from({ length: 25 }, (_, i) => pub(i + 1, { tipo: i % 5 === 0 ? 'FOTO' : 'EXPERIENCIA' }));
    const red = simularRed({ 'GET /foro': foroSimulado(todas) });
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    expect(await screen.findByRole('heading', { level: 1, name: 'Comunidad del Festival' })).toBeInTheDocument();
    await waitFor(() => expect(tarjetas()).toHaveLength(20));
    expect(screen.getByText('Título 1')).toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Cargar más' }));
    await waitFor(() => expect(tarjetas()).toHaveLength(25));
    const ids = tarjetas().map((t) => within(t).getByRole('heading').textContent);
    expect(new Set(ids).size).toBe(25);
    expect(screen.getByText('Has llegado al final · gracias por ser parte de la comunidad')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cargar más' })).not.toBeInTheDocument();

    const segunda = red.peticiones.filter((p) => p.url === '/foro')[1];
    expect(segunda).toBeDefined();

    // Filtro "Fotos": nueva petición con tipo=FOTO y solo esas publicaciones.
    await usuario.click(screen.getByRole('button', { name: 'Fotos' }));
    expect(screen.getByRole('button', { name: 'Fotos' })).toHaveAttribute('aria-pressed', 'true');
    await waitFor(() => expect(tarjetas()).toHaveLength(5));
    for (const t of tarjetas()) expect(within(t).getByText('FOTO')).toBeInTheDocument();
  });

  it('error de carga con reintento y estado vacío', async () => {
    let falla = true;
    simularRed({ 'GET /foro': () => (falla ? { status: 500, data: {} } : { data: [] }) });
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('Sin conexión con el foro');
    falla = false;
    await usuario.click(within(alerta).getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('Aún no hay publicaciones')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Compartir ahora' })).toBeInTheDocument();
  });

  it('si falla "Cargar más" conserva lo cargado y permite reintentar', async () => {
    const todas = Array.from({ length: 22 }, (_, i) => pub(i + 1));
    simularRed({ 'GET /foro': foroSimulado(todas, { mas: true }) });
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    await waitFor(() => expect(tarjetas()).toHaveLength(20));
    await usuario.click(screen.getByRole('button', { name: 'Cargar más' }));
    expect(await screen.findByText(/No se pudieron cargar más publicaciones/)).toBeInTheDocument();
    expect(tarjetas()).toHaveLength(20);
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  it('pinta el contenido de usuario como texto y nunca carga imágenes de hosts no permitidos', async () => {
    const malicioso = '<img src=x onerror="alert(1)"> <b>negrita</b> https://evil.example';
    simularRed({
      'GET /foro': {
        data: [
          pub(1, { titulo: '<script>alert(1)</script>', contenido: malicioso, imagenUrl: 'https://evil.example/a.jpg' }),
          pub(2, { imagenUrl: 'javascript:alert(1)' }),
          pub(3, { imagenUrl: '/uploads/file-1.jpg', titulo: 'Con foto' }),
          { id: 'roto', tipo: 'EXPERIENCIA' },
        ],
      },
    });
    renderizarApp('/comunidad');

    expect(await screen.findByText(malicioso)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '<script>alert(1)</script>' })).toBeInTheDocument();
    expect(document.querySelector('b, script, a[href*="evil"]')).toBeNull();
    expect(document.querySelector('img[src*="evil"], img[src^="javascript"]')).toBeNull();
    expect(screen.getAllByRole('img', { name: 'Imagen no disponible' })).toHaveLength(2);
    // La publicación con forma inválida se descarta sin romper la página.
    expect(tarjetas()).toHaveLength(3);

    // Un archivo subido que ya no existe degrada al marcador.
    const foto = screen.getByRole('img', { name: 'Foto: Con foto' });
    expect(foto.getAttribute('src')).toMatch(/\/uploads\/file-1\.jpg$/);
    fireEvent.error(foto);
    expect(screen.getAllByRole('img', { name: 'Imagen no disponible' })).toHaveLength(3);
  });

  it('las acciones de escritura llevan al login', async () => {
    simularRed({ 'GET /foro': { data: [pub(1)] }, 'GET /foro/p1/comentarios': { data: [] } });
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    await screen.findByText('Título 1');
    await usuario.click(screen.getByRole('button', { name: /Me gusta/ }));
    expect(await screen.findByRole('heading', { level: 1, name: /José Alfredo Jiménez/ })).toBeInTheDocument();
  });
});

describe('comunidad: con sesión', () => {
  it('me gusta optimista que se reconcilia con el servidor y se revierte si falla', async () => {
    let resolver: (r: RespuestaFalsa) => void = () => {};
    let falla = false;
    const red = simularRed({
      'GET /foro': { data: [pub(1, { likes: 2 })] },
      'POST /foro/p1/like': () =>
        falla ? { status: 500, data: {} } : new Promise<RespuestaFalsa>((r) => (resolver = r)),
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    const boton = await screen.findByRole('button', { name: 'Me gusta, 2' });
    await usuario.click(boton);
    // Optimista: ya marcado y con +1 antes de que conteste el servidor.
    expect(boton).toHaveAttribute('aria-pressed', 'true');
    expect(boton).toHaveAccessibleName('Me gusta, 3');
    // Mientras está pendiente no se manda otro.
    await usuario.click(boton);
    expect(red.peticiones.filter((p) => p.metodo === 'POST')).toHaveLength(1);

    resolver({ data: { liked: true, likes: 10 } });
    await waitFor(() => expect(boton).toHaveAccessibleName('Me gusta, 10'));

    falla = true;
    await usuario.click(boton);
    expect(await screen.findByText('No se pudo registrar tu me gusta')).toBeInTheDocument();
    expect(boton).toHaveAttribute('aria-pressed', 'true');
    expect(boton).toHaveAccessibleName('Me gusta, 10');
  });

  it('ver, añadir y borrar comentarios mantiene el contador al día', async () => {
    const red = simularRed({
      'GET /foro': { data: [pub(1, { comentarios: 5 })] },
      'GET /foro/p1/comentarios': { data: [comentario(1), comentario(2, { autorId: USUARIO.id, autorNombre: USUARIO.nombre })] },
      'POST /foro/p1/comentarios': (c) => ({
        status: 201,
        data: comentario(3, { texto: (c as { texto: string }).texto, autorId: USUARIO.id, autorNombre: USUARIO.nombre }),
      }),
      'DELETE /foro/comentarios/c2': { data: { ok: true } },
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    await usuario.click(await screen.findByRole('button', { name: 'Comentarios, 5' }));
    expect(await screen.findByText('Comentario 1')).toBeInTheDocument();
    // Tras cargar, el contador refleja la lista real.
    expect(screen.getByRole('button', { name: 'Comentarios, 2' })).toHaveAttribute('aria-expanded', 'true');

    const campo = screen.getByLabelText('Escribe un comentario');
    expect(campo).toHaveAttribute('maxLength', '400');
    await usuario.type(campo, '  ¡Qué noche!  ');
    expect(screen.getByText('15/400')).toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Enviar comentario' }));
    expect(await screen.findByText('¡Qué noche!')).toBeInTheDocument();
    expect(red.peticiones.find((p) => p.metodo === 'POST')?.cuerpo).toEqual({ texto: '¡Qué noche!' });
    expect(screen.getByRole('button', { name: 'Comentarios, 3' })).toBeInTheDocument();
    expect(campo).toHaveValue('');

    // Solo los propios llevan papelera; borrar pide confirmación.
    expect(screen.getAllByRole('button', { name: 'Eliminar comentario' })).toHaveLength(2);
    await usuario.click(screen.getAllByRole('button', { name: 'Eliminar comentario' })[0]);
    const dialogo = screen.getByRole('dialog', { name: '¿Eliminar comentario?' });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Eliminar' }));
    await waitFor(() => expect(screen.queryByText('Comentario 2')).not.toBeInTheDocument());
    expect(red.peticiones.some((p) => p.metodo === 'DELETE' && p.url === '/foro/comentarios/c2')).toBe(true);
    expect(screen.getByRole('button', { name: 'Comentarios, 2' })).toBeInTheDocument();
  });

  it('borra una publicación propia tras confirmar; no ofrece borrar las ajenas', async () => {
    const red = simularRed({
      'GET /foro': { data: [pub(1, { autorId: USUARIO.id, titulo: 'Mía' }), pub(2, { titulo: 'Ajena' })] },
      'DELETE /foro/p1': { data: { ok: true } },
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    await screen.findByText('Ajena');
    const botones = screen.getAllByRole('button', { name: 'Eliminar publicación' });
    expect(botones).toHaveLength(1);
    await usuario.click(botones[0]);
    const dialogo = screen.getByRole('dialog', { name: '¿Eliminar publicación?' });
    expect(dialogo).toHaveTextContent('Esta acción no se puede deshacer.');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Eliminar' }));
    await waitFor(() => expect(screen.queryByText('Mía')).not.toBeInTheDocument());
    expect(red.peticiones.filter((p) => p.metodo === 'DELETE').map((p) => p.url)).toEqual(['/foro/p1']);
  });

  it('valida la foto antes de subir, conserva el texto si falla la subida y publica una sola vez', async () => {
    let subidas = 0;
    const red = simularRed({
      'GET /foro': { data: [] },
      'POST /upload': () => {
        subidas += 1;
        return subidas === 1 ? { status: 413, data: { message: 'Payload Too Large' } } : { status: 201, data: { url: '/uploads/file-9.jpg' } };
      },
      'POST /foro': (c) => ({
        status: 201,
        data: pub(0, { ...(c as Partial<Publicacion>), id: 'nueva', autorId: USUARIO.id, autorNombre: USUARIO.nombre, titulo: 'En el jardín' }),
      }),
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup({ applyAccept: false });
    renderizarApp('/comunidad');

    await screen.findByText('Aún no hay publicaciones');
    await usuario.click(screen.getByRole('button', { name: 'Compartir' }));
    expect(screen.getByRole('heading', { name: 'Compartir con la comunidad' })).toHaveFocus();

    // Sin texto ni foto no se envía nada.
    await usuario.click(screen.getByRole('button', { name: 'Publicar' }));
    expect(screen.getByText('Escribe algo o agrega una foto')).toBeInTheDocument();

    const selector = screen.getByLabelText(/^Agregar una foto \(opcional\)/);
    await usuario.upload(selector, new File(['gif'], 'a.gif', { type: 'image/gif' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Solo se permiten imágenes JPG, PNG o WEBP.');
    await usuario.upload(selector, new File([new Uint8Array(8 * 1024 * 1024 + 1)], 'b.jpg', { type: 'image/jpeg' }));
    expect(screen.getByRole('alert')).toHaveTextContent(/8 MB/);
    expect(red.peticiones.filter((p) => p.metodo === 'POST')).toHaveLength(0);

    await usuario.upload(selector, new File(['jpg'], 'c.jpg', { type: 'image/jpeg' }));
    expect(screen.getByAltText('Vista previa de la foto elegida')).toHaveAttribute('src', 'blob:vista-previa');
    // Con contenido vacío, elegir foto cambia el tipo a Foto.
    expect(screen.getByRole('radio', { name: 'Foto' })).toBeChecked();

    await usuario.type(screen.getByLabelText('Título (opcional)'), 'En el jardín');
    await usuario.type(screen.getByLabelText('Pie de foto'), 'Gran ambiente');
    expect(screen.getByText('13/1000')).toBeInTheDocument();
    expect(screen.getByLabelText('Título (opcional)')).toHaveAttribute('maxLength', '80');
    expect(screen.getByLabelText('Lugar (opcional)')).toHaveAttribute('maxLength', '60');

    await usuario.click(screen.getByRole('button', { name: 'Publicar' }));
    expect(await screen.findByText(/No se pudo subir la foto/)).toBeInTheDocument();
    expect(screen.getByLabelText('Pie de foto')).toHaveValue('Gran ambiente');
    expect(screen.getByLabelText('Título (opcional)')).toHaveValue('En el jardín');
    expect(red.peticiones.some((p) => p.metodo === 'POST' && p.url === '/foro')).toBe(false);

    // Doble clic: una sola publicación.
    const publicar = screen.getByRole('button', { name: 'Publicar' });
    await usuario.dblClick(publicar);
    expect(await screen.findByText('¡Publicado! Gracias por compartir')).toBeInTheDocument();
    const creaciones = red.peticiones.filter((p) => p.metodo === 'POST' && p.url === '/foro');
    expect(creaciones).toHaveLength(1);
    expect(creaciones[0].cuerpo).toEqual({
      tipo: 'FOTO',
      titulo: 'En el jardín',
      contenido: 'Gran ambiente',
      imagenUrl: '/uploads/file-9.jpg',
    });
    expect(subidas).toBe(2);
    expect(await screen.findByText('Gran ambiente')).toBeInTheDocument();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:vista-previa');
  });

  it('si falla la publicación conserva lo escrito y no vuelve a subir la foto', async () => {
    let intentos = 0;
    let subidas = 0;
    simularRed({
      'GET /foro': { data: [] },
      'POST /upload': () => {
        subidas += 1;
        return { status: 201, data: { url: '/uploads/file-9.jpg' } };
      },
      'POST /foro': () => {
        intentos += 1;
        return intentos === 1
          ? { status: 400, data: { message: 'URL de imagen no válida' } }
          : { status: 201, data: pub(0, { id: 'nueva', contenido: 'Hola', tipo: 'EXPERIENCIA' }) };
      },
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/comunidad');

    await usuario.click(await screen.findByRole('button', { name: 'Compartir' }));
    await usuario.type(screen.getByLabelText('Tu publicación'), 'Hola');
    await usuario.upload(screen.getByLabelText(/^Agregar una foto \(opcional\)/), new File(['x'], 'c.png', { type: 'image/png' }));
    await usuario.click(screen.getByRole('button', { name: 'Publicar' }));
    expect(await screen.findByText('URL de imagen no válida')).toBeInTheDocument();
    expect(screen.getByLabelText('Tu publicación')).toHaveValue('Hola');

    await usuario.click(screen.getByRole('button', { name: 'Publicar' }));
    expect(await screen.findByText('¡Publicado! Gracias por compartir')).toBeInTheDocument();
    expect(subidas).toBe(1);
  });
});
