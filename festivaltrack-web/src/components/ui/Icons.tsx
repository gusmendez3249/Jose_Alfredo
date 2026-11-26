import type { ReactNode, SVGProps } from 'react';

interface IconoProps extends Omit<SVGProps<SVGSVGElement>, 'children'> {
  /** Lado en px (por defecto 24). */
  tamano?: number;
}

/** Base de los iconos: decorativos (aria-hidden), heredan el color del texto. */
function Icono({ tamano = 24, children, ...resto }: IconoProps & { children: ReactNode }) {
  return (
    <svg
      className="icono"
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...resto}
    >
      {children}
    </svg>
  );
}

export const IconoInicio = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M3 11l9-8 9 8" />
    <path d="M5 10v10h5v-6h4v6h5V10" />
  </Icono>
);

export const IconoEventos = (p: IconoProps) => (
  <Icono {...p}>
    <rect x="3" y="5" width="18" height="16" rx="2" />
    <path d="M3 10h18M8 3v4M16 3v4" />
  </Icono>
);

export const IconoBoleto = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4z" />
    <path d="M14 6v12" strokeDasharray="2 3" />
  </Icono>
);

export const IconoLugar = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z" />
    <circle cx="12" cy="9.5" r="2.5" />
  </Icono>
);

export const IconoReloj = (p: IconoProps) => (
  <Icono {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3 2" />
  </Icono>
);

export const IconoEstrella = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M12 3l2.6 5.6 6.1.8-4.5 4.2 1.2 6.1L12 16.7 6.6 19.7l1.2-6.1L3.3 9.4l6.1-.8z" />
  </Icono>
);

export const IconoCheck = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M5 12.5l4.5 4.5L19 7.5" />
  </Icono>
);

export const IconoOjo = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
    <circle cx="12" cy="12" r="3" />
  </Icono>
);

export const IconoOjoCerrado = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M3 3l18 18" />
    <path d="M10.6 6.1A9.8 9.8 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-3.2 4M6.5 6.8C3.8 8.6 2 12 2 12s3.6 7 10 7a9.5 9.5 0 0 0 4.2-1" />
    <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
  </Icono>
);

export const IconoRuta = (p: IconoProps) => (
  <Icono {...p}>
    <circle cx="6" cy="19" r="2" />
    <circle cx="18" cy="5" r="2" />
    <path d="M8 19h7a3.5 3.5 0 0 0 0-7H9a3.5 3.5 0 0 1 0-7h7" />
  </Icono>
);

export const IconoMas = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M12 5v14M5 12h14" />
  </Icono>
);

export const IconoMenos = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M5 12h14" />
  </Icono>
);

export const IconoAlerta = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M12 3L2 20h20z" />
    <path d="M12 10v5M12 17.5v.01" />
  </Icono>
);

export const IconoInfo = (p: IconoProps) => (
  <Icono {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 11v6M12 7.5v.01" />
  </Icono>
);

export const IconoSinEventos = (p: IconoProps) => (
  <Icono {...p}>
    <rect x="3" y="5" width="18" height="16" rx="2" />
    <path d="M3 10h18M8 3v4M16 3v4M9.5 14.5l5 5M14.5 14.5l-5 5" />
  </Icono>
);

export const IconoSalir = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M9 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4" />
    <path d="M16 8l4 4-4 4M20 12H9" />
  </Icono>
);

export const IconoFlecha = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M5 12h14M13 6l6 6-6 6" />
  </Icono>
);

export const IconoTarjeta = (p: IconoProps) => (
  <Icono {...p}>
    <rect x="2.5" y="5" width="19" height="14" rx="2.5" />
    <path d="M2.5 10h19M6 15h4" />
  </Icono>
);

export const IconoMarca = (p: IconoProps) => (
  <Icono {...p} fill="currentColor" stroke="none">
    <path d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4z" />
  </Icono>
);

export const IconoMapa = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2z" />
    <path d="M9 4v14M15 6v14" />
  </Icono>
);

export const IconoComunidad = (p: IconoProps) => (
  <Icono {...p}>
    <circle cx="9" cy="8" r="3" />
    <path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" />
    <path d="M16 5.5a3 3 0 0 1 0 5.5M17 14.5c2.3.7 4 2.9 4 5.5" />
  </Icono>
);

export const IconoLibro = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2V5z" />
    <path d="M4 19a2 2 0 0 1 2-2h13" />
  </Icono>
);

export const IconoMusica = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M9 18V5l11-2v13" />
    <circle cx="6" cy="18" r="3" />
    <circle cx="17" cy="16" r="3" />
  </Icono>
);

export const IconoEnVivo = (p: IconoProps) => (
  <Icono {...p}>
    <circle cx="12" cy="12" r="2.5" />
    <path d="M7.5 7.5a6.4 6.4 0 0 0 0 9M16.5 7.5a6.4 6.4 0 0 1 0 9M4.5 4.5a10.6 10.6 0 0 0 0 15M19.5 4.5a10.6 10.6 0 0 1 0 15" />
  </Icono>
);

export const IconoPlay = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M7 4.5v15l12-7.5-12-7.5z" fill="currentColor" stroke="none" />
  </Icono>
);

export const IconoPausa = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M7 5h3.5v14H7zM13.5 5H17v14h-3.5z" fill="currentColor" stroke="none" />
  </Icono>
);

export const IconoAnterior = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M18 5v14l-9-7 9-7z" fill="currentColor" stroke="none" />
    <path d="M6 5v14" />
  </Icono>
);

export const IconoSiguiente = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M6 5v14l9-7-9-7z" fill="currentColor" stroke="none" />
    <path d="M18 5v14" />
  </Icono>
);

export const IconoCorazon = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z" />
  </Icono>
);

export const IconoComentario = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M4 5h16v11H9l-5 4V5z" />
  </Icono>
);

export const IconoBasura = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6" />
  </Icono>
);

export const IconoEditar = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M4 20h4L19 9l-4-4L4 16v4z" />
    <path d="M13.5 6.5l4 4" />
  </Icono>
);

export const IconoSubir = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M12 16V4M7 9l5-5 5 5" />
    <path d="M4 16v4h16v-4" />
  </Icono>
);

export const IconoUsuario = (p: IconoProps) => (
  <Icono {...p}>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8" />
  </Icono>
);

export const IconoEscudo = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M12 3 5 6v6c0 4.2 2.9 7.7 7 9 4.1-1.3 7-4.8 7-9V6l-7-3z" />
  </Icono>
);

export const IconoCerrar = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M6 6l12 12M18 6 6 18" />
  </Icono>
);

export const IconoEnviar = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M4 12 20 4l-6 16-3-7-7-1z" />
  </Icono>
);

export const IconoImagen = (p: IconoProps) => (
  <Icono {...p}>
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <circle cx="8.5" cy="10" r="1.5" />
    <path d="m21 16-5-5-8 8" />
  </Icono>
);

export const IconoArriba = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M6 15l6-6 6 6" />
  </Icono>
);

export const IconoAbajo = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M6 9l6 6 6-6" />
  </Icono>
);

export const IconoAtras = (p: IconoProps) => (
  <Icono {...p}>
    <path d="M15 5l-7 7 7 7" />
  </Icono>
);
