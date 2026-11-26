import { useState } from 'react';
import { IconoMusica } from '../../ui/Icons';
import { urlMedia } from '../../../lib/media';
import '../../../styles/admin-panel.css';

/** Portada de una canción: por `urlMedia` y con marcador si no hay, no es segura o no carga. */
export function Portada({ url, src }: { url?: string | null; src?: string | null }) {
  const [fallo, setFallo] = useState(false);
  // `src` es una vista previa local (data:); `url` es lo que viene de la API.
  const destino = src ?? urlMedia(url);
  if (!destino || fallo) {
    return (
      <span className="admin-panel-portada admin-panel-portada--vacia" aria-hidden="true">
        <IconoMusica tamano={24} />
      </span>
    );
  }
  return (
    <img
      className="admin-panel-portada"
      src={destino}
      alt=""
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setFallo(true)}
    />
  );
}
