import { useCallback, useId, useLayoutEffect, useRef, useState, type FormEvent } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import { esCancelacion, mensajeDeError } from '../../lib/errores';
import { fusionarMensajes, MAX_CARACTERES_CHAT, streamService, type MensajeChat } from '../../services/stream.service';
import { Alert } from '../ui/Alert';
import { Button, ButtonLink } from '../ui/Button';
import { IconoAbajo, IconoEnviar, IconoEscudo } from '../ui/Icons';
import { Skeleton } from '../ui/Skeleton';
import { useSondeo } from './useSondeo';

/** Más amable que los 4 s del móvil: en el festival muchos usuarios comparten IP y el backend limita por IP. */
export const INTERVALO_CHAT_MS = 8000;
const MARGEN_FINAL_PX = 48;

const formatoHora = new Intl.DateTimeFormat('es-MX', { hour: '2-digit', minute: '2-digit' });

function horaDe(iso: string): string {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime()) ? '' : formatoHora.format(fecha);
}

function Mensaje({ mensaje }: { mensaje: MensajeChat }) {
  const hora = horaDe(mensaje.fechaEnvio);
  return (
    <li className={mensaje.esAdmin ? 'envivo-mensaje envivo-mensaje--admin' : 'envivo-mensaje'}>
      <div className="envivo-mensaje__cabecera">
        <strong className="envivo-mensaje__nombre">{mensaje.usuarioNombre || 'Anónimo'}</strong>
        {mensaje.esAdmin && (
          <span className="envivo-mensaje__admin">
            <IconoEscudo tamano={12} />
            Administrador
          </span>
        )}
        {hora && (
          <time className="envivo-mensaje__hora" dateTime={mensaje.fechaEnvio}>
            {hora}
          </time>
        )}
      </div>
      {/* Contenido de usuarios: siempre texto, nunca HTML ni enlaces. */}
      <p className="envivo-mensaje__texto">{mensaje.mensaje}</p>
    </li>
  );
}

function CajaDeEnvio({ eventoId, alEnviado }: { eventoId: string; alEnviado: (m: MensajeChat) => void }) {
  const idCampo = useId();
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const campo = useRef<HTMLInputElement>(null);

  const limpio = texto.trim();
  const puedeEnviar = limpio.length > 0 && !enviando;

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    if (!puedeEnviar) return;
    setEnviando(true);
    setError(null);
    try {
      const creado = await streamService.enviar(eventoId, limpio);
      alEnviado(creado);
      setTexto('');
    } catch (err) {
      setError(
        mensajeDeError(err, {
          404: 'Este concierto ya no está disponible para el chat.',
          400: 'No se pudo enviar el mensaje. Revisa que no esté vacío ni pase de 300 caracteres.',
        }),
      );
    } finally {
      setEnviando(false);
      campo.current?.focus();
    }
  };

  return (
    <form className="envivo-envio" onSubmit={(e) => void enviar(e)} noValidate>
      <label htmlFor={idCampo} className="solo-lectores">
        Escribe un mensaje
      </label>
      <div className="envivo-envio__fila">
        <input
          ref={campo}
          id={idCampo}
          className="campo__input envivo-envio__input"
          type="text"
          value={texto}
          maxLength={MAX_CARACTERES_CHAT}
          placeholder="Escribe un mensaje..."
          autoComplete="off"
          readOnly={enviando}
          aria-describedby={`${idCampo}-contador`}
          onChange={(e) => setTexto(e.target.value)}
        />
        <Button type="submit" cargando={enviando} disabled={!puedeEnviar} aria-label="Enviar mensaje" className="envivo-envio__boton">
          {!enviando && <IconoEnviar tamano={20} />}
        </Button>
      </div>
      <p id={`${idCampo}-contador`} className="envivo-envio__contador">
        {texto.length}/{MAX_CARACTERES_CHAT}
      </p>
      {error && <Alert tono="error">{error}</Alert>}
    </form>
  );
}

/** Chat de una transmisión: lectura con sondeo amable, y envío si hay sesión. */
export function ChatEnVivo({ eventoId }: { eventoId: string }) {
  const { autenticado } = useAuth();
  const location = useLocation();
  const [mensajes, setMensajes] = useState<MensajeChat[]>([]);
  const [cargado, setCargado] = useState(false);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);
  const [hayNuevos, setHayNuevos] = useState(false);

  const mensajesRef = useRef<MensajeChat[]>([]);
  const lista = useRef<HTMLDivElement>(null);
  // true mientras el usuario está al final: solo entonces el chat se desplaza solo.
  const alFinal = useRef(true);

  const consultar = useCallback(
    async (signal: AbortSignal) => {
      try {
        const recibidos = await streamService.chat(eventoId, signal);
        const previos = mensajesRef.current;
        const fusion = fusionarMensajes(previos, recibidos);
        if (fusion !== previos) {
          if (!alFinal.current && previos.length > 0) setHayNuevos(true);
          setMensajes(fusion);
        }
        setCargado(true);
        setErrorCarga(null);
      } catch (error) {
        if (!esCancelacion(error)) setErrorCarga(mensajeDeError(error));
        throw error;
      }
    },
    [eventoId],
  );
  const { fallos, refrescar } = useSondeo(consultar, { intervaloMs: INTERVALO_CHAT_MS });

  // Tras pintar mensajes nuevos, baja solo si el usuario ya estaba al final.
  useLayoutEffect(() => {
    mensajesRef.current = mensajes;
    const el = lista.current;
    if (el && alFinal.current) el.scrollTop = el.scrollHeight;
  }, [mensajes]);

  const alDesplazar = () => {
    const el = lista.current;
    if (!el) return;
    const alFondo = el.scrollHeight - el.scrollTop - el.clientHeight <= MARGEN_FINAL_PX;
    alFinal.current = alFondo;
    if (alFondo) setHayNuevos(false);
  };

  const irAlFinal = () => {
    const el = lista.current;
    if (el) el.scrollTop = el.scrollHeight;
    alFinal.current = true;
    setHayNuevos(false);
  };

  const alEnviado = (creado: MensajeChat) => {
    // El propio mensaje siempre se ve: baja aunque se hubiera subido a leer.
    alFinal.current = true;
    setHayNuevos(false);
    setMensajes(fusionarMensajes(mensajesRef.current, [creado]));
  };

  return (
    <section className="envivo-chat" aria-labelledby="envivo-chat-titulo">
      <h2 id="envivo-chat-titulo" className="titulo-seccion">
        Chat en vivo
      </h2>

      {!cargado && !errorCarga && (
        <div role="status" aria-label="Cargando el chat" className="envivo-chat__cargando">
          <Skeleton alto={44} />
          <Skeleton alto={44} ancho="80%" />
          <Skeleton alto={44} ancho="60%" />
        </div>
      )}

      {!cargado && errorCarga && (
        <Alert tono="error">
          <p>{errorCarga}</p>
          <Button variante="secundario" tamano="chico" pildora onClick={refrescar}>
            Reintentar
          </Button>
        </Alert>
      )}

      {cargado && (
        <div className="envivo-chat__caja">
          {fallos > 0 && <p className="envivo-chat__aviso">No se pudo actualizar el chat. Reintentando…</p>}
          <div
            ref={lista}
            className="envivo-chat__lista"
            role="log"
            aria-live="polite"
            aria-relevant="additions"
            aria-label="Mensajes del chat"
            tabIndex={0}
            onScroll={alDesplazar}
          >
            {mensajes.length === 0 ? (
              <p className="envivo-chat__vacio">Aún no hay mensajes. ¡Saluda a los demás asistentes!</p>
            ) : (
              <ul className="envivo-chat__mensajes">
                {mensajes.map((m) => (
                  <Mensaje key={m.id} mensaje={m} />
                ))}
              </ul>
            )}
          </div>
          {hayNuevos && (
            <Button variante="secundario" tamano="chico" pildora className="envivo-chat__nuevos" onClick={irAlFinal}>
              <IconoAbajo tamano={16} />
              Mensajes nuevos
            </Button>
          )}
        </div>
      )}

      {autenticado ? (
        <CajaDeEnvio eventoId={eventoId} alEnviado={alEnviado} />
      ) : (
        <div className="envivo-chat__sesion">
          <p>Inicia sesión para escribir en el chat</p>
          <ButtonLink to="/login" state={{ from: location.pathname }} variante="secundario" pildora>
            Iniciar sesión
          </ButtonLink>
        </div>
      )}
    </section>
  );
}
