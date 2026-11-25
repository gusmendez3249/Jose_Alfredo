import { IsEmail, IsString, Matches, MinLength } from 'class-validator';

/** Misma regla que muestran la app y la web al registrarse. */
export const REGLA_CONTRASENA =
  'La contraseña debe tener al menos 8 caracteres, una mayúscula y un número';

export class RegisterDto {
  @IsString() nombre: string;
  @IsEmail({}, { message: 'Escribe un correo válido' }) correo: string;
  @IsString()
  @MinLength(8, { message: REGLA_CONTRASENA })
  @Matches(/(?=.*[A-Z])(?=.*\d)/, { message: REGLA_CONTRASENA })
  contrasena: string;
}
