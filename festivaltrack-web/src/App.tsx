import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
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
          <Route path="/events/:id/map" element={<MapRoute />} />
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
