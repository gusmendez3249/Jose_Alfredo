import type { ButtonHTMLAttributes } from 'react';
import { Link, type LinkProps } from 'react-router-dom';
import { claseBoton, clases, type OpcionesBoton } from './clases';
import { Spinner } from './Spinner';

interface BotonProps extends ButtonHTMLAttributes<HTMLButtonElement>, OpcionesBoton {
  /** Muestra un spinner, deshabilita el botón y marca `aria-busy`. */
  cargando?: boolean;
}

export function Button({
  variante,
  tamano,
  bloque,
  pildora,
  mayusculas,
  cargando = false,
  disabled,
  className,
  type = 'button',
  children,
  ...resto
}: BotonProps) {
  return (
    <button
      type={type}
      className={clases(claseBoton({ variante, tamano, bloque, pildora, mayusculas }), className)}
      disabled={disabled || cargando}
      aria-busy={cargando || undefined}
      {...resto}
    >
      {cargando && <Spinner />}
      {children}
    </button>
  );
}

/** Enlace de react-router con el aspecto de un botón. */
export function ButtonLink({
  variante,
  tamano,
  bloque,
  pildora,
  mayusculas,
  className,
  ...resto
}: LinkProps & OpcionesBoton) {
  return <Link className={clases(claseBoton({ variante, tamano, bloque, pildora, mayusculas }), className)} {...resto} />;
}
