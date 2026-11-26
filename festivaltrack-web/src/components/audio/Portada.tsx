import { useState, type SVGProps } from 'react';
import { IconoMusica } from '../ui/Icons';
import { clases } from '../ui/clases';

/** Portada de una canción; si falta o no carga (archivo borrado del disco efímero) se ve un marcador. */
export function Portada({ src, alt = '', className }: { src: string | null; alt?: string; className?: string }) {
  const [fallida, setFallida] = useState<string | null>(null);
  if (!src || fallida === src) {
    return (
      <span className={clases('reproductor-portada', 'reproductor-portada--vacia', className)} aria-hidden="true">
        <IconoMusica tamano={20} />
      </span>
    );
  }
  return (
    <img
      className={clases('reproductor-portada', className)}
      src={src}
      alt={alt}
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setFallida(src)}
    />
  );
}

/** Ecualizador decorativo de tres barras, solo CSS (se detiene con `prefers-reduced-motion`). */
export function Ecualizador({ activo }: { activo: boolean }) {
  return (
    <span className={clases('reproductor-eq', activo && 'reproductor-eq--activo')} aria-hidden="true">
      <span />
      <span />
      <span />
    </span>
  );
}

/** Icono de orden aleatorio (no está en el juego común). */
export function IconoAleatorio({ tamano = 24, ...resto }: Omit<SVGProps<SVGSVGElement>, 'children'> & { tamano?: number }) {
  return (
    <svg
      className="icono"
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...resto}
    >
      <path d="M16 3h5v5M4 20 21 3M21 16v5h-5M15 15l6 6M4 4l5 5" />
    </svg>
  );
}
