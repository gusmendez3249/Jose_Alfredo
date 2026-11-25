import {
  IsNumber,
  Max,
  IsString,
  IsDateString,
  IsInt,
  IsOptional,
  IsEnum,
  Min,
  MaxLength,
} from 'class-validator';
import { EstadoEvento } from '@prisma/client';

/** Edición parcial de un evento: los mismos campos que el alta, todos opcionales y validados. */
export class UpdateEventoDto {
  @IsOptional() @IsString() @MaxLength(150) nombre?: string;
  @IsOptional() @IsDateString() fechaHora?: string;
  @IsOptional() @IsString() @MaxLength(200) ubicacion?: string;
  @IsOptional() @IsString() @MaxLength(150) escenario?: string;
  @IsOptional() @IsString() @MaxLength(2000) descripcion?: string;
  @IsOptional() @IsInt() @Min(1) capacidad?: number;
  @IsOptional() @IsString() bannerUrl?: string;
  @IsOptional() @IsEnum(EstadoEvento) estado?: EstadoEvento;
  @IsOptional() @IsString() artistaId?: string;

  /** Coordenadas del escenario: el mapa y el modo automóvil las usan para la ruta. */
  @IsOptional() @IsNumber() @Min(-90) @Max(90) latitud?: number;
  @IsOptional() @IsNumber() @Min(-180) @Max(180) longitud?: number;
}
