import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

interface Evento {
  id: string;
  nombre: string;
  artistaNombre?: string;
  fechaHora: string;
  ubicacion: string;
  escenario?: string;
  latitud?: number;
  longitud?: number;
}

export default function Events() {
  const [eventos, setEventos] = useState<Evento[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetch('http://localhost:3001/api/v1/eventos')
      .then(res => {
        if (!res.ok) throw new Error('Error al obtener eventos');
        return res.json();
      })
      .then(data => {
        setEventos(data);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setError('No se pudieron cargar los eventos. Verifica que el backend esté ejecutándose.');
        setLoading(false);
      });
  }, []);

  return (
    <div className="container" style={{ paddingTop: '100px', paddingBottom: '80px' }}>
      <header style={{ textAlign: 'center', marginBottom: '40px' }} className="animate-fade-in">
        <h2 style={{ color: 'var(--primary-color)', fontSize: '2.5rem', marginBottom: '16px' }}>
          Próximos Eventos
        </h2>
        <p style={{ color: 'var(--text-secondary)' }}>
          Selecciona un evento para ver la ruta hacia el escenario.
        </p>
      </header>

      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        {loading && <p style={{ textAlign: 'center' }}>Cargando eventos...</p>}

        {error && (
          <div className="glass-panel" style={{ textAlign: 'center', borderColor: '#ef4444' }}>
            <p style={{ color: '#ef4444' }}>{error}</p>
          </div>
        )}

        {!loading && !error && eventos.length === 0 && (
          <p style={{ textAlign: 'center' }}>No hay eventos próximos en este momento.</p>
        )}

        <div style={{ display: 'grid', gap: '16px' }}>
          {eventos.map((evento, index) => {
            const time = new Date(evento.fechaHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            return (
              <div 
                key={evento.id} 
                className="glass-panel animate-fade-in" 
                onClick={() => navigate('/events/map', { state: { evento } })}
                style={{ 
                  animationDelay: `${index * 0.1}s`, 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'center',
                  cursor: 'pointer'
                }}>
                <div>
                  <h3 style={{ fontSize: '1.25rem', marginBottom: '8px' }}>
                    {evento.artistaNombre || evento.nombre}
                  </h3>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                      <circle cx="12" cy="10" r="3"></circle>
                    </svg>
                    <span>{(evento.escenario || evento.ubicacion).toUpperCase()}</span>
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ color: 'var(--primary-color)', fontWeight: 'bold', fontSize: '1.5rem' }}>
                    {time}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <div style={{ textAlign: 'center', marginTop: '40px' }}>
          <Link to="/" className="btn-primary" style={{ display: 'inline-block', padding: '12px 32px' }}>
            Volver al Inicio
          </Link>
        </div>
      </div>
    </div>
  );
}
