import type { TonoBadge } from '../ui/Badge';
import type { Publicacion, TipoPublicacion } from '../../services/foro.service';

export const ETIQUETA_TIPO: Record<TipoPublicacion, string> = {
  EXPERIENCIA: 'Experiencia',
  RECOMENDACION: 'Recomendación',
  FOTO: 'Foto',
};

export const TONO_TIPO: Record<TipoPublicacion, TonoBadge> = {
  EXPERIENCIA: 'dorada',
  RECOMENDACION: 'exito',
  FOTO: 'neutra',
};

/** Texto alternativo de la foto: el título del autor si lo hay (es texto, no HTML). */
export function altDeFoto(p: Publicacion): string {
  return p.titulo ? `Foto: ${p.titulo}` : `Foto publicada por ${p.autorNombre}`;
}
