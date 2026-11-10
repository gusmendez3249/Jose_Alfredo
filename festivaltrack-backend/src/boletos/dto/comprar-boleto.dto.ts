import { IsEnum, IsInt, IsNotEmpty, IsOptional, IsString, Max, Min } from 'class-validator';
import { CategoriaTicket, MetodoPago } from '@prisma/client';

/**
 * Cuerpo de `POST /boletos/comprar`.
 * El precio NO se acepta del cliente: lo calcula el servidor con {@link PRECIOS_BOLETO}.
 */
export class ComprarBoletoDto {
  /** Evento para el que se compra; debe existir y estar PUBLICADO o EN_CURSO. */
  @IsString() @IsNotEmpty() eventoId: string;
  /** VIP, GENERAL o ESTUDIANTE (por defecto GENERAL). */
  @IsOptional() @IsEnum(CategoriaTicket) categoria?: CategoriaTicket;
  /** De 1 a 10 boletos por compra: evita cantidades negativas o pedidos masivos. */
  @IsOptional() @IsInt() @Min(1) @Max(10) cantidad?: number;
  /** TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA o PAYPAL (por defecto TARJETA_CREDITO). */
  @IsOptional() @IsEnum(MetodoPago) metodoPago?: MetodoPago;
}
// `precioTotal` ya no forma parte del DTO: si un cliente viejo lo manda,
// ValidationPipe({ whitelist: true }) lo descarta.

/** Precio oficial por boleto en MXN (debe coincidir con lo que muestran las apps). */
export const PRECIOS_BOLETO: Record<CategoriaTicket, number> = {
  VIP: 4500,
  GENERAL: 1200,
  ESTUDIANTE: 600,
};
