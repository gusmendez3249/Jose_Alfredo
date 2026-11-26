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
