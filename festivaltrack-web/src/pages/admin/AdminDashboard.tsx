import { AdminNav } from '../../components/admin/AdminNav';
import { estadoTransmision, haceTiempo, ultimasPublicaciones } from '../../components/admin/panel/lecturas';
import { SESION_TERMINO } from '../../components/admin/panel/publicar';
import { useAuth } from '../../auth/useAuth';
import { Button, ButtonLink } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { IconoAlerta, IconoEnVivo } from '../../components/ui/Icons';
import { Skeleton } from '../../components/ui/Skeleton';
import { useRecurso, type EstadoRecurso } from '../../hooks/useRecurso';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { proximos } from '../../lib/proximos';
import { cancionesService } from '../../services/canciones.service';
import { eventosService } from '../../services/eventos.service';
import { usuariosService } from '../../services/usuarios.service';
import '../../styles/admin-panel.css';

/** "…" mientras carga, "—" si falla, el número si llegó. */
function cifra(r: EstadoRecurso<number>): string {
  return r.estado === 'cargando' ? '…' : r.estado === 'error' ? '—' : String(r.datos);
}

function Cifra({ etiqueta, recurso }: { etiqueta: string; recurso: EstadoRecurso<number> }) {
  return (
    <Card como="div" className="admin-panel-cifra">
      <p className="admin-panel-cifra__valor">{cifra(recurso)}</p>
      <p className="meta">{etiqueta}</p>
    </Card>
  );
}

export default function AdminDashboard() {
  useTituloPagina('Administración');
  const { usuario } = useAuth();
  const mensajes = { 401: SESION_TERMINO };

  const canciones = useRecurso(async (s) => (await cancionesService.listar(s)).length, 'resumen-canciones');
  const conciertos = useRecurso(async (s) => proximos(await eventosService.listar(s)).length, 'resumen-conciertos');
  const usuarios = useRecurso(async (s) => (await usuariosService.listar(s)).length, 'resumen-usuarios', mensajes);
  const transmision = useRecurso(estadoTransmision, 'resumen-transmision');
  const foro = useRecurso(ultimasPublicaciones, 'resumen-foro');

  return (
    <div className="pagina">
      <AdminNav />
      <header className="pagina__cabecera">
        <h1 className="titulo-pagina">¡Bienvenido, {usuario?.nombre ?? 'administrador'}!</h1>
        <p className="meta">Gestiona el contenido del festival y la transmisión en vivo de los conciertos.</p>
      </header>

      <Card como="section" destacada aria-labelledby="admin-panel-vivo" className="admin-panel-vivo">
        <IconoEnVivo tamano={28} className="admin-panel-vivo__icono" />
        <div>
          <p className="eyebrow">Transmisión en vivo</p>
          <h2 id="admin-panel-vivo" className="solo-lectores">
            Estado de la transmisión
          </h2>
          <p className="admin-panel-vivo__estado" role="status">
            {transmision.estado === 'cargando' && 'Consultando…'}
            {transmision.estado === 'error' && 'Sin conexión con el servidor'}
            {transmision.estado === 'listo' &&
              (transmision.datos.isLive
                ? `En vivo: ${transmision.datos.eventoNombre ?? 'concierto'}`
                : 'Sin transmisión')}
          </p>
          <p className="meta">La transmisión se emite desde la app móvil; en la web solo se puede ver su estado.</p>
        </div>
      </Card>

      <section className="seccion" aria-labelledby="admin-panel-cifras">
        <h2 id="admin-panel-cifras" className="titulo-seccion">
          Resumen del festival
        </h2>
        <div className="admin-panel-cifras">
          <Cifra etiqueta="Canciones en el catálogo" recurso={canciones} />
          <Cifra etiqueta="Próximos conciertos" recurso={conciertos} />
          <Cifra etiqueta="Usuarios registrados" recurso={usuarios} />
        </div>
      </section>

      <section className="seccion" aria-labelledby="admin-panel-rapida">
        <h2 id="admin-panel-rapida" className="titulo-seccion">
          Gestión rápida
        </h2>
        <div className="admin-panel-accesos">
          <Card>
            <h3 className="admin-panel-acceso__titulo">Catálogo de canciones</h3>
            <p className="meta">Sube audio y portada y publícalos en el catálogo.</p>
            <ButtonLink to="/admin/canciones" mayusculas tamano="chico">
              Subir canción
            </ButtonLink>
          </Card>
          <Card>
            <h3 className="admin-panel-acceso__titulo">Conciertos y repertorio</h3>
            <p className="meta">Crea conciertos y arma lo que suena en cada uno.</p>
            <ButtonLink to="/admin/conciertos" mayusculas tamano="chico">
              Gestionar conciertos
            </ButtonLink>
          </Card>
          <Card>
            <h3 className="admin-panel-acceso__titulo">Usuarios y roles</h3>
            <p className="meta">Nombra o quita administradores.</p>
            <ButtonLink to="/admin/usuarios" mayusculas tamano="chico">
              Gestionar usuarios
            </ButtonLink>
          </Card>
        </div>
      </section>

      <section className="seccion" aria-labelledby="admin-panel-actividad">
        <h2 id="admin-panel-actividad" className="titulo-seccion">
          Actividad reciente de la comunidad
        </h2>
        {foro.estado === 'cargando' && (
          <div role="status" aria-label="Cargando actividad" className="lista-tarjetas">
            <Skeleton alto={56} />
            <Skeleton alto={56} />
          </div>
        )}
        {foro.estado === 'error' && (
          <div className="admin-panel-error" role="alert">
            <IconoAlerta tamano={20} />
            <span>{foro.mensaje}</span>
            <Button variante="fantasma" tamano="chico" onClick={foro.reintentar}>
              Reintentar
            </Button>
          </div>
        )}
        {foro.estado === 'listo' && foro.datos.length === 0 && (
          <p className="meta">Todavía no hay publicaciones en la galería.</p>
        )}
        {foro.estado === 'listo' && foro.datos.length > 0 && (
          <ul className="lista-tarjetas">
            {foro.datos.map((p) => (
              <li key={p.id} className="tarjeta admin-panel-publicacion">
                <p className="admin-panel-texto">
                  {p.autorNombre}: {p.titulo?.trim() || p.contenido}
                </p>
                <p className="meta">
                  {haceTiempo(p.createdAt)} · {p.likes} me gusta
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
