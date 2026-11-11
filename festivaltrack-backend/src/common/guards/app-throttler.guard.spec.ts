import { AppThrottlerGuard } from './app-throttler.guard';

/** Pruebas de la clave con la que se cuentan las peticiones del rate limiting. */
describe('AppThrottlerGuard - getTracker', () => {
  const guard = Object.create(AppThrottlerGuard.prototype) as AppThrottlerGuard;
  const tracker = (req: any) => (guard as any).getTracker(req);

  it('usa la IP real (X-Forwarded-For) cuando hay proxy', async () => {
    expect(await tracker({ ips: ['200.1.1.1'], ip: '10.0.0.1', originalUrl: '/api/v1/eventos' })).toBe('200.1.1.1');
  });

  it('en el login cuenta por IP + correo normalizado', async () => {
    const req = { ip: '1.2.3.4', originalUrl: '/api/v1/auth/login', body: { correo: ' Ana@Test.com ' } };
    expect(await tracker(req)).toBe('1.2.3.4:ana@test.com');
  });

  it('fuera del login ignora el correo del body', async () => {
    const req = { ip: '1.2.3.4', originalUrl: '/api/v1/auth/register', body: { correo: 'ana@test.com' } };
    expect(await tracker(req)).toBe('1.2.3.4');
  });
});
