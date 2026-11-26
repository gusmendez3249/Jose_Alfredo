import type { BoletoConEvento, Evento, Usuario } from '../services/tipos';

export const USUARIO: Usuario = { id: 'u-1', nombre: 'Ana Pérez', correo: 'ana@correo.mx', rol: 'USUARIO' };

/** JWT de mentira (solo importa el payload `exp`, que la web usa para caducar la sesión). */
export function crearJwt(exp?: number): string {
  const base64Url = (o: object) => btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64Url({ alg: 'HS256' })}.${base64Url(exp === undefined ? {} : { exp })}.firma`;
}

export function crearEvento(parcial: Partial<Evento> = {}): Evento {
  return {
    id: 'ev-1',
    nombre: 'Noche de Bohemia',
    fechaHora: new Date(Date.now() + 3 * 24 * 3600 * 1000).toISOString(),
    ubicacion: 'Jardín Principal',
    escenario: 'Plaza Principal',
    descripcion: null,
    capacidad: 800,
    bannerUrl: null,
    estado: 'PUBLICADO',
    artistaId: null,
    latitud: 21.15,
    longitud: -100.93,
    artista: { id: 'a-1', nombre: 'José Alfredo Jiménez', imagenUrl: null },
    ...parcial,
  };
}

export function crearBoleto(parcial: Partial<BoletoConEvento> = {}): BoletoConEvento {
  return {
    id: 'cmwabcdef123456',
    eventoId: 'ev-1',
    usuarioId: 'u-1',
    categoria: 'GENERAL',
    precio: 1200,
    fechaCompra: new Date().toISOString(),
    codigoQR: 'QR-SECRETO-1',
    estado: 'ACTIVO',
    evento: crearEvento(),
    ...parcial,
  };
}
