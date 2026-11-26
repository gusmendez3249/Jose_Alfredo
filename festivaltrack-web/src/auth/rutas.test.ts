import { describe, expect, it } from 'vitest';
import { esRutaInterna, leerEstadoLogin } from './rutas';

describe('esRutaInterna', () => {
  it('acepta rutas internas con ruta, consulta y hash', () => {
    expect(esRutaInterna('/mis-boletos')).toBe(true);
    expect(esRutaInterna('/checkout/ev-1?x=1#a')).toBe(true);
  });

  it('rechaza URLs relativas al protocolo (//dominio) y con barra invertida', () => {
    expect(esRutaInterna('//malo.com')).toBe(false);
    expect(esRutaInterna('/\\malo.com')).toBe(false);
  });

  it('rechaza URLs absolutas, javascript: y valores que no son texto', () => {
    expect(esRutaInterna('https://malo.com')).toBe(false);
    expect(esRutaInterna('javascript:alert(1)')).toBe(false);
    expect(esRutaInterna('mis-boletos')).toBe(false);
    expect(esRutaInterna(undefined)).toBe(false);
    expect(esRutaInterna(42)).toBe(false);
  });

  it('no vuelve a /login ni /register (bucle)', () => {
    expect(esRutaInterna('/login')).toBe(false);
    expect(esRutaInterna('/register')).toBe(false);
  });
});

describe('leerEstadoLogin', () => {
  it('conserva un from válido y la marca de sesión expirada', () => {
    expect(leerEstadoLogin({ from: '/mis-boletos', sesionExpirada: true })).toEqual({
      from: '/mis-boletos',
      sesionExpirada: true,
    });
  });

  it('descarta un from peligroso y tolera estados vacíos', () => {
    expect(leerEstadoLogin({ from: '//malo.com' })).toEqual({ from: undefined, sesionExpirada: false });
    expect(leerEstadoLogin(null)).toEqual({});
    expect(leerEstadoLogin('texto')).toEqual({});
  });
});
