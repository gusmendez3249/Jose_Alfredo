import { useEffect, useState } from 'react';
import { cancionesService } from '../../services/canciones.service';

/**
 * Cuántas demos tiene cada concierto, para la insignia de la cartelera. El móvil pide el repertorio
 * de todos los eventos a la vez; aquí van como mucho MAX_SIMULTANEAS a la vez (el backend limita a
 * 300 peticiones por minuto por IP) y el resultado se recuerda unos minutos para no repetirlas al
 * volver a la cartelera.
 */

const MAX_SIMULTANEAS = 2;
const VIGENCIA_MS = 5 * 60 * 1000;

const memoria = new Map<string, { cantidad: number; en: number }>();
let activas = 0;
const pendientes: Array<() => void> = [];

function liberar(): void {
  activas -= 1;
  pendientes.shift()?.();
}

/** Espera turno (respetando la cancelación) y ejecuta la tarea. */
function conTurno<T>(tarea: () => Promise<T>, signal: AbortSignal): Promise<T> {
  return new Promise<T>((resolver, rechazar) => {
    const ejecutar = () => {
      if (signal.aborted) {
        // Se canceló mientras esperaba: cede el turno al siguiente sin pedir nada.
        activas += 1;
        liberar();
        rechazar(new DOMException('Cancelada', 'AbortError'));
        return;
      }
      activas += 1;
      tarea().then(resolver, rechazar).finally(liberar);
    };
    if (activas < MAX_SIMULTANEAS) ejecutar();
    else pendientes.push(ejecutar);
  });
}

function enMemoria(eventoId: string): number | null {
  const guardado = memoria.get(eventoId);
  if (!guardado || Date.now() - guardado.en > VIGENCIA_MS) return null;
  return guardado.cantidad;
}

/** Solo para pruebas. */
export function olvidarDemosEnMemoria(): void {
  memoria.clear();
}

/** Número de demos publicadas del concierto; null mientras carga o si la consulta falló. */
export function useCantidadDemos(eventoId: string): number | null {
  const [resultado, setResultado] = useState<{ id: string; cantidad: number } | null>(null);
  const recordado = enMemoria(eventoId);

  useEffect(() => {
    if (enMemoria(eventoId) !== null) return;
    const control = new AbortController();
    conTurno(() => cancionesService.deEvento(eventoId, control.signal), control.signal)
      .then((canciones) => {
        memoria.set(eventoId, { cantidad: canciones.length, en: Date.now() });
        setResultado({ id: eventoId, cantidad: canciones.length });
      })
      .catch(() => {
        // Un fallo (o la cancelación) solo deja la tarjeta sin insignia.
      });
    return () => control.abort();
  }, [eventoId]);

  if (recordado !== null) return recordado;
  return resultado?.id === eventoId ? resultado.cantidad : null;
}
