import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

/**
 * Cuerpo de `POST /stream/chat`. El nombre del remitente y si es admin
 * NO se aceptan del cliente: se toman del usuario autenticado.
 */
export class EnviarMensajeDto {
  /** Evento cuya transmisión recibe el mensaje (ej. "EVT-001"). */
  @IsString() @IsNotEmpty() eventoId: string;
  /** Texto del mensaje; máximo 300 caracteres para evitar spam de mensajes gigantes. */
  @IsString() @IsNotEmpty() @MaxLength(300) mensaje: string;
}
// Con ValidationPipe({ whitelist: true }) cualquier campo extra del body
// (usuarioNombre, esAdmin...) se descarta antes de llegar al controlador.
