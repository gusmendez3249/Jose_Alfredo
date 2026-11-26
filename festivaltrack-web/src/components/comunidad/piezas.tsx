import { useId, type TextareaHTMLAttributes } from 'react';
import { clases } from '../ui';
import { tiempoRelativo } from '../../services/foro.service';

/** Inicial del autor sobre un círculo dorado tenue. El nombre es texto de usuario: solo se toma su primera letra. */
export function Avatar({ nombre, chico = false }: { nombre: string; chico?: boolean }) {
  const inicial = Array.from(nombre.trim())[0]?.toLocaleUpperCase('es-MX') ?? '?';
  return (
    <span className={clases('comunidad-avatar', chico && 'comunidad-avatar--chico')} aria-hidden="true">
      {inicial}
    </span>
  );
}

/** Fecha relativa con la fecha completa como `title` y `dateTime`. */
export function Hace({ iso }: { iso: string }) {
  const fecha = new Date(iso);
  const completa = Number.isNaN(fecha.getTime())
    ? ''
    : fecha.toLocaleString('es-MX', { dateStyle: 'long', timeStyle: 'short' });
  return (
    <time dateTime={iso} title={completa}>
      {tiempoRelativo(iso)}
    </time>
  );
}

interface CampoTextoLargoProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'id' | 'value' | 'maxLength'> {
  etiqueta: string;
  /** Oculta la etiqueta a la vista (sigue asociada para lectores de pantalla). */
  etiquetaOculta?: boolean;
  value: string;
  maxLength: number;
  error?: string;
}

/** `<textarea>` con etiqueta y contador "N/máximo" enlazado como descripción. */
export function CampoTextoLargo({ etiqueta, etiquetaOculta, value, maxLength, error, className, ...resto }: CampoTextoLargoProps) {
  const id = useId();
  const idContador = `${id}-contador`;
  const idError = `${id}-error`;
  const cerca = value.length >= maxLength * 0.9;
  return (
    <div className={clases('campo', className)}>
      <label className={clases('campo__etiqueta', etiquetaOculta && 'solo-lectores')} htmlFor={id}>
        {etiqueta}
      </label>
      <textarea
        {...resto}
        id={id}
        className="campo__input comunidad-textarea"
        value={value}
        maxLength={maxLength}
        aria-invalid={error ? true : undefined}
        aria-describedby={[error && idError, idContador].filter(Boolean).join(' ')}
      />
      <div className="comunidad-campo-pie">
        {error ? (
          <p id={idError} className="campo__error">
            {error}
          </p>
        ) : (
          <span />
        )}
        <span id={idContador} className={clases('comunidad-contador', cerca && 'comunidad-contador--cerca')}>
          <span aria-hidden="true">
            {value.length}/{maxLength}
          </span>
          <span className="solo-lectores">
            {value.length} de {maxLength} caracteres
          </span>
        </span>
      </div>
    </div>
  );
}
