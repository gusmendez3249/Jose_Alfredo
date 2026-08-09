import { Link } from 'react-router-dom';

export default function Register() {
  return (
    <div className="container" style={{ paddingTop: '100px', textAlign: 'center' }}>
      <h2 style={{ marginBottom: '24px' }}>Registro</h2>
      <div className="glass-panel" style={{ maxWidth: '400px', margin: '0 auto' }}>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
          Crea una nueva cuenta para acceder a FestivalTrack.
        </p>
        <button className="btn-primary" style={{ width: '100%', marginBottom: '16px' }}>Registrarme</button>
        <Link to="/" style={{ color: 'var(--primary-color)' }}>Volver al Inicio</Link>
      </div>
    </div>
  );
}
