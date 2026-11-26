import { useCallback, useEffect, useRef, useState } from 'react';
import { esCancelacion } from '../../lib/errores';

interface OpcionesSondeo {
  /** Espera entre el final de una consulta y el inicio de la siguiente. */
  intervaloMs: number;
  /** Tope de la espera tras errores consecutivos (retroceso exponencial). */
  maxEsperaMs?: number;
}

/** Espera antes del siguiente intento: el intervalo normal, duplicado por cada fallo seguido, con tope. */
export function esperaSondeo(intervaloMs: number, fallos: number, maxEsperaMs: number): number {
  if (fallos <= 0) return intervaloMs;
  return Math.min(intervaloMs * 2 ** fallos, Math.max(maxEsperaMs, intervaloMs));
}

/**
 * Ejecuta `tarea` ahora y luego repetidamente, de forma amable con el backend:
 * - nunca hay dos consultas a la vez (la siguiente se agenda al terminar la anterior);
 * - con la pestaña oculta no consulta, y al volver consulta de inmediato;
 * - si `tarea` lanza (incluido un 429) la espera se duplica hasta un tope;
 * - al desmontar cancela la consulta en vuelo y los temporizadores.
 * `tarea` debe lanzar ante un fallo; las cancelaciones no cuentan como fallo.
 */
export function useSondeo(tarea: (signal: AbortSignal) => Promise<void>, { intervaloMs, maxEsperaMs = 60_000 }: OpcionesSondeo) {
  const tareaRef = useRef(tarea);
  useEffect(() => {
    tareaRef.current = tarea;
  });

  const [fallos, setFallos] = useState(0);
  const refrescarRef = useRef<() => void>(() => undefined);

  useEffect(() => {
    let cancelado = false;
    let enCurso = false;
    let seguidos = 0;
    let temporizador: ReturnType<typeof setTimeout> | undefined;
    const control = new AbortController();

    const agendar = () => {
      clearTimeout(temporizador);
      // Oculta: no se agenda nada; `alCambiarVisibilidad` reanuda al volver.
      if (cancelado || document.hidden) return;
      temporizador = setTimeout(() => void ejecutar(), esperaSondeo(intervaloMs, seguidos, maxEsperaMs));
    };

    const ejecutar = async () => {
      if (cancelado || enCurso || document.hidden) return;
      clearTimeout(temporizador);
      enCurso = true;
      try {
        await tareaRef.current(control.signal);
        seguidos = 0;
      } catch (error) {
        if (!esCancelacion(error)) seguidos += 1;
      }
      enCurso = false;
      if (cancelado) return;
      setFallos(seguidos);
      agendar();
    };

    const alCambiarVisibilidad = () => {
      if (document.hidden) clearTimeout(temporizador);
      else void ejecutar();
    };

    refrescarRef.current = () => {
      seguidos = 0;
      void ejecutar();
    };
    document.addEventListener('visibilitychange', alCambiarVisibilidad);
    void ejecutar();

    return () => {
      cancelado = true;
      clearTimeout(temporizador);
      control.abort();
      document.removeEventListener('visibilitychange', alCambiarVisibilidad);
    };
  }, [intervaloMs, maxEsperaMs]);

  /** Consulta ya (p. ej. desde un botón "Reintentar") y reinicia el retroceso. */
  const refrescar = useCallback(() => refrescarRef.current(), []);
  return { fallos, refrescar };
}
