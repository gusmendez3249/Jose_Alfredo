import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import '../styles/audio.css';
import { Ecualizador, IconoAleatorio, Portada } from '../components/audio/Portada';
import { formatearTiempo, pistaActual, reproductor, useReproductor, type ContextoCola } from '../components/audio/reproductor';
import { Alert } from '../components/ui/Alert';
import { Badge, type TonoBadge } from '../components/ui/Badge';
import { Button, ButtonLink } from '../components/ui/Button';
import { IconoAlerta, IconoAtras, IconoEnVivo, IconoLugar, IconoMusica, IconoPausa, IconoPlay, IconoReloj, IconoRuta, IconoSinEventos } from '../components/ui/Icons';
import { Skeleton } from '../components/ui/Skeleton';
import { StatusMessage } from '../components/ui/StatusMessage';
import { clases } from '../components/ui/clases';
import { useRecurso, type EstadoRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';
import { esCancelacion, mensajeDeError } from '../lib/errores';
import { formatearFechaEvento } from '../lib/formato';
import { urlMedia } from '../lib/media';
import { cancionesService, type Cancion } from '../services/canciones.service';
import { estaALaVenta, eventosService, lugarDe, tieneCoordenadas } from '../services/eventos.service';
import type { EstadoEvento, Evento } from '../services/tipos';

const ESTADOS: Record<EstadoEvento, { texto: string; tono: TonoBadge }> = {
  BORRADOR: { texto: 'Por anunciar', tono: 'neutra' },
  PUBLICADO: { texto: 'Boletos a la venta', tono: 'exito' },
  EN_CURSO: { texto: 'En curso', tono: 'dorada' },
  FINALIZADO: { texto: 'Finalizado', tono: 'neutra' },
  CANCELADO: { texto: 'Cancelado', tono: 'error' },
};

const CATALOGO: ContextoCola = { eventoId: null, nombre: 'Discografía de José Alfredo' };

function resumenDemos(n: number): string {
  if (n === 0) return 'Sin demos por ahora';
  if (n === 1) return '1 canción · demo de hasta 30 s';
  return `${n} canciones · demos de hasta 30 s cada una`;
}

export default function EventDetail() {
  const { id = '' } = useParams();
  const evento = useRecurso((signal) => eventosService.porId(id, signal), `evento:${id}`);
  const demos = useRecurso((signal) => cancionesService.deEvento(id, signal), `demos:${id}`);
  useTituloPagina(evento.estado === 'listo' ? evento.datos.nombre : 'Concierto');

  if (evento.estado === 'cargando') {
    return (
      <div className="pagina pagina--lectura">
        <div role="status" aria-label="Cargando concierto" className="concierto-cabecera tarjeta">
          <Skeleton alto={12} ancho={160} />
          <Skeleton alto={28} ancho="75%" />
          <Skeleton alto={16} ancho="45%" />
          <Skeleton alto={16} ancho="60%" />
          <span className="solo-lectores">Cargando concierto…</span>
        </div>
      </div>
    );
  }

  if (evento.estado === 'error') {
    const noExiste = evento.status === 404;
    return (
      <div className="pagina pagina--lectura">
        <StatusMessage
          rol="alert"
          nivelTitulo={1}
          icono={noExiste ? IconoSinEventos : IconoAlerta}
          titulo={noExiste ? 'No encontramos este concierto' : 'No se pudo cargar el concierto'}
          texto={noExiste ? 'Puede que se haya retirado de la cartelera. Consulta los próximos eventos.' : evento.mensaje}
          acciones={
            <>
              {!noExiste && (
                <Button pildora onClick={evento.reintentar}>
                  Reintentar
                </Button>
              )}
              <ButtonLink to="/events" variante="secundario" pildora>
                Ver la cartelera
              </ButtonLink>
            </>
          }
        />
      </div>
    );
  }

  return <Concierto evento={evento.datos} demos={demos} />;
}

function Concierto({ evento, demos }: { evento: Evento; demos: EstadoRecurso<Cancion[]> & { reintentar: () => void } }) {
  const lugar = lugarDe(evento);
  const fecha = formatearFechaEvento(evento.fechaHora);
  const estado = ESTADOS[evento.estado];
  const id = encodeURIComponent(evento.id);
  const descripcion = (evento.descripcion ?? '').trim();

  return (
    <div className="pagina pagina--lectura">
      <div>
        <ButtonLink to="/events" variante="fantasma" tamano="chico">
          <IconoAtras tamano={18} />
          Cartelera
        </ButtonLink>
      </div>

      <header className="concierto-cabecera tarjeta tarjeta--destacada">
        <p className="eyebrow">Concierto del festival</p>
        <h1 className="concierto-cabecera__nombre">{evento.nombre}</h1>
        {evento.artista?.nombre && <p className="concierto-cabecera__artista">{evento.artista.nombre}</p>}
        <ul className="concierto-cabecera__datos meta">
          {fecha && (
            <li>
              <IconoReloj tamano={16} />
              <time dateTime={evento.fechaHora}>{fecha} h</time>
            </li>
          )}
          <li>
            <IconoLugar tamano={16} />
            <span>{lugar ?? 'Lugar por confirmar'}</span>
          </li>
        </ul>
        <div>
          <Badge tono={estado.tono}>{estado.texto}</Badge>
        </div>
        {descripcion && <p className="concierto-cabecera__descripcion">{descripcion}</p>}

        <div className="concierto-acciones">
          {estaALaVenta(evento) && (
            <ButtonLink to={`/checkout/${id}`} variante="primario" mayusculas>
              Comprar boletos del concierto
            </ButtonLink>
          )}
          {tieneCoordenadas(evento) && (
            <ButtonLink to={`/events/${id}/map`} variante="secundario">
              <IconoRuta tamano={18} />
              Ver ruta
            </ButtonLink>
          )}
          {evento.estado === 'EN_CURSO' && (
            <ButtonLink to="/en-vivo" variante="secundario">
              <IconoEnVivo tamano={18} />
              Ver transmisión
            </ButtonLink>
          )}
        </div>
      </header>

      <section className="seccion" aria-labelledby="concierto-demos">
        <h2 id="concierto-demos" className="titulo-seccion">
          Demos del concierto
        </h2>

        {demos.estado === 'cargando' && (
          <div role="status" aria-label="Cargando las canciones" className="concierto-lista">
            {[0, 1, 2, 3, 4].map((n) => (
              <div key={n} className="concierto-pista__esqueleto" aria-hidden="true">
                <Skeleton alto={48} ancho={48} />
                <div className="pila--chica pila">
                  <Skeleton alto={14} ancho="60%" />
                  <Skeleton alto={12} ancho="35%" />
                </div>
              </div>
            ))}
            <span className="solo-lectores">Cargando las canciones…</span>
          </div>
        )}

        {demos.estado === 'error' && (
          <StatusMessage
            rol="alert"
            icono={IconoAlerta}
            titulo="No pudimos cargar las canciones"
            texto="Revisa tu conexión a internet e inténtalo de nuevo."
            acciones={
              <Button pildora onClick={demos.reintentar}>
                Reintentar
              </Button>
            }
          />
        )}

        {demos.estado === 'listo' && demos.datos.length === 0 && <SinDemos />}

        {demos.estado === 'listo' && demos.datos.length > 0 && (
          <ListaDemos canciones={demos.datos} contexto={{ eventoId: evento.id, nombre: evento.nombre }} />
        )}
      </section>
    </div>
  );
}

function ListaDemos({ canciones, contexto }: { canciones: Cancion[]; contexto: ContextoCola }) {
  const e = useReproductor();
  const activa = e.contexto?.eventoId === contexto.eventoId;
  const actual = activa ? pistaActual(e) : null;
  const [aleatorioLocal, setAleatorioLocal] = useState(false);
  const aleatorio = activa ? e.aleatorio : aleatorioLocal;

  const reproducirTodo = () => {
    if (activa) {
      reproductor.alternar();
      return;
    }
    const inicio = aleatorio ? Math.floor(Math.random() * canciones.length) : 0;
    reproductor.reproducirCola(canciones, inicio, contexto, { aleatorio });
  };

  const tocarFila = (i: number) => {
    const cancion = canciones[i];
    if (activa) {
      if (actual?.id === cancion.id) {
        reproductor.alternar();
        return;
      }
      const enCola = e.cola.findIndex((p) => p.id === cancion.id);
      if (enCola !== -1) {
        reproductor.seleccionar(enCola);
        return;
      }
    }
    reproductor.reproducirCola(canciones, i, contexto, { aleatorio });
  };

  const sonandoAqui = activa && e.reproduciendo;

  return (
    <>
      <div className="concierto-controles">
        <p className="meta">{resumenDemos(canciones.length)}</p>
        <div className="concierto-controles__botones">
          <Button
            variante="secundario"
            tamano="chico"
            pildora
            aria-pressed={aleatorio}
            disabled={canciones.length < 2}
            className={clases(aleatorio && 'concierto-aleatorio--activo')}
            onClick={() => (activa ? reproductor.alternarAleatorio() : setAleatorioLocal((a) => !a))}
          >
            <IconoAleatorio tamano={18} />
            Aleatorio
          </Button>
          <Button pildora mayusculas onClick={reproducirTodo}>
            {sonandoAqui ? <IconoPausa tamano={18} /> : <IconoPlay tamano={18} />}
            {sonandoAqui ? 'Pausar' : activa ? 'Reanudar' : 'Reproducir todo'}
          </Button>
        </div>
      </div>

      <ol className="concierto-lista">
        {canciones.map((cancion, i) => {
          const esActual = actual?.id === cancion.id;
          const noDisponible = urlMedia(cancion.archivoUrl) === null || (activa && e.noDisponibles.includes(cancion.id));
          const sonando = esActual && e.reproduciendo;
          return (
            <li key={cancion.id}>
              <button
                type="button"
                className={clases('concierto-pista', esActual && 'concierto-pista--actual')}
                aria-current={esActual || undefined}
                aria-label={`${sonando ? 'Pausar' : esActual ? 'Reanudar' : 'Reproducir'} «${cancion.titulo}» de ${cancion.artista}`}
                disabled={noDisponible}
                onClick={() => tocarFila(i)}
              >
                <span className="concierto-pista__numero">
                  {esActual ? <Ecualizador activo={sonando && !e.esperando} /> : i + 1}
                </span>
                <Portada src={urlMedia(cancion.portadaUrl)} />
                <span className="concierto-pista__textos">
                  <span className="concierto-pista__titulo">{cancion.titulo}</span>
                  <span className="meta">{noDisponible ? 'No disponible' : cancion.artista}</span>
                </span>
                <span className="concierto-pista__duracion meta">{formatearTiempo(cancion.duracion)}</span>
                <span className="concierto-pista__icono">
                  {sonando ? <IconoPausa tamano={20} /> : <IconoPlay tamano={20} />}
                </span>
              </button>
            </li>
          );
        })}
      </ol>
    </>
  );
}

/** Estado vacío (el normal hoy): ofrece escuchar el catálogo general mientras se publica el repertorio. */
function SinDemos() {
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const control = useRef<AbortController | null>(null);

  useEffect(() => () => control.current?.abort(), []);

  const escucharCatalogo = async () => {
    control.current?.abort();
    const actual = new AbortController();
    control.current = actual;
    setCargando(true);
    setError(null);
    try {
      const publicadas = (await cancionesService.listar(actual.signal)).filter((c) => c.estado === 'PUBLICADA');
      if (publicadas.length === 0) setError('Todavía no hay canciones publicadas en el catálogo.');
      else reproductor.reproducirCola(publicadas, 0, CATALOGO);
    } catch (err) {
      if (esCancelacion(err)) return;
      setError(mensajeDeError(err));
    } finally {
      if (!actual.signal.aborted) setCargando(false);
    }
  };

  return (
    <div className="pila">
      <StatusMessage
        icono={IconoMusica}
        titulo="Aún no hay demos para este concierto"
        texto="El repertorio se publica antes del concierto. Vuelve pronto para escuchar un adelanto; mientras tanto ya puedes comprar tus boletos."
        acciones={
          <Button variante="secundario" pildora cargando={cargando} onClick={() => void escucharCatalogo()}>
            <IconoPlay tamano={18} />
            Escuchar el catálogo general
          </Button>
        }
      />
      {error && <Alert tono="error">{error}</Alert>}
    </div>
  );
}
