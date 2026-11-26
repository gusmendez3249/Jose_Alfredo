import type { CategoriaBoleto } from '../services/tipos';

/**
 * Categorías que se venden desde la web, igual que en la app móvil.
 * ESTUDIANTE existe en el backend ($600) pero ninguna app cliente la ofrece.
 */
export const CATEGORIAS_A_LA_VENTA = ['VIP', 'GENERAL'] as const satisfies readonly CategoriaBoleto[];
export type CategoriaALaVenta = (typeof CATEGORIAS_A_LA_VENTA)[number];

/**
 * Precio oficial por boleto en MXN. DEBE coincidir con `PRECIOS_BOLETO` de
 * festivaltrack-backend/src/boletos/dto/comprar-boleto.dto.ts: el backend cobra con su
 * propia tabla, así que aquí solo se usa para mostrar el resumen antes de pagar.
 */
export const PRECIOS_BOLETO: Record<CategoriaALaVenta, number> = {
  VIP: 4500,
  GENERAL: 1200,
};

/** Máximo por categoría en una compra (el backend acepta 1–10 por petición). */
export const MAX_BOLETOS_POR_CATEGORIA = 10;
