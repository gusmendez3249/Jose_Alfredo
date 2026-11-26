import type { ReactNode } from 'react';

/** Sustituto de swiper/react para jsdom (el carrusel real depende de medidas de layout). */
export const Swiper = ({ children }: { children: ReactNode }) => <div data-testid="carrusel">{children}</div>;
export const SwiperSlide = ({ children }: { children: ReactNode }) => <div>{children}</div>;
