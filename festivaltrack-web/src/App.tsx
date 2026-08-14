import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Events from './pages/Events';
import MapRoute from './pages/MapRoute';
import Checkout from './pages/Checkout';
import MyTickets from './pages/MyTickets';
import './index.css';

const getStoredToken = () => localStorage.getItem('token') ?? localStorage.getItem('accessToken');

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = getStoredToken();
  return token ? <>{children}</> : <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
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
    </BrowserRouter>
  );
}

export default App;
