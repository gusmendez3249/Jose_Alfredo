import axios from 'axios';
import { useEffect, useId, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { mensajeDeError } from '../../lib/errores';
import {
  foroService,
  LIMITES_FORO,
  TIPOS_IMAGEN,
  validarImagen,
  type Publicacion,
  type TipoPublicacion,
} from '../../services/foro.service';
import { Alert, Button, Card, clases, Field, IconoCerrar, IconoImagen } from '../ui';
import { CampoTextoLargo } from './piezas';

const OPCIONES_TIPO: ReadonlyArray<{ valor: TipoPublicacion; etiqueta: string }> = [
  { valor: 'EXPERIENCIA', etiqueta: 'Experiencia' },
  { valor: 'RECOMENDACION', etiqueta: 'Recomendación' },
  { valor: 'FOTO', etiqueta: 'Foto' },
];

const PLACEHOLDER: Record<TipoPublicacion, string> = {
  EXPERIENCIA: 'Cuéntanos cómo viviste el festival…',
  RECOMENDACION: '¿Qué recomiendas? Un lugar, platillo, ruta, horario…',
  FOTO: 'Pon un pie de foto (opcional)',
};

interface Foto {
  archivo: File;
  /** URL `blob:` local para la vista previa; se revoca al quitarla o al desmontar. */
  vistaPrevia: string;
  /** Ruta ya subida: si falla la publicación, el reintento no vuelve a subir la foto. */
  ruta: string | null;
}

function mensajePublicar(error: unknown): string {
  if (axios.isAxiosError(error) && !error.response && error.code !== 'ECONNABORTED') {
    return 'Sin conexión. No se pudo publicar.';
  }
  const mensaje = mensajeDeError(error);
  return mensaje || 'No se pudo publicar. Intenta de nuevo.';
}

interface CompositorProps {
  onPublicado: (publicacion: Publicacion) => void;
  onCancelar: () => void;
}

export function Compositor({ onPublicado, onCancelar }: CompositorProps) {
  const idTitulo = useId();
  const idFoto = useId();
  const [tipo, setTipo] = useState<TipoPublicacion>('EXPERIENCIA');
  const [titulo, setTitulo] = useState('');
  const [contenido, setContenido] = useState('');
  const [lugar, setLugar] = useState('');
  const [foto, setFoto] = useState<Foto | null>(null);
  const [errorFoto, setErrorFoto] = useState<string | null>(null);
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [progreso, setProgreso] = useState<number | null>(null);
  const enviandoRef = useRef(false);
  const inputFoto = useRef<HTMLInputElement>(null);
  const controlRef = useRef<AbortController | null>(null);
  const tituloRef = useRef<HTMLHeadingElement>(null);

  // Al abrirse, el foco entra en el compositor para que teclado y lector de pantalla lo encuentren.
  useEffect(() => {
    tituloRef.current?.focus();
  }, []);

  const vistaPrevia = foto?.vistaPrevia ?? null;
  useEffect(() => {
    if (!vistaPrevia) return;
    return () => URL.revokeObjectURL(vistaPrevia);
  }, [vistaPrevia]);

  // Al salir de la página se cancela una subida en curso.
  useEffect(() => () => controlRef.current?.abort(), []);

  function elegirFoto(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0];
    // Se limpia el input para poder volver a elegir el mismo archivo tras quitarlo.
    e.target.value = '';
    if (!archivo) return;
    const problema = validarImagen(archivo);
    if (problema) {
      setErrorFoto(problema);
      return;
    }
    setErrorFoto(null);
    setErrorValidacion(null);
    setFoto({ archivo, vistaPrevia: URL.createObjectURL(archivo), ruta: null });
    if (tipo === 'EXPERIENCIA' && !contenido.trim()) setTipo('FOTO');
  }

  function quitarFoto() {
    setFoto(null);
    setErrorFoto(null);
    inputFoto.current?.focus();
  }

  async function publicar(e: FormEvent) {
    e.preventDefault();
    if (enviandoRef.current) return;
    setError(null);

    const texto = contenido.trim();
    if (tipo === 'FOTO' && !foto) {
      setErrorValidacion('Agrega tu foto: es obligatoria en una publicación de foto.');
      return;
    }
    if (!texto && !foto) {
      setErrorValidacion('Escribe algo o agrega una foto');
      return;
    }
    if (texto.length > LIMITES_FORO.contenido) {
      setErrorValidacion(`Máximo ${LIMITES_FORO.contenido} caracteres`);
      return;
    }
    setErrorValidacion(null);

    enviandoRef.current = true;
    setEnviando(true);
    try {
      let ruta = foto?.ruta ?? null;
      if (foto && !ruta) {
        const control = new AbortController();
        controlRef.current = control;
        setProgreso(0);
        try {
          ruta = await foroService.subirImagen(foto.archivo, setProgreso, control.signal);
        } catch (err) {
          setError(`No se pudo subir la foto. ${mensajeDeError(err)}`);
          return;
        } finally {
          setProgreso(null);
        }
        const subida = ruta;
        setFoto((f) => (f && f.archivo === foto.archivo ? { ...f, ruta: subida } : f));
      }

      const creada = await foroService.crear({
        tipo,
        titulo,
        contenido: texto,
        lugar,
        imagenUrl: ruta ?? undefined,
      });
      onPublicado(creada);
    } catch (err) {
      setError(mensajePublicar(err));
    } finally {
      enviandoRef.current = false;
      setEnviando(false);
    }
  }

  const fotoObligatoria = tipo === 'FOTO';

  return (
    <Card como="section" className="comunidad-compositor" aria-labelledby={idTitulo}>
      <form className="comunidad-compositor__form" onSubmit={publicar} noValidate>
        <div>
          <h2 id={idTitulo} ref={tituloRef} tabIndex={-1} className="titulo-seccion comunidad-compositor__titulo">
            Compartir con la comunidad
          </h2>
          <p className="meta">Tu experiencia puede ayudar a otros asistentes</p>
        </div>

        <fieldset className="comunidad-tipos" disabled={enviando}>
          <legend className="campo__etiqueta">Tipo de publicación</legend>
          <div className="comunidad-tipos__opciones">
            {OPCIONES_TIPO.map((o) => (
              <label key={o.valor} className={clases('comunidad-chip', tipo === o.valor && 'comunidad-chip--activo')}>
                <input
                  type="radio"
                  name="tipo-publicacion"
                  className="solo-lectores"
                  value={o.valor}
                  checked={tipo === o.valor}
                  onChange={() => {
                    setTipo(o.valor);
                    setErrorValidacion(null);
                  }}
                />
                {o.etiqueta}
              </label>
            ))}
          </div>
        </fieldset>

        <div className="comunidad-foto">
          {foto ? (
            <div className="comunidad-foto__previa">
              <img src={foto.vistaPrevia} alt="Vista previa de la foto elegida" />
              <Button
                variante="fantasma"
                className="comunidad-foto__quitar"
                onClick={quitarFoto}
                disabled={enviando}
                aria-label="Quitar foto"
              >
                <IconoCerrar />
              </Button>
            </div>
          ) : (
            <label className={clases('comunidad-foto__elegir', fotoObligatoria && 'comunidad-foto__elegir--obligatoria')} htmlFor={idFoto}>
              <IconoImagen tamano={28} />
              <span>{fotoObligatoria ? 'Agrega tu foto (obligatoria)' : 'Agregar una foto (opcional)'}</span>
              <span className="meta" aria-hidden="true">
                JPG, PNG o WEBP · máximo 8 MB
              </span>
            </label>
          )}
          <input
            ref={inputFoto}
            id={idFoto}
            type="file"
            accept={TIPOS_IMAGEN.join(',')}
            className="solo-lectores"
            onChange={elegirFoto}
            disabled={enviando}
            aria-invalid={errorFoto ? true : undefined}
            aria-describedby={`${idFoto}-ayuda`}
            tabIndex={foto ? -1 : 0}
          />
          <span id={`${idFoto}-ayuda`} className="solo-lectores">
            JPG, PNG o WEBP, máximo 8 MB
          </span>
          {errorFoto && (
            <p className="campo__error" role="alert">
              {errorFoto}
            </p>
          )}
          {progreso !== null && (
            <div className="comunidad-progreso">
              <label htmlFor={`${idFoto}-progreso`} className="meta">
                Subiendo foto… {Math.round(progreso * 100)}%
              </label>
              <progress id={`${idFoto}-progreso`} max={1} value={progreso} />
            </div>
          )}
        </div>

        <Field
          etiqueta="Título (opcional)"
          value={titulo}
          maxLength={LIMITES_FORO.titulo}
          onChange={(e) => setTitulo(e.target.value)}
          disabled={enviando}
          autoComplete="off"
        />

        <CampoTextoLargo
          etiqueta={fotoObligatoria ? 'Pie de foto' : 'Tu publicación'}
          value={contenido}
          maxLength={LIMITES_FORO.contenido}
          placeholder={PLACEHOLDER[tipo]}
          rows={5}
          onChange={(e) => {
            setContenido(e.target.value);
            setErrorValidacion(null);
          }}
          disabled={enviando}
          error={errorValidacion ?? undefined}
        />

        <Field
          etiqueta="Lugar (opcional)"
          placeholder="Ej: Mausoleo, Dolores Hidalgo"
          value={lugar}
          maxLength={LIMITES_FORO.lugar}
          onChange={(e) => setLugar(e.target.value)}
          disabled={enviando}
          autoComplete="off"
        />

        {error && <Alert tono="error">{error}</Alert>}

        <div className="comunidad-compositor__acciones">
          <Button variante="fantasma" onClick={onCancelar} disabled={enviando}>
            Cancelar
          </Button>
          <Button type="submit" mayusculas cargando={enviando}>
            {enviando ? 'Publicando…' : 'Publicar'}
          </Button>
        </div>
      </form>
    </Card>
  );
}
