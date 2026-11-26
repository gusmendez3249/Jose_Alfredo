import axios from 'axios';
import { useCallback, useEffect, useRef, useState } from 'react';
import { esCancelacion, mensajeDeError, type MensajesPorEstado } from '../lib/errores';

export type EstadoRecurso<T> =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string; status: number | null }
  | { estado: 'listo'; datos: T };

type Terminado<T> = Exclude<EstadoRecurso<T>, { estado: 'cargando' }>;

/**
 * Carga un recurso al montar (y cuando cambia `clave`), cancela la petición al desmontar
 * y expone `reintentar()` para el botón del estado de error.
 */
export function useRecurso<T>(
  cargar: (signal: AbortSignal) => Promise<T>,
  clave: string,
  mensajes?: MensajesPorEstado,
): EstadoRecurso<T> & { reintentar: () => void } {
  const [intento, setIntento] = useState(0);
  // Cada resultado recuerda para qué petición fue: si no coincide con la actual, está cargando.
  const [resultado, setResultado] = useState<{ peticion: string; valor: Terminado<T> } | null>(null);
  const peticion = `${intento}:${clave}`;

  // `cargar` y `mensajes` suelen ser funciones/objetos nuevos en cada render: se guardan en
  // refs para que solo `clave` e `intento` disparen una nueva carga.
  const cargarRef = useRef(cargar);
  const mensajesRef = useRef(mensajes);
  useEffect(() => {
    cargarRef.current = cargar;
    mensajesRef.current = mensajes;
  });

  useEffect(() => {
    const control = new AbortController();
    cargarRef
      .current(control.signal)
      .then((datos) => setResultado({ peticion, valor: { estado: 'listo', datos } }))
      .catch((error: unknown) => {
        if (esCancelacion(error)) return;
        const status = axios.isAxiosError(error) ? (error.response?.status ?? null) : null;
        setResultado({ peticion, valor: { estado: 'error', mensaje: mensajeDeError(error, mensajesRef.current), status } });
      });
    return () => control.abort();
  }, [peticion]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);
  const actual: EstadoRecurso<T> = resultado?.peticion === peticion ? resultado.valor : { estado: 'cargando' };
  return { ...actual, reintentar };
}
