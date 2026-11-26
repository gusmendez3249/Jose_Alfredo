import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Button, ButtonLink } from '../../ui/Button';
import { Alert } from '../../ui/Alert';
import { Card } from '../../ui/Card';
import { Field } from '../../ui/Field';
import { IconoSubir } from '../../ui/Icons';
import type { Cancion } from '../../../services/canciones.service';
import { ACEPTA_AUDIO, ACEPTA_IMAGEN, formatearMinSeg, leerComoDataUrl, medirDuracion, validarArchivo } from './archivos';
import { Portada } from './Portada';
import {
  ErrorDeEtapa,
  ETIQUETA_ETAPA,
  publicarCancion,
  type Progreso,
  type Subidos,
} from './publicar';
import { ZonaArchivo } from './ZonaArchivo';
import '../../../styles/admin-panel.css';

const ARTISTA_POR_DEFECTO = 'José Alfredo Jiménez';

interface Props {
  alPublicar: (cancion: Cancion) => void;
}

/** Formulario "Subir nueva canción": valida antes de subir y publica por etapas. */
export function FormularioCancion({ alPublicar }: Props) {
  const [titulo, setTitulo] = useState('');
  const [artista, setArtista] = useState(ARTISTA_POR_DEFECTO);
  const [audio, setAudio] = useState<File | null>(null);
  const [portada, setPortada] = useState<File | null>(null);
  const [vistaPortada, setVistaPortada] = useState<string | null>(null);
  const [duracion, setDuracion] = useState<number | null>(null);
  const [errorTitulo, setErrorTitulo] = useState('');
  const [errorAudio, setErrorAudio] = useState<string | null>(null);
  const [errorPortada, setErrorPortada] = useState<string | null>(null);
  const [errorEnvio, setErrorEnvio] = useState('');
  const [exito, setExito] = useState('');
  const [progreso, setProgreso] = useState<Progreso | null>(null);

  const enviando = progreso !== null;
  const bloqueo = useRef(false);
  const subidos = useRef<Subidos>(new Map());
  // Medición en curso: el envío la espera para no mandar la duración vacía por llegar antes.
  const medicion = useRef<Promise<number | null>>(Promise.resolve(null));

  useEffect(() => {
    if (!portada) return;
    let vigente = true;
    void leerComoDataUrl(portada).then((url) => {
      if (vigente) setVistaPortada(url);
    });
    return () => {
      vigente = false;
    };
  }, [portada]);

  const elegirAudio = (archivo: File) => {
    const problema = validarArchivo(archivo, 'audio');
    setErrorAudio(problema);
    if (problema) return;
    setAudio(archivo);
    setDuracion(null);
    medicion.current = medirDuracion(archivo).then((segundos) => {
      setDuracion(segundos);
      return segundos;
    });
  };

  const elegirPortada = (archivo: File) => {
    const problema = validarArchivo(archivo, 'imagen');
    setErrorPortada(problema);
    if (problema) return;
    setVistaPortada(null);
    setPortada(archivo);
  };

  const quitarPortada = () => {
    setPortada(null);
    setVistaPortada(null);
    setErrorPortada(null);
  };

  const quitarAudio = () => {
    setAudio(null);
    setDuracion(null);
    setErrorAudio(null);
    medicion.current = Promise.resolve(null);
  };

  const enviar = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (bloqueo.current) return;
    const tituloLimpio = titulo.trim();
    if (!tituloLimpio) {
      setErrorTitulo('Escribe el título de la canción');
      return;
    }
    setErrorTitulo('');
    setErrorEnvio('');
    setExito('');
    bloqueo.current = true;
    setProgreso({ etapa: audio ? 'audio' : portada ? 'portada' : 'guardar', porcentaje: 0 });
    try {
      const segundos = audio ? await medicion.current : null;
      const cancion = await publicarCancion(
        { titulo: tituloLimpio, artista: artista.trim(), duracion: segundos, audio, portada },
        subidos.current,
        setProgreso,
      );
      setExito(`«${cancion.titulo}» ya está en el catálogo. Asígnala a un concierto en Gestionar → Eventos.`);
      alPublicar(cancion);
      setTitulo('');
      setArtista(ARTISTA_POR_DEFECTO);
      setAudio(null);
      setDuracion(null);
      quitarPortada();
      subidos.current = new Map();
    } catch (err) {
      setErrorEnvio(err instanceof ErrorDeEtapa ? err.message : 'No se pudo publicar la canción. Inténtalo de nuevo.');
    } finally {
      bloqueo.current = false;
      setProgreso(null);
    }
  };

  const duracionTexto = duracion ? formatearMinSeg(duracion) : null;

  return (
    <Card como="section" aria-labelledby="admin-panel-subir" className="admin-panel-subir">
      <header className="pagina__cabecera">
        <p className="eyebrow">Catálogo de canciones</p>
        <h2 id="admin-panel-subir" className="titulo-seccion">
          Subir nueva canción
        </h2>
        <p className="meta">Publícala en el catálogo y después asígnala a un concierto en Gestionar → Eventos.</p>
      </header>

      <form className="formulario" onSubmit={enviar} aria-label="Subir nueva canción" noValidate>
        <ZonaArchivo
          etiqueta="Agregar portada"
          ayuda="Opcional. JPG, PNG o WEBP, de preferencia cuadrada."
          accept={ACEPTA_IMAGEN}
          invitacion="Elige o arrastra una imagen"
          disabled={enviando}
          error={errorPortada}
          onArchivo={elegirPortada}
        />
        {portada && (
          <div className="admin-panel-archivo-elegido">
            {vistaPortada && <img className="admin-panel-vista" src={vistaPortada} alt={`Vista previa de la portada ${portada.name}`} />}
            <span className="meta admin-panel-texto">{portada.name}</span>
            <Button variante="fantasma" tamano="chico" onClick={quitarPortada} disabled={enviando}>
              Quitar portada
            </Button>
          </div>
        )}

        <Field
          etiqueta="Título de la canción"
          placeholder="El Rey"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          maxLength={150}
          required
          disabled={enviando}
          error={errorTitulo || undefined}
        />
        <Field
          etiqueta="Artista o intérprete"
          value={artista}
          onChange={(e) => setArtista(e.target.value)}
          maxLength={150}
          disabled={enviando}
        />

        <ZonaArchivo
          etiqueta="Audio de la canción"
          ayuda="MP3, M4A, WAV u OGG · máximo 30 MB"
          accept={ACEPTA_AUDIO}
          invitacion="Elige o arrastra un archivo de audio"
          disabled={enviando}
          error={errorAudio}
          onArchivo={elegirAudio}
        />
        {audio ? (
          <div className="admin-panel-archivo-elegido">
            <span className="meta admin-panel-texto">
              {audio.name}
              {duracionTexto ? ` · ${duracionTexto}` : ''}
            </span>
            <Button variante="fantasma" tamano="chico" onClick={quitarAudio} disabled={enviando}>
              Quitar audio
            </Button>
          </div>
        ) : (
          <p className="meta">Sin audio propio se usará una pista de muestra.</p>
        )}

        <div className="admin-panel-previa">
          <p className="eyebrow">Así se verá en la app</p>
          <div className="admin-panel-fila">
            <Portada src={vistaPortada} />
            <div className="admin-panel-texto">
              <p className="admin-panel-fila__titulo">{titulo.trim() || 'Título de la canción'}</p>
              <p className="meta">{artista.trim() || ARTISTA_POR_DEFECTO}</p>
            </div>
          </div>
        </div>

        {progreso && (
          <div className="admin-panel-progreso" role="status">
            <label htmlFor="admin-panel-progreso-barra">
              {ETIQUETA_ETAPA[progreso.etapa]} · {progreso.porcentaje}%
            </label>
            <progress id="admin-panel-progreso-barra" max={100} value={progreso.porcentaje} />
          </div>
        )}

        {errorEnvio && <Alert tono="error">{errorEnvio}</Alert>}
        {exito && (
          <Alert tono="exito">
            <p>¡Canción publicada!</p>
            <p>{exito}</p>
            <ButtonLink to="/admin/conciertos" variante="fantasma" tamano="chico">
              Ir a conciertos
            </ButtonLink>
          </Alert>
        )}

        <Button type="submit" tamano="grande" bloque mayusculas cargando={enviando}>
          {!enviando && <IconoSubir tamano={20} />}
          {enviando ? 'Publicando…' : 'Publicar en el catálogo'}
        </Button>
      </form>
    </Card>
  );
}
