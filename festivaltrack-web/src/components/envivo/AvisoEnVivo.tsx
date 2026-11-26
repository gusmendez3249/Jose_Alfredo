import { Link } from 'react-router-dom';
import { IconoEnVivo, IconoFlecha } from '../ui/Icons';
import { useEstadoTransmision } from './useEstadoTransmision';

/** Sondeo lento: en el Inicio el aviso no necesita ser inmediato. */
export const INTERVALO_AVISO_MS = 30_000;

/**
 * Banda "EN VIVO AHORA" del Inicio. Solo aparece con transmisión activa; si la consulta falla
 * (o el backend está despertando) no muestra nada ni afecta al resto de la página.
 */
export function AvisoEnVivo() {
  const { estado } = useEstadoTransmision(INTERVALO_AVISO_MS);
  if (!estado?.isLive) return null;

  return (
    <Link to="/en-vivo" className="inicio-envivo">
      <span className="inicio-envivo__punto" aria-hidden="true" />
      <span className="inicio-envivo__texto">
        <strong>EN VIVO AHORA{estado.eventoNombre ? ` · ${estado.eventoNombre}` : ''}</strong>
        <span>Entra al chat y mira dónde ver el video</span>
      </span>
      <IconoEnVivo tamano={22} />
      <IconoFlecha tamano={20} />
    </Link>
  );
}
