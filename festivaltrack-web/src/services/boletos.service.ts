import { api } from './api';

export interface BoletoDto {
  id: string;
  eventoId: string;
  usuarioId: string;
  categoria: string;
  precio: number;
  codigoQR: string;
  estado: string;
  evento?: any;
}

export const boletosService = {
  comprarBoleto: async (eventoId: string, categoria: string, cantidad: number) => {
    const precioTotal = 4500 * cantidad;
    const response = await api.post('/boletos/comprar', {
      eventoId,
      categoria,
      cantidad,
      precioTotal,
      metodoPago: 'TARJETA_CREDITO'
    });
    return response.data;
  },

  getMisBoletos: async (): Promise<BoletoDto[]> => {
    const response = await api.get('/boletos/mis-boletos');
    return response.data;
  }
};
