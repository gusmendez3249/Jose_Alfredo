import { IsOptional, IsString, MaxLength } from 'class-validator';

/** Alta de una imagen en la galería (solo administradores). */
export class AddImagenDto {
  @IsOptional() @IsString() url?: string;
  @IsOptional() @IsString() @MaxLength(150) titulo?: string;
}
