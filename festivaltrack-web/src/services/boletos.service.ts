import { http } from '../lib/http';
import { RespuestaInvalidaError, type Boleto, type BoletoConEvento, type CompraBoleto } from './tipos';
import { esBoleto, esBoletoConEvento, normalizarEvento } from './validacion';

export const boletosService = {
  /**
   * Compra UN registro de boleto. Ojo: con `cantidad > 1` el backend crea un solo boleto
   * (un solo QR) con precio × cantidad, por eso la web siempre manda `cantidad: 1`.
   * El monto lo calcula el servidor; no se envía precio ni datos de tarjeta.
   */
  async comprar(compra: CompraBoleto): Promise<Boleto> {
    const { data } = await http.post<unknown>('/boletos/comprar', compra);
    if (!esBoleto(data)) throw new RespuestaInvalidaError('compra');
    return data;
  },

  async misBoletos(signal?: AbortSignal): Promise<BoletoConEvento[]> {
    const { data } = await http.get<unknown>('/boletos/mis-boletos', { signal });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('boletos');
    return data.filter(esBoletoConEvento).map((b) => ({
      ...b,
      evento: b.evento ? normalizarEvento(b.evento) : null,
    }));
  },
};
