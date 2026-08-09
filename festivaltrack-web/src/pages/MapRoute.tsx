import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet-routing-machine/dist/leaflet-routing-machine.css';
import L from 'leaflet';
import 'leaflet-routing-machine';

// Fix for default Leaflet markers in React
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

function Routing({ userLoc, eventLoc }: { userLoc: [number, number]; eventLoc: [number, number] }) {
  const map = useMap();
  useEffect(() => {
    if (!map) return;
    const routingControl = L.Routing.control({
      waypoints: [
        L.latLng(userLoc[0], userLoc[1]),
        L.latLng(eventLoc[0], eventLoc[1])
      ],
      routeWhileDragging: false,
      addWaypoints: false,
      show: false,
      createMarker: () => null // Let the routing machine just draw the line, we could add custom markers if needed
    }).addTo(map);

    return () => {
      try {
        map.removeControl(routingControl);
      } catch (e) {
        console.error(e);
      }
    };
  }, [map, userLoc, eventLoc]);
  return null;
}

export default function MapRoute() {
  const location = useLocation();
  const navigate = useNavigate();
  const evento = location.state?.evento;
  
  const [userLoc, setUserLoc] = useState<[number, number] | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    if (!evento || !evento.latitud || !evento.longitud) {
      setError('Este evento no tiene coordenadas registradas.');
      return;
    }

    if (!navigator.geolocation) {
      setError('Tu navegador no soporta geolocalización.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setUserLoc([position.coords.latitude, position.coords.longitude]);
      },
      (err) => {
        setError('No se pudo obtener tu ubicación. Por favor permite el acceso al GPS.');
        console.error(err);
      }
    );
  }, [evento]);

  if (!evento) {
    return (
      <div className="container" style={{ paddingTop: '100px', textAlign: 'center' }}>
        <h2>Evento no encontrado</h2>
        <button className="btn-primary" onClick={() => navigate('/events')} style={{ marginTop: '24px' }}>Volver</button>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw' }}>
      <header style={{ padding: '24px', backgroundColor: 'var(--bg-color)', zIndex: 1000, boxShadow: '0 4px 10px rgba(0,0,0,0.5)' }}>
        <h2 style={{ color: 'var(--primary-color)', fontSize: '1.5rem', marginBottom: '8px' }}>Ruta hacia: {evento.artistaNombre || evento.nombre}</h2>
        <p style={{ color: 'var(--text-secondary)' }}>{(evento.escenario || evento.ubicacion).toUpperCase()}</p>
        <button className="btn-primary" onClick={() => navigate(-1)} style={{ marginTop: '16px', padding: '8px 24px' }}>Volver</button>
      </header>
      
      <div style={{ flex: 1, position: 'relative' }}>
        {error && (
          <div style={{ position: 'absolute', top: '20px', left: '50%', transform: 'translateX(-50%)', backgroundColor: '#ef4444', color: 'white', padding: '12px 24px', borderRadius: '8px', zIndex: 1001 }}>
            {error}
          </div>
        )}

        {!error && !userLoc && (
          <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 1001, backgroundColor: 'var(--surface-color)', padding: '24px', borderRadius: '16px' }}>
            Obteniendo tu ubicación...
          </div>
        )}

        {userLoc && evento.latitud && evento.longitud && (
          <MapContainer 
            center={userLoc} 
            zoom={13} 
            style={{ height: '100%', width: '100%' }}
            zoomControl={false}
          >
            <TileLayer
              attribution='&copy; OpenStreetMap contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <Routing userLoc={userLoc} eventLoc={[evento.latitud, evento.longitud]} />
          </MapContainer>
        )}
      </div>
    </div>
  );
}
