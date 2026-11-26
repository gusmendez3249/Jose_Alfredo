import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import MapaRuta from '../components/MapaRuta';
import { geolocalizacionDisponible, mensajeGeolocalizacion, OPCIONES_GPS } from '../components/mapa/geolocalizacion';
import { Alert } from '../components/ui/Alert';
import { Button, ButtonLink } from '../components/ui/Button';
import { IconoAlerta, IconoLugar, IconoRuta } from '../components/ui/Icons';
import { Skeleton } from '../components/ui/Skeleton';
import { Spinner } from '../components/ui/Spinner';
import { StatusMessage } from '../components/ui/StatusMessage';
import { eventosService, lugarDe, tieneCoordenadas } from '../services/eventos.service';
import { calcularRutaAPie, RutaNoDisponibleError, type Coordenada, type Ruta } from '../services/rutas.service';
import type { Evento } from '../services/tipos';
import { formatearDistancia, formatearDuracion } from '../lib/formato';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';

type EstadoUbicacion =
  | { estado: 'obteniendo' }
  | { estado: 'lista'; coords: Coordenada }
  | { estado: 'error'; mensaje: string };

type EstadoRuta =
  | { estado: 'inactiva' }
  | { estado: 'calculando' }
  | { estado: 'lista'; ruta: Ruta }
  | { estado: 'error'; mensaje: string };

/** Mapa del evento con la ruta a pie desde la ubicación del usuario (si la comparte). */
function MapaDelEvento({ evento }: { evento: Evento & { latitud: number; longitud: number } }) {
  const destino = useMemo<Coordenada>(() => [evento.latitud, evento.longitud], [evento.latitud, evento.longitud]);
  const lugar = lugarDe(evento);

  // Los resultados guardan a qué intento / origen corresponden: si no coinciden con el
  // actual, el estado es "obteniendo" / "calculando" (se deriva en el render, sin setState síncrono).
  const [intentoGps, setIntentoGps] = useState(0);
  const [gps, setGps] = useState<{ intento: number; valor: Exclude<EstadoUbicacion, { estado: 'obteniendo' }> } | null>(null);
  const [calculo, setCalculo] = useState<{ origen: Coordenada; valor: Extract<EstadoRuta, { estado: 'lista' | 'error' }> } | null>(null);

  useEffect(() => {
    if (!geolocalizacionDisponible()) return;
    let vigente = true;
    navigator.geolocation.getCurrentPosition(
      (posicion) => {
        if (vigente) {
          setGps({ intento: intentoGps, valor: { estado: 'lista', coords: [posicion.coords.latitude, posicion.coords.longitude] } });
        }
      },
      (error) => {
        if (vigente) setGps({ intento: intentoGps, valor: { estado: 'error', mensaje: mensajeGeolocalizacion(error) } });
      },
      OPCIONES_GPS,
    );
    return () => {
      vigente = false;
    };
  }, [intentoGps]);

  const ubicacion: EstadoUbicacion = !geolocalizacionDisponible()
    ? { estado: 'error', mensaje: 'Tu navegador no permite obtener la ubicación.' }
    : gps?.intento === intentoGps
      ? gps.valor
      : { estado: 'obteniendo' };
  const origen = ubicacion.estado === 'lista' ? ubicacion.coords : null;

  useEffect(() => {
    if (!origen) return;
    const control = new AbortController();
    calcularRutaAPie(origen, destino, control.signal)
      .then((r) => setCalculo({ origen, valor: { estado: 'lista', ruta: r } }))
      .catch((error: unknown) => {
        if (control.signal.aborted) return;
        setCalculo({
          origen,
          valor: {
            estado: 'error',
            mensaje: error instanceof RutaNoDisponibleError ? error.message : 'No se pudo calcular la ruta a pie.',
          },
        });
      });
    return () => control.abort();
  }, [origen, destino]);

  const ruta: EstadoRuta = !origen
    ? { estado: 'inactiva' }
    : calculo?.origen === origen
      ? calculo.valor
      : { estado: 'calculando' };

  const reintentarUbicacion = useCallback(() => setIntentoGps((n) => n + 1), []);

  return (
    <div className="pagina-mapa">
      <div className="mapa-barra">
        <ButtonLink to="/events" variante="fantasma" tamano="chico">
          Volver a eventos
        </ButtonLink>
        <div className="mapa-barra__titulo">
          <h1 className="mapa-barra__nombre">Ruta hacia: {evento.nombre}</h1>
          {lugar && (
            <p className="meta mapa-barra__lugar">
              <IconoLugar tamano={14} /> {lugar}
            </p>
          )}
        </div>
      </div>

      <div className="mapa-area">
        <MapaRuta
          destino={destino}
          etiquetaDestino={evento.nombre}
          origen={origen}
          ruta={ruta.estado === 'lista' ? ruta.ruta.puntos : null}
        />

        <section className="mapa-panel" aria-label="Ruta a pie">
          <div aria-live="polite" className="pila pila--chica">
            {ubicacion.estado === 'obteniendo' && (
              <p role="status" className="mapa-panel__estado">
                <Spinner /> Obteniendo tu ubicación…
              </p>
            )}
            {ubicacion.estado === 'error' && (
              <Alert tono="error">{ubicacion.mensaje} El mapa muestra la ubicación del evento.</Alert>
            )}
            {ruta.estado === 'calculando' && (
              <p role="status" className="mapa-panel__estado">
                <Spinner /> Calculando la ruta a pie…
              </p>
            )}
            {ruta.estado === 'lista' && (
              <p className="mapa-panel__resumen">
                <IconoRuta tamano={20} />
                <span>
                  A pie: <strong>{formatearDistancia(ruta.ruta.distanciaMetros)}</strong> · unos{' '}
                  <strong>{formatearDuracion(ruta.ruta.duracionSegundos)}</strong>
                </span>
              </p>
            )}
            {ruta.estado === 'error' && (
              <Alert tono="error">{ruta.mensaje} El mapa sigue mostrando tu ubicación y la del evento.</Alert>
            )}
          </div>

          {ubicacion.estado === 'error' && (
            <Button variante="secundario" tamano="chico" onClick={reintentarUbicacion}>
              Reintentar ubicación
            </Button>
          )}

          <p className="meta mapa-panel__privacidad">
            Tu ubicación solo se envía al servicio externo de rutas de OpenStreetMap para calcular el trayecto.
          </p>
        </section>
      </div>
    </div>
  );
}

export default function MapRoute() {
  const { id = '' } = useParams();
  const recurso = useRecurso((signal) => eventosService.porId(id, signal), id, {
    404: 'Este evento no existe o ya no está disponible.',
  });
  useTituloPagina(recurso.estado === 'listo' ? `Ruta a ${recurso.datos.nombre}` : 'Ruta al evento');

  if (recurso.estado === 'cargando') {
    return (
      <div className="pagina pagina--lectura">
        <div role="status" className="pila" aria-label="Cargando evento">
          <Skeleton alto={28} ancho="60%" />
          <Skeleton alto={16} ancho="40%" />
          <Skeleton alto={240} />
          <span className="solo-lectores">Cargando evento…</span>
        </div>
      </div>
    );
  }

  if (recurso.estado === 'error') {
    return (
      <div className="pagina pagina--lectura">
        <StatusMessage
          rol="alert"
          nivelTitulo={1}
          icono={IconoAlerta}
          titulo="No se pudo mostrar el mapa"
          texto={recurso.mensaje}
          acciones={
            <>
              {recurso.status !== 404 && (
                <Button pildora onClick={recurso.reintentar}>
                  Reintentar
                </Button>
              )}
              <ButtonLink to="/events" variante="secundario" pildora>
                Volver a eventos
              </ButtonLink>
            </>
          }
        />
      </div>
    );
  }

  const evento = recurso.datos;
  if (!tieneCoordenadas(evento)) {
    return (
      <div className="pagina pagina--lectura">
        <StatusMessage
          nivelTitulo={1}
          icono={IconoLugar}
          titulo={evento.nombre}
          texto="Este evento no tiene coordenadas registradas."
          acciones={
            <ButtonLink to="/events" pildora>
              Volver a eventos
            </ButtonLink>
          }
        />
      </div>
    );
  }

  return <MapaDelEvento evento={evento} />;
}
