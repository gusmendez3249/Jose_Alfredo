import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { RequireAdmin } from './auth/RequireAdmin';
import { RequireAuth } from './auth/RequireAuth';
import { ErrorBoundary } from './components/ErrorBoundary';
import NotFound from './components/NotFound';
import { AppLayout } from './components/ui/AppLayout';
import { Spinner } from './components/ui/Spinner';

// Cada página se descarga solo cuando se visita (#93): la primera carga ya no trae
// el mapa (Leaflet), el carrusel (Swiper) ni el generador de QR de golpe.
const Home = lazy(() => import('./pages/Home'));
const Login = lazy(() => import('./pages/Login'));
const Register = lazy(() => import('./pages/Register'));
const Events = lazy(() => import('./pages/Events'));
const MapRoute = lazy(() => import('./pages/MapRoute'));
const Checkout = lazy(() => import('./pages/Checkout'));
const MyTickets = lazy(() => import('./pages/MyTickets'));
const Biography = lazy(() => import('./pages/Biography'));
const EventDetail = lazy(() => import('./pages/EventDetail'));
const Community = lazy(() => import('./pages/Community'));
const Live = lazy(() => import('./pages/Live'));
const FestivalMap = lazy(() => import('./pages/FestivalMap'));
const VincularTv = lazy(() => import('./pages/VincularTv'));
const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard'));
const AdminEvents = lazy(() => import('./pages/admin/AdminEvents'));
const AdminEventForm = lazy(() => import('./pages/admin/AdminEventForm'));
const AdminRepertoire = lazy(() => import('./pages/admin/AdminRepertoire'));
const AdminSongs = lazy(() => import('./pages/admin/AdminSongs'));
const AdminUsers = lazy(() => import('./pages/admin/AdminUsers'));

/** Rutas del panel: [ruta, página]. Todas exigen rol de administrador. */
const RUTAS_ADMIN = [
  ['/admin', AdminDashboard],
  ['/admin/conciertos', AdminEvents],
  ['/admin/conciertos/nuevo', AdminEventForm],
  ['/admin/conciertos/:id', AdminEventForm],
  ['/admin/conciertos/:id/repertorio', AdminRepertoire],
  ['/admin/canciones', AdminSongs],
  ['/admin/usuarios', AdminUsers],
] as const;

/** Se muestra mientras llega el código de la página. */
function Cargando() {
  return (
    <div role="status" className="carga-pagina">
      <Spinner grande />
      <span>Cargando…</span>
    </div>
  );
}

function Rutas() {
  const location = useLocation();

  return (
    // key = ruta: al navegar a otra página el ErrorBoundary empieza limpio.
    <ErrorBoundary key={location.pathname}>
      <Suspense fallback={<Cargando />}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/events" element={<Events />} />
          <Route path="/events/:id" element={<EventDetail />} />
          <Route path="/events/:id/map" element={<MapRoute />} />
          <Route path="/biografia" element={<Biography />} />
          <Route path="/comunidad" element={<Community />} />
          <Route path="/en-vivo" element={<Live />} />
          <Route path="/mapa" element={<FestivalMap />} />
          {RUTAS_ADMIN.map(([ruta, Pagina]) => (
            <Route
              key={ruta}
              path={ruta}
              element={
                <RequireAdmin>
                  <Pagina />
                </RequireAdmin>
              }
            />
          ))}
          {/* Ruta vieja que dependía de location.state: ya no hay evento que mostrar. */}
          <Route path="/events/map" element={<Navigate to="/events" replace />} />
          <Route
            path="/checkout/:eventoId"
            element={
              <RequireAuth>
                <Checkout />
              </RequireAuth>
            }
          />
          {/* La TV muestra un QR que abre esta ruta con ?codigo=...; sin sesión, el login regresa aquí. */}
          <Route
            path="/vincular-tv"
            element={
              <RequireAuth>
                <VincularTv />
              </RequireAuth>
            }
          />
          <Route
            path="/mis-boletos"
            element={
              <RequireAuth>
                <MyTickets />
              </RequireAuth>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
    </ErrorBoundary>
  );
}

/** Estructura común (cabecera, navegación, pie) más las rutas; sin router propio para poder probarla. */
export function Aplicacion() {
  return (
    <AppLayout>
      <Rutas />
    </AppLayout>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Aplicacion />
    </BrowserRouter>
  );
}
