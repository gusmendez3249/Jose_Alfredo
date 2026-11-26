import { useState } from 'react';
import { urlMedia } from '../../lib/media';
import { clases, IconoImagen } from '../ui';

interface ImagenForoProps {
  /** Valor crudo de `imagenUrl` tal como llega de la API. */
  url: string;
  alt: string;
  className?: string;
  /** Si se pasa, la imagen es un botón (abre el visor). */
  onAbrir?: () => void;
}

/**
 * Imagen del foro. El foro admite cualquier https, pero la CSP solo deja cargar el backend y la
 * lista de `urlMedia()`: lo demás ni se intenta y se muestra el marcador. Los archivos subidos
 * pueden haber desaparecido del disco del servidor, así que un fallo de carga degrada igual.
 */
export function ImagenForo({ url, alt, className, onAbrir }: ImagenForoProps) {
  const src = urlMedia(url);
  const [fallida, setFallida] = useState<string | null>(null);

  if (!src || fallida === src) {
    return (
      <div className={clases('comunidad-imagen-marcador', className)} role="img" aria-label="Imagen no disponible">
        <IconoImagen tamano={32} />
        <span aria-hidden="true">Imagen no disponible</span>
      </div>
    );
  }

  const imagen = (
    <img
      className={clases('comunidad-imagen', className)}
      src={src}
      alt={alt}
      loading="lazy"
      decoding="async"
      referrerPolicy="no-referrer"
      onError={() => setFallida(src)}
    />
  );

  if (!onAbrir) return imagen;
  return (
    <button type="button" className="comunidad-imagen-boton" onClick={onAbrir} aria-label={`Ampliar imagen: ${alt}`}>
      {imagen}
    </button>
  );
}
