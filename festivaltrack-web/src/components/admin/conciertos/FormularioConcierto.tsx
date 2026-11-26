import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, ButtonLink, Field } from '../../ui';
import { adminEventosService } from '../../../services/adminEventos.service';
import type { Evento } from '../../../services/tipos';
import { CampoArea, CampoSeleccion } from './Campos';
import {
  cambiosDeEdicion,
  cuerpoDeAlta,
  LUGAR_ACTUAL,
  MAX_DESCRIPCION,
  validarFormulario,
  valoresDeEvento,
  VALORES_NUEVOS,
  type CampoFormulario,
  type ErroresFormulario,
  type ValoresFormulario,
} from './formulario';
import { etiquetaDeLugar, LUGARES } from './lugares';
import { mensajeAlGuardarConcierto } from './mensajes';
import '../../../styles/admin-conciertos.css';

const ORDEN_CAMPOS: readonly CampoFormulario[] = ['nombre', 'fecha', 'hora', 'lugar', 'capacidad', 'descripcion'];

interface Props {
  /** Concierto a editar; sin él, el formulario da de alta uno nuevo. */
  evento?: Evento;
}

/** Alta y edición de un concierto. La fecha y la hora se escriben (y se muestran) en hora del centro de México. */
export function FormularioConcierto({ evento }: Props) {
  const navigate = useNavigate();
  const editando = evento !== undefined;
  const inicial = useMemo(() => (evento ? valoresDeEvento(evento) : VALORES_NUEVOS), [evento]);
  const [valores, setValores] = useState<ValoresFormulario>(inicial);
  const [errores, setErrores] = useState<ErroresFormulario>({});
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const enCurso = useRef(false);
  const guardado = useRef(false);

  const cambios = useMemo(() => (evento ? cambiosDeEdicion(evento, valores) : null), [evento, valores]);
  const sinCambios = editando && cambios !== null && Object.keys(cambios).length === 0;
  const sucio = useMemo(() => ORDEN_CAMPOS.some((c) => valores[c] !== inicial[c]), [valores, inicial]);

  // Avisa al cerrar o recargar la pestaña con cambios sin guardar. No cubre la navegación interna
  // de la app (el enrutador en uso no ofrece un bloqueo fiable), por eso no se promete más.
  useEffect(() => {
    if (!sucio) return;
    const avisar = (e: BeforeUnloadEvent) => {
      if (guardado.current) return;
      e.preventDefault();
    };
    window.addEventListener('beforeunload', avisar);
    return () => window.removeEventListener('beforeunload', avisar);
  }, [sucio]);

  function cambiar(campo: CampoFormulario, valor: string) {
    setValores((v) => ({ ...v, [campo]: valor }));
    setErrores((e) => (e[campo] ? { ...e, [campo]: undefined } : e));
    setErrorEnvio(null);
  }

  async function enviar(e: FormEvent) {
    e.preventDefault();
    if (enCurso.current || sinCambios) return;

    const encontrados = validarFormulario(valores, { editando, ahora: Date.now() });
    if (Object.values(encontrados).some(Boolean)) {
      setErrores(encontrados);
      const primero = ORDEN_CAMPOS.find((c) => encontrados[c]);
      if (primero) document.getElementById(`concierto-${primero}`)?.focus();
      return;
    }

    enCurso.current = true;
    setEnviando(true);
    setErrorEnvio(null);
    try {
      if (evento) {
        if (!cambios) throw new Error('cambios inválidos');
        await adminEventosService.actualizar(evento.id, cambios);
      } else {
        const cuerpo = cuerpoDeAlta(valores);
        if (!cuerpo) throw new Error('cuerpo inválido');
        await adminEventosService.crear(cuerpo);
      }
      guardado.current = true;
      const nombre = valores.nombre.trim();
      navigate('/admin/conciertos', {
        state: { aviso: editando ? `Cambios guardados en «${nombre}».` : `«${nombre}» ya está publicado.` },
      });
    } catch (error) {
      setErrorEnvio(mensajeAlGuardarConcierto(error));
      enCurso.current = false;
      setEnviando(false);
    }
  }

  return (
    <form className="admin-conciertos-formulario" onSubmit={enviar} noValidate>
      {errorEnvio && <Alert tono="error">{errorEnvio}</Alert>}

      <Field
        id="concierto-nombre"
        etiqueta="Nombre del concierto"
        value={valores.nombre}
        onChange={(e) => cambiar('nombre', e.target.value)}
        error={errores.nombre}
        autoComplete="off"
        disabled={enviando}
      />

      <div className="admin-conciertos-fila">
        <Field
          id="concierto-fecha"
          etiqueta="Fecha"
          type="date"
          value={valores.fecha}
          onChange={(e) => cambiar('fecha', e.target.value)}
          error={errores.fecha}
          disabled={enviando}
        />
        <Field
          id="concierto-hora"
          etiqueta="Hora"
          type="time"
          value={valores.hora}
          onChange={(e) => cambiar('hora', e.target.value)}
          error={errores.hora}
          ayuda="Hora del centro de México."
          disabled={enviando}
        />
      </div>

      <CampoSeleccion
          id="concierto-lugar"
          etiqueta="Lugar"
          value={valores.lugar}
          onChange={(e) => cambiar('lugar', e.target.value)}
          error={errores.lugar}
          disabled={enviando}
        >
          <option value="">Elige el lugar…</option>
          {valores.lugar === LUGAR_ACTUAL && evento && (
            <option value={LUGAR_ACTUAL}>
              {[evento.escenario, evento.ubicacion].filter(Boolean).join(' · ')} (actual, se conserva)
            </option>
          )}
          {LUGARES.map((lugar) => (
            <option key={lugar.escenario} value={lugar.escenario}>
              {etiquetaDeLugar(lugar)}
            </option>
          ))}
      </CampoSeleccion>

      <Field
        id="concierto-capacidad"
        etiqueta="Capacidad (personas)"
        inputMode="numeric"
        value={valores.capacidad}
        onChange={(e) => cambiar('capacidad', e.target.value)}
        error={errores.capacidad}
        ayuda={editando ? 'Déjalo vacío para conservar la capacidad actual.' : undefined}
        autoComplete="off"
        disabled={enviando}
      />

      <CampoArea
        id="concierto-descripcion"
        etiqueta="Descripción (opcional)"
        rows={4}
        value={valores.descripcion}
        onChange={(e) => cambiar('descripcion', e.target.value)}
        error={errores.descripcion}
        ayuda={`${valores.descripcion.length} de ${MAX_DESCRIPCION} caracteres`}
        disabled={enviando}
      />

      <p className="admin-conciertos-precios">
        Los boletos se venden con los precios oficiales del festival: VIP $4,500 y General $1,200.
      </p>

      <div className="admin-conciertos-acciones">
        <ButtonLink to="/admin/conciertos" variante="fantasma">
          Volver a conciertos
        </ButtonLink>
        <Button type="submit" mayusculas cargando={enviando} disabled={sinCambios}>
          {editando ? 'Guardar cambios' : 'Publicar concierto'}
        </Button>
      </div>
      {sinCambios && (
        <p className="meta admin-conciertos-nota" role="status">
          Sin cambios que guardar.
        </p>
      )}
    </form>
  );
}
