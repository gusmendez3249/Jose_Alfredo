import { AxiosError, CanceledError } from 'axios';
import { describe, expect, it } from 'vitest';
import { RespuestaInvalidaError } from '../services/tipos';
import { esCancelacion, mensajeDeError } from './errores';

function errorHttp(status: number, data?: unknown): AxiosError {
  const config = { headers: {} } as AxiosError['config'];
  return new AxiosError('fallo', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: String(status),
    headers: {},
    config: config!,
    data,
  });
}

describe('mensajeDeError', () => {
  it('toma el primer mensaje cuando class-validator manda un arreglo', () => {
    const error = errorHttp(400, { message: ['El correo no es válido', 'La contraseña es corta'] });
    expect(mensajeDeError(error)).toBe('El correo no es válido');
  });

  it('usa el mensaje de texto del backend en 400, 404 y 409', () => {
    expect(mensajeDeError(errorHttp(409, { message: 'Correo ya registrado' }))).toBe('Correo ya registrado');
    expect(mensajeDeError(errorHttp(404, { message: 'Evento no encontrado' }))).toBe('Evento no encontrado');
  });

  it('cae en un texto genérico si el cuerpo no trae mensaje', () => {
    expect(mensajeDeError(errorHttp(400, {}))).toMatch(/Revisa los datos/);
    expect(mensajeDeError(errorHttp(409, null))).toMatch(/Ya existe/);
  });

  it('401 pide iniciar sesión de nuevo', () => {
    expect(mensajeDeError(errorHttp(401))).toMatch(/sesión expiró/i);
  });

  it('429 nunca muestra el texto crudo de ThrottlerException', () => {
    const mensaje = mensajeDeError(errorHttp(429, { message: 'ThrottlerException: Too Many Requests' }));
    expect(mensaje).toMatch(/Demasiados intentos/);
    expect(mensaje).not.toMatch(/Throttler/);
  });

  it('un mensaje propio por estado tiene prioridad', () => {
    expect(mensajeDeError(errorHttp(401), { 401: 'Correo o contraseña incorrectos.' })).toBe(
      'Correo o contraseña incorrectos.',
    );
  });

  it('distingue errores del servidor (5xx) y servidor despertando (503)', () => {
    expect(mensajeDeError(errorHttp(500))).toMatch(/problema/);
    expect(mensajeDeError(errorHttp(503))).toMatch(/despertando/);
  });

  it('sin respuesta del servidor habla de conexión', () => {
    const sinRespuesta = new AxiosError('Network Error', 'ERR_NETWORK');
    expect(mensajeDeError(sinRespuesta)).toMatch(/conectar con el servidor/);
  });

  it('un tiempo agotado se explica como servidor despertando', () => {
    expect(mensajeDeError(new AxiosError('timeout', 'ECONNABORTED'))).toMatch(/despertando o tarda/);
  });

  it('respuestas con forma inesperada y errores desconocidos', () => {
    expect(mensajeDeError(new RespuestaInvalidaError('eventos'))).toMatch(/datos inesperados/);
    expect(mensajeDeError(new Error('x'))).toMatch(/error inesperado/);
  });
});

describe('esCancelacion', () => {
  it('reconoce cancelaciones de axios y AbortError', () => {
    expect(esCancelacion(new CanceledError())).toBe(true);
    expect(esCancelacion(new DOMException('abortado', 'AbortError'))).toBe(true);
    expect(esCancelacion(new Error('otro'))).toBe(false);
  });
});
