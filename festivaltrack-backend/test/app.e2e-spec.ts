import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from './../src/app.module';

/**
 * Suite de pruebas de Integración (E2E) para FestivalTrack API.
 *
 * A diferencia de las pruebas unitarias, estas pruebas levantan la aplicación
 * completa con NestJS y realizan peticiones HTTP reales a la API,
 * probando la integración entre Controller → Service → Prisma → Neon DB.
 *
 * Tipos de prueba: INTEGRACIÓN / E2E (Jest + Supertest)
 *
 * NOTA: Requiere que la variable DATABASE_URL apunte a una base de datos
 * de pruebas o a Neon con datos existentes.
 */
describe('FestivalTrack API — Pruebas de Integración (E2E)', () => {
  let app: INestApplication;
  let authToken: string;
  const correoTest = `test.e2e.${Date.now()}@festivaltrack.com`;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
    app.setGlobalPrefix('api/v1');
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  // ─── Grupo 1: Endpoints de Autenticación ─────────────────────────────────

  describe('POST /api/v1/auth/register', () => {
    it('debe registrar un usuario nuevo y retornar token JWT (201)', async () => {
      const response = await request(app.getHttpServer())
        .post('/api/v1/auth/register')
        .send({
          nombre: 'Usuario E2E',
          correo: correoTest,
          contrasena: 'password123',
        })
        .expect(201);

      expect(response.body).toHaveProperty('accessToken');
      expect(response.body.usuario.correo).toBe(correoTest);
      authToken = response.body.accessToken;
    });

    it('debe retornar 409 Conflict si el correo ya está registrado', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/auth/register')
        .send({
          nombre: 'Duplicado',
          correo: correoTest,
          contrasena: 'otrapass',
        })
        .expect(409);
    });

    it('debe retornar 400 si faltan campos requeridos (ValidationPipe)', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/auth/register')
        .send({ correo: 'incompleto@test.com' }) // Falta nombre y contrasena
        .expect(400);
    });
  });

  describe('POST /api/v1/auth/login', () => {
    it('debe iniciar sesión con credenciales válidas y retornar JWT (201)', async () => {
      const response = await request(app.getHttpServer())
        .post('/api/v1/auth/login')
        .send({ correo: correoTest, contrasena: 'password123' })
        .expect(201);

      expect(response.body).toHaveProperty('accessToken');
      expect(response.body.usuario.rol).toBe('USUARIO');
    });

    it('debe retornar 401 con contraseña incorrecta', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/auth/login')
        .send({ correo: correoTest, contrasena: 'wrong_password' })
        .expect(401);
    });

    it('debe retornar 401 con correo inexistente', async () => {
      await request(app.getHttpServer())
        .post('/api/v1/auth/login')
        .send({ correo: 'noexiste@festivaltrack.com', contrasena: 'cualquier' })
        .expect(401);
    });
  });

  // ─── Grupo 2: Endpoints de Eventos (API pública) ──────────────────────────

  describe('GET /api/v1/eventos', () => {
    it('debe retornar la lista de eventos sin autenticación (200)', async () => {
      const response = await request(app.getHttpServer())
        .get('/api/v1/eventos')
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });
  });

  // ─── Grupo 3: Endpoints Protegidos con JWT ────────────────────────────────

  describe('GET /api/v1/auth/usuarios (requiere JWT)', () => {
    it('debe retornar 401 sin token de autenticación', async () => {
      await request(app.getHttpServer())
        .get('/api/v1/auth/usuarios')
        .expect(401);
    });

    it('debe retornar la lista de usuarios con JWT válido (200)', async () => {
      const response = await request(app.getHttpServer())
        .get('/api/v1/auth/usuarios')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });
  });

  // ─── Grupo 4: Endpoints del Chat (Integración API + BD) ──────────────────

  describe('GET /api/v1/stream/chat/:eventoId', () => {
    it('debe retornar mensajes del chat para un evento (200)', async () => {
      const response = await request(app.getHttpServer())
        .get('/api/v1/stream/chat/EVT-001')
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });
  });
});
