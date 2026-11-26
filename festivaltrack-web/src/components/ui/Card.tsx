import type { HTMLAttributes } from 'react';
import { clases } from './clases';

interface CardProps extends HTMLAttributes<HTMLElement> {
  /** Fondo verde oscuro con borde dorado (banner destacado). */
  destacada?: boolean;
  /** Solo el borde dorado (p. ej. la categoría VIP). */
  dorada?: boolean;
  /** Elemento HTML a usar (por defecto `div`). */
  como?: 'div' | 'article' | 'section' | 'aside';
}

export function Card({ destacada, dorada, como: Etiqueta = 'div', className, ...resto }: CardProps) {
  return (
    <Etiqueta
      className={clases('tarjeta', destacada && 'tarjeta--destacada', dorada && 'tarjeta--dorada', className)}
      {...resto}
    />
  );
}
