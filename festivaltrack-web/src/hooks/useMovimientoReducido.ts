import { useSyncExternalStore } from 'react';

const CONSULTA = '(prefers-reduced-motion: reduce)';

function suscribir(avisar: () => void): () => void {
  const lista = window.matchMedia(CONSULTA);
  lista.addEventListener('change', avisar);
  return () => lista.removeEventListener('change', avisar);
}

const leer = (): boolean => window.matchMedia(CONSULTA).matches;

/** true si la persona pidió menos movimiento: se usa para apagar el autoplay del carrusel. */
export function useMovimientoReducido(): boolean {
  return useSyncExternalStore(suscribir, leer, () => false);
}
