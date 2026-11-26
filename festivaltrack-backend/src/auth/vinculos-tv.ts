import { randomBytes, randomInt } from 'crypto';

/**
 * Vinculaciones pendientes de Smart TV (inicio de sesión con QR desde el celular).
 *
 * Flujo, como el de las apps de streaming en la tele:
 * 1. La TV pide un código ({@link crear}): recibe un `codigo` corto para mostrar en pantalla
 *    (y en el QR) y un `tvToken` secreto que solo ella conoce.
 * 2. El usuario, ya con sesión en la web o en la app, aprueba el código ({@link aprobar}).
 * 3. La TV pregunta con su `tvToken` ({@link consultar}) hasta que el código está aprobado;
 *    entonces recibe el id del usuario una sola vez y la vinculación se borra.
 *
 * Se guardan en memoria: duran 5 minutos y se pueden volver a pedir, así que no hace falta
 * una tabla (si el servidor se reinicia, la TV simplemente pide un código nuevo).
 */

/** Letras y números sin los que se confunden al leerlos en la tele (0/O, 1/I). */
export const ALFABETO_CODIGO = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
export const LARGO_CODIGO = 8;
export const VIGENCIA_MS = 5 * 60_000;
/** Tope de vinculaciones pendientes a la vez, para que nadie llene la memoria pidiendo códigos. */
export const MAX_PENDIENTES = 2_000;

interface Vinculo {
  codigo: string;
  tvToken: string;
  expira: number;
  usuarioId: string | null;
}

export type EstadoVinculo =
  | { estado: 'PENDIENTE' }
  | { estado: 'EXPIRADO' }
  | { estado: 'APROBADO'; usuarioId: string };

/** Deja el código como se guarda: mayúsculas y sin guiones ni espacios ("kx7m-3qpa" → "KX7M3QPA"). */
export function normalizarCodigo(texto: string): string {
  return texto.toUpperCase().replace(/[^A-Z0-9]/g, '');
}

/** Código para mostrar: en dos grupos de 4 ("KX7M3QPA" → "KX7M-3QPA"). */
export function formatearCodigo(codigo: string): string {
  return `${codigo.slice(0, 4)}-${codigo.slice(4)}`;
}

export class VinculosTv {
  private porCodigo = new Map<string, Vinculo>();
  private porToken = new Map<string, Vinculo>();

  constructor(private ahora: () => number = Date.now) {}

  /** Nueva vinculación pendiente; null si ya hay demasiadas (la TV reintenta después). */
  crear(): {
    codigo: string;
    tvToken: string;
    expiraEnSegundos: number;
  } | null {
    this.limpiarVencidos();
    if (this.porCodigo.size >= MAX_PENDIENTES) return null;

    let codigo = '';
    do {
      codigo = Array.from(
        { length: LARGO_CODIGO },
        () => ALFABETO_CODIGO[randomInt(ALFABETO_CODIGO.length)],
      ).join('');
    } while (this.porCodigo.has(codigo));

    const vinculo: Vinculo = {
      codigo,
      tvToken: randomBytes(32).toString('hex'),
      expira: this.ahora() + VIGENCIA_MS,
      usuarioId: null,
    };
    this.porCodigo.set(codigo, vinculo);
    this.porToken.set(vinculo.tvToken, vinculo);
    return {
      codigo: formatearCodigo(codigo),
      tvToken: vinculo.tvToken,
      expiraEnSegundos: VIGENCIA_MS / 1000,
    };
  }

  /**
   * El usuario con sesión aprueba el código que ve en la TV. Devuelve false si el código no
   * existe, ya venció o ya lo aprobó otra cuenta (no se puede "robar" una TV ya aprobada).
   */
  aprobar(codigoEscrito: string, usuarioId: string): boolean {
    const vinculo = this.porCodigo.get(normalizarCodigo(codigoEscrito));
    if (!vinculo || this.vencido(vinculo)) return false;
    if (vinculo.usuarioId && vinculo.usuarioId !== usuarioId) return false;
    vinculo.usuarioId = usuarioId;
    return true;
  }

  /** Lo que pregunta la TV. Al estar aprobado entrega el usuario una sola vez y se borra. */
  consultar(tvToken: string): EstadoVinculo {
    const vinculo = this.porToken.get(tvToken);
    if (!vinculo || this.vencido(vinculo)) {
      if (vinculo) this.borrar(vinculo);
      return { estado: 'EXPIRADO' };
    }
    if (!vinculo.usuarioId) return { estado: 'PENDIENTE' };
    this.borrar(vinculo);
    return { estado: 'APROBADO', usuarioId: vinculo.usuarioId };
  }

  /** Vinculaciones guardadas ahora mismo (para pruebas). */
  get pendientes(): number {
    return this.porCodigo.size;
  }

  private vencido(vinculo: Vinculo): boolean {
    return this.ahora() >= vinculo.expira;
  }

  private borrar(vinculo: Vinculo) {
    this.porCodigo.delete(vinculo.codigo);
    this.porToken.delete(vinculo.tvToken);
  }

  private limpiarVencidos() {
    for (const vinculo of this.porCodigo.values()) {
      if (this.vencido(vinculo)) this.borrar(vinculo);
    }
  }
}
