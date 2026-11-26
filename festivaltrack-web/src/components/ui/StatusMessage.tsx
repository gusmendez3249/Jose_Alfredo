import type { ComponentType, ReactNode } from 'react';

interface StatusMessageProps {
  icono: ComponentType<{ tamano?: number }>;
  titulo: string;
  texto?: ReactNode;
  /** Botones o enlaces de acción (p. ej. "Reintentar"). */
  acciones?: ReactNode;
  /** `alert` para errores (se anuncian al instante); `status` para vacíos. */
  rol?: 'alert' | 'status';
  /** Nivel del título: 1 si es el título de la página, 2 por defecto. */
  nivelTitulo?: 1 | 2;
}

/** Patrón de la app móvil: icono dorado sobre círculos tenues, título, texto y acción opcional. */
export function StatusMessage({ icono: Icono, titulo, texto, acciones, rol = 'status', nivelTitulo = 2 }: StatusMessageProps) {
  const Titulo = nivelTitulo === 1 ? 'h1' : 'h2';
  return (
    <div className="estado" role={rol}>
      <div className="estado__icono">
        <Icono tamano={44} />
      </div>
      <Titulo className="estado__titulo">{titulo}</Titulo>
      {texto && <p className="estado__texto">{texto}</p>}
      {acciones && <div className="estado__acciones">{acciones}</div>}
    </div>
  );
}
