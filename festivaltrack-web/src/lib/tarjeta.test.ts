import { describe, expect, it } from 'vitest';
import {
  formatearNumeroTarjeta,
  formatearVencimiento,
  pasaLuhn,
  validarTarjeta,
  vencimientoVigente,
} from './tarjeta';

describe('pasaLuhn', () => {
  it('acepta números de prueba válidos', () => {
    expect(pasaLuhn('4242424242424242')).toBe(true);
    expect(pasaLuhn('378282246310005')).toBe(true); // American Express
  });

  it('rechaza un dígito cambiado y la cadena vacía', () => {
    expect(pasaLuhn('4242424242424241')).toBe(false);
    expect(pasaLuhn('')).toBe(false);
  });
});

describe('formateo', () => {
  it('agrupa de cuatro en cuatro y limpia lo que no es dígito', () => {
    expect(formatearNumeroTarjeta('4242-4242 4242.4242')).toBe('4242 4242 4242 4242');
    expect(formatearNumeroTarjeta('42424')).toBe('4242 4');
  });

  it('usa el agrupamiento 4-6-5 en American Express y corta en 15 dígitos', () => {
    expect(formatearNumeroTarjeta('378282246310005')).toBe('3782 822463 10005');
    expect(formatearNumeroTarjeta('3782822463100059999')).toBe('3782 822463 10005');
  });

  it('pone la barra del vencimiento tras el mes', () => {
    expect(formatearVencimiento('1')).toBe('1');
    expect(formatearVencimiento('1228')).toBe('12/28');
    expect(formatearVencimiento('12/2899')).toBe('12/28');
  });
});

describe('vencimientoVigente', () => {
  const hoy = new Date(2026, 5, 15); // 15 de junio de 2026

  it('vale hasta el final del mes de vencimiento', () => {
    expect(vencimientoVigente('06/26', hoy)).toBe(true);
    expect(vencimientoVigente('07/26', hoy)).toBe(true);
  });

  it('rechaza meses ya pasados, meses inválidos y formatos raros', () => {
    expect(vencimientoVigente('05/26', hoy)).toBe(false);
    expect(vencimientoVigente('12/25', hoy)).toBe(false);
    expect(vencimientoVigente('13/30', hoy)).toBe(false);
    expect(vencimientoVigente('00/30', hoy)).toBe(false);
    expect(vencimientoVigente('1/30', hoy)).toBe(false);
  });
});

describe('validarTarjeta', () => {
  const buena = { titular: 'Ana Pérez', numero: '4242 4242 4242 4242', vencimiento: '12/99', cvv: '123' };

  it('no marca errores con datos válidos', () => {
    expect(validarTarjeta(buena)).toEqual({});
  });

  it('exige titular y número válido', () => {
    const errores = validarTarjeta({ ...buena, titular: '  ', numero: '4242 4242 4242 4241' });
    expect(errores.titular).toMatch(/nombre/i);
    expect(errores.numero).toMatch(/no es válido/i);
  });

  it('pide 15 o 16 dígitos', () => {
    expect(validarTarjeta({ ...buena, numero: '4242 4242' }).numero).toMatch(/15 o 16/);
  });

  it('rechaza una tarjeta caducada y un formato distinto de MM/AA', () => {
    expect(validarTarjeta({ ...buena, vencimiento: '01/20' }).vencimiento).toMatch(/ya pasó/);
    expect(validarTarjeta({ ...buena, vencimiento: '1299' }).vencimiento).toMatch(/MM\/AA/);
  });

  it('valida el CVV: 3 dígitos en general y 4 en American Express', () => {
    expect(validarTarjeta({ ...buena, cvv: '12' }).cvv).toMatch(/3 o 4/);
    expect(validarTarjeta({ ...buena, cvv: '1234' }).cvv).toMatch(/3 dígitos/);
    const amex = { ...buena, numero: '3782 822463 10005' };
    expect(validarTarjeta({ ...amex, cvv: '123' }).cvv).toMatch(/4 dígitos/);
    expect(validarTarjeta({ ...amex, cvv: '1234' })).toEqual({});
  });
});
