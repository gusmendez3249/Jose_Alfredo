import { useCallback, useEffect, useReducer, useRef } from 'react';
import { esCancelacion, mensajeDeError } from '../../lib/errores';
import { foroService, fusionarPaginas, type Publicacion, type TipoPublicacion } from '../../services/foro.service';

export interface EstadoForo {
  estado: 'cargando' | 'error' | 'listo';
  publicaciones: Publicacion[];
  /** null: no hay más páginas. */
  cursor: string | null;
  error: string | null;
  cargandoMas: boolean;
  errorMas: string | null;
}

type Accion =
  | { tipo: 'inicio' }
  | { tipo: 'primera'; publicaciones: Publicacion[]; cursor: string | null }
  | { tipo: 'error'; mensaje: string }
  | { tipo: 'mas-inicio' }
  | { tipo: 'mas-ok'; publicaciones: Publicacion[]; cursor: string | null }
  | { tipo: 'mas-error'; mensaje: string }
  | { tipo: 'actualizar'; id: string; cambio: (p: Publicacion) => Publicacion }
  | { tipo: 'quitar'; id: string }
  | { tipo: 'agregar'; publicacion: Publicacion };

const INICIAL: EstadoForo = {
  estado: 'cargando',
  publicaciones: [],
  cursor: null,
  error: null,
  cargandoMas: false,
  errorMas: null,
};

function reductor(s: EstadoForo, a: Accion): EstadoForo {
  switch (a.tipo) {
    case 'inicio':
      return INICIAL;
    case 'primera':
      return { ...INICIAL, estado: 'listo', publicaciones: fusionarPaginas([], a.publicaciones), cursor: a.cursor };
    case 'error':
      return { ...INICIAL, estado: 'error', error: a.mensaje };
    case 'mas-inicio':
      return { ...s, cargandoMas: true, errorMas: null };
    case 'mas-ok':
      return {
        ...s,
        cargandoMas: false,
        publicaciones: fusionarPaginas(s.publicaciones, a.publicaciones),
        cursor: a.cursor,
      };
    case 'mas-error':
      return { ...s, cargandoMas: false, errorMas: a.mensaje };
    case 'actualizar':
      return { ...s, publicaciones: s.publicaciones.map((p) => (p.id === a.id ? a.cambio(p) : p)) };
    case 'quitar':
      return { ...s, publicaciones: s.publicaciones.filter((p) => p.id !== a.id) };
    case 'agregar':
      return { ...s, publicaciones: fusionarPaginas([a.publicacion], s.publicaciones) };
  }
}

/**
 * Lista del foro con paginación por cursor. Cambiar `tipo` o llamar a `recargar()` empieza de
 * cero y cancela lo que estuviera en vuelo (también la página siguiente pendiente).
 */
export function useForo(tipo: TipoPublicacion | null) {
  const [estado, despachar] = useReducer(reductor, INICIAL);
  const [version, recargar] = useReducer((n: number) => n + 1, 0);
  const controlMas = useRef<AbortController | null>(null);
  const cursorRef = useRef<string | null>(null);
  const cargandoMasRef = useRef(false);

  useEffect(() => {
    cursorRef.current = estado.cursor;
  }, [estado.cursor]);

  useEffect(() => {
    const control = new AbortController();
    controlMas.current?.abort();
    cargandoMasRef.current = false;
    despachar({ tipo: 'inicio' });
    foroService
      .listar({ tipo }, control.signal)
      .then((pagina) => despachar({ tipo: 'primera', publicaciones: pagina.publicaciones, cursor: pagina.siguienteCursor }))
      .catch((error: unknown) => {
        if (!esCancelacion(error)) despachar({ tipo: 'error', mensaje: mensajeDeError(error) });
      });
    return () => {
      control.abort();
      controlMas.current?.abort();
    };
  }, [tipo, version]);

  const cargarMas = useCallback(() => {
    const before = cursorRef.current;
    if (!before || cargandoMasRef.current) return;
    cargandoMasRef.current = true;
    const control = new AbortController();
    controlMas.current = control;
    despachar({ tipo: 'mas-inicio' });
    foroService
      .listar({ tipo, before }, control.signal)
      .then((pagina) => despachar({ tipo: 'mas-ok', publicaciones: pagina.publicaciones, cursor: pagina.siguienteCursor }))
      .catch((error: unknown) => {
        if (!esCancelacion(error)) despachar({ tipo: 'mas-error', mensaje: mensajeDeError(error) });
      })
      .finally(() => {
        if (controlMas.current === control) cargandoMasRef.current = false;
      });
  }, [tipo]);

  const actualizar = useCallback(
    (id: string, cambio: (p: Publicacion) => Publicacion) => despachar({ tipo: 'actualizar', id, cambio }),
    [],
  );
  const quitar = useCallback((id: string) => despachar({ tipo: 'quitar', id }), []);
  const agregar = useCallback((publicacion: Publicacion) => despachar({ tipo: 'agregar', publicacion }), []);

  return { ...estado, cargarMas, recargar, actualizar, quitar, agregar };
}
