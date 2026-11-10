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
  // El monto lo calcula el backend según la categoría; no se envía precio desde el cliente.
  comprarBoleto: async (eventoId: string, categoria: string, cantidad: number) => {
    const response = await api.post('/boletos/comprar', {
      eventoId,
      categoria,
      cantidad,
      metodoPago: 'TARJETA_CREDITO'
    });
    return response.data;
  },

  getMisBoletos: async (): Promise<BoletoDto[]> => {
    const response = await api.get('/boletos/mis-boletos');
    return response.data;
  }
};
