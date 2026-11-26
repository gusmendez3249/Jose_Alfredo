import { useId, useState, type InputHTMLAttributes } from 'react';
import { clases } from './clases';
import { IconoOjo, IconoOjoCerrado } from './Icons';

interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  etiqueta: string;
  id?: string;
  error?: string;
  ayuda?: string;
  /** Con `type="password"`, añade el botón mostrar/ocultar. */
  alternarContrasena?: boolean;
}

/** Campo con etiqueta visible, ayuda y error enlazados con `aria-describedby`. */
export function Field({ etiqueta, id, error, ayuda, alternarContrasena = false, className, type = 'text', ...resto }: FieldProps) {
  const generado = useId();
  const idCampo = id ?? generado;
  const idAyuda = `${idCampo}-ayuda`;
  const idError = `${idCampo}-error`;
  const [visible, setVisible] = useState(false);

  const conBoton = type === 'password' && alternarContrasena;
  const descripcion = [error && idError, ayuda && idAyuda].filter(Boolean).join(' ') || undefined;

  return (
    <div className={clases('campo', conBoton && 'campo--con-boton', className)}>
      <label className="campo__etiqueta" htmlFor={idCampo}>
        {etiqueta}
      </label>
      <div className="campo__control">
        <input
          {...resto}
          id={idCampo}
          className="campo__input"
          type={conBoton && visible ? 'text' : type}
          aria-invalid={error ? true : resto['aria-invalid']}
          aria-describedby={descripcion}
        />
        {conBoton && (
          <button
            type="button"
            className="campo__alternar"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={visible}
            disabled={resto.disabled}
          >
            {visible ? <IconoOjoCerrado /> : <IconoOjo />}
          </button>
        )}
      </div>
      {ayuda && (
        <p id={idAyuda} className="campo__ayuda">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={idError} className="campo__error">
          {error}
        </p>
      )}
    </div>
  );
}
