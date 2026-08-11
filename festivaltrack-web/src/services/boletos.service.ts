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
    const response = await api.post('/boletos/comprar', { eventoId, categoria, cantidad });
    return response.data;
  },

  getMisBoletos: async (): Promise<BoletoDto[]> => {
    const response = await api.get('/boletos/mis-boletos');
    return response.data;
  }
};
