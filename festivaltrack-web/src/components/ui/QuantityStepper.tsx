import { useId } from 'react';
import { IconoMas, IconoMenos } from './Icons';

interface QuantityStepperProps {
  /** Nombre de lo que se cuenta ("VIP"), para las etiquetas accesibles. */
  nombre: string;
  valor: number;
  min?: number;
  max: number;
  deshabilitado?: boolean;
  onCambiar: (nuevo: number) => void;
}

export function QuantityStepper({ nombre, valor, min = 0, max, deshabilitado = false, onCambiar }: QuantityStepperProps) {
  const idValor = useId();
  return (
    <div className="cantidad" role="group" aria-label={`Cantidad de boletos ${nombre}`}>
      <button
        type="button"
        className="cantidad__boton"
        aria-label={`Quitar un boleto ${nombre}`}
        disabled={deshabilitado || valor <= min}
        onClick={() => onCambiar(valor - 1)}
      >
        <IconoMenos tamano={20} />
      </button>
      <output id={idValor} className="cantidad__valor" aria-live="polite">
        {valor}
      </output>
      <button
        type="button"
        className="cantidad__boton"
        aria-label={`Agregar un boleto ${nombre}`}
        disabled={deshabilitado || valor >= max}
        onClick={() => onCambiar(valor + 1)}
      >
        <IconoMas tamano={20} />
      </button>
    </div>
  );
}
