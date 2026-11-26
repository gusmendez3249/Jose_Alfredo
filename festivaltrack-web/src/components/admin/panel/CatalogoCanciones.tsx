import { useState } from 'react';
import { Alert } from '../../ui/Alert';
import { Badge } from '../../ui/Badge';
import { Button } from '../../ui/Button';
import { ConfirmDialog } from '../../ui/ConfirmDialog';
import { IconoAlerta, IconoBasura, IconoMusica } from '../../ui/Icons';
import { Skeleton } from '../../ui/Skeleton';
import { StatusMessage } from '../../ui/StatusMessage';
import type { EstadoRecurso } from '../../../hooks/useRecurso';
import { cancionesService, type Cancion, type EstadoCancion } from '../../../services/canciones.service';
import { formatearMinSeg } from './archivos';
import { Portada } from './Portada';
import { mensajeAlEliminar } from './publicar';
import '../../../styles/admin-panel.css';

const ESTADO: Record<EstadoCancion, { texto: string; tono: 'exito' | 'neutra' | 'dorada' }> = {
  PUBLICADA: { texto: 'Publicada', tono: 'exito' },
  BORRADOR: { texto: 'Borrador', tono: 'neutra' },
  ARCHIVADA: { texto: 'Archivada', tono: 'dorada' },
};

interface Props {
  recurso: EstadoRecurso<Cancion[]> & { reintentar: () => void };
  /** Canciones publicadas en esta sesión: se muestran sin volver a pedir el catálogo. */
  nuevas: Cancion[];
}

export function CatalogoCanciones({ recurso, nuevas }: Props) {
  const [eliminadas, setEliminadas] = useState<ReadonlySet<string>>(new Set());
  const [pendiente, setPendiente] = useState<Cancion | null>(null);
  const [eliminando, setEliminando] = useState(false);
  const [aviso, setAviso] = useState<{ tono: 'exito' | 'error'; texto: string } | null>(null);

  const confirmar = async () => {
    if (!pendiente || eliminando) return;
    const cancion = pendiente;
    setEliminando(true);
    try {
      await cancionesService.eliminar(cancion.id);
      setEliminadas((previas) => new Set(previas).add(cancion.id));
      setAviso({ tono: 'exito', texto: 'Canción eliminada del catálogo' });
    } catch (e) {
      setAviso({ tono: 'error', texto: mensajeAlEliminar(e) });
    } finally {
      setEliminando(false);
      setPendiente(null);
    }
  };

  let lista: Cancion[] = [];
  if (recurso.estado === 'listo') {
    const ids = new Set(recurso.datos.map((c) => c.id));
    lista = [...nuevas.filter((c) => !ids.has(c.id)), ...recurso.datos].filter((c) => !eliminadas.has(c.id));
  }

  return (
    <section className="seccion" aria-labelledby="admin-panel-catalogo">
      <h2 id="admin-panel-catalogo" className="titulo-seccion">
        Catálogo{recurso.estado === 'listo' ? ` · ${lista.length}` : ''}
      </h2>

      {aviso && <Alert tono={aviso.tono}>{aviso.texto}</Alert>}

      {recurso.estado === 'cargando' && (
        <div role="status" aria-label="Cargando el catálogo" className="lista-tarjetas">
          <Skeleton alto={72} />
          <Skeleton alto={72} />
          <Skeleton alto={72} />
        </div>
      )}

      {recurso.estado === 'error' && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="No se pudo cargar el catálogo"
          texto={recurso.mensaje}
          acciones={
            <Button pildora onClick={recurso.reintentar}>
              Reintentar
            </Button>
          }
        />
      )}

      {recurso.estado === 'listo' && lista.length === 0 && (
        <StatusMessage
          icono={IconoMusica}
          titulo="El catálogo está vacío"
          texto="Sube la primera canción con el formulario de arriba."
        />
      )}

      {lista.length > 0 && (
        <ul className="lista-tarjetas">
          {lista.map((cancion) => {
            const estado = ESTADO[cancion.estado] ?? ESTADO.BORRADOR;
            return (
              <li key={cancion.id} className="admin-panel-fila admin-panel-fila--tarjeta">
                <Portada url={cancion.portadaUrl} />
                <div className="admin-panel-texto">
                  <p className="admin-panel-fila__titulo">{cancion.titulo}</p>
                  <p className="meta">
                    {cancion.artista} · {formatearMinSeg(cancion.duracion)}
                  </p>
                </div>
                <Badge tono={estado.tono}>{estado.texto}</Badge>
                <Button
                  variante="fantasma"
                  className="admin-panel-icono-boton"
                  aria-label={`Eliminar ${cancion.titulo}`}
                  onClick={() => setPendiente(cancion)}
                >
                  <IconoBasura tamano={20} />
                </Button>
              </li>
            );
          })}
        </ul>
      )}

      <ConfirmDialog
        abierto={pendiente !== null}
        titulo={`¿Eliminar «${pendiente?.titulo ?? ''}»?`}
        texto="Se quitará del catálogo. Si está en el repertorio de un concierto, primero hay que quitarla de ahí."
        textoConfirmar={eliminando ? 'Eliminando…' : 'Eliminar'}
        peligro
        onConfirmar={() => void confirmar()}
        onCancelar={() => !eliminando && setPendiente(null)}
      />
    </section>
  );
}
