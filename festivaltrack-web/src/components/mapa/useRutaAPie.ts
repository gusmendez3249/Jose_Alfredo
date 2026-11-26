import { useCallback, useEffect, useState } from 'react';
import {
  calcularRutaAPie,
  rutaEnLinea,
  type Coordenada,
  type Ruta,
} from '../../services/rutas.service';
import { geolocalizacionDisponible, mensajeGeolocalizacion, OPCIONES_GPS } from './geolocalizacion';

export type EstadoUbicacion =
  | { estado: 'inactiva' }
  | { estado: 'obteniendo' }
  | { estado: 'lista'; coords: Coordenada }
  | { estado: 'error'; mensaje: string };

export type EstadoRuta =
  | { estado: 'inactiva' }
  | { estado: 'calculando' }
  /** `enLinea`: el servicio de rutas falló y se muestra el trazo recto. */
  | { estado: 'lista'; ruta: Ruta; enLinea: boolean };

type ResultadoGps = Exclude<EstadoUbicacion, { estado: 'inactiva' | 'obteniendo' }>;

/**
 * Ubicación del usuario + ruta a pie hacia `destino`. No pide nada hasta que se llama a `solicitar()`;
 * a partir de ahí, cambiar de destino recalcula la ruta (cancelando la petición anterior) sin volver
 * a pedir permiso. Los resultados guardan para qué intento/origen/destino fueron, de modo que el
 * estado "obteniendo" / "calculando" se deriva en el render sin setState síncrono en efectos.
 */
export function useRutaAPie(destino: Coordenada | null) {
  const [activa, setActiva] = useState(false);
  const [intento, setIntento] = useState(0);
  const [gps, setGps] = useState<{ intento: number; valor: ResultadoGps } | null>(null);
  const [calculo, setCalculo] = useState<{
    origen: Coordenada;
    destino: Coordenada;
    valor: Extract<EstadoRuta, { estado: 'lista' }>;
  } | null>(null);

  useEffect(() => {
    if (!activa || !geolocalizacionDisponible()) return;
    let vigente = true;
    navigator.geolocation.getCurrentPosition(
      (posicion) => {
        if (vigente) {
          setGps({ intento, valor: { estado: 'lista', coords: [posicion.coords.latitude, posicion.coords.longitude] } });
        }
      },
      (error) => {
        if (vigente) setGps({ intento, valor: { estado: 'error', mensaje: mensajeGeolocalizacion(error) } });
      },
      OPCIONES_GPS,
    );
    return () => {
      vigente = false;
    };
  }, [activa, intento]);

  const ubicacion: EstadoUbicacion = !activa
    ? { estado: 'inactiva' }
    : !geolocalizacionDisponible()
      ? { estado: 'error', mensaje: 'Tu navegador no permite obtener la ubicación.' }
      : gps?.intento === intento
        ? gps.valor
        : { estado: 'obteniendo' };
  const origen = ubicacion.estado === 'lista' ? ubicacion.coords : null;

  useEffect(() => {
    if (!origen || !destino) return;
    const control = new AbortController();
    calcularRutaAPie(origen, destino, control.signal)
      .then((ruta) => setCalculo({ origen, destino, valor: { estado: 'lista', ruta, enLinea: false } }))
      .catch(() => {
        if (control.signal.aborted) return;
        setCalculo({ origen, destino, valor: { estado: 'lista', ruta: rutaEnLinea(origen, destino), enLinea: true } });
      });
    return () => control.abort();
  }, [origen, destino]);

  const ruta: EstadoRuta =
    !origen || !destino
      ? { estado: 'inactiva' }
      : calculo?.origen === origen && calculo.destino === destino
        ? calculo.valor
        : { estado: 'calculando' };

  /** Pide la ubicación (o la vuelve a pedir tras un error) y traza la ruta. */
  const solicitar = useCallback(() => {
    setActiva(true);
    setIntento((n) => n + 1);
  }, []);
  const quitar = useCallback(() => setActiva(false), []);

  return { activa, ubicacion, origen, ruta, solicitar, quitar };
}
