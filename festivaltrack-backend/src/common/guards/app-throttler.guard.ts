import { Injectable } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';

/**
 * Rate limiting global (ver ThrottlerModule en AppModule).
 *
 * Por defecto cuenta las peticiones por IP. En el login cuenta por IP + correo:
 * en el festival muchos celulares salen por la misma IP pública (Wi-Fi del evento),
 * así que limitar solo por IP bloquearía a usuarios legítimos; por correo se sigue
 * frenando la fuerza bruta contra una cuenta.
 */
@Injectable()
export class AppThrottlerGuard extends ThrottlerGuard {
  /**
   * Devuelve la "clave" con la que se cuentan las peticiones (#51).
   * Peticiones con la misma clave comparten el mismo contador de límite.
   *
   * @param req Petición HTTP de Express.
   * @returns La IP del cliente, o `IP:correo` cuando es un intento de login.
   */
  protected async getTracker(req: Record<string, any>): Promise<string> {
    // Con `trust proxy` activo, req.ips trae la cadena de X-Forwarded-For; la primera es el cliente real.
    const ip = req.ips?.length ? req.ips[0] : req.ip;
    // El correo se normaliza para que "Ana@Test.com" y "ana@test.com" cuenten como la misma cuenta.
    const esLogin = typeof req.originalUrl === 'string' && req.originalUrl.includes('/auth/login');
    const correo = typeof req.body?.correo === 'string' ? req.body.correo.trim().toLowerCase() : '';
    return esLogin && correo ? `${ip}:${correo}` : ip;
  }
}
