import { useState } from 'react';
import { AdminNav } from '../../components/admin/AdminNav';
import { FormularioAdministrador } from '../../components/admin/panel/FormularioAdministrador';
import { SESION_TERMINO } from '../../components/admin/panel/publicar';
import { useAuth } from '../../auth/useAuth';
import { Alert } from '../../components/ui/Alert';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { IconoAlerta, IconoEscudo, IconoUsuario } from '../../components/ui/Icons';
import { Skeleton } from '../../components/ui/Skeleton';
import { StatusMessage } from '../../components/ui/StatusMessage';
import { useRecurso } from '../../hooks/useRecurso';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { mensajeDeError } from '../../lib/errores';
import type { Rol } from '../../services/tipos';
import { usuariosService, type UsuarioAdmin } from '../../services/usuarios.service';
import '../../styles/admin-panel.css';

const PROPIOS = {
  401: SESION_TERMINO,
  403: 'No tienes permisos de administrador para hacer esto.',
};

interface Cambio {
  usuario: UsuarioAdmin;
  rol: Rol;
}

export default function AdminUsers() {
  useTituloPagina('Usuarios (administración)');
  const { usuario: yo } = useAuth();
  const recurso = useRecurso((signal) => usuariosService.listar(signal), 'admin-usuarios', PROPIOS);
  // Copia local tras cambiar un rol o dar de alta: evita volver a mostrar el esqueleto.
  const [local, setLocal] = useState<UsuarioAdmin[] | null>(null);
  const [formularioAbierto, setFormularioAbierto] = useState(false);
  const [cambio, setCambio] = useState<Cambio | null>(null);
  const [cambiando, setCambiando] = useState(false);
  const [aviso, setAviso] = useState<{ tono: 'exito' | 'error'; texto: string } | null>(null);

  const usuarios = local ?? (recurso.estado === 'listo' ? recurso.datos : []);

  const confirmar = async () => {
    if (!cambio || cambiando) return;
    const { usuario, rol } = cambio;
    setCambiando(true);
    try {
      const actualizado = await usuariosService.cambiarRol(usuario.id, rol);
      setLocal(usuarios.map((u) => (u.id === actualizado.id ? actualizado : u)));
      setAviso({ tono: 'exito', texto: 'Rol actualizado correctamente' });
    } catch (e) {
      setAviso({
        tono: 'error',
        texto: mensajeDeError(e, {
          ...PROPIOS,
          400: 'No se puede: debe quedar al menos un administrador y no puedes quitarte tu propio rol',
        }),
      });
    } finally {
      setCambiando(false);
      setCambio(null);
    }
  };

  const alRegistrar = async () => {
    setFormularioAbierto(false);
    setAviso({ tono: 'exito', texto: 'Administrador registrado exitosamente' });
    try {
      setLocal(await usuariosService.listar());
    } catch {
      // El alta ya se hizo; si falla releer la lista, se queda la anterior.
    }
  };

  return (
    <div className="pagina pagina--lectura">
      <AdminNav />
      <header className="pagina__cabecera">
        <h1 className="titulo-pagina">Gestión de usuarios</h1>
        <p className="meta">Cambia quién administra el festival y da de alta a nuevos administradores.</p>
      </header>

      {aviso && <Alert tono={aviso.tono}>{aviso.texto}</Alert>}

      {formularioAbierto ? (
        <FormularioAdministrador
          alRegistrar={() => void alRegistrar()}
          alCancelar={() => setFormularioAbierto(false)}
        />
      ) : (
        <div>
          <Button mayusculas onClick={() => setFormularioAbierto(true)}>
            Registrar administrador
          </Button>
        </div>
      )}

      {recurso.estado === 'cargando' && local === null && (
        <div role="status" aria-label="Cargando usuarios" className="lista-tarjetas">
          <Skeleton alto={64} />
          <Skeleton alto={64} />
          <Skeleton alto={64} />
        </div>
      )}

      {recurso.estado === 'error' && local === null && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="No se pudieron cargar los usuarios"
          texto={recurso.mensaje}
          acciones={
            <Button pildora onClick={recurso.reintentar}>
              Reintentar
            </Button>
          }
        />
      )}

      {(recurso.estado === 'listo' || local !== null) && usuarios.length === 0 && (
        <StatusMessage icono={IconoUsuario} titulo="No hay usuarios registrados." />
      )}

      {usuarios.length > 0 && (
        <table className="admin-panel-tabla">
          <caption className="solo-lectores">Usuarios registrados</caption>
          <thead>
            <tr>
              <th scope="col">Nombre</th>
              <th scope="col">Correo</th>
              <th scope="col">Rol</th>
              <th scope="col">Acción</th>
            </tr>
          </thead>
          <tbody>
            {usuarios.map((u) => {
              const esAdmin = u.rol === 'ADMINISTRADOR';
              const esYo = u.id === yo?.id;
              return (
                <tr key={u.id}>
                  <th scope="row" data-etiqueta="Nombre" className="admin-panel-texto">
                    {u.nombre}
                    {esYo && <span className="admin-panel-yo"> Tú</span>}
                  </th>
                  <td data-etiqueta="Correo" className="admin-panel-texto">
                    {u.correo}
                  </td>
                  <td data-etiqueta="Rol">
                    {esAdmin ? (
                      <Badge tono="dorada">
                        <IconoEscudo tamano={14} /> Administrador
                      </Badge>
                    ) : (
                      <Badge>Usuario</Badge>
                    )}
                  </td>
                  <td data-etiqueta="Acción">
                    {!esYo && (
                      <Button
                        variante={esAdmin ? 'fantasma' : 'secundario'}
                        tamano="chico"
                        aria-label={`${esAdmin ? 'Quitar Admin' : 'Hacer Admin'} a ${u.nombre}`}
                        onClick={() => setCambio({ usuario: u, rol: esAdmin ? 'USUARIO' : 'ADMINISTRADOR' })}
                      >
                        {esAdmin ? 'Quitar Admin' : 'Hacer Admin'}
                      </Button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}

      <ConfirmDialog
        abierto={cambio !== null}
        titulo={
          cambio?.rol === 'ADMINISTRADOR'
            ? `¿Hacer administrador a ${cambio.usuario.nombre}?`
            : `¿Quitar el rol de administrador a ${cambio?.usuario.nombre ?? ''}?`
        }
        texto={
          cambio?.rol === 'ADMINISTRADOR'
            ? 'Podrá crear y borrar conciertos, canciones y usuarios, y transmitir en vivo.'
            : 'Ya no podrá entrar al panel de administración.'
        }
        textoConfirmar={cambio?.rol === 'ADMINISTRADOR' ? 'Hacer Admin' : 'Quitar Admin'}
        peligro={cambio?.rol === 'USUARIO'}
        onConfirmar={() => void confirmar()}
        onCancelar={() => !cambiando && setCambio(null)}
      />
    </div>
  );
}
