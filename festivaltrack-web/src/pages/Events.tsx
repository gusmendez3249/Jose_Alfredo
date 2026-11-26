import { EventoCard, EventoCardSkeleton } from '../components/EventoCard';
import { Button, ButtonLink } from '../components/ui/Button';
import { IconoAlerta, IconoSinEventos } from '../components/ui/Icons';
import { StatusMessage } from '../components/ui/StatusMessage';
import { cargarProximos } from '../lib/proximos';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';

export default function Events() {
  useTituloPagina('Eventos');
  const recurso = useRecurso(cargarProximos, 'eventos');

  return (
    <div className="pagina pagina--lectura">
      <header className="pagina__cabecera">
        <h1 className="titulo-pagina">Próximos eventos</h1>
        <p className="meta">Consulta la cartelera, escucha un fragmento de las canciones de cada concierto, compra tu boleto o mira la ruta hacia el escenario.</p>
      </header>

      {recurso.estado === 'cargando' && (
        <div role="status" aria-label="Cargando eventos" className="lista-tarjetas">
          <EventoCardSkeleton />
          <EventoCardSkeleton />
          <EventoCardSkeleton />
          <span className="solo-lectores">Cargando eventos…</span>
        </div>
      )}

      {recurso.estado === 'error' && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="No se pudieron cargar los eventos"
          texto={recurso.mensaje}
          acciones={
            <Button pildora onClick={recurso.reintentar}>
              Reintentar
            </Button>
          }
        />
      )}

      {recurso.estado === 'listo' && recurso.datos.length === 0 && (
        <StatusMessage
          icono={IconoSinEventos}
          titulo="No hay eventos próximos"
          texto="No hay eventos próximos en este momento. Vuelve pronto: aquí aparecerá la cartelera."
        />
      )}

      {recurso.estado === 'listo' && recurso.datos.length > 0 && (
        <ul className="lista-tarjetas">
          {recurso.datos.map((evento) => (
            <li key={evento.id}>
              <EventoCard evento={evento} />
            </li>
          ))}
        </ul>
      )}

      <div className="pagina__pie">
        <ButtonLink to="/" variante="secundario" mayusculas>
          Volver al inicio
        </ButtonLink>
      </div>
    </div>
  );
}
