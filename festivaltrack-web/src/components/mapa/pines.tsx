import { createElement } from 'react';
import type { CategoriaDestino } from './destinos';
import { claseSvg, FORMAS } from './formasPin';

/** Pin decorativo para la interfaz (lista, filtros). */
export function PinCategoria({ categoria, tamano = 22 }: { categoria: CategoriaDestino; tamano?: number }) {
  return (
    <svg
      className={claseSvg(categoria, false)}
      width={tamano}
      height={tamano}
      viewBox="0 0 32 32"
      aria-hidden="true"
      focusable="false"
    >
      {FORMAS[categoria].map((f, i) => createElement(f.tag, { key: i, className: `mapa-pin__${f.clase}`, ...f.atributos }))}
    </svg>
  );
}
