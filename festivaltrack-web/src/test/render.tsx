import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Aplicacion } from '../App';

/** Monta la aplicación completa (layout + rutas) en una ruta inicial, sin navegador real. */
export function renderizarApp(ruta = '/', estado?: unknown) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: ruta, state: estado }]}>
      <Aplicacion />
    </MemoryRouter>,
  );
}
