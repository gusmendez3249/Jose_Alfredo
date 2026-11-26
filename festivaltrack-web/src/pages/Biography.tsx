import '../styles/biografia.css';
import { ImagenBio } from '../components/biografia/ImagenBio';
import { ALBUMES, ARCHIVO_HISTORICO, CITA_FIJA, RESUMEN_FIJO, RETRATO_LOCAL } from '../components/biografia/contenido';
import { Button, ButtonLink, IconoAlerta, IconoLugar, IconoMusica, Skeleton, StatusMessage } from '../components/ui';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';
import { urlMedia } from '../lib/media';
import { biografiasService, type Biografia } from '../services/biografias.service';

function Cabecera({ bio }: { bio: Biografia | null }) {
  const nombre = bio?.artista.nombre ?? 'José Alfredo Jiménez';
  const cita = bio?.citaCelebre ?? `“${CITA_FIJA}”`;
  return (
    <header className="bio-hero">
      <ImagenBio
        className="bio-hero__retrato"
        src={urlMedia(bio?.artista.imagenUrl)}
        respaldo={RETRATO_LOCAL}
        alt={`Retrato de ${nombre}`}
        prioridad
      />
      <div className="bio-hero__texto">
        <p className="bio-insignia">Patrimonio musical de México</p>
        <p className="eyebrow">Dolores Hidalgo, Guanajuato</p>
        <h1 className="bio-hero__titulo">
          Vida y Obra <span className="bio-hero__dorado">del Rey</span>
        </h1>
        <p className="bio-hero__nombre">{nombre}</p>
        <figure className="bio-cita">
          <figcaption className="eyebrow">Cita célebre del maestro</figcaption>
          <blockquote className="bio-cita__texto">{cita}</blockquote>
        </figure>
      </div>
    </header>
  );
}

function Resumen({ bio }: { bio: Biografia | null }) {
  const descripcion = bio?.descripcion.trim() ? bio.descripcion : RESUMEN_FIJO;
  return (
    <section className="bio-seccion" aria-labelledby="bio-resumen">
      <h2 id="bio-resumen" className="titulo-seccion">
        Resumen biográfico
      </h2>
      <p className="bio-texto">{descripcion}</p>
    </section>
  );
}

function Hitos({ bio }: { bio: Biografia | null }) {
  if (!bio || bio.hitos.length === 0) return null;
  return (
    <section className="bio-seccion" aria-labelledby="bio-hitos">
      <h2 id="bio-hitos" className="titulo-seccion">
        Hitos
      </h2>
      <ol className="bio-hitos">
        {bio.hitos.map((hito, i) => (
          <li key={`${i}-${hito.titulo}`} className="bio-hito">
            {hito.ano && <span className="bio-hito__ano">{hito.ano}</span>}
            <span className="bio-hito__titulo">{hito.titulo}</span>
            {hito.descripcion && <span className="bio-hito__descripcion">{hito.descripcion}</span>}
          </li>
        ))}
      </ol>
    </section>
  );
}

function Discografia({ bio }: { bio: Biografia | null }) {
  const grabaciones = bio?.discografia ?? [];
  return (
    <section className="bio-seccion" aria-labelledby="bio-disco">
      <h2 id="bio-disco" className="titulo-seccion">
        Discografía y grabaciones maestras
      </h2>
      <ul className="bio-albumes">
        {ALBUMES.map((album) => (
          <li key={album.titulo} className="bio-album">
            <ImagenBio className="bio-album__portada" src={album.imagen} alt={`Portada de ${album.titulo}`} />
            <span className="bio-album__titulo">{album.titulo}</span>
            <span className="meta">{album.subtitulo}</span>
          </li>
        ))}
      </ul>
      {grabaciones.length > 0 && (
        <div className="bio-grabaciones">
          <h3 className="bio-subtitulo">Grabaciones destacadas</h3>
          <ul className="bio-lista">
            {grabaciones.map((titulo, i) => (
              <li key={`${i}-${titulo}`}>
                <IconoMusica tamano={16} />
                <span>{titulo}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
      <ButtonLink to="/events" variante="secundario" mayusculas>
        Escuchar demos en los conciertos
      </ButtonLink>
    </section>
  );
}

function Archivo() {
  return (
    <section className="bio-seccion bio-archivo" aria-labelledby="bio-archivo">
      <div className="pagina__cabecera">
        <p className="eyebrow">{ARCHIVO_HISTORICO.length} fotos</p>
        <h2 id="bio-archivo" className="titulo-seccion">
          Archivo histórico
        </h2>
      </div>
      <ol className="bio-archivo__lista">
        {ARCHIVO_HISTORICO.map((e) => (
          <li key={e.id}>
            <article className="bio-entrada">
              <ImagenBio className="bio-entrada__imagen" src={e.imagen} alt={e.alt} />
              <div className="bio-entrada__cuerpo">
                <p className="bio-entrada__epoca">{e.epoca}</p>
                <h3 className="bio-entrada__titulo">{e.titulo}</h3>
                <p className="bio-entrada__lugar">
                  <IconoLugar tamano={14} />
                  <span>{e.ubicacion}</span>
                </p>
                <p className="bio-texto">{e.descripcion}</p>
                <blockquote className="bio-entrada__cita">{e.cita}</blockquote>
              </div>
            </article>
          </li>
        ))}
      </ol>
    </section>
  );
}

function Cargando() {
  return (
    <div role="status" aria-label="Cargando biografía" className="bio-cargando">
      <Skeleton alto={320} />
      <Skeleton alto={20} ancho="60%" />
      <Skeleton alto={90} />
      <span className="solo-lectores">Cargando biografía…</span>
    </div>
  );
}

export default function Biography() {
  useTituloPagina('Biografía');
  const recurso = useRecurso((signal) => biografiasService.principal(signal), 'biografia');

  if (recurso.estado === 'cargando') {
    return (
      <div className="pagina bio-pagina">
        <h1 className="solo-lectores">Biografía</h1>
        <Cargando />
      </div>
    );
  }

  const bio = recurso.estado === 'listo' ? recurso.datos : null;

  return (
    <div className="pagina bio-pagina">
      <Cabecera bio={bio} />

      {recurso.estado === 'error' && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="No se pudo cargar la biografía"
          texto={recurso.mensaje}
          acciones={
            <Button pildora onClick={recurso.reintentar}>
              Reintentar
            </Button>
          }
        />
      )}

      <div className="bio-cuerpo">
        <div className="bio-cuerpo__principal">
          <Resumen bio={bio} />
          <Hitos bio={bio} />
        </div>
        <aside className="bio-cuerpo__lateral">
          <Discografia bio={bio} />
        </aside>
      </div>

      <Archivo />

      <div className="pagina__pie">
        <ButtonLink to="/" variante="secundario" mayusculas>
          Volver al inicio
        </ButtonLink>
      </div>
    </div>
  );
}
