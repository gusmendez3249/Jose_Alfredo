import { useParams } from 'react-router-dom';
import { AdminNav } from '../../components/admin/AdminNav';
import { EditorRepertorio } from '../../components/admin/conciertos/EditorRepertorio';
import { MENSAJES_ADMIN } from '../../components/admin/conciertos/mensajes';
import { Button, ButtonLink, IconoAlerta, IconoEventos, Skeleton, StatusMessage } from '../../components/ui';
import { useRecurso } from '../../hooks/useRecurso';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { cancionesService } from '../../services/canciones.service';
import { eventosService } from '../../services/eventos.service';
import '../../styles/admin-conciertos.css';

/** Repertorio de un concierto: `/admin/conciertos/:id/repertorio`. */
export default function AdminRepertoire() {
  const { id = '' } = useParams<{ id: string }>();
  return <Repertorio key={id} id={id} />;
}

function Repertorio({ id }: { id: string }) {
  useTituloPagina('Repertorio del concierto');
  const recurso = useRecurso(
    async (signal) => {
      const [evento, catalogo, repertorio] = await Promise.all([
        eventosService.porId(id, signal),
        cancionesService.listar(signal),
        cancionesService.deEvento(id, signal),
      ]);
      return { evento, catalogo, repertorio };
    },
    `repertorio:${id}`,
    MENSAJES_ADMIN,
  );

  return (
    <div className="pagina">
      <AdminNav />

      {recurso.estado === 'cargando' && (
        <div role="status" aria-label="Cargando el repertorio" className="pila">
          <Skeleton alto={32} ancho="50%" />
          <Skeleton alto={64} />
          <Skeleton alto={64} />
          <Skeleton alto={64} />
          <span className="solo-lectores">Cargando el repertorio…</span>
        </div>
      )}

      {recurso.estado === 'error' && recurso.status === 404 && (
        <StatusMessage
          nivelTitulo={1}
          icono={IconoEventos}
          titulo="Concierto no encontrado"
          texto="Ese concierto no existe o ya no está disponible."
          acciones={
            <ButtonLink to="/admin/conciertos" pildora>
              Volver a conciertos
            </ButtonLink>
          }
        />
      )}

      {recurso.estado === 'error' && recurso.status !== 404 && (
        <StatusMessage
          nivelTitulo={1}
          rol="alert"
          icono={IconoAlerta}
          titulo="No se pudo cargar el catálogo"
          texto={recurso.mensaje}
          acciones={
            <Button pildora onClick={recurso.reintentar}>
              Reintentar
            </Button>
          }
        />
      )}

      {recurso.estado === 'listo' && (
        <>
          <header className="pagina__cabecera">
            <p className="eyebrow">Repertorio del concierto</p>
            <h1 className="titulo-pagina">{recurso.datos.evento.nombre}</h1>
          </header>
          <EditorRepertorio
            eventoId={recurso.datos.evento.id}
            catalogo={recurso.datos.catalogo}
            repertorioInicial={recurso.datos.repertorio}
          />
        </>
      )}
    </div>
  );
}
