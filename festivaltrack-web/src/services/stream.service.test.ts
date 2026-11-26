import { describe, expect, it } from 'vitest';
import { simularRed } from '../test/red';
import { esMensajeChat, fusionarMensajes, streamService, type MensajeChat } from './stream.service';

const msg = (id: string, minuto: number, parcial: Partial<MensajeChat> = {}): MensajeChat => ({
  id,
  eventoId: 'EVT-001',
  usuarioNombre: 'Ana',
  mensaje: `mensaje ${id}`,
  esAdmin: false,
  fechaEnvio: new Date(Date.UTC(2026, 10, 25, 20, minuto)).toISOString(),
  ...parcial,
});

describe('fusionarMensajes', () => {
  it('no repite por id y ordena cronológicamente', () => {
    const a = [msg('1', 1), msg('3', 3)];
    const r = fusionarMensajes(a, [msg('2', 2), msg('3', 3), msg('2', 2)]);
    expect(r.map((m) => m.id)).toEqual(['1', '2', '3']);
  });

  it('devuelve la misma referencia si no hay nada nuevo', () => {
    const a = [msg('1', 1), msg('2', 2)];
    expect(fusionarMensajes(a, [msg('2', 2), msg('1', 1)])).toBe(a);
  });

  it('conserva mensajes viejos que el servidor ya no manda y acota la memoria', () => {
    const muchos = Array.from({ length: 250 }, (_, i) => msg(`m${i}`, i % 60, { fechaEnvio: new Date(Date.UTC(2026, 0, 1, 0, i)).toISOString() }));
    const r = fusionarMensajes([], muchos);
    expect(r).toHaveLength(200);
    expect(r.at(-1)?.id).toBe('m249');
  });
});

describe('esMensajeChat', () => {
  it('descarta formas inválidas', () => {
    expect(esMensajeChat(msg('1', 1))).toBe(true);
    expect(esMensajeChat({ ...msg('1', 1), esAdmin: 'si' })).toBe(false);
    expect(esMensajeChat({ ...msg('1', 1), fechaEnvio: 'ayer' })).toBe(false);
    expect(esMensajeChat({ ...msg('1', 1), id: '' })).toBe(false);
    expect(esMensajeChat(null)).toBe(false);
  });
});

describe('streamService', () => {
  it('estado: conserva solo lo usable y descarta las URL RTSP', async () => {
    simularRed({
      'GET /stream/status': {
        data: { streamUrl: 'rtsp://192.168.1.5:1935', emulatorUrl: 'rtsp://10.0.2.2:1935', isLive: true, port: 1935, eventoId: 'EVT-001', eventoNombre: 'Gran Noche' },
      },
    });
    const estado = await streamService.estado();
    expect(estado).toEqual({ isLive: true, eventoId: 'EVT-001', eventoNombre: 'Gran Noche' });
    expect(estado).not.toHaveProperty('streamUrl');
  });

  it('estado: una respuesta sin isLive booleano es inválida', async () => {
    simularRed({ 'GET /stream/status': { data: '<html>' } });
    await expect(streamService.estado()).rejects.toThrow(/Respuesta inesperada/);
  });

  it('chat: invierte el orden del servidor y descarta elementos inválidos', async () => {
    simularRed({ 'GET /stream/chat/EVT-001': { data: [msg('3', 3), { basura: true }, msg('2', 2), msg('1', 1)] } });
    expect((await streamService.chat('EVT-001')).map((m) => m.id)).toEqual(['1', '2', '3']);
  });

  it('enviar: manda eventoId y mensaje y devuelve el mensaje creado', async () => {
    const { peticiones } = simularRed({ 'POST /stream/chat': { status: 201, data: msg('9', 9, { mensaje: 'hola' }) } });
    const creado = await streamService.enviar('EVT-001', 'hola');
    expect(creado.id).toBe('9');
    expect(peticiones[0].cuerpo).toEqual({ eventoId: 'EVT-001', mensaje: 'hola' });
  });
});
