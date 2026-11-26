import { useCallback, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import FichaDestino from '../components/mapa/FichaDestino';
import MapaFestival, { type MarcadorMapa, type TrazoMapa } from '../components/mapa/MapaFestival';
import SelectorDestinos from '../components/mapa/SelectorDestinos';
import {
  CENTRO_DOLORES,
  construirDestinos,
  destinoDesdeParametro,
  RADIO_FESTIVAL_M,
} from '../components/mapa/destinos';
import { useRutaAPie } from '../components/mapa/useRutaAPie';
import { Button } from '../components/ui/Button';
import { IconoAbajo, IconoArriba } from '../components/ui/Icons';
import { Spinner } from '../components/ui/Spinner';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';
import { eventosService } from '../services/eventos.service';
import { distanciaEnLinea } from '../services/rutas.service';
import { sitiosService } from '../services/sitios.service';
import '../styles/mapa.css';

/** Mapa del festival y "GPS del festival": destinos fijos, sitios históricos y conciertos próximos. */
export default function FestivalMap() {
  useTituloPagina('Mapa del festival');
  const [params, setParams] = useSearchParams();

  const sitios = useRecurso((signal) => sitiosService.listar(signal), 'sitios');
  const eventos = useRecurso((signal) => eventosService.listar(signal), 'eventos');

  const sitiosListos = sitios.estado === 'listo' ? sitios.datos : null;
  const eventosListos = eventos.estado === 'listo' ? eventos.datos : null;
  const destinos = useMemo(
    () => construirDestinos(sitiosListos ?? [], eventosListos ?? []),
    [sitiosListos, eventosListos],
  );

  // La URL es la única fuente de la selección: `?destino=` se valida contra la lista actual.
  const seleccion = useMemo(() => destinoDesdeParametro(params.get('destino'), destinos), [params, destinos]);
  const seleccionId = seleccion?.id ?? null;
  const [listaAbierta, setListaAbierta] = useState(seleccion === null);

  const elegir = useCallback(
    (id: string) => {
      setParams(
        (previos) => {
          const siguientes = new URLSearchParams(previos);
          siguientes.set('destino', id);
          return siguientes;
        },
        { replace: true },
      );
      setListaAbierta(false);
    },
    [setParams],
  );

  const coordsDestino = seleccion?.coords ?? null;
  const { activa, ubicacion, origen, ruta, solicitar, quitar } = useRutaAPie(coordsDestino);

  const trazo = useMemo<TrazoMapa | null>(
    () => (ruta.estado === 'lista' ? { puntos: ruta.ruta.puntos, enLinea: ruta.enLinea } : null),
    [ruta],
  );

  const marcadores = useMemo<MarcadorMapa[]>(
    () =>
      destinos.flatMap((d) =>
        d.coords ? [{ id: d.id, categoria: d.categoria, coords: d.coords, nombre: d.nombre }] : [],
      ),
    [destinos],
  );

  const lejosKm = useMemo(() => {
    if (!origen) return null;
    const metros = distanciaEnLinea(origen, CENTRO_DOLORES);
    return metros > RADIO_FESTIVAL_M ? metros / 1000 : null;
  }, [origen]);

  const cargandoDatos = sitios.estado === 'cargando' || eventos.estado === 'cargando';

  return (
    <div className="mapa-fest">
      <div className="mapa-fest__panel">
        <header className="mapa-fest__cabecera">
          <p className="eyebrow">GPS del festival</p>
          <h1 className="mapa-fest__titulo">Mapa del festival</h1>
          <p className="meta">Elige el escenario o zona a la que quieres llegar</p>
        </header>

        {(sitios.estado === 'error' || eventos.estado === 'error') && (
          <div className="mapa-fest__avisos">
            {sitios.estado === 'error' && (
              <p className="mapa-fest__aviso" role="status">
                <span>No se pudieron cargar los sitios históricos.</span>
                <Button variante="fantasma" tamano="chico" onClick={sitios.reintentar}>
                  Reintentar
                </Button>
              </p>
            )}
            {eventos.estado === 'error' && (
              <p className="mapa-fest__aviso" role="status">
                <span>No se pudieron cargar los conciertos.</span>
                <Button variante="fantasma" tamano="chico" onClick={eventos.reintentar}>
                  Reintentar
                </Button>
              </p>
            )}
          </div>
        )}
        {cargandoDatos && (
          <p className="meta mapa-fest__cargando" role="status">
            <Spinner /> Cargando sitios y conciertos…
          </p>
        )}

        {seleccion && (
          <FichaDestino
            destino={seleccion}
            rutaActiva={activa}
            ubicacion={ubicacion}
            ruta={ruta}
            lejosDelFestivalKm={lejosKm}
            onComoLlegar={solicitar}
            onQuitarRuta={quitar}
          />
        )}

        <button
          type="button"
          className="mapa-fest__alternar"
          aria-expanded={listaAbierta}
          aria-controls="mapa-fest-lista"
          onClick={() => setListaAbierta((v) => !v)}
        >
          {listaAbierta ? <IconoArriba tamano={18} /> : <IconoAbajo tamano={18} />}
          Destinos ({destinos.length})
        </button>
        <div id="mapa-fest-lista" className={listaAbierta ? 'mapa-fest__destinos' : 'mapa-fest__destinos mapa-fest__destinos--cerrada'}>
          <SelectorDestinos destinos={destinos} seleccionId={seleccionId} onElegir={elegir} />
        </div>
      </div>

      <div className="mapa-fest__area">
        <MapaFestival
          marcadores={marcadores}
          seleccionId={seleccionId}
          onSeleccionar={elegir}
          origen={origen}
          ruta={trazo}
        />
      </div>
    </div>
  );
}
