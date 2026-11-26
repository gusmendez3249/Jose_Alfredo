import { useEffect, useId, useRef, useState, type CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import '../../styles/audio.css';
import { IconoAbajo, IconoAnterior, IconoArriba, IconoCerrar, IconoPausa, IconoPlay, IconoSiguiente } from '../ui/Icons';
import { clases } from '../ui/clases';
import { Ecualizador, IconoAleatorio, Portada } from './Portada';
import { estaDisponible, formatearTiempo, haySiguiente, pistaActual, reproductor, useReproductor } from './reproductor';

/**
 * Mini reproductor fijo abajo (encima de la barra de navegación del móvil) con un panel
 * desplegable: búsqueda, controles completos y la cola. Va en el flujo del documento con
 * `position: sticky`, así reserva su propio alto y nunca tapa el final de las páginas; en las
 * de mapa (que ocupan el alto de la pantalla) simplemente le quita ese alto al mapa.
 */
export function ReproductorGlobal() {
  const e = useReproductor();
  const pista = pistaActual(e);
  const [abierto, setAbierto] = useState(false);
  const idPanel = useId();
  const botonPanel = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!abierto) return;
    const alPulsar = (ev: KeyboardEvent) => {
      if (ev.key !== 'Escape') return;
      setAbierto(false);
      botonPanel.current?.focus();
    };
    document.addEventListener('keydown', alPulsar);
    return () => document.removeEventListener('keydown', alPulsar);
  }, [abierto]);

  if (!pista) return null;

  const disponible = estaDisponible(e, pista);
  const siguiente = haySiguiente(e);
  const sonando = e.reproduciendo && !e.esperando;
  const progreso: CSSProperties = { width: `${e.duracion > 0 ? Math.min(100, (e.posicion / e.duracion) * 100) : 0}%` };
  const etiquetaPlay = e.reproduciendo ? `Pausar «${pista.titulo}»` : `Reproducir «${pista.titulo}»`;
  const concierto = e.contexto;

  return (
    <section className="reproductor" aria-label="Reproductor de demos">
      <div className="reproductor-barra">
        <span className="reproductor-barra__progreso" style={progreso} aria-hidden="true" />
        <div className="reproductor-barra__interior">
          <button
            ref={botonPanel}
            type="button"
            className="reproductor-info"
            aria-expanded={abierto}
            aria-controls={idPanel}
            aria-label={`${abierto ? 'Cerrar' : 'Abrir'} el reproductor: ${pista.titulo}, ${pista.artista}`}
            onClick={() => setAbierto((a) => !a)}
          >
            <Portada src={pista.portada} />
            <span className="reproductor-info__textos">
              <span className="reproductor-info__titulo">{pista.titulo}</span>
              <span className="reproductor-info__sub">
                {pista.artista}
                {concierto ? ` · ${concierto.nombre}` : ''}
              </span>
            </span>
            <Ecualizador activo={sonando} />
            {abierto ? <IconoAbajo tamano={18} /> : <IconoArriba tamano={18} />}
          </button>

          <div className="reproductor-controles">
            <button
              type="button"
              className="reproductor-boton reproductor-boton--solo-ancho"
              aria-label="Anterior"
              onClick={reproductor.anterior}
            >
              <IconoAnterior tamano={20} />
            </button>
            <button
              type="button"
              className="reproductor-boton reproductor-boton--principal"
              aria-label={etiquetaPlay}
              disabled={!disponible}
              onClick={reproductor.alternar}
            >
              {e.reproduciendo ? <IconoPausa tamano={22} /> : <IconoPlay tamano={22} />}
            </button>
            <button
              type="button"
              className="reproductor-boton"
              aria-label="Siguiente"
              disabled={!siguiente}
              onClick={reproductor.siguiente}
            >
              <IconoSiguiente tamano={20} />
            </button>
            <button
              type="button"
              className="reproductor-boton"
              aria-label="Cerrar reproductor"
              onClick={() => {
                setAbierto(false);
                reproductor.cerrar();
              }}
            >
              <IconoCerrar tamano={20} />
            </button>
          </div>
        </div>
        <p className="reproductor-aviso" role="status">
          {e.aviso}
        </p>
      </div>

      <div id={idPanel} className="reproductor-panel" hidden={!abierto}>
        {abierto && <PanelReproductor />}
      </div>
    </section>
  );
}

/** Contenido del panel desplegado: "REPRODUCIENDO DEMO", búsqueda, controles y cola. */
function PanelReproductor() {
  const e = useReproductor();
  const pista = pistaActual(e);
  const idBusqueda = useId();
  if (!pista) return null;

  const disponible = estaDisponible(e, pista);
  const concierto = e.contexto;
  const posicion = Math.min(e.posicion, e.duracion);

  return (
    <div className="reproductor-panel__interior">
      <div className="reproductor-panel__cabecera">
        <Portada src={pista.portada} className="reproductor-portada--grande" />
        <div className="reproductor-panel__textos">
          <p className="eyebrow">Reproduciendo demo</p>
          <p className="reproductor-panel__titulo">{pista.titulo}</p>
          <p className="meta">{pista.artista}</p>
          {concierto?.eventoId ? (
            <Link className="reproductor-panel__concierto" to={`/events/${encodeURIComponent(concierto.eventoId)}`}>
              {concierto.nombre}
            </Link>
          ) : (
            concierto && <p className="meta">{concierto.nombre}</p>
          )}
        </div>
      </div>

      <div className="reproductor-busqueda">
        <label htmlFor={idBusqueda} className="solo-lectores">
          Posición de la demo
        </label>
        <span className="reproductor-busqueda__tiempo">{formatearTiempo(posicion)}</span>
        <input
          id={idBusqueda}
          type="range"
          className="reproductor-busqueda__rango"
          min={0}
          max={Math.max(1, Math.floor(e.duracion))}
          step={1}
          value={Math.floor(posicion)}
          disabled={!disponible}
          aria-valuetext={`${formatearTiempo(posicion)} de ${formatearTiempo(e.duracion)}`}
          onChange={(ev) => reproductor.buscar(Number(ev.target.value))}
        />
        <span className="reproductor-busqueda__tiempo">{formatearTiempo(e.duracion)}</span>
      </div>

      <div className="reproductor-panel__controles">
        <button
          type="button"
          className={clases('reproductor-boton', e.aleatorio && 'reproductor-boton--activo')}
          aria-label="Orden aleatorio"
          aria-pressed={e.aleatorio}
          disabled={e.original.length < 2}
          onClick={reproductor.alternarAleatorio}
        >
          <IconoAleatorio tamano={20} />
        </button>
        <button type="button" className="reproductor-boton" aria-label="Anterior" onClick={reproductor.anterior}>
          <IconoAnterior tamano={24} />
        </button>
        <button
          type="button"
          className="reproductor-boton reproductor-boton--principal reproductor-boton--grande"
          aria-label={e.reproduciendo ? 'Pausar' : 'Reproducir'}
          disabled={!disponible}
          onClick={reproductor.alternar}
        >
          {e.reproduciendo ? <IconoPausa tamano={28} /> : <IconoPlay tamano={28} />}
        </button>
        <button
          type="button"
          className="reproductor-boton"
          aria-label="Siguiente"
          disabled={!haySiguiente(e)}
          onClick={reproductor.siguiente}
        >
          <IconoSiguiente tamano={24} />
        </button>
      </div>

      <div className="reproductor-cola">
        <div className="reproductor-cola__cabecera">
          <p className="eyebrow">
            {e.aleatorio ? 'Cola · orden aleatorio' : concierto?.eventoId ? 'Cola del concierto' : 'Cola de reproducción'}
          </p>
          <p className="meta">
            {e.indice + 1} de {e.cola.length}
          </p>
        </div>
        <ol className="reproductor-cola__lista">
          {e.cola.map((p, i) => {
            const actual = i === e.indice;
            const ok = estaDisponible(e, p);
            return (
              <li key={`${p.id}-${i}`}>
                <button
                  type="button"
                  className={clases('reproductor-cola__fila', actual && 'reproductor-cola__fila--actual')}
                  aria-current={actual || undefined}
                  disabled={!ok}
                  onClick={() => (actual ? reproductor.alternar() : reproductor.seleccionar(i))}
                >
                  <span className="reproductor-cola__numero">{actual ? <Ecualizador activo={e.reproduciendo && !e.esperando} /> : i + 1}</span>
                  <span className="reproductor-cola__textos">
                    <span className="reproductor-cola__titulo">{p.titulo}</span>
                    <span className="meta">{ok ? p.artista : 'No disponible'}</span>
                  </span>
                </button>
              </li>
            );
          })}
        </ol>
      </div>
    </div>
  );
}
