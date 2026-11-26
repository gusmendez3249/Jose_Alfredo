import {
  ALFABETO_CODIGO,
  MAX_PENDIENTES,
  VIGENCIA_MS,
  VinculosTv,
  formatearCodigo,
  normalizarCodigo,
} from './vinculos-tv';

/** Pruebas unitarias del inicio de sesión de la TV con el celular (sin base de datos). */
describe('VinculosTv', () => {
  let reloj: number;
  let vinculos: VinculosTv;

  beforeEach(() => {
    reloj = 1_000_000;
    vinculos = new VinculosTv(() => reloj);
  });

  it('crea un código de 8 caracteres legibles y un token secreto distinto', () => {
    const v = vinculos.crear()!;
    expect(v.codigo).toMatch(/^[A-Z0-9]{4}-[A-Z0-9]{4}$/);
    for (const letra of normalizarCodigo(v.codigo)) {
      expect(ALFABETO_CODIGO).toContain(letra);
    }
    expect(v.tvToken).toMatch(/^[a-f0-9]{64}$/);
    expect(v.expiraEnSegundos).toBe(300);
    expect(vinculos.crear()!.codigo).not.toBe(v.codigo);
  });

  it('mientras nadie aprueba, la TV ve PENDIENTE', () => {
    const v = vinculos.crear()!;
    expect(vinculos.consultar(v.tvToken)).toEqual({ estado: 'PENDIENTE' });
  });

  it('al aprobar, la TV recibe el usuario una sola vez', () => {
    const v = vinculos.crear()!;
    expect(vinculos.aprobar(v.codigo, 'usuario-1')).toBe(true);
    expect(vinculos.consultar(v.tvToken)).toEqual({
      estado: 'APROBADO',
      usuarioId: 'usuario-1',
    });
    // Ya se entregó: no se puede reutilizar ni el token ni el código.
    expect(vinculos.consultar(v.tvToken)).toEqual({ estado: 'EXPIRADO' });
    expect(vinculos.aprobar(v.codigo, 'usuario-1')).toBe(false);
    expect(vinculos.pendientes).toBe(0);
  });

  it('acepta el código escrito con minúsculas, espacios o sin guion', () => {
    const v = vinculos.crear()!;
    const escrito = ` ${normalizarCodigo(v.codigo).toLowerCase().slice(0, 4)} ${normalizarCodigo(v.codigo).toLowerCase().slice(4)} `;
    expect(vinculos.aprobar(escrito, 'usuario-1')).toBe(true);
  });

  it('un código que no existe no se aprueba', () => {
    vinculos.crear();
    expect(vinculos.aprobar('ZZZZ-ZZZZ', 'usuario-1')).toBe(false);
  });

  it('otra cuenta no puede quedarse con una TV ya aprobada', () => {
    const v = vinculos.crear()!;
    expect(vinculos.aprobar(v.codigo, 'usuario-1')).toBe(true);
    expect(vinculos.aprobar(v.codigo, 'intruso')).toBe(false);
    expect(vinculos.consultar(v.tvToken)).toEqual({
      estado: 'APROBADO',
      usuarioId: 'usuario-1',
    });
  });

  it('a los 5 minutos el código vence: no se aprueba y la TV ve EXPIRADO', () => {
    const v = vinculos.crear()!;
    reloj += VIGENCIA_MS;
    expect(vinculos.aprobar(v.codigo, 'usuario-1')).toBe(false);
    expect(vinculos.consultar(v.tvToken)).toEqual({ estado: 'EXPIRADO' });
  });

  it('un token desconocido se toma como vencido', () => {
    expect(vinculos.consultar('f'.repeat(64))).toEqual({ estado: 'EXPIRADO' });
  });

  it('borra los vencidos y pone tope a los pendientes', () => {
    for (let i = 0; i < MAX_PENDIENTES; i++) vinculos.crear();
    expect(vinculos.crear()).toBeNull();
    reloj += VIGENCIA_MS;
    expect(vinculos.crear()).not.toBeNull();
    expect(vinculos.pendientes).toBe(1);
  });

  it('formatea y normaliza el código', () => {
    expect(formatearCodigo('KX7M3QPA')).toBe('KX7M-3QPA');
    expect(normalizarCodigo('kx7m-3qpa')).toBe('KX7M3QPA');
  });
});
