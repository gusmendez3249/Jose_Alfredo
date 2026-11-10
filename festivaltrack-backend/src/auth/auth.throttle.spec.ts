import { INestApplication } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import { ThrottlerModule } from '@nestjs/throttler';
import request from 'supertest';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { AppThrottlerGuard } from '../common/guards/app-throttler.guard';

/**
 * Verifica el rate limiting del login con el controlador real y el guard global,
 * sin base de datos (AuthService simulado).
 */
describe('Rate limiting de /auth/login', () => {
  let app: INestApplication;

  beforeEach(async () => {
    const module = await Test.createTestingModule({
      imports: [ThrottlerModule.forRoot([{ ttl: 60_000, limit: 300 }])],
      controllers: [AuthController],
      providers: [
        { provide: AuthService, useValue: { login: jest.fn().mockResolvedValue({ token: 't' }) } },
        { provide: APP_GUARD, useClass: AppThrottlerGuard },
      ],
    }).compile();
    app = module.createNestApplication();
    await app.init();
  });

  afterEach(() => app.close());

  const login = (correo: string) =>
    request(app.getHttpServer()).post('/auth/login').send({ correo, contrasena: 'x' });

  it('responde 429 al 6.º intento contra la misma cuenta en un minuto', async () => {
    for (let i = 0; i < 5; i++) expect((await login('ana@test.com')).status).toBe(201);
    expect((await login('ana@test.com')).status).toBe(429);
  });

  it('no bloquea a otra cuenta que sale por la misma IP', async () => {
    for (let i = 0; i < 6; i++) await login('ana@test.com');
    expect((await login('beto@test.com')).status).toBe(201);
  });
});
