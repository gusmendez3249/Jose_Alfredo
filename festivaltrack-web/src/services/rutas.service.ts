import { esObjeto } from './tipos';

/**
 * Router OSRM con perfil a pie (servidores de FOSSGIS que usa openstreetmap.org).
 * Si se cambia, actualizar también `connect-src` en public/serve.json.
 */
export const ORIGEN_ROUTER = 'https://routing.openstreetmap.de';
const TIEMPO_LIMITE_MS = 15_000;

/** [latitud, longitud] */
export type Coordenada = [number, number];

export interface Ruta {
  /** Puntos de la ruta en orden, como [lat, lon] para Leaflet. */
  puntos: Coordenada[];
  distanciaMetros: number;
  duracionSegundos: number;
}

export class RutaNoDisponibleError extends Error {
  constructor(motivo: string) {
    super(motivo);
    this.name = 'RutaNoDisponibleError';
  }
}

const esNumero = (v: unknown): v is number => typeof v === 'number' && Number.isFinite(v);

/** Valida la respuesta de OSRM: { code: 'Ok', routes: [{ distance, duration, geometry: LineString }] }. */
function leerRuta(datos: unknown): Ruta {
  if (!esObjeto(datos) || datos.code !== 'Ok' || !Array.isArray(datos.routes) || datos.routes.length === 0) {
    throw new RutaNoDisponibleError('El servicio de rutas no encontró un trayecto.');
  }
  const ruta: unknown = datos.routes[0];
  if (!esObjeto(ruta) || !esNumero(ruta.distance) || !esNumero(ruta.duration) || !esObjeto(ruta.geometry)) {
    throw new RutaNoDisponibleError('Respuesta inesperada del servicio de rutas.');
  }
  const { type, coordinates } = ruta.geometry;
  if (type !== 'LineString' || !Array.isArray(coordinates)) {
    throw new RutaNoDisponibleError('Respuesta inesperada del servicio de rutas.');
  }
  const puntos: Coordenada[] = [];
  for (const par of coordinates) {
    if (!Array.isArray(par) || !esNumero(par[0]) || !esNumero(par[1])) {
      throw new RutaNoDisponibleError('Respuesta inesperada del servicio de rutas.');
    }
    // GeoJSON viene como [lon, lat]; Leaflet usa [lat, lon].
    puntos.push([par[1], par[0]]);
  }
  if (puntos.length < 2) throw new RutaNoDisponibleError('El servicio de rutas no encontró un trayecto.');
  return { puntos, distanciaMetros: ruta.distance, duracionSegundos: ruta.duration };
}

/** Calcula la ruta a pie. Las coordenadas del usuario salen del navegador solo hacia este servicio. */
export async function calcularRutaAPie(origen: Coordenada, destino: Coordenada, signal: AbortSignal): Promise<Ruta> {
  const tramo = `${origen[1]},${origen[0]};${destino[1]},${destino[0]}`;
  const url = `${ORIGEN_ROUTER}/routed-foot/route/v1/foot/${tramo}?overview=full&geometries=geojson`;

  // Se combina la cancelación del componente con un tiempo límite propio (sin AbortSignal.any,
  // que no existe en navegadores de antes de 2024).
  const control = new AbortController();
  let agotado = false;
  const temporizador = setTimeout(() => {
    agotado = true;
    control.abort();
  }, TIEMPO_LIMITE_MS);
  const alCancelar = () => control.abort();
  signal.addEventListener('abort', alCancelar, { once: true });

  try {
    const respuesta = await fetch(url, { signal: control.signal, referrerPolicy: 'no-referrer', credentials: 'omit' });
    if (!respuesta.ok) {
      // OSRM responde 400 con code "NoRoute" cuando no hay camino entre los puntos.
      throw new RutaNoDisponibleError(
        respuesta.status === 429 ? 'El servicio de rutas está saturado. Inténtalo en un momento.' : 'No se pudo calcular la ruta a pie.',
      );
    }
    return leerRuta(await respuesta.json());
  } catch (error) {
    if (agotado) throw new RutaNoDisponibleError('El servicio de rutas tardó demasiado en responder.');
    if (error instanceof RutaNoDisponibleError || signal.aborted) throw error;
    // Red caída, CORS o JSON inválido.
    throw new RutaNoDisponibleError('No se pudo contactar con el servicio de rutas.');
  } finally {
    clearTimeout(temporizador);
    signal.removeEventListener('abort', alCancelar);
  }
}
