import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

/**
 * Igual que JwtAuthGuard, pero no rechaza peticiones sin token válido:
 * deja `req.user` en null. Útil para lecturas públicas que personalizan
 * la respuesta cuando hay sesión (p. ej. "likedByMe" en el foro).
 */
@Injectable()
export class OptionalJwtAuthGuard extends AuthGuard('jwt') {
  handleRequest<TUser = any>(_err: any, user: any): TUser {
    return (user || null) as TUser;
  }
}
