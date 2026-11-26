import { useEffect, useMemo } from 'react';
import { MapContainer, Marker, Polyline, TileLayer, Tooltip, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { Coordenada } from '../../services/rutas.service';
import { CENTRO_DOLORES, type CategoriaDestino } from './destinos';
import { htmlPin } from './formasPin';

export interface MarcadorMapa {
  id: string;
  categoria: CategoriaDestino;
  coords: Coordenada;
  nombre: string;
}

export interface TrazoMapa {
  puntos: Coordenada[];
  /** Trazo recto de respaldo: se dibuja discontinuo. */
  enLinea: boolean;
}

interface Props {
  marcadores: readonly MarcadorMapa[];
  seleccionId: string | null;
  onSeleccionar: (id: string) => void;
  origen: Coordenada | null;
  ruta: TrazoMapa | null;
}

const ZOOM_INICIAL = 15;
const ZOOM_DESTINO = 16;
const ZOOM_MAXIMO_RUTA = 17.5;
/** Dorado primario de la marca (--color-primario en styles/tokens.css). */
const COLOR_RUTA = '#E6C27A';

const TAMANO_PIN = 34;
const TAMANO_PIN_SELECCIONADO = 46;

function iconoPin(categoria: CategoriaDestino, seleccionado: boolean): L.DivIcon {
  const lado = seleccionado ? TAMANO_PIN_SELECCIONADO : TAMANO_PIN;
  return L.divIcon({
    className: 'mapa-pin',
    html: htmlPin(categoria, seleccionado),
    iconSize: [lado, lado],
    iconAnchor: [lado / 2, lado / 2],
    tooltipAnchor: [lado / 2, 0],
  });
}

const ICONO_YO = L.divIcon({
  className: 'mapa-yo',
  html: '<span class="mapa-yo__punto"></span>',
  iconSize: [22, 22],
  iconAnchor: [11, 11],
});

/** Centra en el destino elegido o encuadra la ruta; no se repite en cada render porque depende de objetos estables. */
function Encuadre({ centro, ruta }: { centro: Coordenada | null; ruta: TrazoMapa | null }) {
  const map = useMap();
  useEffect(() => {
    if (ruta) {
      map.fitBounds(L.latLngBounds(ruta.puntos), { padding: [48, 48], maxZoom: ZOOM_MAXIMO_RUTA });
    } else if (centro) {
      map.setView(centro, Math.max(map.getZoom(), ZOOM_DESTINO));
    }
  }, [map, centro, ruta]);
  return null;
}

/** El panel cambia de alto al elegir un destino: Leaflet no se entera solo de que su contenedor cambió. */
function AjustarTamano() {
  const map = useMap();
  useEffect(() => {
    if (typeof ResizeObserver === 'undefined') return;
    const observador = new ResizeObserver(() => map.invalidateSize());
    observador.observe(map.getContainer());
    return () => observador.disconnect();
  }, [map]);
  return null;
}

export default function MapaFestival({ marcadores, seleccionId, onSeleccionar, origen, ruta }: Props) {
  const centro = useMemo(
    () => marcadores.find((m) => m.id === seleccionId)?.coords ?? null,
    [marcadores, seleccionId],
  );

  return (
    <MapContainer center={CENTRO_DOLORES} zoom={ZOOM_INICIAL} className="mapa-fest__mapa">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        maxZoom={19}
      />
      {marcadores.map((m) => {
        const seleccionado = m.id === seleccionId;
        return (
          <Marker
            key={m.id}
            position={m.coords}
            icon={iconoPin(m.categoria, seleccionado)}
            title={m.nombre}
            alt={m.nombre}
            zIndexOffset={seleccionado ? 1000 : 0}
            eventHandlers={{ click: () => onSeleccionar(m.id) }}
          >
            <Tooltip>{m.nombre}</Tooltip>
          </Marker>
        );
      })}
      {origen && (
        <Marker position={origen} icon={ICONO_YO} title="Tú estás aquí" alt="Tú estás aquí" zIndexOffset={2000}>
          <Tooltip>Tú estás aquí</Tooltip>
        </Marker>
      )}
      {ruta && (
        <Polyline
          positions={ruta.puntos}
          pathOptions={{ color: COLOR_RUTA, weight: 5, opacity: 0.9, dashArray: ruta.enLinea ? '8 10' : undefined }}
        />
      )}
      <Encuadre centro={centro} ruta={ruta} />
      <AjustarTamano />
    </MapContainer>
  );
}
