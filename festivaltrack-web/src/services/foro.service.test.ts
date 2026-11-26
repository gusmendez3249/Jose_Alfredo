import { AxiosHeaders } from 'axios';
import { describe, expect, it } from 'vitest';
import { simularRed } from '../test/red';
import { USUARIO } from '../test/datos';
import {
  aPublicacion,
  foroService,
  fusionarPaginas,
  puedeBorrar,
  tiempoRelativo,
  validarImagen,
  type Publicacion,
} from './foro.service';

function pub(id: string, parcial: Partial<Publicacion> = {}): Publicacion {
  return {
    id,
    tipo: 'EXPERIENCIA',
    titulo: null,
    contenido: `Texto ${id}`,
    imagenUrl: null,
    lugar: null,
    createdAt: '2026-11-20T10:00:00.000Z',
    autorId: 'u-2',
    autorNombre: 'Luis',
    likes: 0,
    comentarios: 0,
    likedByMe: false,
    ...parcial,
  };
}

function archivo(tipo: string, bytes: number, nombre = 'foto.jpg'): File {
  return new File([new Uint8Array(bytes)], nombre, { type: tipo });
}

describe('tiempoRelativo', () => {
  const AHORA = Date.parse('2026-11-25T12:00:00.000Z');
  const hace = (ms: number) => new Date(AHORA - ms).toISOString();
  const MIN = 60_000;
  const DIA = 24 * 60 * MIN;

  it.each([
    [hace(30_000), 'ahora'],
    [new Date(AHORA + 5 * MIN).toISOString(), 'ahora'],
    [hace(5 * MIN), 'hace 5 min'],
    [hace(3 * 60 * MIN), 'hace 3 h'],
    [hace(2 * DIA), 'hace 2 d'],
    [hace(15 * DIA), 'hace 2 sem'],
    [hace(31 * DIA), 'hace 1 mes'],
    [hace(95 * DIA), 'hace 3 meses'],
    [hace(800 * DIA), 'hace 2 años'],
    ['no es fecha', ''],
  ])('%s → "%s"', (iso, esperado) => {
    expect(tiempoRelativo(iso, AHORA)).toBe(esperado);
  });
});

describe('validarImagen', () => {
  it('acepta JPG, PNG y WEBP de hasta 8 MB', () => {
    expect(validarImagen(archivo('image/jpeg', 10))).toBeNull();
    expect(validarImagen(archivo('image/png', 10))).toBeNull();
    expect(validarImagen(archivo('image/webp', 8 * 1024 * 1024))).toBeNull();
  });

  it('rechaza otros tipos, archivos de más de 8 MB y vacíos', () => {
    expect(validarImagen(archivo('image/gif', 10))).toMatch(/JPG, PNG o WEBP/);
    expect(validarImagen(archivo('image/svg+xml', 10))).toMatch(/JPG, PNG o WEBP/);
    expect(validarImagen(archivo('image/jpeg', 8 * 1024 * 1024 + 1))).toMatch(/8 MB/);
    expect(validarImagen(archivo('image/jpeg', 0))).toMatch(/vacío/);
  });
});

describe('reglas auxiliares', () => {
  it('fusionarPaginas añade al final sin repetir ids', () => {
    const r = fusionarPaginas([pub('a'), pub('b')], [pub('b'), pub('c')]);
    expect(r.map((p) => p.id)).toEqual(['a', 'b', 'c']);
  });

  it('puedeBorrar: autor o administrador; nadie sin sesión', () => {
    expect(puedeBorrar(USUARIO, USUARIO.id)).toBe(true);
    expect(puedeBorrar(USUARIO, 'otro')).toBe(false);
    expect(puedeBorrar({ ...USUARIO, rol: 'ADMINISTRADOR' }, 'otro')).toBe(true);
    expect(puedeBorrar(null, 'otro')).toBe(false);
  });

  it('aPublicacion descarta formas inválidas y normaliza vacíos a null', () => {
    expect(aPublicacion({ ...pub('a'), likes: '3' })).toBeNull();
    expect(aPublicacion({ ...pub('a'), tipo: 'OTRO' })).toBeNull();
    expect(aPublicacion({ ...pub('a'), createdAt: 'ayer' })).toBeNull();
    expect(aPublicacion(null)).toBeNull();
    const normal = aPublicacion({ ...pub('a'), titulo: '  ', lugar: '', imagenUrl: '', contenido: null, likedByMe: 'sí' });
    expect(normal).toMatchObject({ titulo: null, lugar: null, imagenUrl: null, contenido: '', likedByMe: false });
  });
});

describe('foroService', () => {
  it('listar manda tipo, before y limit como parámetros, descarta inválidos y calcula el cursor', async () => {
    const parametros: unknown[] = [];
    const pagina = [pub('a'), { roto: true }, ...Array.from({ length: 18 }, (_, i) => pub(`p${i}`)), pub('z', { createdAt: '2026-11-01T00:00:00.000Z' })];
    simularRed({
      'GET /foro': (_c, config) => {
        parametros.push(config.params);
        return { data: pagina };
      },
    });
    const r = await foroService.listar({ tipo: 'FOTO', before: '2026-11-24T00:00:00.000Z' });
    expect(parametros[0]).toEqual({ tipo: 'FOTO', before: '2026-11-24T00:00:00.000Z', limit: '20' });
    expect(r.publicaciones).toHaveLength(20);
    expect(r.siguienteCursor).toBe('2026-11-01T00:00:00.000Z');
  });

  it('listar: con menos elementos que el límite no hay más páginas', async () => {
    simularRed({ 'GET /foro': { data: [pub('a')] } });
    expect((await foroService.listar({})).siguienteCursor).toBeNull();
  });

  it('listar falla con una respuesta que no es lista', async () => {
    simularRed({ 'GET /foro': { data: '<html>' } });
    await expect(foroService.listar({})).rejects.toThrow(/inesperada/);
  });

  it('crear envía solo textos recortados y omite los vacíos', async () => {
    const red = simularRed({ 'POST /foro': { status: 201, data: pub('n') } });
    await foroService.crear({ tipo: 'RECOMENDACION', titulo: '   ', contenido: '  Hola  ', lugar: ' Jardín ' });
    expect(red.peticiones[0].cuerpo).toEqual({ tipo: 'RECOMENDACION', contenido: 'Hola', lugar: 'Jardín' });
  });

  it('subirImagen manda multipart (campo file) sin forzar JSON y valida la ruta devuelta', async () => {
    let tipoContenido: unknown = 'sin revisar';
    let cuerpo: unknown;
    simularRed({
      'POST /upload': (c, config) => {
        cuerpo = c;
        tipoContenido = AxiosHeaders.from(config.headers).get('Content-Type');
        return { status: 201, data: { url: '/uploads/file-1-2.jpg' } };
      },
    });
    const foto = archivo('image/jpeg', 20);
    await expect(foroService.subirImagen(foto)).resolves.toBe('/uploads/file-1-2.jpg');
    expect(cuerpo).toBeInstanceOf(FormData);
    expect((cuerpo as FormData).get('file')).toBe(foto);
    expect(String(tipoContenido ?? '')).not.toMatch(/application\/json/);

    simularRed({ 'POST /upload': { status: 201, data: { url: 'https://evil.example/x.jpg' } } });
    await expect(foroService.subirImagen(foto)).rejects.toThrow(/inesperada/);
  });

  it('alternarLike devuelve el estado final del servidor', async () => {
    simularRed({ 'POST /foro/p%201/like': { data: { liked: true, likes: 7 } } });
    await expect(foroService.alternarLike('p 1')).resolves.toEqual({ liked: true, likes: 7 });
  });
});
