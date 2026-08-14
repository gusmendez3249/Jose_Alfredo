import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { boletosService } from '../services/boletos.service';
import type { BoletoDto } from '../services/boletos.service';

export default function MyTickets() {
  const [boletos, setBoletos] = useState<BoletoDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchBoletos = async () => {
      const token = localStorage.getItem('token') ?? localStorage.getItem('accessToken');
      if (!token) {
        navigate('/login', { replace: true });
        return;
      }

      try {
        const data = await boletosService.getMisBoletos();
        setBoletos(data);
      } catch (err: any) {
        if (err.response?.status === 401) {
          localStorage.removeItem('token');
          localStorage.removeItem('accessToken');
          navigate('/login', { replace: true });
        } else {
          setError('No se pudieron cargar tus boletos.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchBoletos();
  }, [navigate]);

  return (
    <div className="container" style={{ paddingTop: '100px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h2 style={{ color: 'var(--primary-color)' }}>Mis Boletos</h2>
        <Link to="/" className="btn-primary" style={{ padding: '8px 16px', fontSize: '14px', textDecoration: 'none' }}>
          Volver al Inicio
        </Link>
      </div>
      
      {loading && <p style={{ color: 'var(--text-secondary)' }}>Cargando tus boletos...</p>}
      
      {error && <p style={{ color: 'red' }}>{error}</p>}
      
      {!loading && !error && boletos.length === 0 && (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '48px 24px' }}>
          <h3 style={{ marginBottom: '16px' }}>Aún no tienes boletos</h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            Explora los eventos y adquiere tu acceso para disfrutar del festival.
          </p>
          <Link to="/events" className="btn-primary" style={{ textDecoration: 'none' }}>Ver Eventos</Link>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '24px' }}>
        {boletos.map((boleto) => (
          <div key={boleto.id} className="glass-panel" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
            <div style={{ width: '100%', marginBottom: '16px' }}>
              <h3 style={{ color: 'white', marginBottom: '4px' }}>{boleto.evento?.nombre || 'Gran Gala Mariachi'}</h3>
              <p style={{ color: 'var(--primary-color)', fontSize: '14px', fontWeight: 'bold' }}>Categoría: {boleto.categoria}</p>
              <p style={{ color: 'var(--text-secondary)', fontSize: '12px' }}>{new Date(boleto.evento?.fechaHora || Date.now()).toLocaleDateString()}</p>
            </div>
            
            <div style={{ background: 'white', padding: '16px', borderRadius: '8px', marginBottom: '16px' }}>
              <QRCodeSVG value={boleto.codigoQR} size={150} />
            </div>
            
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>ID Boleto: {boleto.id}</p>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Estado: <span style={{ color: boleto.estado === 'VENDIDO' ? '#4caf50' : 'var(--primary-color)' }}>{boleto.estado}</span></p>
          </div>
        ))}
      </div>
    </div>
  );
}
