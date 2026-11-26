import { describe, expect, it } from 'vitest';
import { crearEvento } from '../../../test/datos';
import { cambiosDeEdicion, cuerpoDeAlta, LUGAR_ACTUAL, validarFormulario, valoresDeEvento, VALORES_NUEVOS, type ValoresFormulario } from './formulario';

// 1 de noviembre de 2026, 12:00 en México.
const AHORA = Date.UTC(2026, 10, 1, 18, 0);

const validos: ValoresFormulario = {
  nombre: 'Noche de Bohemia',
  fecha: '2026-11-25',
  hora: '20:00',
  lugar: 'Plaza Principal',
  capacidad: '800',
  descripcion: '',
};

describe('validarFormulario', () => {
  it('acepta un formulario completo', () => {
    expect(validarFormulario(validos, { editando: false, ahora: AHORA })).toEqual({});
  });

  it('pide lo que falta con los textos del móvil', () => {
    const errores = validarFormulario(
      { ...VALORES_NUEVOS, capacidad: '' },
      { editando: false, ahora: AHORA },
    );
    expect(errores).toEqual({
      nombre: 'Escribe el nombre del concierto',
      fecha: 'Elige la fecha y la hora',
      hora: 'Elige la fecha y la hora',
      lugar: 'Elige el lugar del concierto',
      capacidad: 'Escribe la capacidad',
    });
  });

  it('rechaza una fecha pasada solo al crear', () => {
    const pasado = { ...validos, fecha: '2026-10-01' };
    expect(validarFormulario(pasado, { editando: false, ahora: AHORA }).fecha).toBe('La fecha y hora ya pasaron');
    expect(validarFormulario(pasado, { editando: true, ahora: AHORA }).fecha).toBeUndefined();
  });

  it('compara la fecha en hora de México: 11:59 de hoy en México aún no pasó a las 11:58', () => {
    const hoy = { ...validos, fecha: '2026-11-01', hora: '12:01' };
    expect(validarFormulario(hoy, { editando: false, ahora: AHORA }).fecha).toBeUndefined();
    expect(validarFormulario({ ...hoy, hora: '11:59' }, { editando: false, ahora: AHORA }).fecha).toBe(
      'La fecha y hora ya pasaron',
    );
  });

  it('valida la capacidad: número entero entre 1 y 100,000', () => {
    const c = (capacidad: string, editando = false) =>
      validarFormulario({ ...validos, capacidad }, { editando, ahora: AHORA }).capacidad;
    expect(c('abc')).toBe('La capacidad debe ser un número');
    expect(c('1.5')).toBe('La capacidad debe ser un número');
    expect(c('0')).toBe('La capacidad debe estar entre 1 y 100,000');
    expect(c('-3')).toBe('La capacidad debe estar entre 1 y 100,000');
    expect(c('100001')).toBe('La capacidad debe estar entre 1 y 100,000');
    expect(c('100000')).toBeUndefined();
    expect(c('1')).toBeUndefined();
    expect(c('')).toBe('Escribe la capacidad');
    expect(c('', true)).toBeUndefined(); // al editar, vacío conserva la actual
  });

  it('limita el largo del nombre y de la descripción como el backend', () => {
    const errores = validarFormulario(
      { ...validos, nombre: 'a'.repeat(151), descripcion: 'b'.repeat(2001) },
      { editando: false, ahora: AHORA },
    );
    expect(errores.nombre).toMatch(/150/);
    expect(errores.descripcion).toMatch(/2000/);
  });

  it('un nombre de solo espacios cuenta como vacío', () => {
    expect(validarFormulario({ ...validos, nombre: '   ' }, { editando: false, ahora: AHORA }).nombre).toBe(
      'Escribe el nombre del concierto',
    );
  });
});

describe('cuerpoDeAlta', () => {
  it('arma el cuerpo con la fecha en UTC y los datos del lugar', () => {
    expect(cuerpoDeAlta({ ...validos, nombre: '  Noche de Bohemia ', descripcion: ' Con mariachi ' })).toEqual({
      nombre: 'Noche de Bohemia',
      fechaHora: '2026-11-26T02:00:00.000Z',
      ubicacion: 'Jardín Principal, Dolores Hidalgo',
      escenario: 'Plaza Principal',
      latitud: 21.1563,
      longitud: -100.9338,
      capacidad: 800,
      descripcion: 'Con mariachi',
      estado: 'PUBLICADO',
    });
  });

  it('omite la descripción vacía', () => {
    expect(cuerpoDeAlta(validos)).not.toHaveProperty('descripcion');
  });
});

describe('valoresDeEvento', () => {
  it('muestra la fecha guardada en hora de México y preselecciona el lugar', () => {
    const v = valoresDeEvento(
      crearEvento({ fechaHora: '2026-11-26T02:00:00.000Z', escenario: 'Casa Museo', capacidad: 500, descripcion: null }),
    );
    expect(v).toEqual({
      nombre: 'Noche de Bohemia',
      fecha: '2026-11-25',
      hora: '20:00',
      lugar: 'Casa Museo',
      capacidad: '500',
      descripcion: '',
    });
  });

  it('reconoce el lugar por coordenadas si el escenario no coincide', () => {
    const v = valoresDeEvento(crearEvento({ escenario: 'Otro nombre', latitud: 21.1571, longitud: -100.9406 }));
    expect(v.lugar).toBe('Mausoleo de José Alfredo Jiménez');
  });

  it('conserva un lugar desconocido con la opción "actual"', () => {
    const v = valoresDeEvento(crearEvento({ escenario: 'Teatro X', ubicacion: 'Otra parte', latitud: 10, longitud: 10 }));
    expect(v.lugar).toBe(LUGAR_ACTUAL);
  });
});

describe('cambiosDeEdicion', () => {
  const original = crearEvento({
    nombre: 'Noche de Bohemia',
    fechaHora: '2026-11-26T02:00:00.000Z',
    escenario: 'Plaza Principal',
    capacidad: 800,
    descripcion: null,
  });

  it('sin tocar nada no hay cambios (los segundos del servidor no cuentan)', () => {
    const conSegundos = crearEvento({ ...original, fechaHora: '2026-11-26T02:00:41.000Z' });
    expect(cambiosDeEdicion(conSegundos, valoresDeEvento(conSegundos))).toEqual({});
    expect(cambiosDeEdicion(original, valoresDeEvento(original))).toEqual({});
  });

  it('envía solo lo que cambió', () => {
    const v = { ...valoresDeEvento(original), nombre: 'Otro nombre', hora: '21:30', capacidad: '900' };
    expect(cambiosDeEdicion(original, v)).toEqual({
      nombre: 'Otro nombre',
      fechaHora: '2026-11-26T03:30:00.000Z',
      capacidad: 900,
    });
  });

  it('al cambiar de lugar manda escenario, ubicación y coordenadas', () => {
    const v = { ...valoresDeEvento(original), lugar: 'Casa Museo' };
    expect(cambiosDeEdicion(original, v)).toEqual({
      escenario: 'Casa Museo',
      ubicacion: 'Calle Guanajuato 13, Centro, Dolores Hidalgo',
      latitud: 21.1574,
      longitud: -100.933,
    });
  });

  it('con la capacidad vacía conserva la actual y vaciar la descripción se envía como cadena vacía', () => {
    const conDescripcion = crearEvento({ ...original, descripcion: 'Texto' });
    const v = { ...valoresDeEvento(conDescripcion), capacidad: '', descripcion: '  ' };
    expect(cambiosDeEdicion(conDescripcion, v)).toEqual({ descripcion: '' });
  });
});
