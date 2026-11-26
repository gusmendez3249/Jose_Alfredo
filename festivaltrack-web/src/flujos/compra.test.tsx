import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { crearBoleto, crearEvento } from '../test/datos';
import { renderizarApp } from '../test/render';
import { simularRed, type RespuestaFalsa } from '../test/red';
import { iniciarSesionDePrueba } from '../test/sesion';

type Usuario = ReturnType<typeof userEvent.setup>;

async function llenarTarjeta(usuario: Usuario) {
  await usuario.type(screen.getByLabelText('Titular de la tarjeta'), 'Ana Pérez');
  await usuario.type(screen.getByLabelText('Número de tarjeta'), '4242424242424242');
  await usuario.type(screen.getByLabelText('Vencimiento'), '1299');
  await usuario.type(screen.getByLabelText('CVV'), '123');
}

/** Cada POST devuelve un boleto con id y categoría distintos, según lo pedido. */
function comprarOk(): (cuerpo: unknown) => RespuestaFalsa {
  let n = 0;
  return (cuerpo) => {
    n += 1;
    const { categoria } = cuerpo as { categoria: 'VIP' | 'GENERAL' };
    return {
      status: 201,
      data: crearBoleto({
        id: `cmwboleto00000${n}`,
        categoria,
        precio: categoria === 'VIP' ? 4500 : 1200,
        evento: undefined,
      }),
    };
  };
}

describe('checkout', () => {
  it('exige sesión: sin ella lleva al login', async () => {
    simularRed({});
    renderizarApp('/checkout/ev-1');
    expect(await screen.findByRole('heading', { level: 1, name: /José Alfredo Jiménez/ })).toBeInTheDocument();
  });

  it('calcula el total al cambiar las cantidades', async () => {
    simularRed({ 'GET /eventos/ev-1': { data: crearEvento() } });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/checkout/ev-1');

    const resumen = await screen.findByRole('complementary');
    // Estado inicial: 1 boleto General.
    expect(within(resumen).getByRole('button', { name: 'Confirmar pago · $1,200 MXN' })).toBeInTheDocument();
    expect(within(resumen).getByText('1 boleto')).toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Agregar un boleto VIP' }));
    await usuario.click(screen.getByRole('button', { name: 'Agregar un boleto General' }));

    expect(within(resumen).getByText('$6,900 MXN')).toBeInTheDocument(); // 4500 + 2 × 1200 = 6900
    expect(within(resumen).getByText('3 boletos')).toBeInTheDocument();
    expect(within(resumen).getByText('VIP × 1')).toBeInTheDocument();
    expect(within(resumen).getByText('General × 2')).toBeInTheDocument();
    expect(within(resumen).getByRole('button', { name: 'Confirmar pago · $6,900 MXN' })).toBeEnabled();

    // Con cero boletos no se puede pagar y no se puede bajar de cero.
    await usuario.click(screen.getByRole('button', { name: 'Quitar un boleto VIP' }));
    await usuario.click(screen.getByRole('button', { name: 'Quitar un boleto General' }));
    await usuario.click(screen.getByRole('button', { name: 'Quitar un boleto General' }));
    expect(screen.getByRole('button', { name: 'Quitar un boleto General' })).toBeDisabled();
    expect(within(resumen).getByRole('button', { name: /Confirmar pago/ })).toBeDisabled();
  });

  it('con una tarjeta inválida muestra errores en cada campo y no compra nada', async () => {
    const red = simularRed({ 'GET /eventos/ev-1': { data: crearEvento() } });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/checkout/ev-1');

    await screen.findByRole('complementary');
    await usuario.type(screen.getByLabelText('Número de tarjeta'), '4242424242424241');
    await usuario.click(screen.getByRole('button', { name: /Confirmar pago/ }));

    expect(await screen.findByText('El número de tarjeta no es válido.')).toBeInTheDocument();
    expect(screen.getByText('Escribe el nombre como aparece en la tarjeta.')).toBeInTheDocument();
    const numero = screen.getByLabelText('Número de tarjeta');
    expect(numero).toHaveAttribute('aria-invalid', 'true');
    expect(numero).toHaveAccessibleDescription('El número de tarjeta no es válido.');
    expect(red.peticiones.filter((p) => p.metodo === 'POST')).toHaveLength(0);
  });

  it('envía UNA petición por boleto con cantidad 1 y muestra la pantalla de éxito', async () => {
    const red = simularRed({
      'GET /eventos/ev-1': { data: crearEvento() },
      'POST /boletos/comprar': comprarOk(),
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/checkout/ev-1');

    await screen.findByRole('complementary');
    await usuario.click(screen.getByRole('button', { name: 'Agregar un boleto VIP' }));
    await usuario.click(screen.getByRole('button', { name: 'Agregar un boleto General' }));
    await llenarTarjeta(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Confirmar pago · $6,900 MXN' }));

    expect(await screen.findByRole('heading', { level: 1, name: '¡Compra realizada!' })).toBeInTheDocument();
    expect(screen.getByText(/Tus 3 boletos para Noche de Bohemia/)).toBeInTheDocument();

    const compras = red.peticiones.filter((p) => p.metodo === 'POST');
    expect(compras).toHaveLength(3);
    expect(compras.map((p) => p.cuerpo)).toEqual([
      { eventoId: 'ev-1', categoria: 'VIP', cantidad: 1, metodoPago: 'TARJETA_CREDITO' },
      { eventoId: 'ev-1', categoria: 'GENERAL', cantidad: 1, metodoPago: 'TARJETA_CREDITO' },
      { eventoId: 'ev-1', categoria: 'GENERAL', cantidad: 1, metodoPago: 'TARJETA_CREDITO' },
    ]);
    // Los datos de la tarjeta nunca salen del navegador.
    expect(JSON.stringify(compras)).not.toMatch(/4242|123|Ana/);

    expect(screen.getByRole('link', { name: 'Ver mis boletos' })).toHaveAttribute('href', '/mis-boletos');
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/');
  });

  it('si falla una compra a mitad informa "Se compró 1 de 2" y permite reintentar solo lo que faltó', async () => {
    let n = 0;
    const ok = comprarOk();
    const red = simularRed({
      'GET /eventos/ev-1': { data: crearEvento() },
      'POST /boletos/comprar': (cuerpo) => {
        n += 1;
        // Segunda petición: el servidor falla. Tercera (el reintento): funciona.
        if (n === 2) return { status: 500, data: {} };
        return ok(cuerpo);
      },
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/checkout/ev-1');

    await screen.findByRole('complementary');
    await usuario.click(screen.getByRole('button', { name: 'Agregar un boleto VIP' }));
    await llenarTarjeta(usuario);
    await usuario.click(screen.getByRole('button', { name: 'Confirmar pago · $5,700 MXN' }));

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('Se compró 1 de 2 boletos.');
    expect(alerta).toHaveTextContent(/Puedes reintentar la compra de los que faltan/);

    // Lo ya comprado queda a la vista y se descuenta del pedido (singular correcto).
    expect(screen.getByRole('heading', { name: 'Boletos ya comprados' })).toBeInTheDocument();
    const reintento = screen.getByRole('button', { name: 'Comprar 1 restante · $1,200 MXN' });
    expect(screen.queryByText(/1 restantes/)).not.toBeInTheDocument();

    await usuario.click(reintento);
    expect(await screen.findByRole('heading', { level: 1, name: '¡Compra realizada!' })).toBeInTheDocument();
    // En total: 1 (ok) + 1 (falló) + 1 (reintento) peticiones, nunca se repitió el boleto ya comprado.
    const categorias = red.peticiones.filter((p) => p.metodo === 'POST').map((p) => (p.cuerpo as { categoria: string }).categoria);
    expect(categorias).toEqual(['VIP', 'GENERAL', 'GENERAL']);
  });

  it('si el primer boleto falla informa que no se completó la compra', async () => {
    simularRed({
      'GET /eventos/ev-1': { data: crearEvento() },
      'POST /boletos/comprar': { status: 400, data: { message: ['El evento ya no vende boletos'] } },
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/checkout/ev-1');

    await screen.findByRole('complementary');
    await llenarTarjeta(usuario);
    await usuario.click(screen.getByRole('button', { name: /Confirmar pago/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo completar la compra. El evento ya no vende boletos');
    expect(screen.queryByRole('heading', { name: 'Boletos ya comprados' })).not.toBeInTheDocument();
  });

  it('un evento que no está a la venta no permite comprar', async () => {
    simularRed({ 'GET /eventos/ev-1': { data: crearEvento({ estado: 'FINALIZADO' }) } });
    iniciarSesionDePrueba();
    renderizarApp('/checkout/ev-1');

    expect(await screen.findByRole('heading', { level: 1, name: 'No se puede comprar' })).toBeInTheDocument();
    expect(screen.getByText('Este evento ya no está a la venta.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Confirmar pago/ })).not.toBeInTheDocument();
  });

  it('un evento inexistente (404) explica el motivo y no ofrece reintentar', async () => {
    simularRed({ 'GET /eventos/ev-1': { status: 404, data: { message: 'x' } } });
    iniciarSesionDePrueba();
    renderizarApp('/checkout/ev-1');

    expect(await screen.findByText('Este evento no existe o ya no está disponible.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });
});

describe('mis boletos', () => {
  it('muestra el QR solo en boletos ACTIVOS y nunca el id completo', async () => {
    const activo = crearBoleto({ id: 'cmwactivo123456', codigoQR: 'CODIGO-QR-ACTIVO' });
    const usado = crearBoleto({ id: 'cmwusado654321', estado: 'USADO', codigoQR: 'CODIGO-QR-USADO' });
    const cancelado = crearBoleto({ id: 'cmwcancel777777', estado: 'CANCELADO', codigoQR: 'CODIGO-QR-CANCELADO' });
    simularRed({ 'GET /boletos/mis-boletos': { data: [activo, usado, cancelado] } });
    iniciarSesionDePrueba();
    renderizarApp('/mis-boletos');

    const tarjetas = await screen.findAllByRole('article');
    expect(tarjetas).toHaveLength(3);

    // Solo el activo tiene QR.
    expect(screen.getAllByTitle(/Código QR del boleto/)).toHaveLength(1);
    expect(within(tarjetas[0]).getByTitle('Código QR del boleto 123456')).toBeInTheDocument();
    expect(within(tarjetas[0]).getByText('Activo')).toBeInTheDocument();

    expect(within(tarjetas[1]).queryByTitle(/Código QR/)).not.toBeInTheDocument();
    expect(within(tarjetas[1]).getByText('Este boleto ya se usó para entrar.')).toBeInTheDocument();
    expect(within(tarjetas[1]).getByText('Usado')).toBeInTheDocument();
    expect(within(tarjetas[2]).queryByTitle(/Código QR/)).not.toBeInTheDocument();
    expect(within(tarjetas[2]).getByText('Este boleto fue cancelado y no da acceso.')).toBeInTheDocument();

    // Solo se enseña el folio corto; el id completo y el código del QR no aparecen en pantalla.
    const texto = document.body.textContent ?? '';
    expect(texto).toContain('123456');
    expect(texto).not.toContain('cmwactivo123456');
    expect(texto).not.toContain('CODIGO-QR');
  });

  it('muestra categoría, precio y fecha del evento', async () => {
    simularRed({
      'GET /boletos/mis-boletos': {
        data: [crearBoleto({ categoria: 'VIP', precio: 4500, evento: crearEvento({ nombre: 'Gran Gala Mariachi' }) })],
      },
    });
    iniciarSesionDePrueba();
    renderizarApp('/mis-boletos');

    const tarjeta = await screen.findByRole('article');
    expect(within(tarjeta).getByRole('heading', { name: 'Gran Gala Mariachi' })).toBeInTheDocument();
    expect(within(tarjeta).getByText('VIP')).toBeInTheDocument();
    expect(within(tarjeta).getByText('$4,500 MXN')).toBeInTheDocument();
  });

  it('sin boletos invita a explorar los eventos', async () => {
    simularRed({ 'GET /boletos/mis-boletos': { data: [] } });
    iniciarSesionDePrueba();
    renderizarApp('/mis-boletos');

    expect(await screen.findByRole('heading', { name: 'Aún no tienes boletos' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver eventos' })).toHaveAttribute('href', '/events');
  });

  it('con error de red muestra "Reintentar" que vuelve a cargar', async () => {
    let intentos = 0;
    simularRed({
      'GET /boletos/mis-boletos': () => {
        intentos += 1;
        return intentos === 1 ? { status: 503, data: {} } : { data: [crearBoleto()] };
      },
    });
    iniciarSesionDePrueba();
    const usuario = userEvent.setup();
    renderizarApp('/mis-boletos');

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar tus boletos');
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }));
    await waitFor(() => expect(screen.getAllByRole('article')).toHaveLength(1));
  });
});
