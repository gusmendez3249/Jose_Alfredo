import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { leerEstadoLogin } from '../auth/rutas';
import { mensajeDeError } from '../lib/errores';
import { Card } from '../components/ui/Card';
import { IconoMarca } from '../components/ui/Icons';
import { Alert } from '../components/ui/Alert';
import { Button } from '../components/ui/Button';
import { Field } from '../components/ui/Field';
import { useTituloPagina } from '../hooks/useTituloPagina';

export default function Login() {
  useTituloPagina('Iniciar sesión');
  const { autenticado, iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { from, sesionExpirada } = leerEstadoLogin(location.state);
  const destino = from ?? '/mis-boletos';

  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Con sesión ya iniciada (p. ej. vuelta atrás en el historial) no tiene sentido el formulario.
  if (autenticado && !enviando) return <Navigate to={destino} replace />;

  const handleLogin = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (enviando) return;
    setEnviando(true);
    setError('');
    try {
      await iniciarSesion(correo.trim(), password);
      navigate(destino, { replace: true });
    } catch (err) {
      setError(
        mensajeDeError(err, {
          400: 'Revisa tu correo y contraseña.',
          401: 'Correo o contraseña incorrectos.',
          429: 'Demasiados intentos de inicio de sesión. Espera un minuto y vuelve a intentarlo.',
        }),
      );
      setEnviando(false);
    }
  };

  return (
    <div className="pagina pagina--centrada">
      <Card como="section" aria-labelledby="login-titulo" className="acceso-tarjeta">
        <header className="acceso-tarjeta__cabecera">
          <IconoMarca tamano={40} className="acceso-tarjeta__marca" />
          <p className="eyebrow">Acceso exclusivo</p>
          <h1 id="login-titulo" className="titulo-pagina">
            José Alfredo Jiménez
          </h1>
          <p className="meta">Ingresa con tus credenciales para continuar.</p>
        </header>

        {sesionExpirada && !error && <Alert tono="info">Tu sesión expiró. Inicia sesión de nuevo.</Alert>}
        {error && <Alert tono="error">{error}</Alert>}

        <form onSubmit={handleLogin} className="formulario" aria-label="Iniciar sesión">
          <Field
            etiqueta="Correo electrónico"
            id="login-correo"
            type="email"
            autoComplete="email"
            inputMode="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            required
            disabled={enviando}
          />
          <Field
            etiqueta="Contraseña"
            id="login-contrasena"
            type="password"
            alternarContrasena
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            disabled={enviando}
          />
          <Button type="submit" tamano="grande" bloque mayusculas cargando={enviando}>
            {enviando ? 'Entrando…' : 'Iniciar sesión'}
          </Button>
        </form>

        <div className="acceso-tarjeta__enlaces">
          <Link to="/register" state={from ? { from } : undefined}>
            ¿No tienes cuenta? Regístrate
          </Link>
          <Link to="/" className="meta">
            Volver al inicio
          </Link>
        </div>
      </Card>
    </div>
  );
}
