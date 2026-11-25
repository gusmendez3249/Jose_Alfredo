import {
  IsBoolean,
  IsOptional,
  IsString,
  Matches,
  MaxLength,
} from 'class-validator';

/** Estado de la transmisión que publica el admin móvil. Solo se aceptan URLs rtsp:// (o vacía al detener). */
export class StreamStatusBodyDto {
  @IsOptional()
  @IsString()
  @Matches(/^(rtsp:\/\/.+)?$/, {
    message: 'streamUrl debe ser una URL rtsp://',
  })
  streamUrl?: string;

  @IsOptional() @IsBoolean() isLive?: boolean;

  /** Concierto que se está transmitiendo. */
  @IsOptional() @IsString() @MaxLength(64) eventoId?: string;

  /** Nombre del concierto, para mostrarlo sin otra consulta. */
  @IsOptional() @IsString() @MaxLength(200) eventoNombre?: string;
}
