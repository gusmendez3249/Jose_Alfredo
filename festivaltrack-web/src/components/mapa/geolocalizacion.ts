/** Opciones y mensajes de la geolocalización del navegador, compartidos por los dos mapas. */
export const OPCIONES_GPS: PositionOptions = { timeout: 10_000, maximumAge: 60_000, enableHighAccuracy: false };

export const geolocalizacionDisponible = (): boolean => typeof navigator !== 'undefined' && 'geolocation' in navigator;

export function mensajeGeolocalizacion(error: GeolocationPositionError): string {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return 'No se permitió el acceso a tu ubicación. Actívalo en tu navegador para ver la ruta.';
    case error.TIMEOUT:
      return 'Se agotó el tiempo para obtener tu ubicación.';
    default:
      return 'No se pudo determinar tu ubicación.';
  }
}
