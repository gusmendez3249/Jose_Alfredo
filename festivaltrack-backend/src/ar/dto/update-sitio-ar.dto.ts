import { IsBoolean, IsNumber, IsOptional, IsString } from 'class-validator';

/** Edición parcial de un sitio AR (solo administradores): todos los campos opcionales y validados. */
export class UpdateSitioArDto {
  @IsOptional() @IsString() nombre?: string;
  @IsOptional() @IsString() ubicacion?: string;
  @IsOptional() @IsString() descripcion?: string;
  @IsOptional() @IsString() pistasVisuales?: string;
  @IsOptional() @IsString() imagenUrl?: string;
  @IsOptional() @IsString() audioUrl?: string;
  @IsOptional() @IsString() galeriaId?: string;
  @IsOptional() @IsNumber() latitud?: number;
  @IsOptional() @IsNumber() longitud?: number;
  @IsOptional() @IsBoolean() activo?: boolean;
}
