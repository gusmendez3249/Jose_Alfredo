import { http } from '../lib/http';
import { esObjeto, RespuestaInvalidaError } from './tipos';

/** Sitio histórico del catálogo (GET /ar/sitios, público). Solo se usa el catálogo, no el reconocimiento por foto. */
export interface SitioAR {
  id: string;
  nombre: string;
  ubicacion: string;
  descripcion: string;
  imagenUrl: string | null;
  latitud: number | null;
  longitud: number | null;
}

const esTexto = (v: unknown): v is string => typeof v === 'string';
const esNumero = (v: unknown): v is number => typeof v === 'number' && Number.isFinite(v);
const textoONulo = (v: unknown): string | null => (esTexto(v) && v.trim() ? v : null);

function leerSitio(v: unknown): SitioAR | null {
  if (!esObjeto(v) || !esTexto(v.id) || !v.id || !esTexto(v.nombre) || !v.nombre.trim()) return null;
  if (v.activo === false) return null;
  return {
    id: v.id,
    nombre: v.nombre,
    ubicacion: esTexto(v.ubicacion) ? v.ubicacion : '',
    descripcion: esTexto(v.descripcion) ? v.descripcion : '',
    imagenUrl: textoONulo(v.imagenUrl),
    latitud: esNumero(v.latitud) ? v.latitud : null,
    longitud: esNumero(v.longitud) ? v.longitud : null,
  };
}

export const sitiosService = {
  async listar(signal?: AbortSignal): Promise<SitioAR[]> {
    const { data } = await http.get<unknown>('/ar/sitios', { signal });
    if (!Array.isArray(data)) throw new RespuestaInvalidaError('sitios');
    // Un elemento con forma rara se descarta en lugar de romper el mapa.
    return data.map(leerSitio).filter((s): s is SitioAR => s !== null);
  },
};
