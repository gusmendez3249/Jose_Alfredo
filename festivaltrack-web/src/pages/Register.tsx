import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { leerEstadoLogin } from '../auth/rutas';
import { mensajeDeError } from '../lib/errores';
import { Alert } from '../components/ui/Alert';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Field } from '../components/ui/Field';
import { IconoMarca } from '../components/ui/Icons';
import { useTituloPagina } from '../hooks/useTituloPagina';

/** Mismo texto y regla que REGLA_CONTRASENA del backend (register.dto.ts). */
const REGLA_CONTRASENA = 'La contraseña debe tener al menos 8 caracteres, una mayúscula y un número';

function cumpleRegla(contrasena: string): boolean {
  return contrasena.length >= 8 && /[A-Z]/.test(contrasena) && /\d/.test(contrasena);
}

export default function Register() {
  useTituloPagina('Crear cuenta');
  const { autenticado, registrarse } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { from } = leerEstadoLogin(location.state);
  const destino = from ?? '/events';

  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (autenticado && !enviando) return <Navigate to={destino} replace />;

  const handleRegister = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (enviando) return;

    const nombreLimpio = nombre.trim();
    const correoLimpio = correo.trim();
    if (!nombreLimpio) {
      setError('Escribe tu nombre.');
      return;
    }
    if (!cumpleRegla(password)) {
      setError(REGLA_CONTRASENA);
      return;
    }

    setEnviando(true);
    setError('');
    try {
      // El backend devuelve accessToken y usuario: se entra directamente con la sesión iniciada.
      await registrarse({ nombre: nombreLimpio, correo: correoLimpio, contrasena: password });
      navigate(destino, { replace: true });
    } catch (err) {
      setError(
        mensajeDeError(err, {
          409: 'Ese correo ya está registrado. Inicia sesión o usa otro correo.',
          429: 'Demasiados registros desde esta conexión. Espera un minuto y vuelve a intentarlo.',
        }),
      );
      setEnviando(false);
    }
  };

  return (
    <div className="pagina pagina--centrada">
      <Card como="section" aria-labelledby="registro-titulo" className="acceso-tarjeta">
        <header className="acceso-tarjeta__cabecera">
          <IconoMarca tamano={40} className="acceso-tarjeta__marca" />
          <p className="eyebrow">Nueva cuenta</p>
          <h1 id="registro-titulo" className="titulo-pagina">
            Crear cuenta
          </h1>
          <p className="meta">Crea una cuenta para comprar boletos y llevarlos contigo en FestivalTrack.</p>
        </header>

        {error && <Alert tono="error">{error}</Alert>}

        <form onSubmit={handleRegister} className="formulario" aria-label="Crear cuenta">
          <Field
            etiqueta="Nombre completo"
            id="registro-nombre"
            type="text"
            autoComplete="name"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
            maxLength={100}
            disabled={enviando}
          />
          <Field
            etiqueta="Correo electrónico"
            id="registro-correo"
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
            id="registro-contrasena"
            type="password"
            alternarContrasena
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            ayuda="Al menos 8 caracteres, una mayúscula y un número."
            aria-invalid={password !== '' && !cumpleRegla(password)}
            disabled={enviando}
          />
          <Button type="submit" tamano="grande" bloque mayusculas cargando={enviando}>
            {enviando ? 'Creando cuenta…' : 'Registrarse ahora'}
          </Button>
        </form>

        <div className="acceso-tarjeta__enlaces">
          <Link to="/login" state={from ? { from } : undefined}>
            ¿Ya tienes cuenta? Inicia sesión
          </Link>
          <Link to="/" className="meta">
            Volver al inicio
          </Link>
        </div>
      </Card>
    </div>
  );
}
