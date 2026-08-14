import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../services/api';

export default function Login() {
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const successMessage = (location.state as { message?: string } | null)?.message;

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await api.post('/auth/login', { correo, contrasena: password });
      const token = res.data.accessToken ?? res.data.token;

      if (token) {
        localStorage.setItem('token', token);
        localStorage.setItem('accessToken', token);
        navigate('/mis-boletos');
      } else {
        setError('No se recibió un token válido del servidor.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error al iniciar sesión');
    }
  };

  return (
    <div className="container" style={{ paddingTop: '100px', textAlign: 'center' }}>
      <h2 style={{ marginBottom: '24px' }}>Iniciar Sesión</h2>
      <div className="glass-panel" style={{ maxWidth: '400px', margin: '0 auto' }}>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
          Ingresa con tus credenciales para continuar.
        </p>
        
        {successMessage && <p style={{ color: '#4caf50', marginBottom: '16px' }}>{successMessage}</p>}
        {error && <p style={{ color: 'red', marginBottom: '16px' }}>{error}</p>}
        
        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '16px' }}>
          <input
            type="email"
            placeholder="Correo electrónico"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            style={{ padding: '12px', borderRadius: '4px', border: '1px solid #333', background: '#1e1e1e', color: 'white' }}
            required
          />
          <input
            type="password"
            placeholder="Contraseña"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={{ padding: '12px', borderRadius: '4px', border: '1px solid #333', background: '#1e1e1e', color: 'white' }}
            required
          />
          <button type="submit" className="btn-primary" style={{ width: '100%' }}>Entrar</button>
        </form>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <Link to="/register" style={{ color: 'var(--primary-color)' }}>¿No tienes cuenta? Regístrate</Link>
          <Link to="/" style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>Volver al Inicio</Link>
        </div>
      </div>
    </div>
  );
}
