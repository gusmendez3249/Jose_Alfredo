import { useEffect, useId, useRef } from 'react';
import { Button } from './Button';

interface ConfirmDialogProps {
  abierto: boolean;
  titulo: string;
  texto: string;
  textoConfirmar: string;
  textoCancelar?: string;
  /** Acción destructiva: el botón de confirmar va en rojo. */
  peligro?: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
}

/**
 * Diálogo modal con `<dialog>` nativo: el navegador atrapa el foco, cierra con Escape
 * y devuelve el foco al botón que lo abrió.
 */
export function ConfirmDialog({
  abierto,
  titulo,
  texto,
  textoConfirmar,
  textoCancelar = 'Cancelar',
  peligro = false,
  onConfirmar,
  onCancelar,
}: ConfirmDialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const idTitulo = useId();
  const idTexto = useId();

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (abierto && !dialogo.open) dialogo.showModal();
    if (!abierto && dialogo.open) dialogo.close();
  }, [abierto]);

  return (
    <dialog
      ref={ref}
      className="dialogo"
      aria-labelledby={idTitulo}
      aria-describedby={idTexto}
      // Escape dispara "cancel": se avisa al padre para que su estado no quede desfasado.
      onCancel={(e) => {
        e.preventDefault();
        onCancelar();
      }}
      onClick={(e) => {
        // Clic en el fondo (el propio <dialog>), no en su contenido.
        if (e.target === e.currentTarget) onCancelar();
      }}
    >
      <div className="dialogo__contenido">
        <h2 id={idTitulo} className="dialogo__titulo">
          {titulo}
        </h2>
        <p id={idTexto} className="dialogo__texto">
          {texto}
        </p>
        <div className="dialogo__acciones">
          <Button variante="fantasma" onClick={onCancelar} autoFocus>
            {textoCancelar}
          </Button>
          <Button variante={peligro ? 'peligro' : 'primario'} onClick={onConfirmar}>
            {textoConfirmar}
          </Button>
        </div>
      </div>
    </dialog>
  );
}
