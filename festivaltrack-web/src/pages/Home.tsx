import { Link, useNavigate } from 'react-router-dom';
import { Swiper, SwiperSlide } from 'swiper/react';
import { Autoplay, EffectFade, Pagination } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/effect-fade';
import 'swiper/css/pagination';

export default function Home() {
  const navigate = useNavigate();

  const handleVerEventos = () => {
    // Al igual que en el wearable, aquí eventualmente solicitaremos GPS 
    // y aplicaremos filtros en tiempo real. Por ahora redirige a Eventos.
    navigate('/events');
  };

  return (
    <div className="container" style={{ paddingTop: '24px', paddingBottom: '80px' }}>
      
      {/* Navbar Minimalista */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }} className="animate-fade-in">
        <div style={{ fontSize: '1.25rem', fontWeight: 'bold', color: 'var(--primary-color)', fontFamily: 'var(--font-heading)' }}>
          FT
        </div>
        <div style={{ display: 'flex', gap: '16px' }}>
          <Link to="/login" style={{ color: 'var(--text-primary)', textDecoration: 'none', padding: '8px 16px', border: '1px solid var(--glass-border)', borderRadius: '20px' }}>
            Iniciar Sesión
          </Link>
          <Link to="/register" style={{ color: 'var(--text-primary)', textDecoration: 'none', padding: '8px 16px', border: '1px solid var(--primary-color)', borderRadius: '20px', backgroundColor: 'rgba(201,160,48,0.1)' }}>
            Registrarse
          </Link>
        </div>
      </div>

      <header style={{ textAlign: 'center', marginBottom: '40px' }} className="animate-fade-in">
        <h1 style={{ fontSize: '3.5rem', marginBottom: '16px', letterSpacing: '-1px' }}>
          Festival <span className="gradient-text">José Alfredo Jiménez</span>
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.15rem', maxWidth: '700px', margin: '0 auto', lineHeight: '1.6' }}>
          El Rey sigue cantando. Únete a la celebración musical más grande, donde los mejores artistas rinden tributo al maestro de la música ranchera.
        </p>
      </header>

      {/* Botón Principal - Ver Eventos */}
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '60px', animationDelay: '0.2s' }} className="animate-fade-in">
        <button 
          onClick={handleVerEventos}
          className="btn-primary" 
          style={{ fontSize: '1.25rem', padding: '16px 48px', boxShadow: '0 0 20px rgba(201, 160, 48, 0.4)' }}>
          VER EVENTOS
        </button>
      </div>

      {/* Swiper Carousel */}
      <div style={{ borderRadius: '16px', overflow: 'hidden', boxShadow: '0 8px 30px rgba(0,0,0,0.5)', maxWidth: '900px', margin: '0 auto', animationDelay: '0.4s' }} className="animate-fade-in">
        <Swiper
          modules={[Autoplay, EffectFade, Pagination]}
          effect="fade"
          pagination={{ clickable: true }}
          autoplay={{ delay: 3500, disableOnInteraction: false }}
          loop={true}
          style={{ height: '450px' }}
        >
          <SwiperSlide>
            <img src="/images/img1.png" alt="Escenario del festival" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </SwiperSlide>
          <SwiperSlide>
            <img src="/images/img2.png" alt="Mariachi en vivo" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </SwiperSlide>
          <SwiperSlide>
            <img src="/images/img3.png" alt="Estatua de José Alfredo Jiménez" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </SwiperSlide>
        </Swiper>
      </div>
    </div>
  );
}
