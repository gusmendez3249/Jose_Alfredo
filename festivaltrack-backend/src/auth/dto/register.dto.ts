import { IsEmail, IsString, MinLength } from 'class-validator';

export class RegisterDto {
  @IsString() nombre: string;
  @IsEmail() correo: string;
  @IsString() @MinLength(6) contrasena: string;
}
