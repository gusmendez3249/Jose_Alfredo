/** Une clases CSS ignorando los valores vacíos. */
export function clases(...partes: Array<string | false | null | undefined>): string {
  return partes.filter(Boolean).join(' ');
}

export type VarianteBoton = 'primario' | 'secundario' | 'peligro' | 'fantasma';
export type TamanoBoton = 'normal' | 'grande' | 'chico';

export interface OpcionesBoton {
  variante?: VarianteBoton;
  tamano?: TamanoBoton;
  bloque?: boolean;
  pildora?: boolean;
  /** MAYÚSCULAS, para los llamados a la acción principales. */
  mayusculas?: boolean;
}

/** Clases de un botón; sirve igual para `<button>` y para `<Link>`. */
export function claseBoton({
  variante = 'primario',
  tamano = 'normal',
  bloque = false,
  pildora = false,
  mayusculas = false,
}: OpcionesBoton = {}): string {
  return clases(
    'btn',
    `btn--${variante}`,
    tamano === 'grande' && 'btn--grande',
    tamano === 'chico' && 'btn--chico',
    bloque && 'btn--bloque',
    pildora && 'btn--pildora',
    mayusculas && 'btn--mayus',
  );
}
