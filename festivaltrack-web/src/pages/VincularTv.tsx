import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { authService, limpiarCodigoTv } from '../services/auth.service';
import { mensajeDeError } from '../lib/errores';
import { Alert } from '../components/ui/Alert';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Field } from '../components/ui/Field';
import { IconoMarca } from '../components/ui/Icons';
import { useTituloPagina } from '../hooks/useTituloPagina';

/**
 * Entrar en la TV del festival con la cuenta de esta sesión.
 *
 * La TV muestra un código y un QR que abre esta página con `?codigo=...`. La ruta exige sesión:
 * sin ella, el login (o el registro) regresa aquí con el código ya escrito. Al confirmar, la TV
 * entra sola en unos segundos (pregunta al servidor cada 3 s).
 */
export default function VincularTv() {
  useTituloPagina('Vincular TV');
  const { usuario } = useAuth();
  const [parametros] = useSearchParams();
  const [codigo, setCodigo] = useState(() => limpiarCodigoTv(parametros.get('codigo') ?? ''));
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [listo, setListo] = useState(false);

  const completo = codigo.length === 9;

  const vincular = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (enviando || !completo) return;
    setEnviando(true);
    setError('');
    try {
      await authService.vincularTv(codigo);
      setListo(true);
    } catch (err) {
      setError(
        mensajeDeError(err, {
          400: 'Escribe el código de 8 letras y números que muestra la TV.',
          404: 'Ese código no existe o ya venció. Revisa el que muestra la TV (cambia cada 5 minutos).',
          429: 'Demasiados intentos. Espera un minuto y vuelve a intentarlo.',
        }),
      );
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="pagina pagina--centrada">
      <Card como="section" aria-labelledby="vincular-titulo" className="acceso-tarjeta">
        <header className="acceso-tarjeta__cabecera">
          <IconoMarca tamano={40} className="acceso-tarjeta__marca" />
          <p className="eyebrow">Smart TV</p>
          <h1 id="vincular-titulo" className="titulo-pagina">
            Vincular TV
          </h1>
          <p className="meta">
            {usuario ? `La TV entrará con tu cuenta (${usuario.nombre}).` : 'La TV entrará con tu cuenta.'}
          </p>
        </header>

        {listo ? (
          <>
            <Alert tono="exito">¡Listo! La TV entrará con tu cuenta en unos segundos.</Alert>
            <div className="acceso-tarjeta__enlaces">
              <Link to="/">Ir al inicio</Link>
            </div>
          </>
        ) : (
          <>
            {error && <Alert tono="error">{error}</Alert>}
            <form onSubmit={vincular} className="formulario" aria-label="Vincular TV">
              <Field
                etiqueta="Código de la TV"
                id="vincular-codigo"
                autoComplete="off"
                autoCapitalize="characters"
                placeholder="ABCD-1234"
                value={codigo}
                onChange={(e) => setCodigo(limpiarCodigoTv(e.target.value))}
                ayuda="Es el que aparece en la pantalla de la TV, junto al QR."
                required
                disabled={enviando}
              />
              <Alert tono="info">Confirma solo si la TV está frente a ti: entrará con tu cuenta.</Alert>
              <Button type="submit" tamano="grande" bloque mayusculas cargando={enviando} disabled={!completo}>
                {enviando ? 'Vinculando…' : 'Entrar en la TV'}
              </Button>
            </form>
          </>
        )}
      </Card>
    </div>
  );
}
