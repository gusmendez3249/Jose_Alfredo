import { useState } from 'react';
import { IconoImagen } from '../ui';

interface ImagenBioProps {
  /** Ya resuelta con `urlMedia()` o ruta propia; null muestra el marcador. */
  src: string | null;
  alt: string;
  /** Imagen de respaldo (propia del sitio) si la principal falla al cargar. */
  respaldo?: string;
  className?: string;
  prioridad?: boolean;
}

/** Imagen con marcador cuando no hay URL segura o el archivo ya no existe en el servidor. */
export function ImagenBio({ src, alt, respaldo, className, prioridad }: ImagenBioProps) {
  const [fallo, setFallo] = useState<'ninguno' | 'principal' | 'todo'>('ninguno');
  const actual = fallo === 'todo' ? null : fallo === 'principal' ? (respaldo ?? null) : (src ?? respaldo ?? null);

  if (!actual) {
    return (
      <div className={`bio-marcador ${className ?? ''}`} role="img" aria-label={alt}>
        <IconoImagen tamano={40} />
      </div>
    );
  }
  return (
    <img
      className={className}
      src={actual}
      alt={alt}
      loading={prioridad ? 'eager' : 'lazy'}
      referrerPolicy="no-referrer"
      decoding="async"
      onError={() => setFallo(fallo === 'ninguno' && respaldo && actual !== respaldo ? 'principal' : 'todo')}
    />
  );
}
