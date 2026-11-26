import type { CategoriaDestino } from './destinos';

/**
 * Formas de los pines. Cada categoría tiene su propia silueta (cuadrado, círculo, rombo) y un glifo,
 * así que no dependen del color. Las mismas formas se pintan en React (lista, filtros) y como
 * texto SVG para los marcadores de Leaflet.
 */
export interface Forma {
  tag: 'path' | 'circle' | 'rect';
  clase: 'forma' | 'glifo';
  atributos: Record<string, string | number>;
}

export const FORMAS: Record<CategoriaDestino, readonly Forma[]> = {
  escenario: [
    { tag: 'rect', clase: 'forma', atributos: { x: 3, y: 3, width: 26, height: 26, rx: 6 } },
    { tag: 'path', clase: 'glifo', atributos: { d: 'M16 8.5l2.4 4.9 5.4.8-3.9 3.8.9 5.4-4.8-2.5-4.8 2.5.9-5.4-3.9-3.8 5.4-.8z' } },
  ],
  concierto: [
    { tag: 'circle', clase: 'forma', atributos: { cx: 16, cy: 16, r: 14 } },
    { tag: 'path', clase: 'glifo', atributos: { d: 'M14 8v9.3a3.2 3.2 0 1 0 2 2.9V12h4V8z' } },
  ],
  historico: [
    { tag: 'path', clase: 'forma', atributos: { d: 'M16 1.5 30.5 16 16 30.5 1.5 16z' } },
    { tag: 'path', clase: 'glifo', atributos: { d: 'M16 9l6.5 3H9.5zM10 20.5h12v1.8H10zM11 13.5h1.8v6.2H11zm4.1 0h1.8v6.2h-1.8zm4.1 0H21v6.2h-1.8z' } },
  ],
};

export const claseSvg = (categoria: CategoriaDestino, seleccionado: boolean) =>
  `mapa-pin__svg mapa-pin__svg--${categoria}${seleccionado ? ' mapa-pin__svg--sel' : ''}`;

const ESPACIO_SVG = 'http://www.w3.org/2000/svg';
const cacheHtml = new Map<string, string>();

/** SVG del marcador como texto, para `L.divIcon` (Leaflet lo inserta con innerHTML; no lleva atributos `style`). */
export function htmlPin(categoria: CategoriaDestino, seleccionado: boolean): string {
  const clave = `${categoria}:${seleccionado}`;
  const guardado = cacheHtml.get(clave);
  if (guardado) return guardado;

  const svg = document.createElementNS(ESPACIO_SVG, 'svg');
  svg.setAttribute('class', claseSvg(categoria, seleccionado));
  svg.setAttribute('viewBox', '0 0 32 32');
  svg.setAttribute('aria-hidden', 'true');
  svg.setAttribute('focusable', 'false');
  for (const f of FORMAS[categoria]) {
    const nodo = document.createElementNS(ESPACIO_SVG, f.tag);
    nodo.setAttribute('class', `mapa-pin__${f.clase}`);
    for (const [nombre, valor] of Object.entries(f.atributos)) nodo.setAttribute(nombre, String(valor));
    svg.appendChild(nodo);
  }
  cacheHtml.set(clave, svg.outerHTML);
  return svg.outerHTML;
}
