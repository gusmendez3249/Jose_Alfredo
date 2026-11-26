import { useParams } from 'react-router-dom';
import { AdminNav } from '../../components/admin/AdminNav';
import { FormularioConcierto } from '../../components/admin/conciertos/FormularioConcierto';
import { EstadoConcierto } from '../../components/admin/conciertos/EstadoConcierto';
import { MENSAJES_ADMIN } from '../../components/admin/conciertos/mensajes';
import { Alert, Button, ButtonLink, IconoAlerta, IconoEventos, Skeleton, StatusMessage } from '../../components/ui';
import { useRecurso } from '../../hooks/useRecurso';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { eventosService } from '../../services/eventos.service';
import '../../styles/admin-conciertos.css';

/** Alta (`/admin/conciertos/nuevo`) y edición (`/admin/conciertos/:id`) de un concierto. */
export default function AdminEventForm() {
  const { id } = useParams<{ id: string }>();
  return id ? <Edicion key={id} id={id} /> : <Alta />;
}

function Alta() {
  useTituloPagina('Nuevo concierto');
  return (
    <div className="pagina pagina--lectura">
      <AdminNav />
      <header className="pagina__cabecera">
        <h1 className="titulo-pagina">Nuevo concierto</h1>
        <p className="meta">Se publica de inmediato y aparece en la cartelera.</p>
      </header>
      <FormularioConcierto />
    </div>
  );
}

function Edicion({ id }: { id: string }) {
  useTituloPagina('Editar concierto');
  const recurso = useRecurso((signal) => eventosService.porId(id, signal), `evento:${id}`, {
    ...MENSAJES_ADMIN,
    404: 'Ese concierto no existe o ya no está disponible.',
  });

  return (
    <div className="pagina pagina--lectura">
      <AdminNav />

      {recurso.estado === 'cargando' && (
        <div role="status" aria-label="Cargando el concierto" className="admin-conciertos-formulario">
          <Skeleton alto={32} ancho="50%" />
          <Skeleton alto={52} />
          <Skeleton alto={52} />
          <Skeleton alto={52} />
          <span className="solo-lectores">Cargando el concierto…</span>
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
          titulo="No se pudo cargar el concierto"
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
            <h1 className="titulo-pagina">Editar concierto</h1>
            <p className="admin-conciertos-estado-linea">
              <EstadoConcierto estado={recurso.datos.estado} />
            </p>
          </header>
          {recurso.datos.estado === 'CANCELADO' && (
            <Alert tono="info">Este concierto está cancelado. Puedes corregir sus datos, pero no vuelve a la cartelera.</Alert>
          )}
          <FormularioConcierto key={recurso.datos.id} evento={recurso.datos} />
        </>
      )}
    </div>
  );
}
