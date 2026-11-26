import { NavLink } from 'react-router-dom';

const SECCIONES = [
  { a: '/admin', etiqueta: 'Resumen', exacto: true },
  { a: '/admin/conciertos', etiqueta: 'Conciertos', exacto: false },
  { a: '/admin/canciones', etiqueta: 'Canciones', exacto: false },
  { a: '/admin/usuarios', etiqueta: 'Usuarios', exacto: false },
] as const;

/** Pestañas del panel de administración; va al inicio de cada página de /admin. */
export function AdminNav() {
  return (
    <nav className="admin-nav" aria-label="Administración">
      {SECCIONES.map(({ a, etiqueta, exacto }) => (
        <NavLink key={a} to={a} end={exacto} className="admin-nav__enlace">
          {etiqueta}
        </NavLink>
      ))}
    </nav>
  );
}
