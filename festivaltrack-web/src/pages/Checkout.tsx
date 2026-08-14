import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { boletosService } from '../services/boletos.service';

export default function Checkout() {
  const { eventoId } = useParams();
  const navigate = useNavigate();
  
  const [tarjeta, setTarjeta] = useState('');
  const [titular, setTitular] = useState('');
  const [expiracion, setExpiracion] = useState('');
  const [cvv, setCvv] = useState('');
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCheckout = async (e: React.FormEvent) => {
    e.preventDefault();

    const token = localStorage.getItem('token') ?? localStorage.getItem('accessToken');
    if (!token) {
      navigate('/login', { replace: true });
      return;
    }

    if (tarjeta.length < 15) {
      setError('Número de tarjeta no válido.');
      return;
    }
    
    setLoading(true);
    setError('');
    
    try {
      await boletosService.comprarBoleto(eventoId || 'EVT-001', 'GENERAL', 1);
      navigate('/mis-boletos', { replace: true });
    } catch (err: any) {
      if (err.response?.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('accessToken');
        navigate('/login', { replace: true });
      }
      setError(err.response?.data?.message || 'Error al procesar el pago.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ paddingTop: '100px' }}>
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <h2 style={{ marginBottom: '8px', color: 'var(--primary-color)' }}>Finalizar Compra</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '32px' }}>
          Estás a punto de adquirir tus boletos. Ingresa tu método de pago.
        </p>

        <div className="glass-panel">
          <h3 style={{ marginBottom: '24px' }}>Detalles de Pago</h3>
          
          {error && <p style={{ color: '#ff4444', marginBottom: '16px', background: 'rgba(255,0,0,0.1)', padding: '12px', borderRadius: '4px' }}>{error}</p>}
          
          <form onSubmit={handleCheckout} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)' }}>Titular de la tarjeta</label>
              <input
                type="text"
                placeholder="Nombre como aparece en la tarjeta"
                value={titular}
                onChange={(e) => setTitular(e.target.value)}
                style={{ width: '100%', padding: '12px', borderRadius: '4px', border: '1px solid #333', background: '#1e1e1e', color: 'white' }}
                required
              />
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)' }}>Número de tarjeta</label>
              <input
                type="text"
                placeholder="0000 0000 0000 0000"
                value={tarjeta}
                onChange={(e) => setTarjeta(e.target.value.replace(/\D/g, '').substring(0, 16))}
                style={{ width: '100%', padding: '12px', borderRadius: '4px', border: '1px solid #333', background: '#1e1e1e', color: 'white' }}
                required
              />
            </div>

            <div style={{ display: 'flex', gap: '16px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)' }}>Vencimiento</label>
                <input
                  type="text"
                  placeholder="MM/AA"
                  value={expiracion}
                  onChange={(e) => setExpiracion(e.target.value)}
                  style={{ width: '100%', padding: '12px', borderRadius: '4px', border: '1px solid #333', background: '#1e1e1e', color: 'white' }}
                  required
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)' }}>CVV</label>
                <input
                  type="password"
                  placeholder="123"
                  value={cvv}
                  maxLength={4}
                  onChange={(e) => setCvv(e.target.value.replace(/\D/g, ''))}
                  style={{ width: '100%', padding: '12px', borderRadius: '4px', border: '1px solid #333', background: '#1e1e1e', color: 'white' }}
                  required
                />
              </div>
            </div>

            <div style={{ marginTop: '24px', display: 'flex', gap: '16px', alignItems: 'center' }}>
              <button 
                type="submit" 
                className="btn-primary" 
                style={{ flex: 1, padding: '16px', fontSize: '16px' }}
                disabled={loading}
              >
                {loading ? 'Procesando...' : 'PAGAR AHORA'}
              </button>
              <Link to="/" style={{ color: 'var(--text-secondary)' }}>Cancelar</Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
