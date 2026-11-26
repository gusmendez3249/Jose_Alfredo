import { describe, expect, it } from 'vitest';
import { API_URL } from '../config/env';
import { urlMedia } from './media';

const ORIGEN = new URL(API_URL).origin;

describe('urlMedia', () => {
  it('resuelve los archivos subidos contra el origen del backend, sin /api/v1', () => {
    expect(urlMedia('/uploads/file-1-2.jpg')).toBe(`${ORIGEN}/uploads/file-1-2.jpg`);
  });

  it('acepta https de los hosts permitidos', () => {
    expect(urlMedia('https://images.unsplash.com/photo-1?w=800')).toBe('https://images.unsplash.com/photo-1?w=800');
    expect(urlMedia('https://www.soundhelix.com/examples/mp3/a.mp3')).toBe('https://www.soundhelix.com/examples/mp3/a.mp3');
  });

  it.each([
    ['vacío', ''],
    ['nulo', null],
    ['javascript:', 'javascript:alert(1)'],
    ['data:', 'data:image/png;base64,AAAA'],
    ['http externo', 'http://images.unsplash.com/a.jpg'],
    ['host no permitido', 'https://evil.example/a.jpg'],
    ['protocolo relativo', '//evil.example/a.jpg'],
    ['ruta fuera de uploads', '/api/v1/eventos'],
    ['uploads con subcarpeta', '/uploads/../secreto'],
    ['esquema de almacenamiento', 'gs://bucket/a.jpg'],
  ])('rechaza %s', (_nombre, valor) => {
    expect(urlMedia(valor)).toBeNull();
  });
});
