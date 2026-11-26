import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { Aplicacion } from '../App';
import { limpiarCodigoTv } from '../services/auth.service';
import { crearJwt, USUARIO } from '../test/datos';
import { simularRed } from '../test/red';
import { iniciarSesionDePrueba } from '../test/sesion';

vi.mock('swiper/react', async () => import('../test/swiper'));

/** Abre la app como lo hace el QR de la TV: con el código en la URL. */
function abrirDesdeQr(url = '/vincular-tv?codigo=KX7M-3QPA') {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Aplicacion />
    </MemoryRouter>,
  );
}

describe('vincular TV', () => {
  it('con sesión trae el código del QR y aprueba la TV con la cuenta', async () => {
    const red = simularRed({ 'POST /auth/tv-sync': { status: 201, data: { status: 'SUCCESS' } } });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    abrirDesdeQr();

    expect(await screen.findByRole('heading', { level: 1, name: 'Vincular TV' })).toBeInTheDocument();
    expect(screen.getByText(`La TV entrará con tu cuenta (${USUARIO.nombre}).`)).toBeInTheDocument();
    expect(screen.getByLabelText('Código de la TV')).toHaveValue('KX7M-3QPA');

    await usuario.click(screen.getByRole('button', { name: 'Entrar en la TV' }));

    expect(await screen.findByText('¡Listo! La TV entrará con tu cuenta en unos segundos.')).toBeInTheDocument();
    const peticion = red.peticiones.find((p) => p.url === '/auth/tv-sync');
    expect(peticion?.cuerpo).toEqual({ codigo: 'KX7M-3QPA' });
    expect(peticion?.autorizacion).toMatch(/^Bearer /);
  });

  it('sin sesión pide entrar y regresa con el código ya escrito', async () => {
    simularRed({
      'POST /auth/login': { data: { accessToken: crearJwt(Math.floor(Date.now() / 1000) + 3600), usuario: USUARIO } },
    });
    const usuario = userEvent.setup();
    abrirDesdeQr();

    expect(await screen.findByRole('heading', { level: 1, name: /José Alfredo Jiménez/ })).toBeInTheDocument();
    await usuario.type(screen.getByLabelText('Correo electrónico'), 'ana@correo.mx');
    await usuario.type(screen.getByLabelText('Contraseña'), 'Secreta123');
    await usuario.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Vincular TV' })).toBeInTheDocument();
    expect(screen.getByLabelText('Código de la TV')).toHaveValue('KX7M-3QPA');
  });

  it('con un código vencido explica qué revisar', async () => {
    simularRed({ 'POST /auth/tv-sync': { status: 404, data: { message: 'Not Found' } } });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    abrirDesdeQr();

    await usuario.click(await screen.findByRole('button', { name: 'Entrar en la TV' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ese código no existe o ya venció.');
    expect(screen.getByRole('button', { name: 'Entrar en la TV' })).toBeEnabled();
  });

  it('sin un código completo no deja enviar', async () => {
    simularRed({});
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    abrirDesdeQr('/vincular-tv');

    const campo = await screen.findByLabelText('Código de la TV');
    expect(screen.getByRole('button', { name: 'Entrar en la TV' })).toBeDisabled();
    await usuario.type(campo, 'kx7m 3qpa');
    expect(campo).toHaveValue('KX7M-3QPA');
    expect(screen.getByRole('button', { name: 'Entrar en la TV' })).toBeEnabled();
  });

  it('el código se escribe como lo muestra la TV', () => {
    expect(limpiarCodigoTv('kx7m3q')).toBe('KX7M-3Q');
    expect(limpiarCodigoTv(' KX7M - 3QPA - extra')).toBe('KX7M-3QPA');
    expect(limpiarCodigoTv('ab')).toBe('AB');
  });
});
