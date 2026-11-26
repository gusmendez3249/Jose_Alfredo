import '../styles/envivo.css';
import { ChatEnVivo } from '../components/envivo/ChatEnVivo';
import { useEstadoTransmision } from '../components/envivo/useEstadoTransmision';
import { Alert } from '../components/ui/Alert';
import { Button, ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { IconoAlerta, IconoEnVivo, IconoEventos, IconoInfo, IconoLugar, IconoReloj } from '../components/ui/Icons';
import { Skeleton } from '../components/ui/Skeleton';
import { StatusMessage } from '../components/ui/StatusMessage';
import { formatearFechaEvento } from '../lib/formato';
import { cargarProximos } from '../lib/proximos';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';
import { lugarDe } from '../services/eventos.service';
import type { EstadoTransmision } from '../services/stream.service';

/** Estado + chat cada 8 s: más amable que los 4 s del móvil (el backend limita por IP). */
const INTERVALO_ESTADO_MS = 8000;

function ProximoConcierto() {
  const recurso = useRecurso(cargarProximos, 'eventos');
  if (recurso.estado === 'cargando') {
    return (
      <Card role="status" aria-label="Cargando el próximo concierto">
        <div className="pila pila--chica">
          <Skeleton alto={12} ancho={110} />
          <Skeleton alto={22} ancho="60%" />
        </div>
      </Card>
    );
  }
  // Si la cartelera falla, el estado vacío ya dice lo esencial: no se añade otro error.
  if (recurso.estado === 'error' || recurso.datos.length === 0) return null;

  const evento = recurso.datos[0];
  const fecha = formatearFechaEvento(evento.fechaHora);
  const lugar = lugarDe(evento);
  return (
    <Card destacada como="article" aria-labelledby="envivo-proximo-nombre" className="envivo-proximo">
      <p className="eyebrow">Próximo concierto</p>
      <h2 id="envivo-proximo-nombre" className="destacado__nombre">
        {evento.nombre}
      </h2>
      <ul className="meta evento-card__meta">
        {fecha && (
          <li>
            <IconoReloj tamano={16} />
            <time dateTime={evento.fechaHora}>{fecha} h</time>
          </li>
        )}
        {lugar && (
          <li>
            <IconoLugar tamano={16} />
            <span>{lugar}</span>
          </li>
        )}
      </ul>
      <div>
        <ButtonLink to={`/events/${encodeURIComponent(evento.id)}`} variante="secundario" pildora>
          Ver concierto
        </ButtonLink>
      </div>
    </Card>
  );
}

function SinTransmision() {
  return (
    <div className="envivo-vacio">
      <StatusMessage
        icono={IconoEnVivo}
        titulo="No hay transmisión en este momento"
        texto="Cuando un concierto salga en vivo, aquí verás el aviso y podrás participar en el chat. El video se ve en la app FestivalTrack y en la Smart TV del festival."
        acciones={
          <ButtonLink to="/events" variante="secundario" pildora>
            <IconoEventos tamano={18} />
            Ver cartelera
          </ButtonLink>
        }
      />
      <ProximoConcierto />
    </div>
  );
}

function ConTransmision({ estado }: { estado: EstadoTransmision }) {
  const nombre = estado.eventoNombre ?? 'Concierto del festival';
  return (
    <>
      <Card destacada className="envivo-estado">
        <span className="envivo-insignia">
          <span className="envivo-insignia__punto" aria-hidden="true" />
          EN VIVO
        </span>
        <h2 className="envivo-estado__nombre">{nombre}</h2>
        <Alert tono="info">
          <strong>El concierto está en vivo.</strong> Mira el video en la app FestivalTrack o en la Smart TV del festival: el
          navegador no puede reproducirlo.
        </Alert>
        {estado.eventoId && (
          <div>
            <ButtonLink to={`/events/${encodeURIComponent(estado.eventoId)}`} variante="secundario" pildora>
              Ver detalle del concierto
            </ButtonLink>
          </div>
        )}
      </Card>
      {estado.eventoId ? (
        <ChatEnVivo key={estado.eventoId} eventoId={estado.eventoId} />
      ) : (
        <p className="envivo-nota">
          <IconoInfo tamano={16} /> El chat estará disponible en cuanto se identifique el concierto en transmisión.
        </p>
      )}
    </>
  );
}

export default function Live() {
  useTituloPagina('En vivo');
  const { estado, error, fallos, refrescar } = useEstadoTransmision(INTERVALO_ESTADO_MS);

  return (
    <div className="pagina pagina--lectura envivo">
      <header className="pagina__cabecera">
        <p className="eyebrow">Transmisión</p>
        <h1 className="titulo-pagina">En vivo</h1>
      </header>

      {estado === null && !error && (
        <Card role="status" aria-label="Consultando la transmisión">
          <div className="pila pila--chica">
            <Skeleton alto={24} ancho={110} />
            <Skeleton alto={22} ancho="55%" />
            <p className="meta">Consultando…</p>
          </div>
        </Card>
      )}

      {estado === null && error && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="Sin conexión con el servidor"
          texto={error}
          acciones={
            <Button variante="secundario" pildora onClick={refrescar}>
              Reintentar
            </Button>
          }
        />
      )}

      {estado !== null && fallos > 0 && (
        <Alert tono="info">No se pudo actualizar la transmisión. Seguimos intentando conectar.</Alert>
      )}

      {estado?.isLive === true && <ConTransmision estado={estado} />}
      {estado?.isLive === false && <SinTransmision />}
    </div>
  );
}
