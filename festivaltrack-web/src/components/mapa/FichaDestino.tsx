import { useState } from 'react';
import { urlMedia } from '../../lib/media';
import { formatearDistancia, formatearDuracion, formatearFechaEvento } from '../../lib/formato';
import { Alert } from '../ui/Alert';
import { Button, ButtonLink } from '../ui/Button';
import { IconoLugar, IconoReloj, IconoRuta } from '../ui/Icons';
import { Spinner } from '../ui/Spinner';
import { ETIQUETA_CATEGORIA_SINGULAR, type Destino } from './destinos';
import { PinCategoria } from './pines';
import type { EstadoRuta, EstadoUbicacion } from './useRutaAPie';

interface Props {
  destino: Destino;
  rutaActiva: boolean;
  ubicacion: EstadoUbicacion;
  ruta: EstadoRuta;
  /** Kilómetros entre el usuario y el festival, solo si está lejos (más de 15 km); null en otro caso. */
  lejosDelFestivalKm: number | null;
  onComoLlegar: () => void;
  onQuitarRuta: () => void;
}

function FotoSitio({ url, nombre }: { url: string; nombre: string }) {
  const [fallo, setFallo] = useState(false);
  if (fallo) return null;
  return (
    <img
      className="mapa-fest__foto"
      src={url}
      alt={`Foto de ${nombre}`}
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setFallo(true)}
    />
  );
}

/** Ficha del destino elegido y panel de "Cómo llegar". */
export default function FichaDestino({ destino, rutaActiva, ubicacion, ruta, lejosDelFestivalKm, onComoLlegar, onQuitarRuta }: Props) {
  const imagen = urlMedia(destino.imagenUrl);
  const fecha = destino.evento ? formatearFechaEvento(destino.evento.fechaHora) : '';

  return (
    <section className="mapa-fest__ficha" aria-label="Destino elegido">
      <p className="mapa-fest__categoria">
        <PinCategoria categoria={destino.categoria} tamano={20} />
        {ETIQUETA_CATEGORIA_SINGULAR[destino.categoria]}
      </p>
      <h2 className="mapa-fest__nombre">{destino.nombre}</h2>
      {destino.direccion && (
        <p className="meta mapa-fest__linea">
          <IconoLugar tamano={14} /> {destino.direccion}
        </p>
      )}
      {fecha && (
        <p className="meta mapa-fest__linea">
          <IconoReloj tamano={14} /> {fecha} hrs
        </p>
      )}
      {imagen && <FotoSitio key={imagen} url={imagen} nombre={destino.nombre} />}
      {destino.descripcion && <p className="mapa-fest__descripcion">{destino.descripcion}</p>}

      {destino.evento && (
        <div className="mapa-fest__enlaces">
          <ButtonLink to={`/events/${encodeURIComponent(destino.evento.id)}`} variante="secundario" tamano="chico" pildora>
            Ver concierto
          </ButtonLink>
          {destino.evento.aLaVenta && (
            <ButtonLink to={`/checkout/${encodeURIComponent(destino.evento.id)}`} tamano="chico" pildora mayusculas>
              Comprar boletos
            </ButtonLink>
          )}
        </div>
      )}

      {!destino.coords ? (
        <Alert tono="info">Este destino no tiene coordenadas registradas: no aparece en el mapa ni se puede trazar la ruta.</Alert>
      ) : (
        <div className="mapa-fest__ruta">
          <div className="mapa-fest__acciones">
            {rutaActiva ? (
              <Button variante="secundario" tamano="chico" onClick={onQuitarRuta}>
                Quitar ruta
              </Button>
            ) : (
              <Button tamano="chico" pildora mayusculas onClick={onComoLlegar}>
                <IconoRuta tamano={18} />
                Cómo llegar
              </Button>
            )}
            {ubicacion.estado === 'error' && (
              <Button variante="secundario" tamano="chico" onClick={onComoLlegar}>
                Reintentar ubicación
              </Button>
            )}
          </div>

          <div aria-live="polite" className="pila pila--chica">
            {ubicacion.estado === 'obteniendo' && (
              <p role="status" className="mapa-fest__estado">
                <Spinner /> Buscando tu ubicación…
              </p>
            )}
            {ubicacion.estado === 'error' && (
              <Alert tono="error">{ubicacion.mensaje} El mapa sigue mostrando los destinos del festival.</Alert>
            )}
            {ruta.estado === 'calculando' && (
              <p role="status" className="mapa-fest__estado">
                <Spinner /> Calculando ruta…
              </p>
            )}
            {ruta.estado === 'lista' && (
              <p className="mapa-fest__resumen">
                <IconoRuta tamano={20} />
                <span>
                  {ruta.enLinea ? 'En línea recta: ' : 'A pie: '}
                  <strong>{formatearDistancia(ruta.ruta.distanciaMetros)}</strong> · unos{' '}
                  <strong>{formatearDuracion(ruta.ruta.duracionSegundos)}</strong>
                </span>
              </p>
            )}
            {ruta.estado === 'lista' && ruta.enLinea && (
              <Alert tono="info">Sin servicio de rutas: distancia en línea recta.</Alert>
            )}
            {lejosDelFestivalKm !== null && ruta.estado === 'lista' && (
              <Alert tono="info">Estás a unos {Math.round(lejosDelFestivalKm)} km del festival, en Dolores Hidalgo.</Alert>
            )}
          </div>

          <p className="meta mapa-fest__privacidad">
            Tu ubicación solo se envía al servicio externo de rutas de OpenStreetMap para calcular el trayecto.
          </p>
        </div>
      )}
    </section>
  );
}
