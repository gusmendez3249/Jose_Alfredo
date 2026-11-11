import { IsInt, IsNotEmpty, IsOptional, IsString, MaxLength, Min } from 'class-validator';

/** Alta de una canción del catálogo (solo administradores). */
export class CreateCancionDto {
  @IsString() @IsNotEmpty() @MaxLength(150) titulo: string;
  @IsOptional() @IsString() @MaxLength(150) artista?: string;
  @IsOptional() @IsInt() @Min(1) duracion?: number;
  @IsOptional() @IsString() archivoUrl?: string;
  @IsOptional() @IsString() portadaUrl?: string;
  @IsOptional() @IsString() @MaxLength(60) genero?: string;
}
