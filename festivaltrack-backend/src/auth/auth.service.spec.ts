import { Test, TestingModule } from '@nestjs/testing';
import { AuthService } from './auth.service';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service';
import { ConflictException, UnauthorizedException } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';

/**
 * Suite de pruebas unitarias para AuthService.
 *
 * Utiliza mocks para aislar el servicio de sus dependencias externas
 * (PrismaService y JwtService), garantizando que cada prueba sea determinista
 * y no dependa de la base de datos real de Neon.
 *
 * Tipos de prueba: UNITARIAS (Jest + Mocks)
 */
describe('AuthService - Pruebas Unitarias', () => {
  let service: AuthService;

  // Mock de PrismaService: simula las respuestas de la base de datos
  const prismaMock = {
    usuario: {
      findUnique: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      findMany: jest.fn(),
    },
  };

  // Mock de JwtService: simula la firma del token
  const jwtMock = {
    sign: jest.fn().mockReturnValue('mock.jwt.token'),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: PrismaService, useValue: prismaMock },
        { provide: JwtService, useValue: jwtMock },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    jest.clearAllMocks();
  });

  // ─── Grupo 1: Registro de Usuarios ───────────────────────────────────────

  describe('register()', () => {
    it('debe registrar un usuario nuevo y retornar accessToken', async () => {
      // Arrange: el correo no existe aún en la BD
      prismaMock.usuario.findUnique.mockResolvedValue(null);
      prismaMock.usuario.create.mockResolvedValue({
        id: 'user-123',
        nombre: 'Noé Chavero',
        correo: 'noe@test.com',
        rol: 'USUARIO',
      });

      // Act
      const result = await service.register({
        nombre: 'Noé Chavero',
        correo: 'noe@test.com',
        contrasena: 'password123',
      });

      // Assert
      expect(result.accessToken).toBe('mock.jwt.token');
      expect(result.usuario.correo).toBe('noe@test.com');
      expect(prismaMock.usuario.create).toHaveBeenCalledTimes(1);
    });

    it('debe lanzar ConflictException (409) si el correo ya está registrado', async () => {
      // Arrange: simula que el correo YA existe en la BD
      prismaMock.usuario.findUnique.mockResolvedValue({
        id: 'existing-user',
        correo: 'noe@test.com',
      });

      // Act & Assert
      await expect(
        service.register({
          nombre: 'Noé Duplicado',
          correo: 'noe@test.com',
          contrasena: 'password123',
        }),
      ).rejects.toThrow(ConflictException);

      // Verifica que NO se intentó crear el usuario en BD
      expect(prismaMock.usuario.create).not.toHaveBeenCalled();
    });
  });

  // ─── Grupo 2: Inicio de Sesión ────────────────────────────────────────────

  describe('login()', () => {
    it('debe retornar accessToken con credenciales válidas', async () => {
      // Arrange: hash real de "password123"
      const hash = await bcrypt.hash('password123', 10);
      prismaMock.usuario.findUnique.mockResolvedValue({
        id: 'user-123',
        nombre: 'Noé Chavero',
        correo: 'noe@test.com',
        contrasena: hash,
        rol: 'USUARIO',
      });

      // Act
      const result = await service.login({
        correo: 'noe@test.com',
        contrasena: 'password123',
      });

      // Assert
      expect(result.accessToken).toBeDefined();
      expect(result.usuario.rol).toBe('USUARIO');
    });

    it('debe lanzar UnauthorizedException si el correo no existe', async () => {
      // Arrange: usuario no encontrado
      prismaMock.usuario.findUnique.mockResolvedValue(null);

      // Act & Assert
      await expect(
        service.login({ correo: 'noexiste@test.com', contrasena: 'cualquier' }),
      ).rejects.toThrow(UnauthorizedException);
    });

    it('debe lanzar UnauthorizedException si la contraseña es incorrecta', async () => {
      // Arrange: usuario existe pero contraseña no coincide
      const hash = await bcrypt.hash('contrasena_correcta', 10);
      prismaMock.usuario.findUnique.mockResolvedValue({
        id: 'user-123',
        nombre: 'Test',
        correo: 'test@test.com',
        contrasena: hash,
        rol: 'USUARIO',
      });

      // Act & Assert
      await expect(
        service.login({ correo: 'test@test.com', contrasena: 'contrasena_incorrecta' }),
      ).rejects.toThrow(UnauthorizedException);
    });
  });

  // ─── Grupo 3: Cambio de Rol ───────────────────────────────────────────────

  describe('updateRole()', () => {
    it('debe actualizar el rol de un usuario a ADMINISTRADOR', async () => {
      prismaMock.usuario.update.mockResolvedValue({
        id: 'user-123',
        nombre: 'Noé',
        correo: 'noe@test.com',
        rol: 'ADMINISTRADOR',
      });

      const result = await service.updateRole('user-123', 'ADMINISTRADOR');

      expect(result.rol).toBe('ADMINISTRADOR');
      expect(prismaMock.usuario.update).toHaveBeenCalledWith(
        expect.objectContaining({ where: { id: 'user-123' } }),
      );
    });

    it('debe lanzar ConflictException si el rol es inválido', async () => {
      await expect(
        service.updateRole('user-123', 'ROL_INVALIDO'),
      ).rejects.toThrow(ConflictException);
    });
  });
});
