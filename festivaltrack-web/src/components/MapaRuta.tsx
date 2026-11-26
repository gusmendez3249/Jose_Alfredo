import { useEffect } from 'react';
import { MapContainer, Marker, Polyline, TileLayer, Tooltip, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
// Iconos empaquetados por Vite desde el propio paquete (nada de CDN externos).
import iconoUrl from 'leaflet/dist/images/marker-icon.png';
import iconoRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import sombraUrl from 'leaflet/dist/images/marker-shadow.png';
import type { Coordenada } from '../services/rutas.service';

function crearIcono(className: string): L.Icon {
  return L.icon({
    iconUrl: iconoUrl,
    iconRetinaUrl: iconoRetinaUrl,
    shadowUrl: sombraUrl,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    tooltipAnchor: [12, -28],
    shadowSize: [41, 41],
    className,
  });
}

/** Clases para que el diseño pueda distinguir los marcadores (p. ej. con un filtro de color). */
const ICONO_DESTINO = crearIcono('marcador-destino');
const ICONO_ORIGEN = crearIcono('marcador-origen');

const ZOOM_EVENTO = 16;
/** Dorado primario de la marca (--color-primario en styles/tokens.css). */
const COLOR_RUTA = '#E6C27A';

function AjustarEncuadre({ destino, origen, ruta }: { destino: Coordenada; origen: Coordenada | null; ruta: Coordenada[] | null }) {
  const map = useMap();
  useEffect(() => {
    if (!origen) {
      map.setView(destino, ZOOM_EVENTO);
      return;
    }
    const limites = L.latLngBounds([origen, destino]);
    ruta?.forEach((punto) => limites.extend(punto));
    map.fitBounds(limites, { padding: [48, 48], maxZoom: 17 });
  }, [map, destino, origen, ruta]);
  return null;
}

export interface MapaRutaProps {
  destino: Coordenada;
  /** Texto del marcador del evento. */
  etiquetaDestino: string;
  /** Ubicación del usuario; null si no se tiene (el mapa se centra en el evento). */
  origen: Coordenada | null;
  /** Geometría de la ruta a pie; null mientras no se haya calculado. */
  ruta: Coordenada[] | null;
}

export default function MapaRuta({ destino, etiquetaDestino, origen, ruta }: MapaRutaProps) {
  return (
    <MapContainer center={destino} zoom={ZOOM_EVENTO} className="mapa-ruta">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        maxZoom={19}
      />
      <Marker position={destino} icon={ICONO_DESTINO} title={etiquetaDestino} alt={etiquetaDestino}>
        <Tooltip>{etiquetaDestino}</Tooltip>
      </Marker>
      {origen && (
        <Marker position={origen} icon={ICONO_ORIGEN} title="Tu ubicación" alt="Tu ubicación">
          <Tooltip>Tu ubicación</Tooltip>
        </Marker>
      )}
      {ruta && <Polyline positions={ruta} pathOptions={{ color: COLOR_RUTA, weight: 5, opacity: 0.9 }} />}
      <AjustarEncuadre destino={destino} origen={origen} ruta={ruta} />
    </MapContainer>
  );
}
