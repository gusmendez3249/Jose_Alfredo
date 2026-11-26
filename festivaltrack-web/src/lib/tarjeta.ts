// Validación de FORMATO de la tarjeta para el pago simulado. Los datos nunca salen del
// formulario: no se envían al backend ni se guardan en ningún almacenamiento.

export interface DatosTarjeta {
  titular: string;
  numero: string;
  vencimiento: string;
  cvv: string;
}

export type ErroresTarjeta = Partial<Record<keyof DatosTarjeta, string>>;

export const soloDigitos = (texto: string): string => texto.replace(/\D/g, '');

const esAmex = (digitos: string): boolean => /^3[47]/.test(digitos);

/** "4242424242424242" → "4242 4242 4242 4242"; American Express (15) → "3782 822463 10005". */
export function formatearNumeroTarjeta(entrada: string): string {
  const digitos = soloDigitos(entrada).slice(0, 16);
  if (esAmex(digitos)) {
    const d = digitos.slice(0, 15);
    return [d.slice(0, 4), d.slice(4, 10), d.slice(10)].filter(Boolean).join(' ');
  }
  return digitos.replace(/(\d{4})(?=\d)/g, '$1 ');
}

/** Máscara MM/AA mientras se escribe. */
export function formatearVencimiento(entrada: string): string {
  const digitos = soloDigitos(entrada).slice(0, 4);
  return digitos.length > 2 ? `${digitos.slice(0, 2)}/${digitos.slice(2)}` : digitos;
}

export function pasaLuhn(digitos: string): boolean {
  let suma = 0;
  let duplicar = false;
  for (let i = digitos.length - 1; i >= 0; i--) {
    let n = Number(digitos[i]);
    if (duplicar) {
      n *= 2;
      if (n > 9) n -= 9;
    }
    suma += n;
    duplicar = !duplicar;
  }
  return digitos.length > 0 && suma % 10 === 0;
}

/** true si MM/AA es un mes válido y no ha terminado todavía. */
export function vencimientoVigente(vencimiento: string, hoy = new Date()): boolean {
  const coincide = /^(\d{2})\/(\d{2})$/.exec(vencimiento);
  if (!coincide) return false;
  const mes = Number(coincide[1]);
  const anio = 2000 + Number(coincide[2]);
  if (mes < 1 || mes > 12) return false;
  // La tarjeta vale hasta el último día de su mes de vencimiento.
  const finDeMes = new Date(anio, mes, 1);
  return finDeMes.getTime() > hoy.getTime();
}

export function validarTarjeta(datos: DatosTarjeta): ErroresTarjeta {
  const errores: ErroresTarjeta = {};
  const numero = soloDigitos(datos.numero);
  const cvv = soloDigitos(datos.cvv);

  if (!datos.titular.trim()) errores.titular = 'Escribe el nombre como aparece en la tarjeta.';

  if (numero.length < 15 || numero.length > 16) errores.numero = 'El número de tarjeta debe tener 15 o 16 dígitos.';
  else if (!pasaLuhn(numero)) errores.numero = 'El número de tarjeta no es válido.';

  if (!/^\d{2}\/\d{2}$/.test(datos.vencimiento)) errores.vencimiento = 'Usa el formato MM/AA.';
  else if (!vencimientoVigente(datos.vencimiento)) errores.vencimiento = 'La fecha de vencimiento no es válida o ya pasó.';

  const largoCvv = esAmex(numero) ? 4 : 3;
  if (cvv.length < 3 || cvv.length > 4) errores.cvv = 'El CVV tiene 3 o 4 dígitos.';
  else if (numero.length >= 15 && cvv.length !== largoCvv) errores.cvv = `El CVV de esta tarjeta tiene ${largoCvv} dígitos.`;

  return errores;
}
