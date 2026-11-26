import type { Evento } from '../services/tipos';
import { estaALaVenta, lugarDe, tieneCoordenadas } from '../services/eventos.service';
import { formatearFechaEvento } from '../lib/formato';
import { diaYMes, horaDeEvento } from '../lib/fechaEvento';
import { ButtonLink } from './ui/Button';
import { IconoLugar, IconoReloj, IconoRuta } from './ui/Icons';
import { Skeleton } from './ui/Skeleton';

/** Bloque con el día y el mes, como en las tarjetas de Conciertos de la app móvil. */
function BloqueFecha({ iso }: { iso: string }) {
  const partes = diaYMes(iso);
  if (!partes) return null;
  return (
    <div className="fecha-bloque" aria-hidden="true">
      <span className="fecha-bloque__dia">{partes.dia}</span>
      <span className="fecha-bloque__mes">{partes.mes}</span>
    </div>
  );
}

export function EventoCard({ evento }: { evento: Evento }) {
  const lugar = lugarDe(evento);
  const hora = horaDeEvento(evento.fechaHora);
  const fechaCompleta = formatearFechaEvento(evento.fechaHora);
  const id = encodeURIComponent(evento.id);
  const tituloId = `evento-${evento.id}`;

  return (
    <article className="tarjeta evento-card" aria-labelledby={tituloId}>
      <div className="evento-card__principal">
        <BloqueFecha iso={evento.fechaHora} />
        <div className="evento-card__cuerpo">
          <h2 id={tituloId} className="evento-card__nombre">
            {evento.nombre}
          </h2>
          {evento.artista?.nombre && <p className="evento-card__artista">{evento.artista.nombre}</p>}
          <ul className="evento-card__meta meta">
            <li>
              <IconoLugar tamano={16} />
              <span>{lugar ?? 'Lugar por confirmar'}</span>
            </li>
            {hora && (
              <li>
                <IconoReloj tamano={16} />
                <time dateTime={evento.fechaHora} title={fechaCompleta}>
                  {hora} h
                </time>
              </li>
            )}
          </ul>
        </div>
      </div>

      {/* Controles hermanos (no anidados): cada uno es un enlace real, accesible con teclado. */}
      <div className="evento-card__acciones">
        {tieneCoordenadas(evento) && (
          <ButtonLink
            to={`/events/${id}/map`}
            variante="secundario"
            tamano="chico"
            aria-label={`Ver ruta al evento ${evento.nombre}`}
          >
            <IconoRuta tamano={18} />
            Ver ruta
          </ButtonLink>
        )}
        {estaALaVenta(evento) && (
          <ButtonLink to={`/checkout/${id}`} variante="primario" tamano="chico" aria-label={`Comprar boleto para ${evento.nombre}`}>
            Comprar boleto
          </ButtonLink>
        )}
      </div>
    </article>
  );
}

/** Tarjeta de carga con la misma forma que EventoCard. */
export function EventoCardSkeleton() {
  return (
    <div className="tarjeta evento-card" aria-hidden="true">
      <div className="evento-card__principal">
        <Skeleton ancho={56} alto={64} />
        <div className="evento-card__cuerpo">
          <Skeleton alto={20} ancho="70%" />
          <Skeleton alto={14} ancho="40%" />
          <Skeleton alto={14} ancho="55%" />
        </div>
      </div>
    </div>
  );
}
