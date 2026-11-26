import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { guardarSesion } from '../auth/session';
import type { Cancion } from '../services/canciones.service';
import { crearEvento, crearJwt, USUARIO } from '../test/datos';
import { renderizarApp } from '../test/render';
import { simularRed } from '../test/red';
import { iniciarSesionDePrueba } from '../test/sesion';

type Usuario = ReturnType<typeof userEvent.setup>;

function iniciarSesionAdmin() {
  guardarSesion(crearJwt(Math.floor(Date.now() / 1000) + 3600), { ...USUARIO, rol: 'ADMINISTRADOR' });
}

function crearCancion(parcial: Partial<Cancion> = {}): Cancion {
  return {
    id: 'c-1',
    titulo: 'El Rey',
    artista: 'José Alfredo Jiménez',
    duracion: 180,
    archivoUrl: '/uploads/a.mp3',
    portadaUrl: null,
    genero: null,
    estado: 'PUBLICADA',
    createdAt: '2026-01-01T00:00:00.000Z',
    ...parcial,
  };
}

// 10 de marzo de 2035, 20:00 en México = 11 de marzo, 02:00 UTC.
const FECHA_2035 = '2035-03-11T02:00:00.000Z';

const conciertos = [
  crearEvento({ id: 'ev-1', nombre: 'Noche de Bohemia', fechaHora: FECHA_2035, estado: 'PUBLICADO' }),
  crearEvento({ id: 'ev-2', nombre: 'Gala de Clausura', fechaHora: FECHA_2035, estado: 'FINALIZADO', escenario: 'Casa Museo' }),
  crearEvento({ id: 'ev-3', nombre: 'Serenata Cancelada', fechaHora: FECHA_2035, estado: 'CANCELADO' }),
  crearEvento({ id: 'ev-4', nombre: 'Ensayo General', fechaHora: FECHA_2035, estado: 'BORRADOR' }),
];

describe('lista de conciertos', () => {
  it('muestra estado, fecha en hora de México y lugar, y solo ofrece cancelar los cancelables', async () => {
    simularRed({ 'GET /eventos': { data: conciertos } });
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos');

    expect(await screen.findByRole('heading', { level: 1, name: 'Conciertos' })).toBeInTheDocument();
    const fila = (await screen.findByRole('rowheader', { name: 'Noche de Bohemia' })).closest('tr') as HTMLElement;
    expect(within(fila).getByText('Publicado')).toBeInTheDocument();
    expect(within(fila).getByText(/20:00/)).toBeInTheDocument();
    expect(within(fila).getByText(/2035/)).toBeInTheDocument();
    expect(within(fila).getByText(/Plaza Principal/)).toBeInTheDocument();
    expect(within(fila).getByRole('link', { name: 'Editar Noche de Bohemia' })).toHaveAttribute('href', '/admin/conciertos/ev-1');
    expect(within(fila).getByRole('link', { name: 'Repertorio de Noche de Bohemia' })).toHaveAttribute(
      'href',
      '/admin/conciertos/ev-1/repertorio',
    );

    expect(screen.getByRole('button', { name: 'Cancelar Noche de Bohemia' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancelar Ensayo General' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancelar Gala de Clausura' })).not.toBeInTheDocument(); // finalizado
    expect(screen.queryByRole('button', { name: 'Cancelar Serenata Cancelada' })).not.toBeInTheDocument(); // cancelado
    const tabla = screen.getByRole('table');
    expect(within(tabla).getByText('Cancelado')).toBeInTheDocument();
    expect(within(tabla).getByText('Borrador')).toBeInTheDocument();
    expect(within(tabla).getByText('Finalizado')).toBeInTheDocument();
  });

  it('filtra por estado y busca por nombre sin distinguir acentos', async () => {
    simularRed({ 'GET /eventos': { data: [...conciertos, crearEvento({ id: 'ev-5', nombre: 'Sérenata en el Jardín' })] } });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos');
    await screen.findByRole('rowheader', { name: 'Noche de Bohemia' });

    await usuario.selectOptions(screen.getByLabelText('Estado'), 'CANCELADO');
    expect(screen.getAllByRole('rowheader').map((e) => e.textContent)).toEqual(['Serenata Cancelada']);

    await usuario.selectOptions(screen.getByLabelText('Estado'), '');
    await usuario.type(screen.getByLabelText('Buscar por nombre'), 'serenata');
    expect(screen.getAllByRole('rowheader').map((e) => e.textContent)).toEqual(['Serenata Cancelada', 'Sérenata en el Jardín']);

    await usuario.clear(screen.getByLabelText('Buscar por nombre'));
    await usuario.type(screen.getByLabelText('Buscar por nombre'), 'zzz');
    expect(await screen.findByText('Ningún concierto coincide con la búsqueda.')).toBeInTheDocument();
  });

  it('con una lista vacía lo dice y ofrece crear', async () => {
    simularRed({ 'GET /eventos': { data: [] } });
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos');
    expect(await screen.findByText('No hay conciertos registrados.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Nuevo concierto' })).toHaveAttribute('href', '/admin/conciertos/nuevo');
  });

  it('si falla la carga ofrece reintentar', async () => {
    let intento = 0;
    simularRed({
      'GET /eventos': () => {
        intento += 1;
        return intento === 1 ? { status: 500, data: {} } : { data: conciertos };
      },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos');
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar los conciertos');
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('rowheader', { name: 'Noche de Bohemia' })).toBeInTheDocument();
  });

  it('un usuario sin rol de administrador no entra al panel', async () => {
    simularRed({ 'GET /eventos': { data: conciertos } });
    iniciarSesionDePrueba();
    renderizarApp('/admin/conciertos');
    await waitFor(() => expect(screen.queryByRole('heading', { level: 1, name: 'Conciertos' })).not.toBeInTheDocument());
    expect(screen.queryByRole('rowheader', { name: 'Noche de Bohemia' })).not.toBeInTheDocument();
  });
});

describe('cancelar un concierto', () => {
  it('pide confirmación con un texto claro, envía DELETE y avisa del resultado', async () => {
    let cancelado = false;
    const red = simularRed({
      'GET /eventos': () => ({
        data: conciertos.map((c) => (c.id === 'ev-1' && cancelado ? { ...c, estado: 'CANCELADO' } : c)),
      }),
      'DELETE /eventos/ev-1': () => {
        cancelado = true;
        return { data: { ...conciertos[0], estado: 'CANCELADO' } };
      },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos');

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar Noche de Bohemia' }));
    const dialogo = await screen.findByRole('dialog');
    expect(within(dialogo).getByText(/pasará a estado Cancelado/)).toBeInTheDocument();
    expect(within(dialogo).getByText(/No se borra el registro/)).toBeInTheDocument();
    expect(red.peticiones.some((p) => p.metodo === 'DELETE')).toBe(false); // aún no se envió nada

    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar concierto' }));

    expect(await screen.findByText('«Noche de Bohemia» quedó cancelado.')).toBeInTheDocument();
    const borrados = red.peticiones.filter((p) => p.metodo === 'DELETE');
    expect(borrados).toHaveLength(1);
    expect(borrados[0].url).toBe('/eventos/ev-1');
    // La lista se recarga y ya no ofrece cancelarlo.
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Cancelar Noche de Bohemia' })).not.toBeInTheDocument());
  });

  it('"Volver" cierra el diálogo sin enviar nada', async () => {
    const red = simularRed({ 'GET /eventos': { data: conciertos } });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos');
    await usuario.click(await screen.findByRole('button', { name: 'Cancelar Noche de Bohemia' }));
    await usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Volver' }));
    expect(red.peticiones.some((p) => p.metodo === 'DELETE')).toBe(false);
  });

  it('un 403 se muestra como falta de permisos, no como error genérico', async () => {
    simularRed({ 'GET /eventos': { data: conciertos }, 'DELETE /eventos/ev-1': { status: 403, data: { message: 'Forbidden resource' } } });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos');
    await usuario.click(await screen.findByRole('button', { name: 'Cancelar Noche de Bohemia' }));
    await usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancelar concierto' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/no tiene permisos de administrador/);
  });
});

async function llenarAlta(usuario: Usuario, extra: { descripcion?: string } = {}) {
  await usuario.type(screen.getByLabelText('Nombre del concierto'), '  Noche de Bohemia ');
  fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2035-03-10' } });
  fireEvent.change(screen.getByLabelText('Hora'), { target: { value: '20:00' } });
  await usuario.selectOptions(screen.getByLabelText('Lugar'), 'Plaza Principal');
  if (extra.descripcion) await usuario.type(screen.getByLabelText('Descripción (opcional)'), extra.descripcion);
}

describe('alta de concierto', () => {
  it('muestra el aviso de precios, los valores por defecto y las cuatro opciones de lugar', async () => {
    simularRed({});
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos/nuevo');
    expect(await screen.findByRole('heading', { level: 1, name: 'Nuevo concierto' })).toBeInTheDocument();
    expect(
      screen.getByText('Los boletos se venden con los precios oficiales del festival: VIP $4,500 y General $1,200.'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('Capacidad (personas)')).toHaveValue('1000');
    expect(screen.getByText('Hora del centro de México.')).toBeInTheDocument();
    const opciones = within(screen.getByLabelText('Lugar')).getAllByRole('option').map((o) => o.textContent);
    expect(opciones).toEqual([
      'Elige el lugar…',
      'Escenario Principal · Jardín Principal, Dolores Hidalgo',
      'Plaza Principal · Jardín Principal, Dolores Hidalgo',
      'Mausoleo de José Alfredo Jiménez · Panteón Municipal, Dolores Hidalgo',
      'Casa Museo · Calle Guanajuato 13, Centro, Dolores Hidalgo',
    ]);
    expect(screen.getByRole('button', { name: 'Publicar concierto' })).toBeEnabled();
  });

  it('con el formulario vacío muestra un error por campo y no envía nada', async () => {
    const red = simularRed({});
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/nuevo');
    await screen.findByRole('heading', { level: 1, name: 'Nuevo concierto' });
    await usuario.clear(screen.getByLabelText('Capacidad (personas)'));
    await usuario.click(screen.getByRole('button', { name: 'Publicar concierto' }));

    expect(screen.getByText('Escribe el nombre del concierto')).toBeInTheDocument();
    expect(screen.getAllByText('Elige la fecha y la hora')).toHaveLength(2);
    expect(screen.getByText('Elige el lugar del concierto')).toBeInTheDocument();
    expect(screen.getByText('Escribe la capacidad')).toBeInTheDocument();
    expect(screen.getByLabelText('Nombre del concierto')).toHaveFocus();
    expect(red.peticiones).toHaveLength(0);
  });

  it('valida la capacidad y las fechas pasadas', async () => {
    const red = simularRed({});
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/nuevo');
    await screen.findByRole('heading', { level: 1, name: 'Nuevo concierto' });
    await llenarAlta(usuario);
    fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2020-01-01' } });
    await usuario.clear(screen.getByLabelText('Capacidad (personas)'));
    await usuario.type(screen.getByLabelText('Capacidad (personas)'), '100001');
    await usuario.click(screen.getByRole('button', { name: 'Publicar concierto' }));

    expect(screen.getByText('La fecha y hora ya pasaron')).toBeInTheDocument();
    expect(screen.getByText('La capacidad debe estar entre 1 y 100,000')).toBeInTheDocument();
    expect(red.peticiones).toHaveLength(0);
  });

  it('envía el cuerpo exacto con la fecha en UTC, vuelve a la lista y confirma', async () => {
    const red = simularRed({
      'POST /eventos': (cuerpo) => ({ status: 201, data: crearEvento({ id: 'EVT-009', ...(cuerpo as object) }) }),
      'GET /eventos': { data: conciertos },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/nuevo');
    await screen.findByRole('heading', { level: 1, name: 'Nuevo concierto' });
    await llenarAlta(usuario, { descripcion: 'Con mariachi' });
    await usuario.click(screen.getByRole('button', { name: 'Publicar concierto' }));

    expect(await screen.findByText('«Noche de Bohemia» ya está publicado.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Conciertos' })).toBeInTheDocument();
    const posts = red.peticiones.filter((p) => p.metodo === 'POST');
    expect(posts).toHaveLength(1);
    expect(posts[0].url).toBe('/eventos');
    expect(posts[0].autorizacion).toMatch(/^Bearer /);
    expect(posts[0].cuerpo).toEqual({
      nombre: 'Noche de Bohemia',
      fechaHora: '2035-03-11T02:00:00.000Z',
      ubicacion: 'Jardín Principal, Dolores Hidalgo',
      escenario: 'Plaza Principal',
      latitud: 21.1563,
      longitud: -100.9338,
      capacidad: 1000,
      descripcion: 'Con mariachi',
      estado: 'PUBLICADO',
    });
  });

  it('no permite el doble envío', async () => {
    let liberar: () => void = () => {};
    const espera = new Promise<void>((r) => (liberar = r));
    const red = simularRed({
      'POST /eventos': async () => {
        await espera;
        return { status: 201, data: crearEvento() };
      },
      'GET /eventos': { data: conciertos },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/nuevo');
    await screen.findByRole('heading', { level: 1, name: 'Nuevo concierto' });
    await llenarAlta(usuario);
    const boton = screen.getByRole('button', { name: 'Publicar concierto' });
    await usuario.click(boton);
    await usuario.click(boton);
    await usuario.click(boton);
    liberar();
    await screen.findByText(/ya está publicado/);
    expect(red.peticiones.filter((p) => p.metodo === 'POST')).toHaveLength(1);
  });

  it.each([
    [400, 'El servidor rechazó los datos. Revisa el formulario.'],
    [403, 'Tu sesión no tiene permisos de administrador'],
    [500, 'El servidor tuvo un problema. Inténtalo de nuevo en unos minutos.'],
  ])('ante un %s muestra el mensaje correcto y conserva lo escrito', async (status, mensaje) => {
    simularRed({ 'POST /eventos': { status, data: { message: ['x'] } } });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/nuevo');
    await screen.findByRole('heading', { level: 1, name: 'Nuevo concierto' });
    await llenarAlta(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Publicar concierto' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(mensaje);
    expect(screen.getByLabelText('Nombre del concierto')).toHaveValue('  Noche de Bohemia ');
    expect(screen.getByRole('button', { name: 'Publicar concierto' })).toBeEnabled();
  });

  it('ante un 401 la sesión se cierra y se pide iniciar sesión de nuevo', async () => {
    simularRed({ 'POST /eventos': { status: 401, data: { message: 'Unauthorized' } } });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/nuevo');
    await screen.findByRole('heading', { level: 1, name: 'Nuevo concierto' });
    await llenarAlta(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Publicar concierto' }));
    expect(await screen.findByText('Ingresa con tus credenciales para continuar.')).toBeInTheDocument();
  });
});

describe('edición de concierto', () => {
  const guardado = crearEvento({
    id: 'ev-1',
    nombre: 'Noche de Bohemia',
    fechaHora: FECHA_2035,
    escenario: 'Plaza Principal',
    ubicacion: 'Jardín Principal, Dolores Hidalgo',
    latitud: 21.1563,
    longitud: -100.9338,
    capacidad: 800,
    descripcion: 'Con mariachi',
  });

  it('carga el concierto con la fecha y hora en México y no deja guardar sin cambios', async () => {
    simularRed({ 'GET /eventos/ev-1': { data: guardado } });
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos/ev-1');

    expect(await screen.findByRole('heading', { level: 1, name: 'Editar concierto' })).toBeInTheDocument();
    expect(screen.getByLabelText('Nombre del concierto')).toHaveValue('Noche de Bohemia');
    expect(screen.getByLabelText('Fecha')).toHaveValue('2035-03-10');
    expect(screen.getByLabelText('Hora')).toHaveValue('20:00');
    expect(screen.getByLabelText('Lugar')).toHaveValue('Plaza Principal');
    expect(screen.getByLabelText('Capacidad (personas)')).toHaveValue('800');
    expect(screen.getByLabelText('Descripción (opcional)')).toHaveValue('Con mariachi');
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeDisabled();
  });

  it('envía solo lo que cambió (PATCH) y confirma en la lista', async () => {
    const red = simularRed({
      'GET /eventos/ev-1': { data: guardado },
      'GET /eventos': { data: conciertos },
      'PATCH /eventos/ev-1': { data: guardado },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1');
    await screen.findByRole('heading', { level: 1, name: 'Editar concierto' });

    fireEvent.change(screen.getByLabelText('Hora'), { target: { value: '21:30' } });
    await usuario.clear(screen.getByLabelText('Capacidad (personas)'));
    await usuario.type(screen.getByLabelText('Capacidad (personas)'), '900');
    await usuario.selectOptions(screen.getByLabelText('Lugar'), 'Casa Museo');
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeEnabled();
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Cambios guardados en «Noche de Bohemia».')).toBeInTheDocument();
    const parches = red.peticiones.filter((p) => p.metodo === 'PATCH');
    expect(parches).toHaveLength(1);
    expect(parches[0].url).toBe('/eventos/ev-1');
    expect(parches[0].cuerpo).toEqual({
      fechaHora: '2035-03-11T03:30:00.000Z',
      escenario: 'Casa Museo',
      ubicacion: 'Calle Guanajuato 13, Centro, Dolores Hidalgo',
      latitud: 21.1574,
      longitud: -100.933,
      capacidad: 900,
    });
  });

  it('con un lugar que no está en la lista fija lo conserva si no se toca', async () => {
    const red = simularRed({
      'GET /eventos/ev-1': {
        data: crearEvento({ ...guardado, escenario: 'Teatro Juárez', ubicacion: 'Guanajuato', latitud: 21, longitud: -101 }),
      },
      'GET /eventos': { data: conciertos },
      'PATCH /eventos/ev-1': { data: guardado },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1');
    await screen.findByRole('heading', { level: 1, name: 'Editar concierto' });
    expect(screen.getByLabelText('Lugar')).toHaveDisplayValue(/Teatro Juárez · Guanajuato \(actual/);
    await usuario.clear(screen.getByLabelText('Nombre del concierto'));
    await usuario.type(screen.getByLabelText('Nombre del concierto'), 'Otro nombre');
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    await screen.findByText(/Cambios guardados/);
    expect(red.peticiones.find((p) => p.metodo === 'PATCH')?.cuerpo).toEqual({ nombre: 'Otro nombre' });
  });

  it('si el concierto no existe (404) lo dice con claridad', async () => {
    simularRed({ 'GET /eventos/nada': { status: 404, data: { message: 'Evento no encontrado' } } });
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos/nada');
    expect(await screen.findByRole('heading', { level: 1, name: 'Concierto no encontrado' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver a conciertos' })).toBeInTheDocument();
  });

  it('un 403 al guardar se muestra como falta de permisos', async () => {
    simularRed({
      'GET /eventos/ev-1': { data: guardado },
      'PATCH /eventos/ev-1': { status: 403, data: { message: 'Forbidden resource' } },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1');
    await screen.findByRole('heading', { level: 1, name: 'Editar concierto' });
    await usuario.type(screen.getByLabelText('Nombre del concierto'), '!');
    await usuario.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/no tiene permisos de administrador/);
  });
});

describe('repertorio de un concierto', () => {
  const catalogo = [
    crearCancion({ id: 'c-1', titulo: 'El Rey' }),
    crearCancion({ id: 'c-2', titulo: 'Volver, volver' }),
    crearCancion({ id: 'c-3', titulo: 'Camino de Guanajuato' }),
    crearCancion({ id: 'c-4', titulo: 'Demo sin publicar', estado: 'BORRADOR' }),
  ];

  function titulosEnConcierto(): string[] {
    const lista = screen.getByRole('region', { name: /En el concierto/i });
    return within(lista)
      .queryAllByRole('listitem')
      .map((li) => li.querySelector('.admin-conciertos-cancion__titulo')?.textContent ?? '');
  }

  function montar(extra: Parameters<typeof simularRed>[0] = {}, repertorio = [catalogo[0], catalogo[1]]) {
    return simularRed({
      'GET /eventos/ev-1': { data: conciertos[0] },
      'GET /canciones': { data: catalogo },
      'GET /canciones/evento/ev-1': { data: repertorio },
      ...extra,
    });
  }

  it('muestra las dos listas con sus contadores y el pie sin cambios', async () => {
    montar();
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos/ev-1/repertorio');

    expect(await screen.findByRole('heading', { level: 1, name: 'Noche de Bohemia' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'En el concierto · 2' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Catálogo · 2' })).toBeInTheDocument();
    expect(titulosEnConcierto()).toEqual(['El Rey', 'Volver, volver']);
    expect(screen.getByText('Sin cambios')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
    // Una canción no publicada no se puede añadir y se explica por qué.
    expect(screen.getByRole('button', { name: 'Agregar «Demo sin publicar» al concierto' })).toBeDisabled();
    expect(screen.getByText(/solo las canciones publicadas llegan al público/)).toBeInTheDocument();
  });

  it('añade, quita y reordena con botones, y guarda el orden exacto', async () => {
    const red = montar({
      'PUT /canciones/evento/ev-1': (cuerpo) => ({
        data: (cuerpo as { cancionIds: string[] }).cancionIds.map((id) => catalogo.find((c) => c.id === id)),
      }),
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1/repertorio');
    await screen.findByRole('heading', { level: 1, name: 'Noche de Bohemia' });

    await usuario.click(screen.getByRole('button', { name: 'Agregar «Camino de Guanajuato» al concierto' }));
    expect(titulosEnConcierto()).toEqual(['El Rey', 'Volver, volver', 'Camino de Guanajuato']);
    expect(screen.getByText('Cambios sin guardar')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Catálogo · 1' })).toBeInTheDocument();

    // Subir la última dos lugares con el teclado: el foco la sigue.
    await usuario.click(screen.getByRole('button', { name: 'Subir «Camino de Guanajuato»' }));
    expect(screen.getByRole('button', { name: 'Subir «Camino de Guanajuato»' })).toHaveFocus();
    await usuario.keyboard('{Enter}');
    expect(titulosEnConcierto()).toEqual(['Camino de Guanajuato', 'El Rey', 'Volver, volver']);
    expect(screen.getByRole('button', { name: 'Subir «Camino de Guanajuato»' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Bajar «Camino de Guanajuato»' })).toHaveFocus();

    await usuario.click(screen.getByRole('button', { name: 'Quitar «Volver, volver» del concierto' }));
    expect(titulosEnConcierto()).toEqual(['Camino de Guanajuato', 'El Rey']);

    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(await screen.findByText('Repertorio guardado (2 canciones)')).toBeInTheDocument();
    const puts = red.peticiones.filter((p) => p.metodo === 'PUT');
    expect(puts).toHaveLength(1);
    expect(puts[0].url).toBe('/canciones/evento/ev-1');
    expect(puts[0].cuerpo).toEqual({ cancionIds: ['c-3', 'c-1'] });
    expect(screen.getByText('Sin cambios')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
  });

  it('volver al estado original deja de contar como cambio', async () => {
    montar();
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1/repertorio');
    await screen.findByRole('heading', { level: 1, name: 'Noche de Bohemia' });
    await usuario.click(screen.getByRole('button', { name: 'Bajar «El Rey»' }));
    expect(screen.getByText('Cambios sin guardar')).toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Subir «El Rey»' }));
    expect(screen.getByText('Sin cambios')).toBeInTheDocument();
  });

  it('un repertorio vacío y un catálogo agotado muestran los textos del móvil', async () => {
    montar({}, []);
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1/repertorio');
    expect(await screen.findByText('Todavía no hay canciones. Agrega del catálogo con el botón +.')).toBeInTheDocument();
    for (const t of ['El Rey', 'Volver, volver', 'Camino de Guanajuato']) {
      await usuario.click(screen.getByRole('button', { name: `Agregar «${t}» al concierto` }));
    }
    // Solo queda la no publicada: sigue en el catálogo, deshabilitada.
    expect(screen.getByRole('heading', { level: 2, name: 'Catálogo · 1' })).toBeInTheDocument();
  });

  it('con el catálogo vacío lo explica', async () => {
    montar({ 'GET /canciones': { data: [] } }, []);
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos/ev-1/repertorio');
    expect(await screen.findByText('El catálogo está vacío')).toBeInTheDocument();
    expect(screen.getByText(/Primero da de alta canciones/)).toBeInTheDocument();
  });

  it('si no carga el catálogo muestra el error y reintenta', async () => {
    let intento = 0;
    montar({
      'GET /canciones': () => {
        intento += 1;
        return intento === 1 ? { status: 500, data: {} } : { data: catalogo };
      },
    });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1/repertorio');
    expect(await screen.findByRole('heading', { name: 'No se pudo cargar el catálogo' })).toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByRole('heading', { level: 2, name: 'En el concierto · 2' })).toBeInTheDocument();
  });

  it('un concierto inexistente (404) lo dice', async () => {
    simularRed({
      'GET /eventos/ev-9': { status: 404, data: { message: 'Evento no encontrado' } },
      'GET /canciones': { data: catalogo },
      'GET /canciones/evento/ev-9': { data: [] },
    });
    iniciarSesionAdmin();
    renderizarApp('/admin/conciertos/ev-9/repertorio');
    expect(await screen.findByRole('heading', { level: 1, name: 'Concierto no encontrado' })).toBeInTheDocument();
  });

  it.each([
    [400, /Alguna canción ya no existe/],
    [403, /no tiene permisos de administrador/],
    [404, /Este concierto ya no existe/],
  ])('si el guardado falla con %s lo explica y conserva los cambios', async (status, texto) => {
    montar({ 'PUT /canciones/evento/ev-1': { status, data: { message: 'x' } } });
    iniciarSesionAdmin();
    const usuario = userEvent.setup();
    renderizarApp('/admin/conciertos/ev-1/repertorio');
    await screen.findByRole('heading', { level: 1, name: 'Noche de Bohemia' });
    await usuario.click(screen.getByRole('button', { name: 'Quitar «El Rey» del concierto' }));
    await usuario.click(screen.getByRole('button', { name: 'Guardar' }));
    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('No se pudo guardar el repertorio');
    expect(alerta).toHaveTextContent(texto);
    expect(screen.getByText('Cambios sin guardar')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeEnabled();
  });
});
