import { proximos } from '../../lib/proximos';
import { estaALaVenta, lugarDe, tieneCoordenadas } from '../../services/eventos.service';
import type { Coordenada } from '../../services/rutas.service';
import type { SitioAR } from '../../services/sitios.service';
import type { Evento } from '../../services/tipos';

export type CategoriaDestino = 'escenario' | 'historico' | 'concierto';

export const CATEGORIAS: readonly CategoriaDestino[] = ['escenario', 'concierto', 'historico'];

export const ETIQUETA_CATEGORIA: Record<CategoriaDestino, string> = {
  escenario: 'Escenarios',
  concierto: 'Conciertos',
  historico: 'Sitios históricos',
};

/** Singular, para la insignia de la ficha. */
export const ETIQUETA_CATEGORIA_SINGULAR: Record<CategoriaDestino, string> = {
  escenario: 'Escenario del festival',
  concierto: 'Concierto',
  historico: 'Sitio histórico',
};

export interface Destino {
  /** Identificador estable que se usa en `?destino=`: `lugar-…`, `sitio-<id>` o `evento-<id>`. */
  id: string;
  categoria: CategoriaDestino;
  nombre: string;
  direccion: string;
  descripcion: string | null;
  /** null si el destino no tiene coordenadas: aparece en la lista pero no en el mapa. */
  coords: Coordenada | null;
  imagenUrl: string | null;
  evento: { id: string; fechaHora: string; aLaVenta: boolean } | null;
}

/** Jardín Principal de Dolores Hidalgo (centro de respaldo de la app móvil, DestinosViewModel.kt). */
export const CENTRO_DOLORES: Coordenada = [21.1563, -100.9338];
/** Más lejos que esto del festival, la ruta a pie deja de ser razonable (15 km en el móvil). */
export const RADIO_FESTIVAL_M = 15_000;

function fijo(id: string, nombre: string, direccion: string, coords: Coordenada): Destino {
  return { id, categoria: 'escenario', nombre, direccion, descripcion: null, coords, imagenUrl: null, evento: null };
}

/** Lugares del festival tal como los ofrece el móvil (ui/utils/FormularioEvento.kt, LUGARES, líneas 18-23). */
export const DESTINOS_FIJOS: readonly Destino[] = [
  fijo('lugar-escenario-principal', 'Escenario Principal', 'Jardín Principal, Dolores Hidalgo', [21.1566, -100.9341]),
  fijo('lugar-plaza-principal', 'Plaza Principal', 'Jardín Principal, Dolores Hidalgo', [21.1563, -100.9338]),
  fijo('lugar-mausoleo', 'Mausoleo de José Alfredo Jiménez', 'Panteón Municipal, Dolores Hidalgo', [21.1571, -100.9406]),
  fijo('lugar-casa-museo', 'Casa Museo', 'Calle Guanajuato 13, Centro, Dolores Hidalgo', [21.1574, -100.933]),
];

function coordsDe(latitud: number | null, longitud: number | null): Coordenada | null {
  return latitud != null && longitud != null ? [latitud, longitud] : null;
}

function destinoDeSitio(sitio: SitioAR): Destino {
  return {
    id: `sitio-${sitio.id}`,
    categoria: 'historico',
    nombre: sitio.nombre,
    direccion: sitio.ubicacion,
    descripcion: sitio.descripcion.trim() || null,
    coords: coordsDe(sitio.latitud, sitio.longitud),
    imagenUrl: sitio.imagenUrl,
    evento: null,
  };
}

function destinoDeEvento(evento: Evento): Destino {
  return {
    id: `evento-${evento.id}`,
    categoria: 'concierto',
    nombre: evento.nombre,
    direccion: lugarDe(evento) ?? '',
    descripcion: evento.descripcion?.trim() || null,
    coords: tieneCoordenadas(evento) ? [evento.latitud, evento.longitud] : null,
    imagenUrl: null,
    evento: { id: evento.id, fechaHora: evento.fechaHora, aLaVenta: estaALaVenta(evento) },
  };
}

/** Escenarios fijos, conciertos próximos (por fecha) y sitios históricos (por nombre, como los da la API). */
export function construirDestinos(sitios: readonly SitioAR[], eventos: readonly Evento[], ahora = Date.now()): Destino[] {
  return [
    ...DESTINOS_FIJOS,
    ...proximos([...eventos], ahora).map(destinoDeEvento),
    ...sitios.map(destinoDeSitio),
  ];
}

const FORMA_PARAMETRO = /^[\w.:-]{1,120}$/;

/**
 * Resuelve `?destino=`. Acepta el id del destino (`evento-123`) o el id crudo de un evento o de un
 * sitio (`123`, `SITIO-CASA-MUSEO`) para que otras páginas enlacen sin conocer el prefijo.
 * Cualquier valor con otra forma o que no exista se ignora.
 */
export function destinoDesdeParametro(valor: string | null, destinos: readonly Destino[]): Destino | null {
  if (!valor || !FORMA_PARAMETRO.test(valor)) return null;
  return (
    destinos.find((d) => d.id === valor) ??
    destinos.find((d) => d.id === `evento-${valor}` || d.id === `sitio-${valor}`) ??
    null
  );
}

export function coincideBusqueda(destino: Destino, texto: string): boolean {
  const consulta = texto.trim().toLocaleLowerCase('es');
  if (!consulta) return true;
  return `${destino.nombre} ${destino.direccion}`.toLocaleLowerCase('es').includes(consulta);
}
