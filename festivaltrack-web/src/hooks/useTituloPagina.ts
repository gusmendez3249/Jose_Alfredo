import { useEffect } from 'react';

const TITULO_BASE = 'FestivalTrack';

/** Pone "<titulo> | FestivalTrack" en la pestaña; sin título deja solo "FestivalTrack". */
export function useTituloPagina(titulo?: string): void {
  useEffect(() => {
    document.title = titulo ? `${titulo} | ${TITULO_BASE}` : TITULO_BASE;
  }, [titulo]);
}
