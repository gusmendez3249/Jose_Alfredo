import { mensajeDeError } from '../../../lib/errores';
import { cancionesService, type Cancion, type NuevaCancion } from '../../../services/canciones.service';

export type Etapa = 'audio' | 'portada' | 'guardar';

export interface Progreso {
  etapa: Etapa;
  /** 0 a 100. */
  porcentaje: number;
}

export const ETIQUETA_ETAPA: Record<Etapa, string> = {
  audio: 'Subiendo el audio',
  portada: 'Subiendo la portada',
  guardar: 'Guardando en el catálogo',
};

/** Rangos de la barra por etapa, iguales a los de la app móvil. */
const RANGO: Record<Etapa, [number, number]> = { audio: [0, 80], portada: [80, 95], guardar: [95, 100] };

export const SESION_TERMINO = 'Tu sesión de administrador terminó. Vuelve a iniciar sesión.';

const PROPIOS = {
  401: SESION_TERMINO,
  403: 'No tienes permisos de administrador para hacer esto.',
  413: 'El archivo es demasiado grande para el servidor.',
};

export class ErrorDeEtapa extends Error {
  readonly etapa: Etapa;
  constructor(etapa: Etapa, mensaje: string) {
    super(mensaje);
    this.name = 'ErrorDeEtapa';
    this.etapa = etapa;
  }
}

export interface EntradaPublicacion {
  titulo: string;
  artista: string;
  duracion: number | null;
  audio: File | null;
  portada: File | null;
}

/** Archivos ya subidos: si una etapa posterior falla, al reintentar no se vuelve a subir lo que ya está. */
export type Subidos = Map<File, string>;

async function subir(archivo: File, etapa: Etapa, subidos: Subidos, alProgreso: (p: Progreso) => void): Promise<string> {
  const previa = subidos.get(archivo);
  if (previa) return previa;
  const [desde, hasta] = RANGO[etapa];
  alProgreso({ etapa, porcentaje: desde });
  try {
    const url = await cancionesService.subirArchivo(archivo, (f) =>
      alProgreso({ etapa, porcentaje: Math.round(desde + (hasta - desde) * f) }),
    );
    subidos.set(archivo, url);
    return url;
  } catch (e) {
    const que = etapa === 'audio' ? 'el audio' : 'la portada';
    throw new ErrorDeEtapa(etapa, `No se pudo subir ${que}. ${mensajeDeError(e, PROPIOS)}`);
  }
}

/** Sube audio y portada (en ese orden) y da de alta la canción. Lanza `ErrorDeEtapa` al fallar. */
export async function publicarCancion(
  entrada: EntradaPublicacion,
  subidos: Subidos,
  alProgreso: (p: Progreso) => void,
): Promise<Cancion> {
  const datos: NuevaCancion = { titulo: entrada.titulo };
  if (entrada.artista) datos.artista = entrada.artista;
  if (entrada.duracion) datos.duracion = entrada.duracion;

  if (entrada.audio) datos.archivoUrl = await subir(entrada.audio, 'audio', subidos, alProgreso);
  if (entrada.portada) datos.portadaUrl = await subir(entrada.portada, 'portada', subidos, alProgreso);

  alProgreso({ etapa: 'guardar', porcentaje: RANGO.guardar[0] });
  try {
    const cancion = await cancionesService.crear(datos);
    alProgreso({ etapa: 'guardar', porcentaje: 100 });
    return cancion;
  } catch (e) {
    throw new ErrorDeEtapa('guardar', `No se pudo guardar la canción en el catálogo. ${mensajeDeError(e, PROPIOS)}`);
  }
}

/** Traduce el 500 del backend al borrar (canción inexistente o presente en un repertorio). */
export function mensajeAlEliminar(error: unknown): string {
  return mensajeDeError(error, {
    ...PROPIOS,
    500: 'No se pudo eliminar: puede estar en el repertorio de un concierto. Quítala primero del repertorio.',
  });
}
