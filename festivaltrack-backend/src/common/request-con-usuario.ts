import type { Request } from 'express';

/** Usuario que la estrategia JWT deja en `req.user` (ver auth/jwt.strategy.ts). */
export interface UsuarioToken {
  id: string;
  correo: string;
  rol: string;
}

/** Petición de una ruta protegida con `JwtAuthGuard`: siempre trae el usuario. */
export interface RequestConUsuario extends Request {
  user: UsuarioToken;
}

/** Petición de una ruta con `OptionalJwtAuthGuard`: el usuario puede venir o no. */
export interface RequestConUsuarioOpcional extends Request {
  user?: UsuarioToken;
}
