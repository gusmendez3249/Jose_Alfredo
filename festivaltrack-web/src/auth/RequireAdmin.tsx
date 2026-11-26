import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { RequireAuth } from './RequireAuth';
import { useAuth } from './useAuth';

/**
 * Protege el panel de administración. Es solo una comodidad de interfaz: quien decide de
 * verdad es el backend, que exige el rol ADMINISTRADOR en cada endpoint de gestión.
 */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { usuario } = useAuth();
  return (
    <RequireAuth>
      {usuario?.rol === 'ADMINISTRADOR' ? <>{children}</> : <Navigate to="/" replace />}
    </RequireAuth>
  );
}
