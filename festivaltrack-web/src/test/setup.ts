import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterEach, beforeAll, vi } from 'vitest';
import { limpiarSesion } from '../auth/session';

// Con la máquina cargada, el segundo por defecto de findBy* se queda corto y las pruebas fallan a ratos.
configure({ asyncUtilTimeout: 5000 });

beforeAll(() => {
  // jsdom no implementa <dialog> modal, scrollTo ni matchMedia.
  HTMLDialogElement.prototype.showModal = function showModal(this: HTMLDialogElement) {
    this.setAttribute('open', '');
  };
  HTMLDialogElement.prototype.close = function close(this: HTMLDialogElement) {
    this.removeAttribute('open');
  };
  window.scrollTo = vi.fn();
  window.matchMedia = vi.fn().mockImplementation((consulta: string) => ({
    matches: false,
    media: consulta,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
});

afterEach(() => {
  cleanup();
  limpiarSesion('manual');
  localStorage.clear();
});
