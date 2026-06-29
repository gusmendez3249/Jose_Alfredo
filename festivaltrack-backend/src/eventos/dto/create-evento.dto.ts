import { IsString, IsDateString, IsInt, IsOptional, IsEnum } from 'class-validator';
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
}
