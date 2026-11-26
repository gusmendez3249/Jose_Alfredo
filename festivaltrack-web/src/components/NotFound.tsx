import { useTituloPagina } from '../hooks/useTituloPagina';
import { ButtonLink } from './ui/Button';
import { IconoLugar } from './ui/Icons';
import { StatusMessage } from './ui/StatusMessage';

export default function NotFound() {
  useTituloPagina('Página no encontrada');

  return (
    <div className="pagina pagina--lectura">
      <StatusMessage
        nivelTitulo={1}
        icono={IconoLugar}
        titulo="Página no encontrada"
        texto="La dirección que abriste no existe o cambió."
        acciones={
          <>
            <ButtonLink to="/" pildora>
              Ir al inicio
            </ButtonLink>
            <ButtonLink to="/events" variante="secundario" pildora>
              Ver eventos
            </ButtonLink>
          </>
        }
      />
    </div>
  );
}

