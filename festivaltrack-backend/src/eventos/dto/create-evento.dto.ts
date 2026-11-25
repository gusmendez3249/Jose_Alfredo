import {
  IsNumber,
  Max,
  Min,
  IsString,
  IsDateString,
  IsInt,
  IsOptional,
  IsEnum,
} from 'class-validator';
import { EstadoEvento } from '@prisma/client';

export class CreateEventoDto {
  @IsString() nombre: string;
  @IsDateString() fechaHora: string;
  @IsString() ubicacion: string;
  @IsOptional() @IsString() escenario?: string;
  @IsOptional() @IsString() descripcion?: string;
  @IsInt() capacidad: number;
  @IsOptional() @IsString() bannerUrl?: string;
  @IsOptional() @IsEnum(EstadoEvento) estado?: EstadoEvento;
  @IsOptional() @IsString() artistaId?: string;

  /** Coordenadas del escenario: el mapa y el modo automóvil las usan para la ruta. */
  @IsOptional() @IsNumber() @Min(-90) @Max(90) latitud?: number;
  @IsOptional() @IsNumber() @Min(-180) @Max(180) longitud?: number;
}
