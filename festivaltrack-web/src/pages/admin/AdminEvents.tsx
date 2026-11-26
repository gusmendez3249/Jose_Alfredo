import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AdminNav } from '../../components/admin/AdminNav';
import { CampoSeleccion } from '../../components/admin/conciertos/Campos';
import { EstadoConcierto } from '../../components/admin/conciertos/EstadoConcierto';
import { ESTADOS_EN_ORDEN, ETIQUETA_ESTADO, sePuedeCancelar } from '../../components/admin/conciertos/estados';
import { formatearFechaLista } from '../../components/admin/conciertos/fechaMexico';
import { MENSAJES_ADMIN, mensajeAlCancelarConcierto } from '../../components/admin/conciertos/mensajes';
import { Alert, Button, ButtonLink, ConfirmDialog, Field, IconoAlerta, IconoSinEventos, Skeleton, StatusMessage } from '../../components/ui';
import { useRecurso } from '../../hooks/useRecurso';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { adminEventosService } from '../../services/adminEventos.service';
import { eventosService, lugarDe } from '../../services/eventos.service';
import type { EstadoEvento, Evento } from '../../services/tipos';
import '../../styles/admin-conciertos.css';

type Aviso = { tono: 'exito' | 'error'; texto: string };

/** Minúsculas y sin acentos, para que "jose" encuentre "José". */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

function lugarCompleto(evento: Evento): string {
  const partes = [lugarDe(evento), (evento.ubicacion ?? '').trim()].filter((p): p is string => Boolean(p));
  return [...new Set(partes)].join(' · ') || 'Sin lugar';
}

export default function AdminEvents() {
  useTituloPagina('Conciertos (administración)');
  const location = useLocation();
  const navigate = useNavigate();
  const recurso = useRecurso((signal) => eventosService.listar(signal), 'admin-eventos', MENSAJES_ADMIN);

  // El aviso de "guardado" llega por el estado de la navegación; se toma una vez y se borra del historial.
  const [aviso, setAviso] = useState<Aviso | null>(() => {
    const texto = (location.state as { aviso?: unknown } | null)?.aviso;
    return typeof texto === 'string' ? { tono: 'exito', texto } : null;
  });
  useEffect(() => {
    if ((location.state as { aviso?: unknown } | null)?.aviso !== undefined) {
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, location.state, navigate]);

  const [busqueda, setBusqueda] = useState('');
  const [estado, setEstado] = useState<'' | EstadoEvento>('');
  const [porCancelar, setPorCancelar] = useState<Evento | null>(null);
  const [cancelando, setCancelando] = useState<string | null>(null);

  const todos = recurso.estado === 'listo' ? recurso.datos : null;
  const visibles = useMemo(() => {
    if (!todos) return [];
    const consulta = normalizar(busqueda.trim());
    return todos.filter((e) => (!estado || e.estado === estado) && (!consulta || normalizar(e.nombre).includes(consulta)));
  }, [todos, busqueda, estado]);

  async function confirmarCancelacion() {
    const evento = porCancelar;
    if (!evento) return;
    setPorCancelar(null);
    setCancelando(evento.id);
    setAviso(null);
    try {
      await adminEventosService.cancelar(evento.id);
      setAviso({ tono: 'exito', texto: `«${evento.nombre}» quedó cancelado.` });
      recurso.reintentar();
    } catch (error) {
      setAviso({ tono: 'error', texto: mensajeAlCancelarConcierto(error) });
    } finally {
      setCancelando(null);
    }
  }

  return (
    <div className="pagina">
      <AdminNav />
      <header className="admin-conciertos-cabecera">
        <div className="pagina__cabecera">
          <h1 className="titulo-pagina">Conciertos</h1>
          <p className="meta">Crea, edita y cancela conciertos, y arma el repertorio de cada uno. Las horas son del centro de México.</p>
        </div>
        <ButtonLink to="/admin/conciertos/nuevo" mayusculas>
          Nuevo concierto
        </ButtonLink>
      </header>

      {aviso && <Alert tono={aviso.tono}>{aviso.texto}</Alert>}

      {recurso.estado === 'cargando' && (
        <div role="status" aria-label="Cargando conciertos" className="pila">
          <Skeleton alto={56} />
          <Skeleton alto={56} />
          <Skeleton alto={56} />
          <span className="solo-lectores">Cargando conciertos…</span>
        </div>
      )}

      {recurso.estado === 'error' && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="No se pudieron cargar los conciertos"
          texto={recurso.mensaje}
          acciones={
            <Button pildora onClick={recurso.reintentar}>
              Reintentar
            </Button>
          }
        />
      )}

      {todos && todos.length === 0 && (
        <StatusMessage
          icono={IconoSinEventos}
          titulo="No hay conciertos registrados."
          texto="Crea el primero con el botón «Nuevo concierto»."
        />
      )}

      {todos && todos.length > 0 && (
        <>
          <div className="admin-conciertos-filtros">
            <Field
              etiqueta="Buscar por nombre"
              type="search"
              placeholder="Buscar por nombre..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              autoComplete="off"
            />
            <CampoSeleccion
              etiqueta="Estado"
              value={estado}
              onChange={(e) => setEstado(e.target.value as '' | EstadoEvento)}
            >
              <option value="">Todos</option>
              {ESTADOS_EN_ORDEN.map((e) => (
                <option key={e} value={e}>
                  {ETIQUETA_ESTADO[e]}
                </option>
              ))}
            </CampoSeleccion>
          </div>

          <p className="meta" role="status">
            {visibles.length === todos.length
              ? `${todos.length} ${todos.length === 1 ? 'concierto' : 'conciertos'}`
              : `${visibles.length} de ${todos.length} conciertos`}
          </p>

          {visibles.length === 0 ? (
            <StatusMessage icono={IconoSinEventos} titulo="Ningún concierto coincide con la búsqueda." />
          ) : (
            <table className="admin-conciertos-tabla">
              <thead>
                <tr>
                  <th scope="col">Concierto</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Fecha (hora de México)</th>
                  <th scope="col">Lugar</th>
                  <th scope="col">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {visibles.map((evento) => (
                  <tr key={evento.id}>
                    <th scope="row" data-etiqueta="Concierto" className="admin-conciertos-nombre">
                      {evento.nombre}
                    </th>
                    <td data-etiqueta="Estado">
                      <EstadoConcierto estado={evento.estado} />
                    </td>
                    <td data-etiqueta="Fecha">{formatearFechaLista(evento.fechaHora) || 'Fecha no válida'}</td>
                    <td data-etiqueta="Lugar">{lugarCompleto(evento)}</td>
                    <td data-etiqueta="Acciones">
                      <div className="admin-conciertos-botones">
                        <ButtonLink
                          to={`/admin/conciertos/${encodeURIComponent(evento.id)}`}
                          variante="secundario"
                          tamano="chico"
                          aria-label={`Editar ${evento.nombre}`}
                        >
                          Editar
                        </ButtonLink>
                        <ButtonLink
                          to={`/admin/conciertos/${encodeURIComponent(evento.id)}/repertorio`}
                          variante="secundario"
                          tamano="chico"
                          aria-label={`Repertorio de ${evento.nombre}`}
                        >
                          Repertorio
                        </ButtonLink>
                        {sePuedeCancelar(evento.estado) && (
                          <Button
                            variante="fantasma"
                            tamano="chico"
                            className="admin-conciertos-cancelar"
                            aria-label={`Cancelar ${evento.nombre}`}
                            cargando={cancelando === evento.id}
                            disabled={cancelando !== null}
                            onClick={() => setPorCancelar(evento)}
                          >
                            Cancelar
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      <ConfirmDialog
        abierto={porCancelar !== null}
        peligro
        titulo="¿Cancelar este concierto?"
        texto={
          porCancelar
            ? `«${porCancelar.nombre}» pasará a estado Cancelado y dejará de estar disponible para el público. No se borra el registro (los boletos vendidos se conservan) y desde este panel no se puede deshacer.`
            : ''
        }
        textoConfirmar="Cancelar concierto"
        textoCancelar="Volver"
        onConfirmar={() => void confirmarCancelacion()}
        onCancelar={() => setPorCancelar(null)}
      />
    </div>
  );
}
