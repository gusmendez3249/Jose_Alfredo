import axios from 'axios';
import { useCallback, useId, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import type { EstadoNavegacionLogin } from '../auth/rutas';
import { Compositor } from '../components/comunidad/Compositor';
import { altDeFoto } from '../components/comunidad/etiquetas';
import { TarjetaPublicacion } from '../components/comunidad/TarjetaPublicacion';
import { useForo } from '../components/comunidad/useForo';
import { VisorImagen } from '../components/comunidad/VisorImagen';
import {
  Alert,
  Button,
  ConfirmDialog,
  IconoAlerta,
  IconoCerrar,
  IconoComunidad,
  IconoMas,
  Skeleton,
  StatusMessage,
  clases,
} from '../components/ui';
import { useTituloPagina } from '../hooks/useTituloPagina';
import { mensajeDeError } from '../lib/errores';
import {
  foroService,
  puedeBorrar,
  TIPOS_PUBLICACION,
  type Publicacion,
  type TipoPublicacion,
} from '../services/foro.service';
import '../styles/comunidad.css';

const FILTROS: ReadonlyArray<{ valor: TipoPublicacion | null; etiqueta: string }> = [
  { valor: null, etiqueta: 'Todo' },
  { valor: 'EXPERIENCIA', etiqueta: 'Experiencias' },
  { valor: 'RECOMENDACION', etiqueta: 'Recomendaciones' },
  { valor: 'FOTO', etiqueta: 'Fotos' },
];

function leerTipo(valor: string | null): TipoPublicacion | null {
  return valor && (TIPOS_PUBLICACION as readonly string[]).includes(valor) ? (valor as TipoPublicacion) : null;
}

type Aviso = { tono: 'error' | 'exito'; texto: string };

function PublicacionSkeleton() {
  return (
    <div className="comunidad-publicacion" aria-hidden="true">
      <div className="comunidad-publicacion__cabecera">
        <Skeleton alto={40} ancho={40} redondo />
        <div className="comunidad-publicacion__quien">
          <Skeleton alto={14} ancho="45%" />
          <Skeleton alto={12} ancho="25%" />
        </div>
      </div>
      <Skeleton alto={180} />
      <Skeleton alto={14} ancho="90%" />
      <Skeleton alto={14} ancho="70%" />
    </div>
  );
}

export default function Community() {
  useTituloPagina('Comunidad');
  const { usuario } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [parametros, setParametros] = useSearchParams();
  const tipo = leerTipo(parametros.get('tipo'));
  const foro = useForo(tipo);
  const { actualizar, quitar, agregar } = foro;

  const [compositorAbierto, setCompositorAbierto] = useState(false);
  const [aviso, setAviso] = useState<Aviso | null>(null);
  const [aBorrar, setABorrar] = useState<Publicacion | null>(null);
  const [visor, setVisor] = useState<Publicacion | null>(null);
  const [likesPendientes, setLikesPendientes] = useState<ReadonlySet<string>>(new Set());
  const pendientesRef = useRef(new Set<string>());
  const borrandoRef = useRef(false);
  const idCompartir = useId();

  const irALogin = useCallback(() => {
    const estado: EstadoNavegacionLogin = { from: `${location.pathname}${location.search}` };
    navigate('/login', { state: estado });
  }, [location.pathname, location.search, navigate]);

  function elegirFiltro(valor: TipoPublicacion | null) {
    const nuevos = new URLSearchParams(parametros);
    if (valor) nuevos.set('tipo', valor);
    else nuevos.delete('tipo');
    setParametros(nuevos, { replace: true });
  }

  function abrirCompositor() {
    if (!usuario) {
      irALogin();
      return;
    }
    setAviso(null);
    setCompositorAbierto(true);
  }

  function cerrarCompositor() {
    setCompositorAbierto(false);
    // El foco vuelve al botón que abrió el compositor.
    requestAnimationFrame(() => document.getElementById(idCompartir)?.focus());
  }

  function alPublicar(p: Publicacion) {
    // Solo se inserta si encaja con el filtro activo; si no, el aviso basta.
    if (tipo === null || tipo === p.tipo) agregar(p);
    setCompositorAbierto(false);
    setAviso({ tono: 'exito', texto: '¡Publicado! Gracias por compartir' });
  }

  function marcarPendiente(id: string, pendiente: boolean) {
    if (pendiente) pendientesRef.current.add(id);
    else pendientesRef.current.delete(id);
    setLikesPendientes(new Set(pendientesRef.current));
  }

  async function alternarLike(p: Publicacion) {
    if (!usuario) {
      irALogin();
      return;
    }
    if (pendientesRef.current.has(p.id)) return;
    marcarPendiente(p.id, true);
    const previo = { likedByMe: p.likedByMe, likes: p.likes };
    const deseado = !p.likedByMe;
    actualizar(p.id, (x) => ({ ...x, likedByMe: deseado, likes: Math.max(0, x.likes + (deseado ? 1 : -1)) }));
    try {
      const final = await foroService.alternarLike(p.id);
      actualizar(p.id, (x) => ({ ...x, likedByMe: final.liked, likes: final.likes }));
    } catch {
      actualizar(p.id, (x) => ({ ...x, ...previo }));
      setAviso({ tono: 'error', texto: 'No se pudo registrar tu me gusta' });
    } finally {
      marcarPendiente(p.id, false);
    }
  }

  async function confirmarBorrado() {
    const p = aBorrar;
    setABorrar(null);
    if (!p || borrandoRef.current) return;
    borrandoRef.current = true;
    try {
      await foroService.eliminar(p.id);
      quitar(p.id);
      setAviso({ tono: 'exito', texto: 'Publicación eliminada' });
    } catch (error) {
      // Si ya no existe, se quita igual de la lista.
      if (axios.isAxiosError(error) && error.response?.status === 404) quitar(p.id);
      setAviso({ tono: 'error', texto: mensajeDeError(error) });
    } finally {
      borrandoRef.current = false;
    }
  }

  const listo = foro.estado === 'listo';
  const hayPublicaciones = foro.publicaciones.length > 0;

  return (
    <div className="pagina pagina--lectura comunidad">
      <header className="comunidad-cabecera">
        <div className="pagina__cabecera">
          <p className="eyebrow">Galería interactiva</p>
          <h1 className="titulo-pagina">Comunidad del Festival</h1>
          <p className="meta">Experiencias, recomendaciones y fotos de quienes viven el festival.</p>
        </div>
        <div className="comunidad-cabecera__acciones">
          <Button variante="secundario" onClick={foro.recargar} disabled={foro.estado === 'cargando'}>
            Actualizar
          </Button>
          <Button id={idCompartir} mayusculas onClick={abrirCompositor} disabled={compositorAbierto}>
            <IconoMas tamano={20} />
            Compartir
          </Button>
        </div>
      </header>

      {compositorAbierto && <Compositor onPublicado={alPublicar} onCancelar={cerrarCompositor} />}

      {aviso && (
        <div className="comunidad-aviso">
          <Alert tono={aviso.tono} className="comunidad-aviso__alerta">
            {aviso.texto}
          </Alert>
          <button type="button" className="comunidad-icono-boton" onClick={() => setAviso(null)} aria-label="Cerrar aviso">
            <IconoCerrar tamano={18} />
          </button>
        </div>
      )}

      <div className="comunidad-filtros" role="group" aria-label="Filtrar publicaciones">
        {FILTROS.map((f) => (
          <button
            key={f.etiqueta}
            type="button"
            className={clases('comunidad-chip', tipo === f.valor && 'comunidad-chip--activo')}
            aria-pressed={tipo === f.valor}
            onClick={() => elegirFiltro(f.valor)}
          >
            {f.etiqueta}
          </button>
        ))}
      </div>

      {foro.estado === 'cargando' && (
        <div role="status" className="comunidad-lista">
          <span className="solo-lectores">Cargando publicaciones…</span>
          <PublicacionSkeleton />
          <PublicacionSkeleton />
          <PublicacionSkeleton />
        </div>
      )}

      {foro.estado === 'error' && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="Sin conexión con el foro"
          texto={
            <>
              No pudimos cargar el foro. Revisa tu conexión e intenta de nuevo.
              <br />
              {foro.error}
            </>
          }
          acciones={
            <Button pildora onClick={foro.recargar}>
              Reintentar
            </Button>
          }
        />
      )}

      {listo && !hayPublicaciones && (
        <StatusMessage
          icono={IconoComunidad}
          titulo={tipo === 'FOTO' ? 'Aún no hay fotos' : 'Aún no hay publicaciones'}
          texto="Sé el primero en compartir tu experiencia, una recomendación o una foto del festival."
          acciones={
            !compositorAbierto && (
              <Button pildora onClick={abrirCompositor}>
                Compartir ahora
              </Button>
            )
          }
        />
      )}

      {listo && hayPublicaciones && (
        <>
          <ul className="comunidad-lista" aria-label="Publicaciones">
            {foro.publicaciones.map((p) => (
              <li key={p.id}>
                <TarjetaPublicacion
                  publicacion={p}
                  puedeBorrar={puedeBorrar(usuario, p.autorId)}
                  likePendiente={likesPendientes.has(p.id)}
                  onLike={() => void alternarLike(p)}
                  onBorrar={() => setABorrar(p)}
                  onAbrirImagen={() => setVisor(p)}
                  onConteoComentarios={(c) =>
                    actualizar(p.id, (x) => ({ ...x, comentarios: typeof c === 'function' ? c(x.comentarios) : c }))
                  }
                  onIrALogin={irALogin}
                />
              </li>
            ))}
          </ul>

          <div className="comunidad-fin">
            {foro.errorMas && <Alert tono="error">No se pudieron cargar más publicaciones. {foro.errorMas}</Alert>}
            {foro.cursor ? (
              <Button variante="secundario" onClick={foro.cargarMas} cargando={foro.cargandoMas}>
                {foro.errorMas ? 'Reintentar' : 'Cargar más'}
              </Button>
            ) : (
              foro.publicaciones.length > 3 && (
                <p className="meta">Has llegado al final · gracias por ser parte de la comunidad</p>
              )
            )}
          </div>
        </>
      )}

      <ConfirmDialog
        abierto={aBorrar !== null}
        titulo="¿Eliminar publicación?"
        texto="Esta acción no se puede deshacer."
        textoConfirmar="Eliminar"
        peligro
        onConfirmar={() => void confirmarBorrado()}
        onCancelar={() => setABorrar(null)}
      />

      <VisorImagen url={visor?.imagenUrl ?? null} alt={visor ? altDeFoto(visor) : ''} onCerrar={() => setVisor(null)} />
    </div>
  );
}
