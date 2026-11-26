import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, NavLink, useLocation, useMatch, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import { ButtonLink, Button } from './Button';
import { ConfirmDialog } from './ConfirmDialog';
import { ReproductorGlobal } from '../audio/ReproductorGlobal';
import {
  IconoBoleto,
  IconoComunidad,
  IconoEnVivo,
  IconoEventos,
  IconoInicio,
  IconoLibro,
  IconoMapa,
  IconoMarca,
  IconoSalir,
} from './Icons';
import { clases } from './clases';

/** Navegación de la cabecera (escritorio). */
const ENLACES = [
  { a: '/', etiqueta: 'Inicio', Icono: IconoInicio, exacto: true },
  { a: '/events', etiqueta: 'Eventos', Icono: IconoEventos, exacto: false },
  { a: '/biografia', etiqueta: 'Biografía', Icono: IconoLibro, exacto: false },
  { a: '/comunidad', etiqueta: 'Comunidad', Icono: IconoComunidad, exacto: false },
  { a: '/mapa', etiqueta: 'Mapa', Icono: IconoMapa, exacto: false },
  { a: '/en-vivo', etiqueta: 'En vivo', Icono: IconoEnVivo, exacto: false },
  { a: '/mis-boletos', etiqueta: 'Mis boletos', Icono: IconoBoleto, exacto: false },
] as const;

/** La barra inferior del móvil solo tiene sitio para cinco; el resto se abre desde Inicio. */
const RUTAS_BARRA_INFERIOR: readonly string[] = ['/', '/events', '/mapa', '/comunidad', '/mis-boletos'];
const ENLACES_INFERIORES = ENLACES.filter(({ a }) => RUTAS_BARRA_INFERIOR.includes(a));

/** Estructura común: cabecera fija, navegación (inferior en móvil), contenido y pie. */
export function AppLayout({ children }: { children: ReactNode }) {
  const { autenticado, usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const mapaDeEvento = useMatch('/events/:id/map') !== null;
  // Las páginas de mapa ocupan toda la pantalla: sin pie y sin márgenes del contenido.
  const esMapa = mapaDeEvento || pathname === '/mapa';
  const principal = useRef<HTMLElement>(null);
  const primeraCarga = useRef(true);
  const [confirmando, setConfirmando] = useState(false);
  const saliendo = useRef(false);

  // Al cambiar de página se vuelve arriba y el foco pasa al contenido (como haría una carga normal).
  useEffect(() => {
    if (primeraCarga.current) {
      primeraCarga.current = false;
      return;
    }
    window.scrollTo(0, 0);
    principal.current?.focus({ preventScroll: true });
  }, [pathname]);

  // Primero se va al inicio y solo entonces se cierra la sesión: si se cerrara estando en una ruta
  // protegida, RequireAuth llevaría al login y "Cerrar sesión" no terminaría en el inicio.
  const salir = () => {
    setConfirmando(false);
    if (pathname === '/') {
      cerrarSesion();
      return;
    }
    saliendo.current = true;
    navigate('/', { replace: true });
  };

  useEffect(() => {
    if (saliendo.current && pathname === '/') {
      saliendo.current = false;
      cerrarSesion();
    }
  }, [pathname, cerrarSesion]);

  return (
    <div className={clases('app', esMapa && 'app--mapa')}>
      <a
        className="saltar-contenido"
        href="#contenido"
        onClick={(e) => {
          e.preventDefault();
          principal.current?.focus();
        }}
      >
        Saltar al contenido
      </a>

      <header className="cabecera">
        <div className="cabecera__interior">
          <Link to="/" className="marca" aria-label="FestivalTrack, ir al inicio">
            <IconoMarca tamano={26} />
            <span>FestivalTrack</span>
          </Link>

          <nav className="nav-principal" aria-label="Principal">
            {ENLACES.map(({ a, etiqueta, exacto }) => (
              <NavLink key={a} to={a} end={exacto} className="nav-principal__enlace">
                {etiqueta}
              </NavLink>
            ))}
            {usuario?.rol === 'ADMINISTRADOR' && (
              <NavLink to="/admin" className="nav-principal__enlace">
                Admin
              </NavLink>
            )}
          </nav>

          <div className="cuenta">
            {autenticado ? (
              <>
                {usuario && <span className="cuenta__saludo">Hola, {usuario.nombre}</span>}
                <Button variante="fantasma" tamano="chico" onClick={() => setConfirmando(true)}>
                  <IconoSalir tamano={18} />
                  Cerrar sesión
                </Button>
              </>
            ) : (
              <>
                <ButtonLink to="/login" variante="fantasma" tamano="chico">
                  Iniciar sesión
                </ButtonLink>
                <ButtonLink to="/register" variante="primario" tamano="chico">
                  Registrarse
                </ButtonLink>
              </>
            )}
          </div>
        </div>
      </header>

      <main id="contenido" ref={principal} tabIndex={-1} className="principal">
        {children}
      </main>

      {!esMapa && (
        <footer className="pie">
          <p>FestivalTrack · Festival José Alfredo Jiménez · Dolores Hidalgo, Guanajuato</p>
        </footer>
      )}

      <ReproductorGlobal />

      <nav className="nav-inferior" aria-label="Navegación inferior">
        {ENLACES_INFERIORES.map(({ a, etiqueta, Icono, exacto }) => (
          <NavLink key={a} to={a} end={exacto} className="nav-inferior__enlace">
            <Icono tamano={24} />
            <span>{etiqueta}</span>
          </NavLink>
        ))}
      </nav>

      <ConfirmDialog
        abierto={confirmando}
        titulo="¿Cerrar sesión?"
        texto="Tendrás que iniciar sesión de nuevo para ver tus boletos y comprar."
        textoConfirmar="Cerrar sesión"
        peligro
        onConfirmar={salir}
        onCancelar={() => setConfirmando(false)}
      />
    </div>
  );
}
