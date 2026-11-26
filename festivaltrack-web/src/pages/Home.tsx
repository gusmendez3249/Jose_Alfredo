import { Link } from 'react-router-dom';
import '../styles/envivo.css';
import { AvisoEnVivo } from '../components/envivo/AvisoEnVivo';
import { Swiper, SwiperSlide } from 'swiper/react';
import { A11y, Autoplay, EffectFade, Pagination } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/effect-fade';
import 'swiper/css/pagination';
import { Button, ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import {
  IconoAlerta,
  IconoBoleto,
  IconoComunidad,
  IconoEnVivo,
  IconoEventos,
  IconoFlecha,
  IconoLibro,
  IconoLugar,
  IconoMapa,
  IconoReloj,
  IconoRuta,
} from '../components/ui/Icons';
import { Skeleton } from '../components/ui/Skeleton';
import { estaALaVenta, lugarDe, tieneCoordenadas } from '../services/eventos.service';
import type { Evento } from '../services/tipos';
import { formatearFechaEvento } from '../lib/formato';
import { cargarProximos } from '../lib/proximos';
import { useMovimientoReducido } from '../hooks/useMovimientoReducido';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';

const IMAGENES = [
  { src: '/images/img1.webp', alt: 'Escenario del festival' },
  { src: '/images/img2.webp', alt: 'Mariachi en vivo' },
  { src: '/images/img3.webp', alt: 'Estatua de José Alfredo Jiménez' },
] as const;

const LADO_IMAGEN = 1024;

function Carrusel() {
  const reducido = useMovimientoReducido();
  return (
    <section className="carrusel" aria-label="Imágenes del festival">
      <Swiper
        // `key` fuerza a Swiper a reiniciarse si cambia la preferencia de movimiento.
        key={reducido ? 'quieto' : 'animado'}
        modules={[A11y, Autoplay, EffectFade, Pagination]}
        effect="fade"
        pagination={{ clickable: true }}
        autoplay={reducido ? false : { delay: 3500, disableOnInteraction: false, pauseOnMouseEnter: true }}
        loop
        a11y={{ paginationBulletMessage: 'Ir a la imagen {{index}}' }}
      >
        {IMAGENES.map((imagen, i) => (
          <SwiperSlide key={imagen.src}>
            <img
              src={imagen.src}
              alt={imagen.alt}
              width={LADO_IMAGEN}
              height={LADO_IMAGEN}
              loading={i === 0 ? 'eager' : 'lazy'}
              fetchPriority={i === 0 ? 'high' : 'auto'}
              decoding="async"
            />
          </SwiperSlide>
        ))}
      </Swiper>
    </section>
  );
}

function EventoDestacado({ evento }: { evento: Evento }) {
  const lugar = lugarDe(evento);
  const fecha = formatearFechaEvento(evento.fechaHora);
  const id = encodeURIComponent(evento.id);

  return (
    <Card destacada como="article" aria-labelledby="proximo-evento-nombre" className="destacado">
      <p className="eyebrow">Próximo evento</p>
      <h3 id="proximo-evento-nombre" className="destacado__nombre">
        {evento.nombre}
      </h3>
      {evento.artista?.nombre && <p className="destacado__artista">{evento.artista.nombre}</p>}
      <ul className="evento-card__meta meta">
        {fecha && (
          <li>
            <IconoReloj tamano={16} />
            <time dateTime={evento.fechaHora}>{fecha} h</time>
          </li>
        )}
        <li>
          <IconoLugar tamano={16} />
          <span>{lugar ?? 'Lugar por confirmar'}</span>
        </li>
      </ul>
      <div className="destacado__acciones">
        {estaALaVenta(evento) && (
          <ButtonLink to={`/checkout/${id}`} mayusculas aria-label={`Comprar boletos para ${evento.nombre}`}>
            Comprar boletos
          </ButtonLink>
        )}
        {tieneCoordenadas(evento) && (
          <ButtonLink to={`/events/${id}/map`} variante="secundario" aria-label={`Ver ruta al evento ${evento.nombre}`}>
            <IconoRuta tamano={18} />
            Ver ruta
          </ButtonLink>
        )}
      </div>
    </Card>
  );
}

function ProximoEvento() {
  const recurso = useRecurso(cargarProximos, 'eventos');

  return (
    <section className="seccion" aria-labelledby="titulo-proximo">
      <h2 id="titulo-proximo" className="solo-lectores">
        Próximo evento
      </h2>

      {recurso.estado === 'cargando' && (
        <Card destacada role="status" aria-label="Cargando el próximo evento">
          <div className="pila">
            <Skeleton alto={12} ancho={110} />
            <Skeleton alto={22} ancho="65%" />
            <Skeleton alto={14} ancho="45%" />
            <Skeleton alto={44} ancho={180} />
          </div>
        </Card>
      )}

      {recurso.estado === 'error' && (
        <Card destacada role="alert" className="aviso-evento">
          <IconoAlerta tamano={22} />
          <div>
            <p className="titulo-seccion">No se pudo cargar el próximo evento</p>
            <p className="meta">{recurso.mensaje}</p>
          </div>
          <Button variante="secundario" pildora tamano="chico" onClick={recurso.reintentar}>
            Reintentar
          </Button>
        </Card>
      )}

      {recurso.estado === 'listo' && recurso.datos.length === 0 && (
        <Card destacada className="aviso-evento">
          <IconoEventos tamano={22} />
          <div>
            <p className="titulo-seccion">Aún no hay eventos próximos</p>
            <p className="meta">Cuando haya conciertos programados aparecerán aquí.</p>
          </div>
        </Card>
      )}

      {recurso.estado === 'listo' && recurso.datos.length > 0 && <EventoDestacado evento={recurso.datos[0]} />}
    </section>
  );
}

const ACCESOS = [
  { a: '/en-vivo', Icono: IconoEnVivo, titulo: 'En vivo', texto: 'Transmisión y chat' },
  { a: '/biografia', Icono: IconoLibro, titulo: 'Biografía', texto: 'Conoce al Maestro' },
  { a: '/comunidad', Icono: IconoComunidad, titulo: 'Comunidad', texto: 'Comparte tu experiencia' },
  { a: '/mapa', Icono: IconoMapa, titulo: 'Mapa del festival', texto: 'Llega a tu destino' },
  { a: '/events', Icono: IconoEventos, titulo: 'Eventos', texto: 'Cartelera completa' },
  { a: '/mis-boletos', Icono: IconoBoleto, titulo: 'Mis boletos', texto: 'Tus accesos con QR' },
] as const;

function Accesos() {
  return (
    <nav className="inicio-accesos" aria-label="Accesos rápidos">
      {ACCESOS.map(({ a, Icono, titulo, texto }) => (
        <Link key={a} to={a} className="tarjeta tarjeta--enlace inicio-acceso">
          <span className="inicio-acceso__icono">
            <Icono tamano={24} />
          </span>
          <span className="inicio-acceso__texto">
            <strong>{titulo}</strong>
            <span className="meta">{texto}</span>
          </span>
          <IconoFlecha tamano={18} className="inicio-acceso__flecha" />
        </Link>
      ))}
    </nav>
  );
}

export default function Home() {
  useTituloPagina('Inicio');

  return (
    <div className="pagina">
      <header className="hero">
        <p className="eyebrow">Dolores Hidalgo, cuna de la independencia</p>
        <h1 className="hero__titulo">El Rey Vive en Su Tierra</h1>
        <p className="hero__texto">
          Festival José Alfredo Jiménez: cartelera, boletos y mapa para llegar al escenario, en un solo lugar.
        </p>
        <ButtonLink to="/events" tamano="grande" mayusculas className="hero__cta">
          Ver eventos
          <IconoFlecha tamano={20} />
        </ButtonLink>
      </header>

      <AvisoEnVivo />
      <Accesos />
      <Carrusel />
      <ProximoEvento />
    </div>
  );
}
