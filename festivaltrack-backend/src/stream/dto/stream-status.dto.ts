import { IsBoolean, IsOptional, IsString, Matches } from 'class-validator';

/** Estado de la transmisión que publica el admin móvil. Solo se aceptan URLs rtsp:// (o vacía al detener). */
export class StreamStatusBodyDto {
  @IsOptional()
  @IsString()
  @Matches(/^(rtsp:\/\/.+)?$/, { message: 'streamUrl debe ser una URL rtsp://' })
  streamUrl?: string;

  @IsOptional() @IsBoolean() isLive?: boolean;
}
