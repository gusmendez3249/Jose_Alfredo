import { guardarSesion } from '../auth/session';
import { crearJwt, USUARIO } from './datos';

/** Inicia sesión "por debajo" (sin pasar por la pantalla de login). */
export function iniciarSesionDePrueba(): void {
  guardarSesion(crearJwt(Math.floor(Date.now() / 1000) + 3600), USUARIO);
}
