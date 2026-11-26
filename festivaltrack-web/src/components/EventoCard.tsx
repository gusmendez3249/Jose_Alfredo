import { Link } from 'react-router-dom';
import '../styles/audio.css';
import type { Evento } from '../services/tipos';
import { estaALaVenta, lugarDe, tieneCoordenadas } from '../services/eventos.service';
import { formatearFechaEvento } from '../lib/formato';
import { diaYMes, horaDeEvento } from '../lib/fechaEvento';
import { useCantidadDemos } from './audio/demosEvento';
import { useReproductor } from './audio/reproductor';
import { Badge } from './ui/Badge';
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

/** Insignia de demos (como `RepertoireBadge` del móvil); nada mientras carga, si no hay o si falla. */
function InsigniaDemos({ eventoId }: { eventoId: string }) {
  const cantidad = useCantidadDemos(eventoId);
  const { contexto, reproduciendo } = useReproductor();
  if (contexto?.eventoId === eventoId) {
    return <Badge tono="dorada">{reproduciendo ? 'Sonando ahora' : 'En pausa'}</Badge>;
  }
  if (!cantidad) return null;
  return <Badge tono="dorada">{cantidad === 1 ? '1 demo para escuchar' : `${cantidad} demos para escuchar`}</Badge>;
}

export function EventoCard({ evento }: { evento: Evento }) {
  const lugar = lugarDe(evento);
  const hora = horaDeEvento(evento.fechaHora);
  const fechaCompleta = formatearFechaEvento(evento.fechaHora);
  const id = encodeURIComponent(evento.id);
  const tituloId = `evento-${evento.id}`;

  return (
    <article className="tarjeta evento-card concierto-tarjeta" aria-labelledby={tituloId}>
      <div className="evento-card__principal">
        <BloqueFecha iso={evento.fechaHora} />
        <div className="evento-card__cuerpo">
          <h2 id={tituloId} className="evento-card__nombre">
            {/* Su ::after cubre la tarjeta entera; los botones de abajo quedan por encima. */}
            <Link to={`/events/${id}`} className="concierto-tarjeta__enlace">
              {evento.nombre}
            </Link>
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
          <div className="concierto-tarjeta__insignia">
            <InsigniaDemos eventoId={evento.id} />
          </div>
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
