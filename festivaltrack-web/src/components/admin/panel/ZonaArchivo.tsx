import { useId, useState, type ChangeEvent, type DragEvent, type ReactNode } from 'react';
import { clases } from '../../ui/clases';
import '../../../styles/admin-panel.css';

interface Props {
  etiqueta: string;
  ayuda: string;
  accept: string;
  /** Texto de la acción cuando no hay archivo. */
  invitacion: string;
  disabled?: boolean;
  error?: string | null;
  onArchivo: (archivo: File) => void;
  children?: ReactNode;
}

/**
 * Selector de archivo que también admite arrastrar y soltar. El `<input type="file">` cubre toda
 * la zona (transparente) para seguir siendo operable con teclado y lectores de pantalla.
 */
export function ZonaArchivo({ etiqueta, ayuda, accept, invitacion, disabled, error, onArchivo, children }: Props) {
  const id = useId();
  const [encima, setEncima] = useState(false);

  const alElegir = (e: ChangeEvent<HTMLInputElement>) => {
    const archivo = e.target.files?.[0];
    if (archivo) onArchivo(archivo);
    // Permite volver a elegir el mismo archivo después de quitarlo o de un error.
    e.target.value = '';
  };

  const alSoltar = (e: DragEvent<HTMLElement>) => {
    e.preventDefault();
    setEncima(false);
    if (disabled) return;
    const archivo = e.dataTransfer.files[0];
    if (archivo) onArchivo(archivo);
  };

  return (
    <div className="admin-panel-campo">
      <label className="campo__etiqueta" htmlFor={id}>
        {etiqueta}
      </label>
      <div
        className={clases('admin-panel-zona', encima && 'admin-panel-zona--encima', disabled && 'admin-panel-zona--inactiva')}
        onDragOver={(e) => {
          e.preventDefault();
          if (!disabled) setEncima(true);
        }}
        onDragLeave={() => setEncima(false)}
        onDrop={alSoltar}
      >
        <input
          id={id}
          type="file"
          className="admin-panel-zona__entrada"
          accept={accept}
          disabled={disabled}
          onChange={alElegir}
          aria-describedby={`${id}-ayuda`}
          aria-invalid={error ? true : undefined}
        />
        <span className="admin-panel-zona__texto">{invitacion}</span>
        {children}
      </div>
      <p id={`${id}-ayuda`} className="campo__ayuda">
        {ayuda}
      </p>
      {error && (
        <p className="campo__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
