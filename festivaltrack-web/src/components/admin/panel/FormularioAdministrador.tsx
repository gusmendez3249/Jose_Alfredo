import { useState, type FormEvent } from 'react';
import axios from 'axios';
import { mensajeDeError } from '../../../lib/errores';
import { usuariosService } from '../../../services/usuarios.service';
import { Alert } from '../../ui/Alert';
import { Button } from '../../ui/Button';
import { Card } from '../../ui/Card';
import { Field } from '../../ui/Field';
import { SESION_TERMINO } from './publicar';
import '../../../styles/admin-panel.css';

/** Mismo texto y regla que REGLA_CONTRASENA del backend (register.dto.ts) y que Register.tsx. */
const REGLA_CONTRASENA = 'La contraseña debe tener al menos 8 caracteres, una mayúscula y un número';

function cumpleRegla(contrasena: string): boolean {
  return contrasena.length >= 8 && /[A-Z]/.test(contrasena) && /\d/.test(contrasena);
}

const CORREO = /^\S+@\S+\.\S+$/;

interface Props {
  alRegistrar: () => void;
  alCancelar: () => void;
}

export function FormularioAdministrador({ alRegistrar, alCancelar }: Props) {
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [errores, setErrores] = useState<{ general?: string; correo?: string; contrasena?: string }>({});
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (enviando) return;
    const datos = { nombre: nombre.trim(), correo: correo.trim(), contrasena };
    if (!datos.nombre || !datos.correo || !datos.contrasena) {
      setErrores({ general: 'Por favor, llena todos los campos' });
      return;
    }
    const nuevos: typeof errores = {};
    if (!CORREO.test(datos.correo)) nuevos.correo = 'Escribe un correo válido';
    if (!cumpleRegla(datos.contrasena)) nuevos.contrasena = REGLA_CONTRASENA;
    if (nuevos.correo || nuevos.contrasena) {
      setErrores(nuevos);
      return;
    }
    setErrores({});
    setEnviando(true);
    try {
      await usuariosService.registrarAdministrador(datos);
      alRegistrar();
    } catch (err) {
      const sinRespuesta = axios.isAxiosError(err) && !err.response;
      setErrores({
        general: sinRespuesta
          ? 'Sin conexión con el servidor'
          : mensajeDeError(err, {
              401: SESION_TERMINO,
              403: 'No tienes permisos de administrador para hacer esto.',
              409: 'Ese correo ya está registrado',
              500: 'No se pudo registrar al administrador',
            }),
      });
      setEnviando(false);
    }
  };

  return (
    <Card como="section" aria-labelledby="admin-panel-alta" className="admin-panel-alta">
      <h2 id="admin-panel-alta" className="titulo-seccion">
        Registrar Administrador
      </h2>
      <form className="formulario" onSubmit={enviar} noValidate aria-label="Registrar Administrador">
        <Field
          etiqueta="Nombre Completo"
          autoComplete="off"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          maxLength={100}
          disabled={enviando}
        />
        <Field
          etiqueta="Correo Electrónico"
          type="email"
          inputMode="email"
          autoComplete="off"
          value={correo}
          onChange={(e) => setCorreo(e.target.value)}
          disabled={enviando}
          error={errores.correo}
        />
        <Field
          etiqueta="Contraseña"
          type="password"
          alternarContrasena
          autoComplete="new-password"
          value={contrasena}
          onChange={(e) => setContrasena(e.target.value)}
          ayuda="Al menos 8 caracteres, una mayúscula y un número."
          disabled={enviando}
          error={errores.contrasena}
        />
        {errores.general && <Alert tono="error">{errores.general}</Alert>}
        <div className="admin-panel-acciones">
          <Button variante="fantasma" onClick={alCancelar} disabled={enviando}>
            Cancelar
          </Button>
          <Button type="submit" mayusculas cargando={enviando}>
            {enviando ? 'Registrando…' : 'Registrar administrador'}
          </Button>
        </div>
      </form>
    </Card>
  );
}
