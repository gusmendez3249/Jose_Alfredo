import { IsBoolean, IsNumber, IsOptional, IsString } from 'class-validator';

/** Alta / edición de un sitio del catálogo AR (solo administradores). */
export class SitioArDto {
  @IsString() nombre: string;
  @IsString() ubicacion: string;
  @IsString() descripcion: string;
  /** Descripción de cómo luce el sitio; la usa el reconocimiento visual. */
  @IsString() pistasVisuales: string;
  @IsOptional() @IsString() imagenUrl?: string;
  @IsOptional() @IsString() audioUrl?: string;
  @IsOptional() @IsString() galeriaId?: string;
  @IsOptional() @IsNumber() latitud?: number;
  @IsOptional() @IsNumber() longitud?: number;
  @IsOptional() @IsBoolean() activo?: boolean;
}
