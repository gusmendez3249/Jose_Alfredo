import type { CambiosEvento, NuevoEvento } from '../../../services/adminEventos.service';
import type { Evento } from '../../../services/tipos';
import { instanteDeMexico, isoAMexico, mexicoAIso } from './fechaMexico';
import { lugarDeEvento, lugarPorEscenario } from './lugares';

export const CAPACIDAD_MAXIMA = 100_000;
export const MAX_NOMBRE = 150;
export const MAX_DESCRIPCION = 2000;

/** Valor del menú de lugar cuando el concierto tiene un lugar que no está en la lista fija: se conserva tal cual. */
export const LUGAR_ACTUAL = '__actual__';

export interface ValoresFormulario {
  nombre: string;
  /** "AAAA-MM-DD" (lo que da `<input type="date">`). */
  fecha: string;
  /** "HH:mm" en hora de México. */
  hora: string;
  /** Escenario de un lugar de `LUGARES`, `LUGAR_ACTUAL` o vacío. */
  lugar: string;
  capacidad: string;
  descripcion: string;
}

export type CampoFormulario = keyof ValoresFormulario;
export type ErroresFormulario = Partial<Record<CampoFormulario, string>>;

export const VALORES_NUEVOS: ValoresFormulario = {
  nombre: '',
  fecha: '',
  hora: '',
  lugar: '',
  capacidad: '1000',
  descripcion: '',
};

/** Valores iniciales para editar un concierto: la fecha vuelve a verse en hora de México. */
export function valoresDeEvento(evento: Evento): ValoresFormulario {
  const cuando = isoAMexico(evento.fechaHora);
  const conocido = lugarDeEvento(evento);
  const tieneLugar = Boolean((evento.escenario ?? '').trim() || (evento.ubicacion ?? '').trim());
  return {
    nombre: evento.nombre,
    fecha: cuando?.fecha ?? '',
    hora: cuando?.hora ?? '',
    lugar: conocido ? conocido.escenario : tieneLugar ? LUGAR_ACTUAL : '',
    capacidad: String(evento.capacidad),
    descripcion: evento.descripcion ?? '',
  };
}

interface OpcionesValidacion {
  editando: boolean;
  /** Instante actual en ms (se inyecta para poder probar). */
  ahora: number;
}

/** Mismas reglas y textos que el móvil (`FormularioEvento.validar`), pero con el error junto a cada campo. */
export function validarFormulario(v: ValoresFormulario, { editando, ahora }: OpcionesValidacion): ErroresFormulario {
  const errores: ErroresFormulario = {};
  const nombre = v.nombre.trim();
  if (!nombre) errores.nombre = 'Escribe el nombre del concierto';
  else if (nombre.length > MAX_NOMBRE) errores.nombre = `El nombre no puede pasar de ${MAX_NOMBRE} caracteres`;

  if (!v.fecha || !v.hora) {
    if (!v.fecha) errores.fecha = 'Elige la fecha y la hora';
    if (!v.hora) errores.hora = 'Elige la fecha y la hora';
  } else {
    const instante = instanteDeMexico(v.fecha, v.hora);
    if (instante === null) errores.fecha = 'La fecha u hora no es válida';
    else if (!editando && instante < ahora) errores.fecha = 'La fecha y hora ya pasaron';
  }

  if (!v.lugar) errores.lugar = 'Elige el lugar del concierto';

  const capacidad = v.capacidad.trim();
  if (!capacidad) {
    if (!editando) errores.capacidad = 'Escribe la capacidad';
  } else if (!/^-?\d+$/.test(capacidad)) {
    errores.capacidad = 'La capacidad debe ser un número';
  } else {
    const n = Number(capacidad);
    if (n < 1 || n > CAPACIDAD_MAXIMA) errores.capacidad = 'La capacidad debe estar entre 1 y 100,000';
  }

  if (v.descripcion.trim().length > MAX_DESCRIPCION) {
    errores.descripcion = `La descripción no puede pasar de ${MAX_DESCRIPCION} caracteres`;
  }
  return errores;
}

/** Cuerpo del alta. Debe llamarse solo con valores ya validados. */
export function cuerpoDeAlta(v: ValoresFormulario): NuevoEvento | null {
  const fechaHora = mexicoAIso(v.fecha, v.hora);
  const lugar = lugarPorEscenario(v.lugar);
  const capacidad = Number(v.capacidad.trim());
  if (!fechaHora || !lugar || !Number.isInteger(capacidad)) return null;
  const descripcion = v.descripcion.trim();
  return {
    nombre: v.nombre.trim(),
    fechaHora,
    ubicacion: lugar.ubicacion,
    escenario: lugar.escenario,
    latitud: lugar.latitud,
    longitud: lugar.longitud,
    capacidad,
    ...(descripcion ? { descripcion } : {}),
    estado: 'PUBLICADO',
  };
}

/**
 * Solo lo que cambió respecto al concierto guardado; `{}` si no hay nada que enviar.
 * La fecha se compara por sus partes en hora de México (no por el ISO) para que los segundos
 * del servidor no cuenten como un cambio.
 */
export function cambiosDeEdicion(original: Evento, v: ValoresFormulario): CambiosEvento | null {
  const cambios: CambiosEvento = {};

  const nombre = v.nombre.trim();
  if (nombre !== original.nombre) cambios.nombre = nombre;

  const antes = isoAMexico(original.fechaHora);
  if (!antes || antes.fecha !== v.fecha || antes.hora !== v.hora) {
    const fechaHora = mexicoAIso(v.fecha, v.hora);
    if (!fechaHora) return null;
    cambios.fechaHora = fechaHora;
  }

  if (v.lugar !== LUGAR_ACTUAL) {
    const lugar = lugarPorEscenario(v.lugar);
    if (!lugar) return null;
    if (lugarDeEvento(original)?.escenario !== lugar.escenario) {
      cambios.escenario = lugar.escenario;
      cambios.ubicacion = lugar.ubicacion;
      cambios.latitud = lugar.latitud;
      cambios.longitud = lugar.longitud;
    }
  }

  const capacidadTexto = v.capacidad.trim();
  if (capacidadTexto) {
    const capacidad = Number(capacidadTexto);
    if (!Number.isInteger(capacidad)) return null;
    if (capacidad !== original.capacidad) cambios.capacidad = capacidad;
  }

  const descripcion = v.descripcion.trim();
  if (descripcion !== (original.descripcion ?? '').trim()) cambios.descripcion = descripcion;

  return cambios;
}
