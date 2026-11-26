import { useEffect, useRef } from 'react';
import { Button, IconoCerrar } from '../ui';
import { ImagenForo } from './ImagenForo';

interface VisorImagenProps {
  url: string | null;
  alt: string;
  onCerrar: () => void;
}

/** Foto a tamaño completo en un `<dialog>` nativo (Escape y clic fuera cierran). */
export function VisorImagen({ url, alt, onCerrar }: VisorImagenProps) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (url && !dialogo.open) dialogo.showModal();
    if (!url && dialogo.open) dialogo.close();
  }, [url]);

  return (
    <dialog
      ref={ref}
      className="comunidad-visor"
      aria-label="Imagen ampliada"
      onCancel={(e) => {
        e.preventDefault();
        onCerrar();
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onCerrar();
      }}
    >
      {url && (
        <div className="comunidad-visor__contenido">
          <Button variante="fantasma" className="comunidad-visor__cerrar" onClick={onCerrar} aria-label="Cerrar imagen" autoFocus>
            <IconoCerrar />
          </Button>
          <ImagenForo url={url} alt={alt} className="comunidad-visor__imagen" />
        </div>
      )}
    </dialog>
  );
}
