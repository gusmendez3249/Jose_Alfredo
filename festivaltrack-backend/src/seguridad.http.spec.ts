/**
 * Pruebas de seguridad de la API por HTTP (Jest + Supertest).
 *
 * Levantan los controladores reales con los guards reales (JWT + roles) y la misma
 * configuración de main.ts (helmet, CORS, ValidationPipe). Solo los servicios y Prisma
 * son dobles de prueba, así que no necesitan base de datos ni credenciales: corren en el CI.
 *
 * Cubre: autenticación (401), autorización por rol (403), validación de entradas (400),
 * datos que el cliente no debe controlar, errores sin detalles internos, cabeceras y CORS.
 */
import { INestApplication, Module } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { JwtModule, JwtService } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import request from 'supertest';

process.env.JWT_SECRET = process.env.JWT_SECRET || 'clave-solo-para-pruebas';

import { JwtStrategy } from './auth/jwt.strategy';
import { PrismaService } from './prisma/prisma.service';
import { AuthController } from './auth/auth.controller';
import { AuthService } from './auth/auth.service';
import { BiografiasController } from './biografias/biografias.controller';
import { BiografiasService } from './biografias/biografias.service';
import { EventosController } from './eventos/eventos.controller';
import { EventosService } from './eventos/eventos.service';
import { BoletosController } from './boletos/boletos.controller';
import { BoletosService } from './boletos/boletos.service';
import { configurarSeguridad } from './configurar-seguridad';

// Usuarios ficticios que "existen" en la base de datos simulada.
const USUARIOS: Record<string, { id: string; correo: string; rol: string; activo: boolean }> = {
  'u-admin': { id: 'u-admin', correo: 'admin@ejemplo.test', rol: 'ADMINISTRADOR', activo: true },
  'u-normal': { id: 'u-normal', correo: 'usuario@ejemplo.test', rol: 'USUARIO', activo: true },
  'u-baja': { id: 'u-baja', correo: 'baja@ejemplo.test', rol: 'USUARIO', activo: false },
};

describe('Seguridad de la API (HTTP)', () => {
  let app: INestApplication;
  let jwt: JwtService;

  const prismaMock = {
    usuario: { findUnique: jest.fn(({ where }) => Promise.resolve(USUARIOS[where.id] ?? null)) },
  };
  const authService = { tvSync: jest.fn((b) => ({ status: 'SUCCESS', ...b })) };
  const biografiasService = {
    findAll: jest.fn(() => []),
    findOne: jest.fn(),
    altaConFactory: jest.fn((dto) => ({ id: 'bio-1', ...dto })),
  };
  const eventosService = {
    findAll: jest.fn(() => []),
    findOne: jest.fn(),
    create: jest.fn((dto) => ({ id: 'evt-1', ...dto })),
    update: jest.fn((id, dto) => ({ id, ...dto })),
    remove: jest.fn(),
  };
  const boletosService = {
    comprar: jest.fn((usuarioId, dto) => ({ usuarioId, ...dto })),
    misBoletos: jest.fn(() => {
      throw new Error('detalle interno: fallo en la consulta a tabla boleto');
    }),
    validarQr: jest.fn(),
  };

  const token = (sub: string, secreto = process.env.JWT_SECRET!) =>
    `Bearer ${jwt.sign({ sub, correo: USUARIOS[sub]?.correo ?? 'x', rol: USUARIOS[sub]?.rol ?? 'USUARIO' }, { secret: secreto })}`;

  beforeAll(async () => {
    @Module({
      imports: [PassportModule, JwtModule.register({ secret: process.env.JWT_SECRET })],
      controllers: [AuthController, BiografiasController, EventosController, BoletosController],
      providers: [
        JwtStrategy,
        { provide: PrismaService, useValue: prismaMock },
        { provide: AuthService, useValue: authService },
        { provide: BiografiasService, useValue: biografiasService },
        { provide: EventosService, useValue: eventosService },
        { provide: BoletosService, useValue: boletosService },
      ],
    })
    class ModuloPrueba {}

    const moduleRef = await Test.createTestingModule({ imports: [ModuloPrueba] }).compile();
    app = moduleRef.createNestApplication({ logger: false });
    configurarSeguridad(app);
    app.setGlobalPrefix('api/v1');
    await app.init();
    jwt = moduleRef.get(JwtService);
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(() => jest.clearAllMocks());

  describe('Autenticación', () => {
    it('rechaza con 401 una petición protegida sin token', async () => {
      await request(app.getHttpServer()).post('/api/v1/boletos/comprar').send({ eventoId: 'evt-1' }).expect(401);
      expect(boletosService.comprar).not.toHaveBeenCalled();
    });

    it('rechaza con 401 un token firmado con otra clave', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/boletos/comprar')
        .set('Authorization', token('u-normal', 'clave-falsa'))
        .send({ eventoId: 'evt-1' })
        .expect(401);
    });

    it('rechaza con 401 el token de un usuario dado de baja', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/boletos/comprar')
        .set('Authorization', token('u-baja'))
        .send({ eventoId: 'evt-1' })
        .expect(401);
    });

    it('vincular una TV exige sesión y usa el usuario del token, no el del cuerpo', async () => {
      await request(app.getHttpServer()).post('/api/v1/auth/tv-sync').send({ tvToken: 'tv-123' }).expect(401);

      await request(app.getHttpServer())
        .post('/api/v1/auth/tv-sync')
        .set('Authorization', token('u-normal'))
        .send({ tvToken: 'tv-123', userId: 'u-admin' })
        .expect(201);
      expect(authService.tvSync).toHaveBeenCalledWith({ tvToken: 'tv-123', userId: 'u-normal' });
    });
  });

  describe('Autorización por rol', () => {
    it('un USUARIO recibe 403 al crear un evento', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/eventos')
        .set('Authorization', token('u-normal'))
        .send({ nombre: 'X', fechaHora: '2026-12-01T20:00:00Z', ubicacion: 'Dolores', capacidad: 10 })
        .expect(403);
      expect(eventosService.create).not.toHaveBeenCalled();
    });

    it('alta de biografía: 401 sin sesión, 403 para USUARIO, 201 para ADMINISTRADOR', async () => {
      const body = { titulo: 'Hito', descripcion: 'Descripción de prueba' };
      await request(app.getHttpServer()).post('/api/v1/biografias/alta').send(body).expect(401);
      await request(app.getHttpServer())
        .post('/api/v1/biografias/alta')
        .set('Authorization', token('u-normal'))
        .send(body)
        .expect(403);
      await request(app.getHttpServer())
        .post('/api/v1/biografias/alta')
        .set('Authorization', token('u-admin'))
        .send(body)
        .expect(201);
      expect(biografiasService.altaConFactory).toHaveBeenCalledTimes(1);
    });
  });

  describe('Validación de entradas en el servidor', () => {
    it('responde 400 si a la biografía le faltan campos obligatorios', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/biografias/alta')
        .set('Authorization', token('u-admin'))
        .send({})
        .expect(400);
      expect(biografiasService.altaConFactory).not.toHaveBeenCalled();
    });

    it('responde 400 si la capacidad del evento no es un número entero (PUT y PATCH)', async () => {
      for (const metodo of ['put', 'patch'] as const) {
        await request(app.getHttpServer())
          [metodo]('/api/v1/eventos/evt-1')
          .set('Authorization', token('u-admin'))
          .send({ capacidad: 'muchos' })
          .expect(400);
      }
      expect(eventosService.update).not.toHaveBeenCalled();
    });

    it('edita el evento con PUT (como la app) y PATCH cuando los datos son válidos', async () => {
      for (const metodo of ['put', 'patch'] as const) {
        await request(app.getHttpServer())
          [metodo]('/api/v1/eventos/evt-1')
          .set('Authorization', token('u-admin'))
          .send({ capacidad: 500 })
          .expect(200);
      }
      expect(eventosService.update).toHaveBeenCalledTimes(2);
    });

    it('responde 400 si se piden más de 10 boletos o una cantidad negativa', async () => {
      for (const cantidad of [11, -1]) {
        await request(app.getHttpServer())
          .post('/api/v1/boletos/comprar')
          .set('Authorization', token('u-normal'))
          .send({ eventoId: 'evt-1', cantidad })
          .expect(400);
      }
    });

    it('descarta el precio y el usuario que manda el cliente', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/boletos/comprar')
        .set('Authorization', token('u-normal'))
        .send({ eventoId: 'evt-1', cantidad: 2, precioTotal: 1, usuarioId: 'u-admin' })
        .expect(201);
      expect(boletosService.comprar).toHaveBeenCalledWith('u-normal', { eventoId: 'evt-1', cantidad: 2 });
    });
  });

  describe('Manejo de errores y cabeceras', () => {
    it('un error interno responde 500 genérico, sin detalles de la base de datos', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/boletos/mis-boletos')
        .set('Authorization', token('u-normal'))
        .expect(500);
      expect(JSON.stringify(res.body)).not.toContain('tabla boleto');
      expect(res.body.message).toBe('Internal server error');
    });

    it('incluye cabeceras de seguridad de helmet y oculta X-Powered-By', async () => {
      const res = await request(app.getHttpServer()).get('/api/v1/eventos').expect(200);
      expect(res.headers['x-content-type-options']).toBe('nosniff');
      expect(res.headers['strict-transport-security']).toBeDefined();
      expect(res.headers['x-powered-by']).toBeUndefined();
    });

    it('CORS no autoriza un origen desconocido', async () => {
      const res = await request(app.getHttpServer())
        .get('/api/v1/eventos')
        .set('Origin', 'https://sitio-malicioso.example');
      expect(res.headers['access-control-allow-origin']).toBeUndefined();
    });
  });
});
