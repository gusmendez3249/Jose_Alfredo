import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import './index.css';

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
    <div role="status" style={{ minHeight: '60vh', display: 'grid', placeItems: 'center', color: '#E6C27A' }}>
      Cargando…
    </div>
  );
}

const getStoredToken = () => localStorage.getItem('token') ?? localStorage.getItem('accessToken');

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = getStoredToken();
  return token ? <>{children}</> : <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Cargando />}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/events" element={<Events />} />
        <Route path="/events/map" element={<MapRoute />} />
        <Route
          path="/checkout/:eventoId"
          element={
            <ProtectedRoute>
              <Checkout />
            </ProtectedRoute>
          }
        />
        <Route
          path="/mis-boletos"
          element={
            <ProtectedRoute>
              <MyTickets />
            </ProtectedRoute>
          }
        />
      </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
