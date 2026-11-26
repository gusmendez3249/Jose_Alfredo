import { useId, useState } from 'react';
import type { Publicacion } from '../../services/foro.service';
import { Badge, clases, IconoBasura, IconoComentario, IconoCorazon, IconoLugar } from '../ui';
import { altDeFoto, ETIQUETA_TIPO, TONO_TIPO } from './etiquetas';
import { ImagenForo } from './ImagenForo';
import { PanelComentarios } from './PanelComentarios';
import { Avatar, Hace } from './piezas';

/** Como el móvil: a partir de 220 caracteres el texto se recorta a 4 líneas con "Ver más". */
const LARGO_RECORTE = 220;

interface TarjetaPublicacionProps {
  publicacion: Publicacion;
  puedeBorrar: boolean;
  likePendiente: boolean;
  onLike: () => void;
  onBorrar: () => void;
  onAbrirImagen: () => void;
  onConteoComentarios: (conteo: number | ((actual: number) => number)) => void;
  onIrALogin: () => void;
}

export function TarjetaPublicacion({
  publicacion: p,
  puedeBorrar,
  likePendiente,
  onLike,
  onBorrar,
  onAbrirImagen,
  onConteoComentarios,
  onIrALogin,
}: TarjetaPublicacionProps) {
  const idBase = useId();
  const idTexto = `${idBase}-texto`;
  const idComentarios = `${idBase}-comentarios`;
  const [expandido, setExpandido] = useState(false);
  const [comentariosAbiertos, setComentariosAbiertos] = useState(false);
  const largo = p.contenido.length > LARGO_RECORTE;

  return (
    <article className="comunidad-publicacion" aria-labelledby={`${idBase}-autor`}>
      <header className="comunidad-publicacion__cabecera">
        <Avatar nombre={p.autorNombre} />
        <div className="comunidad-publicacion__quien">
          <p id={`${idBase}-autor`} className="comunidad-autor">
            {p.autorNombre}
          </p>
          <p className="meta comunidad-publicacion__meta">
            <Hace iso={p.createdAt} />
            {p.lugar && (
              <>
                <span aria-hidden="true"> · </span>
                <span className="comunidad-lugar">
                  <IconoLugar tamano={14} />
                  <span className="solo-lectores">Lugar: </span>
                  {p.lugar}
                </span>
              </>
            )}
          </p>
        </div>
        <Badge tono={TONO_TIPO[p.tipo]}>{ETIQUETA_TIPO[p.tipo].toLocaleUpperCase('es-MX')}</Badge>
        {puedeBorrar && (
          <button type="button" className="comunidad-icono-boton" onClick={onBorrar} aria-label="Eliminar publicación">
            <IconoBasura tamano={20} />
          </button>
        )}
      </header>

      {p.imagenUrl && <ImagenForo key={p.imagenUrl} url={p.imagenUrl} alt={altDeFoto(p)} onAbrir={onAbrirImagen} />}

      {(p.titulo || p.contenido) && (
        <div className="comunidad-publicacion__cuerpo">
          {p.titulo && <h2 className="comunidad-publicacion__titulo">{p.titulo}</h2>}
          {p.contenido && (
            <p id={idTexto} className={clases('comunidad-texto-usuario', largo && !expandido && 'comunidad-texto-usuario--recortado')}>
              {p.contenido}
            </p>
          )}
          {largo && (
            <button
              type="button"
              className="comunidad-enlace-boton"
              aria-expanded={expandido}
              aria-controls={idTexto}
              onClick={() => setExpandido((v) => !v)}
            >
              {expandido ? 'Ver menos' : 'Ver más'}
            </button>
          )}
        </div>
      )}

      <footer className="comunidad-publicacion__pie">
        <button
          type="button"
          className="comunidad-accion comunidad-accion--like"
          aria-pressed={p.likedByMe}
          aria-label={`Me gusta, ${p.likes}`}
          aria-disabled={likePendiente || undefined}
          onClick={() => {
            if (!likePendiente) onLike();
          }}
        >
          <IconoCorazon tamano={20} />
          <span aria-hidden="true">{p.likes}</span>
        </button>
        <button
          type="button"
          className="comunidad-accion"
          aria-expanded={comentariosAbiertos}
          aria-controls={comentariosAbiertos ? idComentarios : undefined}
          aria-label={`Comentarios, ${p.comentarios}`}
          onClick={() => setComentariosAbiertos((v) => !v)}
        >
          <IconoComentario tamano={20} />
          <span aria-hidden="true">{p.comentarios}</span>
        </button>
      </footer>

      {comentariosAbiertos && (
        <PanelComentarios
          id={idComentarios}
          publicacionId={p.id}
          onConteo={onConteoComentarios}
          onIrALogin={onIrALogin}
        />
      )}
    </article>
  );
}
