import { describe, expect, it } from 'vitest';
import { formatearMinSeg, MAX_BYTES, validarArchivo } from './archivos';
import { haceTiempo } from './lecturas';

function archivo(nombre: string, tipo: string, bytes = 10): File {
  const f = new File(['x'], nombre, { type: tipo });
  Object.defineProperty(f, 'size', { value: bytes });
  return f;
}

describe('validarArchivo', () => {
  it('acepta audio e imagen válidos', () => {
    expect(validarArchivo(archivo('a.mp3', 'audio/mpeg'), 'audio')).toBeNull();
    expect(validarArchivo(archivo('a.jpg', 'image/jpeg'), 'imagen')).toBeNull();
  });

  it('completa el tipo por extensión cuando el navegador no lo informa', () => {
    expect(validarArchivo(archivo('a.m4a', ''), 'audio')).toBeNull();
    expect(validarArchivo(archivo('a.ogg', ''), 'audio')).toBeNull();
  });

  it('rechaza formatos que no corresponden', () => {
    expect(validarArchivo(archivo('a.pdf', 'application/pdf'), 'audio')).toMatch(/Formato no permitido/);
    expect(validarArchivo(archivo('a.mp3', 'audio/mpeg'), 'imagen')).toMatch(/Formato no permitido/);
  });

  it('rechaza lo que pesa más de 30 MB con los textos del móvil', () => {
    expect(validarArchivo(archivo('a.mp3', 'audio/mpeg', MAX_BYTES + 1), 'audio')).toBe(
      'El audio pesa más de 30 MB; elige un archivo más ligero',
    );
    expect(validarArchivo(archivo('a.png', 'image/png', MAX_BYTES + 1), 'imagen')).toBe(
      'La portada pesa más de 30 MB; elige una imagen más ligera',
    );
    expect(validarArchivo(archivo('a.mp3', 'audio/mpeg', MAX_BYTES), 'audio')).toBeNull();
  });
});

describe('formatearMinSeg', () => {
  it('da minutos y segundos', () => {
    expect(formatearMinSeg(185)).toBe('3:05');
    expect(formatearMinSeg(59.6)).toBe('1:00');
  });
});

describe('haceTiempo', () => {
  const ahora = Date.parse('2026-01-01T12:00:00Z');
  it.each([
    ['2026-01-01T11:59:50Z', 'hace un momento'],
    ['2026-01-01T11:55:00Z', 'hace 5 min'],
    ['2026-01-01T09:00:00Z', 'hace 3 h'],
    ['2025-12-29T12:00:00Z', 'hace 3 d'],
    ['no es fecha', ''],
  ])('%s -> %s', (iso, texto) => {
    expect(haceTiempo(iso, ahora)).toBe(texto);
  });
});
