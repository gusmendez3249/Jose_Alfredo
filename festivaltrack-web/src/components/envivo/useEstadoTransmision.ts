import { useCallback, useState } from 'react';
import { esCancelacion, mensajeDeError } from '../../lib/errores';
import { streamService, type EstadoTransmision } from '../../services/stream.service';
import { useSondeo } from './useSondeo';

/**
 * Estado de la transmisión con sondeo. `estado` es null hasta la primera respuesta; si una
 * consulta posterior falla se conserva el último estado conocido y se anota el error.
 */
export function useEstadoTransmision(intervaloMs: number) {
  const [estado, setEstado] = useState<EstadoTransmision | null>(null);
  const [error, setError] = useState<string | null>(null);

  const consultar = useCallback(async (signal: AbortSignal) => {
    try {
      setEstado(await streamService.estado(signal));
      setError(null);
    } catch (e) {
      if (!esCancelacion(e)) setError(mensajeDeError(e));
      throw e;
    }
  }, []);

  const { fallos, refrescar } = useSondeo(consultar, { intervaloMs });
  return { estado, error, fallos, refrescar };
}
