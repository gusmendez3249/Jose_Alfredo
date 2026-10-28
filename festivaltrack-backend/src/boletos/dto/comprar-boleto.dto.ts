import { IsEnum, IsInt, IsNotEmpty, IsOptional, IsString, Max, Min } from 'class-validator';
import { CategoriaTicket, MetodoPago } from '@prisma/client';

/**
 * Cuerpo de `POST /boletos/comprar`.
 * El precio NO se acepta del cliente: lo calcula el servidor con {@link PRECIOS_BOLETO}.
 */
export class ComprarBoletoDto {
  @IsString() @IsNotEmpty() eventoId: string;
  @IsOptional() @IsEnum(CategoriaTicket) categoria?: CategoriaTicket;
  @IsOptional() @IsInt() @Min(1) @Max(10) cantidad?: number;
  @IsOptional() @IsEnum(MetodoPago) metodoPago?: MetodoPago;
}

/** Precio oficial por boleto en MXN (debe coincidir con lo que muestran las apps). */
export const PRECIOS_BOLETO: Record<CategoriaTicket, number> = {
  VIP: 4500,
  GENERAL: 1200,
  ESTUDIANTE: 600,
};
