import type { ReactNode } from 'react';
import { clases } from './clases';

export type TonoBadge = 'dorada' | 'exito' | 'error' | 'neutra';

/** Etiqueta de estado. El color nunca va solo: siempre lleva texto. */
export function Badge({ tono = 'neutra', children }: { tono?: TonoBadge; children: ReactNode }) {
  return <span className={clases('insignia', `insignia--${tono}`)}>{children}</span>;
}
