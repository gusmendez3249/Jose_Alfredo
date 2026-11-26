import { useId, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { clases } from '../../ui';

interface BaseProps {
  etiqueta: string;
  error?: string;
  ayuda?: string;
}

function Envoltura({
  idCampo,
  etiqueta,
  error,
  ayuda,
  children,
}: BaseProps & { idCampo: string; children: ReactNode }) {
  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={idCampo}>
        {etiqueta}
      </label>
      <div className="campo__control">{children}</div>
      {ayuda && (
        <p id={`${idCampo}-ayuda`} className="campo__ayuda">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={`${idCampo}-error`} className="campo__error">
          {error}
        </p>
      )}
    </div>
  );
}

const describir = (id: string, error?: string, ayuda?: string) =>
  [error && `${id}-error`, ayuda && `${id}-ayuda`].filter(Boolean).join(' ') || undefined;

/** Menú desplegable con el aspecto de `Field` (que solo cubre `<input>`). */
export function CampoSeleccion({
  etiqueta,
  error,
  ayuda,
  className,
  children,
  id: idPropio,
  ...resto
}: BaseProps & SelectHTMLAttributes<HTMLSelectElement>) {
  const generado = useId();
  const id = idPropio ?? generado;
  return (
    <Envoltura idCampo={id} etiqueta={etiqueta} error={error} ayuda={ayuda}>
      <select
        {...resto}
        id={id}
        className={clases('campo__input', 'admin-conciertos-seleccion', className)}
        aria-invalid={error ? true : undefined}
        aria-describedby={describir(id, error, ayuda)}
      >
        {children}
      </select>
    </Envoltura>
  );
}

/** Área de texto con el aspecto de `Field`. */
export function CampoArea({
  etiqueta,
  error,
  ayuda,
  className,
  id: idPropio,
  ...resto
}: BaseProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const generado = useId();
  const id = idPropio ?? generado;
  return (
    <Envoltura idCampo={id} etiqueta={etiqueta} error={error} ayuda={ayuda}>
      <textarea
        {...resto}
        id={id}
        className={clases('campo__input', 'admin-conciertos-area', className)}
        aria-invalid={error ? true : undefined}
        aria-describedby={describir(id, error, ayuda)}
      />
    </Envoltura>
  );
}
