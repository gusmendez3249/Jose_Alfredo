/**
 * Coordenadas oficiales de los lugares del festival en Dolores Hidalgo, Gto.
 *
 * Fuente: OpenStreetMap (el mismo mapa que muestra la app), consultado el 25/11/2026.
 * Las usan los seeds de sitios AR y de cartelera, y `corregir-ubicaciones.ts` para
 * arreglar los registros que ya están en la base. Si se cambia una, hay que cambiarla
 * también en `FormularioEvento.LUGARES` de la app (lista del formulario de conciertos).
 */
export const UBICACIONES = {
  /** Centro del Jardín Principal, donde está el Monumento a Miguel Hidalgo. */
  jardinPrincipal: { latitud: 21.157511, longitud: -100.934529 },
  /** Parroquia de Nuestra Señora de los Dolores (lado norte del Jardín). */
  parroquia: { latitud: 21.158601, longitud: -100.933947 },
  /**
   * Escenario de los conciertos grandes: en la plaza, frente al atrio de la Parroquia.
   * OpenStreetMap no marca escenarios temporales; es el punto del Jardín frente a la Parroquia.
   */
  escenarioPrincipal: { latitud: 21.1583, longitud: -100.9341 },
  /** Mausoleo de José Alfredo Jiménez, dentro del Panteón Municipal. */
  mausoleo: { latitud: 21.156687, longitud: -100.948773 },
  /** Casa Museo José Alfredo Jiménez, Calle Guanajuato 13. */
  casaMuseo: { latitud: 21.157341, longitud: -100.93277 },
} as const;

/** Escenario de un evento → coordenadas oficiales. */
export const UBICACION_POR_ESCENARIO: Record<
  string,
  { latitud: number; longitud: number }
> = {
  'Plaza Principal': UBICACIONES.jardinPrincipal,
  'Escenario Principal': UBICACIONES.escenarioPrincipal,
  'Mausoleo de José Alfredo Jiménez': UBICACIONES.mausoleo,
  'Casa Museo': UBICACIONES.casaMuseo,
};

/** Id del sitio AR → coordenadas oficiales. */
export const UBICACION_POR_SITIO: Record<
  string,
  { latitud: number; longitud: number }
> = {
  'SITIO-MAUSOLEO': UBICACIONES.mausoleo,
  'SITIO-PARROQUIA': UBICACIONES.parroquia,
  'SITIO-HIDALGO': UBICACIONES.jardinPrincipal,
  'SITIO-CASA-MUSEO': UBICACIONES.casaMuseo,
};
