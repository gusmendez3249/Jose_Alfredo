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
  protected async getTracker(req: Record<string, any>): Promise<string> {
    const ip = req.ips?.length ? req.ips[0] : req.ip;
    const esLogin = typeof req.originalUrl === 'string' && req.originalUrl.includes('/auth/login');
    const correo = typeof req.body?.correo === 'string' ? req.body.correo.trim().toLowerCase() : '';
    return esLogin && correo ? `${ip}:${correo}` : ip;
  }
}
