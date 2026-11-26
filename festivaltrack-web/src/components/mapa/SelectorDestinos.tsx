import { useId, useState } from 'react';
import { clases } from '../ui/clases';
import { CATEGORIAS, coincideBusqueda, ETIQUETA_CATEGORIA, type CategoriaDestino, type Destino } from './destinos';
import { PinCategoria } from './pines';

interface Props {
  destinos: readonly Destino[];
  seleccionId: string | null;
  onElegir: (id: string) => void;
}

/** Filtro por categoría (chips), búsqueda por nombre o dirección y lista de destinos. */
export default function SelectorDestinos({ destinos, seleccionId, onElegir }: Props) {
  const [categoria, setCategoria] = useState<CategoriaDestino | 'todos'>('todos');
  const [texto, setTexto] = useState('');
  const idBusqueda = useId();

  const visibles = destinos.filter((d) => (categoria === 'todos' || d.categoria === categoria) && coincideBusqueda(d, texto));

  return (
    <div className="mapa-fest__selector">
      <div className="mapa-fest__buscador">
        <label htmlFor={idBusqueda} className="solo-lectores">
          Buscar destino
        </label>
        <input
          id={idBusqueda}
          type="search"
          className="mapa-fest__buscar"
          placeholder="Buscar destino"
          value={texto}
          maxLength={80}
          autoComplete="off"
          onChange={(e) => setTexto(e.target.value)}
        />
      </div>

      <div className="mapa-fest__chips" role="group" aria-label="Filtrar por categoría">
        <button
          type="button"
          className={clases('mapa-fest__chip', categoria === 'todos' && 'mapa-fest__chip--activo')}
          aria-pressed={categoria === 'todos'}
          onClick={() => setCategoria('todos')}
        >
          Todos
        </button>
        {CATEGORIAS.map((c) => (
          <button
            key={c}
            type="button"
            className={clases('mapa-fest__chip', categoria === c && 'mapa-fest__chip--activo')}
            aria-pressed={categoria === c}
            onClick={() => setCategoria(c)}
          >
            <PinCategoria categoria={c} tamano={18} />
            {ETIQUETA_CATEGORIA[c]}
          </button>
        ))}
      </div>

      {visibles.length === 0 ? (
        <p className="meta mapa-fest__vacio" role="status">
          Ningún destino coincide con tu búsqueda.
        </p>
      ) : (
        <ul className="mapa-fest__lista" aria-label="Destinos">
          {visibles.map((d) => {
            const elegido = d.id === seleccionId;
            return (
              <li key={d.id}>
                <button
                  type="button"
                  className={clases('mapa-fest__destino', elegido && 'mapa-fest__destino--elegido')}
                  aria-pressed={elegido}
                  onClick={() => onElegir(d.id)}
                >
                  <PinCategoria categoria={d.categoria} tamano={26} />
                  <span className="mapa-fest__destino-texto">
                    <span className="mapa-fest__destino-nombre">{d.nombre}</span>
                    {d.direccion && <span className="mapa-fest__destino-direccion">{d.direccion}</span>}
                    {!d.coords && <span className="mapa-fest__destino-aviso">Sin ubicación en el mapa</span>}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
