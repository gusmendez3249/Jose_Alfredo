import type { CSSProperties } from 'react';
import { clases } from './clases';

interface SkeletonProps {
  /** Alto y ancho son valores dinámicos por bloque, por eso van como estilo. */
  alto?: number | string;
  ancho?: number | string;
  redondo?: boolean;
  className?: string;
}

/** Bloque gris con brillo mientras carga el contenido (decorativo). */
export function Skeleton({ alto = 16, ancho = '100%', redondo = false, className }: SkeletonProps) {
  const estilo: CSSProperties = { height: alto, width: ancho, borderRadius: redondo ? '50%' : undefined };
  return <span className={clases('skeleton', className)} style={estilo} aria-hidden="true" />;
}
