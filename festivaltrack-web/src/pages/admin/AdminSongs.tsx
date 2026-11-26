import { useState } from 'react';
import { AdminNav } from '../../components/admin/AdminNav';
import { CatalogoCanciones } from '../../components/admin/panel/CatalogoCanciones';
import { FormularioCancion } from '../../components/admin/panel/FormularioCancion';
import { useRecurso } from '../../hooks/useRecurso';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { cancionesService, type Cancion } from '../../services/canciones.service';
import { SESION_TERMINO } from '../../components/admin/panel/publicar';
import '../../styles/admin-panel.css';

export default function AdminSongs() {
  useTituloPagina('Canciones (administración)');
  const recurso = useRecurso((signal) => cancionesService.listar(signal), 'admin-canciones', {
    401: SESION_TERMINO,
  });
  const [nuevas, setNuevas] = useState<Cancion[]>([]);

  return (
    <div className="pagina pagina--lectura">
      <AdminNav />
      <header className="pagina__cabecera">
        <h1 className="titulo-pagina">Canciones</h1>
        <p className="meta">Sube canciones al catálogo del festival y elimina las que ya no hagan falta.</p>
      </header>
      <FormularioCancion alPublicar={(c) => setNuevas((previas) => [c, ...previas])} />
      <CatalogoCanciones recurso={recurso} nuevas={nuevas} />
    </div>
  );
}
