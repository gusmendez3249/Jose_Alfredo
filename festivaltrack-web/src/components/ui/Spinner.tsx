import { clases } from './clases';

/** Indicador de carga decorativo: el texto que lo acompaña es el que se anuncia. */
export function Spinner({ grande = false }: { grande?: boolean }) {
  return <span className={clases('spinner', grande && 'spinner--grande')} aria-hidden="true" />;
}
