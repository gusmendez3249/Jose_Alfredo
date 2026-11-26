import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/useAuth';
import { esCancelacion, mensajeDeError } from '../../lib/errores';
import { foroService, LIMITES_FORO, puedeBorrar, type ComentarioForo } from '../../services/foro.service';
import { Alert, Button, ConfirmDialog, IconoBasura, IconoComentario, IconoEnviar, Skeleton } from '../ui';
import { Avatar, CampoTextoLargo, Hace } from './piezas';

/** Máximo que devuelve el servidor: con menos, la longitud de la lista es el conteo real. */
const MAX_COMENTARIOS_SERVIDOR = 200;

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'listo' };

interface PanelComentariosProps {
  id: string;
  publicacionId: string;
  /** Se llama con el conteo real tras cargar, comentar o borrar. */
  onConteo: (conteo: number | ((actual: number) => number)) => void;
  onIrALogin: () => void;
}

export function PanelComentarios({ id, publicacionId, onConteo, onIrALogin }: PanelComentariosProps) {
  const { usuario } = useAuth();
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' });
  const [lista, setLista] = useState<ComentarioForo[]>([]);
  const [intento, setIntento] = useState(0);
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);
  const [aBorrar, setABorrar] = useState<ComentarioForo | null>(null);
  const [errorBorrar, setErrorBorrar] = useState<string | null>(null);
  const enviandoRef = useRef(false);
  const borrandoRef = useRef(false);
  const onConteoRef = useRef(onConteo);
  useEffect(() => {
    onConteoRef.current = onConteo;
  });

  useEffect(() => {
    const control = new AbortController();
    foroService
      .comentarios(publicacionId, control.signal)
      .then((datos) => {
        setLista(datos);
        setCarga({ estado: 'listo' });
        if (datos.length < MAX_COMENTARIOS_SERVIDOR) onConteoRef.current(datos.length);
      })
      .catch((error: unknown) => {
        if (!esCancelacion(error)) setCarga({ estado: 'error', mensaje: mensajeDeError(error) });
      });
    return () => control.abort();
  }, [publicacionId, intento]);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const limpio = texto.trim();
    if (enviandoRef.current) return;
    if (!limpio) {
      setErrorEnvio('El comentario está vacío');
      return;
    }
    enviandoRef.current = true;
    setEnviando(true);
    setErrorEnvio(null);
    try {
      const nuevo = await foroService.comentar(publicacionId, limpio);
      setLista((l) => (l.some((c) => c.id === nuevo.id) ? l : [...l, nuevo]));
      setTexto('');
      onConteo((n) => n + 1);
    } catch (error) {
      setErrorEnvio(mensajeDeError(error, { 404: 'Esta publicación ya no existe.' }));
    } finally {
      enviandoRef.current = false;
      setEnviando(false);
    }
  }

  async function borrar() {
    const comentario = aBorrar;
    setABorrar(null);
    if (!comentario || borrandoRef.current) return;
    borrandoRef.current = true;
    setErrorBorrar(null);
    try {
      await foroService.eliminarComentario(comentario.id);
      setLista((l) => l.filter((c) => c.id !== comentario.id));
      onConteo((n) => Math.max(0, n - 1));
    } catch (error) {
      setErrorBorrar(mensajeDeError(error));
    } finally {
      borrandoRef.current = false;
    }
  }

  return (
    <section id={id} className="comunidad-comentarios" aria-label="Comentarios">
      {carga.estado === 'cargando' && (
        <div role="status" className="comunidad-comentarios__lista">
          <span className="solo-lectores">Cargando comentarios…</span>
          {[0, 1].map((n) => (
            <div key={n} className="comunidad-comentario" aria-hidden="true">
              <Skeleton alto={32} ancho={32} redondo />
              <div className="comunidad-comentario__cuerpo">
                <Skeleton alto={12} ancho="40%" />
                <Skeleton alto={14} ancho="85%" />
              </div>
            </div>
          ))}
        </div>
      )}

      {carga.estado === 'error' && (
        <div className="comunidad-comentarios__error" role="alert">
          <p className="meta">No se pudieron cargar los comentarios. {carga.mensaje}</p>
          <Button variante="secundario" tamano="chico" onClick={() => {
              setCarga({ estado: 'cargando' });
              setIntento((n) => n + 1);
            }}>
            Reintentar
          </Button>
        </div>
      )}

      {carga.estado === 'listo' && lista.length === 0 && (
        <p className="comunidad-comentarios__vacio">
          <IconoComentario tamano={20} />
          <span>
            Aún no hay comentarios.
            <br />
            ¡Sé el primero en responder!
          </span>
        </p>
      )}

      {carga.estado === 'listo' && lista.length > 0 && (
        <ul className="comunidad-comentarios__lista">
          {lista.map((c) => (
            <li key={c.id} className="comunidad-comentario">
              <Avatar nombre={c.autorNombre} chico />
              <div className="comunidad-comentario__cuerpo">
                <p className="comunidad-comentario__cabecera">
                  <span className="comunidad-autor">{c.autorNombre}</span>
                  <span className="meta">
                    <Hace iso={c.createdAt} />
                  </span>
                </p>
                <p className="comunidad-texto-usuario">{c.texto}</p>
              </div>
              {puedeBorrar(usuario, c.autorId) && (
                <button
                  type="button"
                  className="comunidad-icono-boton"
                  onClick={() => setABorrar(c)}
                  aria-label="Eliminar comentario"
                >
                  <IconoBasura tamano={18} />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {errorBorrar && <Alert tono="error">{errorBorrar}</Alert>}

      {usuario ? (
        <form className="comunidad-comentarios__form" onSubmit={enviar} noValidate>
          <CampoTextoLargo
            etiqueta="Escribe un comentario"
            etiquetaOculta
            placeholder="Escribe un comentario…"
            rows={2}
            value={texto}
            maxLength={LIMITES_FORO.comentario}
            onChange={(e) => {
              setTexto(e.target.value);
              setErrorEnvio(null);
            }}
            disabled={enviando}
            error={errorEnvio ?? undefined}
          />
          <Button type="submit" cargando={enviando} disabled={!texto.trim()} aria-label="Enviar comentario">
            {!enviando && <IconoEnviar tamano={20} />}
          </Button>
        </form>
      ) : (
        <Button variante="secundario" onClick={onIrALogin}>
          Inicia sesión para comentar
        </Button>
      )}

      <ConfirmDialog
        abierto={aBorrar !== null}
        titulo="¿Eliminar comentario?"
        texto="Esta acción no se puede deshacer."
        textoConfirmar="Eliminar"
        peligro
        onConfirmar={() => void borrar()}
        onCancelar={() => setABorrar(null)}
      />
    </section>
  );
}
