import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

/**
 * Cuerpo de `POST /stream/chat`. El nombre del remitente y si es admin
 * NO se aceptan del cliente: se toman del usuario autenticado.
 */
export class EnviarMensajeDto {
  @IsString() @IsNotEmpty() eventoId: string;
  @IsString() @IsNotEmpty() @MaxLength(300) mensaje: string;
}
