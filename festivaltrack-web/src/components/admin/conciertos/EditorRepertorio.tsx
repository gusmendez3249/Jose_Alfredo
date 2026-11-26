import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Badge, Button, ButtonLink, IconoAbajo, IconoArriba, IconoBasura, IconoMas, IconoMusica, StatusMessage } from '../../ui';
import { formatearDuracion } from '../../../lib/formato';
import { cancionesService, type Cancion, type EstadoCancion } from '../../../services/canciones.service';
import { mensajeAlGuardarRepertorio } from './mensajes';
import '../../../styles/admin-conciertos.css';

const ETIQUETA_ESTADO_CANCION: Record<EstadoCancion, string> = {
  PUBLICADA: 'Publicada',
  BORRADOR: 'Borrador',
  ARCHIVADA: 'Archivada',
};

interface Props {
  eventoId: string;
  catalogo: Cancion[];
  /** Repertorio guardado, en orden de reproducción (el backend solo devuelve canciones publicadas). */
  repertorioInicial: Cancion[];
}

function Meta({ cancion }: { cancion: Cancion }) {
  return (
    <span className="admin-conciertos-cancion__meta">
      {cancion.artista} · {formatearDuracion(cancion.duracion)}
    </span>
  );
}

/**
 * Dos listas: lo que suena en el concierto (con ↑/↓ y quitar) y el catálogo (con añadir).
 * Del catálogo solo se pueden añadir canciones PUBLICADAS: el público únicamente ve esas, así que
 * añadir un borrador o una archivada dejaría un repertorio que no se ve. Las demás se muestran
 * deshabilitadas con su estado para que el administrador sepa por qué.
 */
export function EditorRepertorio({ eventoId, catalogo, repertorioInicial }: Props) {
  const porId = useMemo(() => new Map(catalogo.map((c) => [c.id, c])), [catalogo]);
  // Una canción del repertorio que ya no está en el catálogo (carrera entre peticiones) se descarta.
  const idsIniciales = useMemo(
    () => repertorioInicial.map((c) => c.id).filter((id) => porId.has(id)),
    [repertorioInicial, porId],
  );

  const [guardados, setGuardados] = useState<string[]>(idsIniciales);
  const [ids, setIds] = useState<string[]>(idsIniciales);
  const [guardando, setGuardando] = useState(false);
  const [resultado, setResultado] = useState<{ tono: 'exito' | 'error'; texto: string } | null>(null);
  const [anuncio, setAnuncio] = useState('');
  const enCurso = useRef(false);
  const contenedor = useRef<HTMLDivElement>(null);
  const [foco, setFoco] = useState<{ id: string; accion: 'subir' | 'bajar' } | null>(null);

  const sucio = ids.length !== guardados.length || ids.some((id, i) => id !== guardados[i]);

  useEffect(() => {
    if (!sucio) return;
    const avisar = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener('beforeunload', avisar);
    return () => window.removeEventListener('beforeunload', avisar);
  }, [sucio]);

  // Tras mover una canción, el foco se queda en su botón (o en el opuesto si ya llegó a un extremo).
  useEffect(() => {
    if (!foco || !contenedor.current) return;
    const buscar = (accion: string) =>
      contenedor.current?.querySelector<HTMLButtonElement>(`[data-cancion="${CSS.escape(foco.id)}"][data-accion="${accion}"]`);
    const principal = buscar(foco.accion);
    const alterno = buscar(foco.accion === 'subir' ? 'bajar' : 'subir');
    (principal && !principal.disabled ? principal : alterno)?.focus();
  }, [foco, ids]);

  const enConcierto = ids.map((id) => porId.get(id)).filter((c): c is Cancion => c !== undefined);
  const disponibles = catalogo.filter((c) => !ids.includes(c.id));

  function cambiar(siguiente: string[]) {
    setIds(siguiente);
    setResultado(null);
  }

  function mover(id: string, desplazamiento: -1 | 1) {
    const i = ids.indexOf(id);
    const j = i + desplazamiento;
    if (i < 0 || j < 0 || j >= ids.length) return;
    const copia = [...ids];
    [copia[i], copia[j]] = [copia[j], copia[i]];
    cambiar(copia);
    setFoco({ id, accion: desplazamiento === -1 ? 'subir' : 'bajar' });
    setAnuncio(`«${porId.get(id)?.titulo ?? ''}» pasó a la posición ${j + 1} de ${ids.length}.`);
  }

  function agregar(cancion: Cancion) {
    cambiar([...ids, cancion.id]);
    setFoco(null);
    setAnuncio(`«${cancion.titulo}» se añadió al concierto.`);
  }

  function quitar(cancion: Cancion) {
    cambiar(ids.filter((id) => id !== cancion.id));
    setFoco(null);
    setAnuncio(`«${cancion.titulo}» se quitó del concierto.`);
  }

  async function guardar() {
    if (enCurso.current || !sucio) return;
    enCurso.current = true;
    setGuardando(true);
    setResultado(null);
    const enviados = ids;
    try {
      await cancionesService.guardarRepertorio(eventoId, enviados);
      setGuardados(enviados);
      setResultado({
        tono: 'exito',
        texto: `Repertorio guardado (${enviados.length} ${enviados.length === 1 ? 'canción' : 'canciones'})`,
      });
    } catch (error) {
      setResultado({ tono: 'error', texto: `No se pudo guardar el repertorio. ${mensajeAlGuardarRepertorio(error)}` });
    } finally {
      enCurso.current = false;
      setGuardando(false);
    }
  }

  if (catalogo.length === 0) {
    return (
      <StatusMessage
        icono={IconoMusica}
        titulo="El catálogo está vacío"
        texto="Primero da de alta canciones en la sección Canciones; después podrás asignarlas a este concierto."
        acciones={
          <ButtonLink to="/admin/canciones" pildora>
            Ir a Canciones
          </ButtonLink>
        }
      />
    );
  }

  return (
    <div ref={contenedor} className="admin-conciertos-repertorio">
      <div className="solo-lectores" role="status" aria-live="polite">
        {anuncio}
      </div>

      <div className="admin-conciertos-listas">
        <section className="admin-conciertos-lista" aria-labelledby="rep-en-concierto">
          <h2 id="rep-en-concierto" className="eyebrow">
            En el concierto · {enConcierto.length}
          </h2>
          {enConcierto.length === 0 ? (
            <p className="admin-conciertos-vacio">Todavía no hay canciones. Agrega del catálogo con el botón +.</p>
          ) : (
            <>
              <p className="meta">Usa las flechas para cambiar el orden en que suenan.</p>
              <ol className="admin-conciertos-canciones">
                {enConcierto.map((cancion, i) => (
                  <li key={cancion.id} className="admin-conciertos-cancion">
                    <span className="admin-conciertos-cancion__numero" aria-hidden="true">
                      {i + 1}
                    </span>
                    <span className="admin-conciertos-cancion__texto">
                      <span className="admin-conciertos-cancion__titulo">{cancion.titulo}</span>
                      <Meta cancion={cancion} />
                    </span>
                    <span className="admin-conciertos-cancion__acciones">
                      <Button
                        variante="fantasma"
                        className="admin-conciertos-icono"
                        aria-label={`Subir «${cancion.titulo}»`}
                        data-cancion={cancion.id}
                        data-accion="subir"
                        disabled={i === 0 || guardando}
                        onClick={() => mover(cancion.id, -1)}
                      >
                        <IconoArriba />
                      </Button>
                      <Button
                        variante="fantasma"
                        className="admin-conciertos-icono"
                        aria-label={`Bajar «${cancion.titulo}»`}
                        data-cancion={cancion.id}
                        data-accion="bajar"
                        disabled={i === enConcierto.length - 1 || guardando}
                        onClick={() => mover(cancion.id, 1)}
                      >
                        <IconoAbajo />
                      </Button>
                      <Button
                        variante="fantasma"
                        className="admin-conciertos-icono admin-conciertos-cancelar"
                        aria-label={`Quitar «${cancion.titulo}» del concierto`}
                        disabled={guardando}
                        onClick={() => quitar(cancion)}
                      >
                        <IconoBasura />
                      </Button>
                    </span>
                  </li>
                ))}
              </ol>
            </>
          )}
        </section>

        <section className="admin-conciertos-lista" aria-labelledby="rep-catalogo">
          <h2 id="rep-catalogo" className="eyebrow">
            Catálogo · {disponibles.length}
          </h2>
          {disponibles.length === 0 ? (
            <p className="admin-conciertos-vacio">Todas las canciones ya están en el concierto.</p>
          ) : (
            <ul className="admin-conciertos-canciones">
              {disponibles.map((cancion) => {
                const publicada = cancion.estado === 'PUBLICADA';
                return (
                  <li key={cancion.id} className="admin-conciertos-cancion">
                    <span className="admin-conciertos-cancion__texto">
                      <span className="admin-conciertos-cancion__titulo">{cancion.titulo}</span>
                      <Meta cancion={cancion} />
                      {!publicada && (
                        <span className="admin-conciertos-cancion__aviso">
                          <Badge tono="neutra">{ETIQUETA_ESTADO_CANCION[cancion.estado]}</Badge> No se puede añadir: solo las
                          canciones publicadas llegan al público.
                        </span>
                      )}
                    </span>
                    <Button
                      variante="secundario"
                      className="admin-conciertos-icono"
                      aria-label={`Agregar «${cancion.titulo}» al concierto`}
                      disabled={!publicada || guardando}
                      onClick={() => agregar(cancion)}
                    >
                      <IconoMas />
                    </Button>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>

      {resultado && <Alert tono={resultado.tono}>{resultado.texto}</Alert>}

      <div className="admin-conciertos-pie">
        <p className="meta" role="status">
          {sucio ? 'Cambios sin guardar' : 'Sin cambios'}
        </p>
        <div className="admin-conciertos-botones">
          <ButtonLink to="/admin/conciertos" variante="fantasma">
            Volver a conciertos
          </ButtonLink>
          <Button mayusculas disabled={!sucio} cargando={guardando} onClick={() => void guardar()}>
            Guardar
          </Button>
        </div>
      </div>
    </div>
  );
}
