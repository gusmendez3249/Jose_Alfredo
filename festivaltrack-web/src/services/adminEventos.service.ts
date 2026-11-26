import { http } from '../lib/http';
import { RespuestaInvalidaError, type Evento } from './tipos';
import { esEvento, normalizarEvento } from './validacion';

/** Cuerpo del alta (POST /eventos). `fechaHora` va en UTC (ISO). */
export interface NuevoEvento {
  nombre: string;
  fechaHora: string;
  ubicacion: string;
  escenario: string;
  latitud: number;
  longitud: number;
  capacidad: number;
  descripcion?: string;
  estado: 'PUBLICADO';
}

/** Cuerpo de la edición (PATCH /eventos/:id): solo los campos que cambiaron. */
export type CambiosEvento = Partial<
  Pick<Evento, 'nombre' | 'fechaHora' | 'ubicacion' | 'escenario' | 'latitud' | 'longitud' | 'capacidad' | 'descripcion'>
>;

function leerEvento(data: unknown): Evento {
  if (!esEvento(data)) throw new RespuestaInvalidaError('evento');
  return normalizarEvento(data);
}

/** Escrituras de conciertos. Todas exigen rol ADMINISTRADOR (lo valida el backend). Para leer usa `eventosService`. */
export const adminEventosService = {
  async crear(datos: NuevoEvento): Promise<Evento> {
    const { data } = await http.post<unknown>('/eventos', datos);
    return leerEvento(data);
  },

  async actualizar(id: string, cambios: CambiosEvento): Promise<Evento> {
    const { data } = await http.patch<unknown>(`/eventos/${encodeURIComponent(id)}`, cambios);
    return leerEvento(data);
  },

  /** No borra: el backend deja el evento en estado CANCELADO. */
  async cancelar(id: string): Promise<Evento> {
    const { data } = await http.delete<unknown>(`/eventos/${encodeURIComponent(id)}`);
    return leerEvento(data);
  },
};
