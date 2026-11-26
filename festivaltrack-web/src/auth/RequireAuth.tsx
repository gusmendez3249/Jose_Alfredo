import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './useAuth';
import type { EstadoNavegacionLogin } from './rutas';

/** Protege una ruta: sin sesión manda al login recordando a dónde se quería ir. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { autenticado, motivoCierre } = useAuth();
  const location = useLocation();

  if (!autenticado) {
    const estado: EstadoNavegacionLogin = {
      from: `${location.pathname}${location.search}`,
      sesionExpirada: motivoCierre === 'expirada',
    };
    return <Navigate to="/login" replace state={estado} />;
  }
  return <>{children}</>;
}
