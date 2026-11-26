import { Test, TestingModule } from '@nestjs/testing';
import { AuthService } from './auth.service';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service';
import {
  ConflictException,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
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
      count: jest.fn(),
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
        service.login({
          correo: 'test@test.com',
          contrasena: 'contrasena_incorrecta',
        }),
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

    it('no deja que un admin se quite el rol a sí mismo', async () => {
      await expect(
        service.updateRole('admin-1', 'USUARIO', 'admin-1'),
      ).rejects.toThrow(
        'No puedes quitarte el rol de administrador a ti mismo',
      );
    });

    it('no deja al festival sin administradores', async () => {
      prismaMock.usuario.count.mockResolvedValue(1);
      await expect(
        service.updateRole('admin-2', 'USUARIO', 'admin-1'),
      ).rejects.toThrow('Debe quedar al menos un administrador');
    });

    it('quita el rol a otro admin si quedan más', async () => {
      prismaMock.usuario.count.mockResolvedValue(2);
      prismaMock.usuario.update.mockResolvedValue({
        id: 'admin-2',
        nombre: 'Otro',
        correo: 'otro@test.com',
        rol: 'USUARIO',
      });
      const result = await service.updateRole('admin-2', 'USUARIO', 'admin-1');
      expect(result.rol).toBe('USUARIO');
    });

    it('debe lanzar ConflictException si el rol es inválido', async () => {
      await expect(
        service.updateRole('user-123', 'ROL_INVALIDO'),
      ).rejects.toThrow(ConflictException);
    });
  });

  // ─── Grupo 4: Inicio de sesión en la TV con el celular ────────────────────

  describe('TV con el celular (tv-sync)', () => {
    it('la TV recibe el login de la cuenta que aprobó su código', async () => {
      const { codigo, tvToken } = service.crearCodigoTv();
      await expect(service.estadoTv(tvToken)).resolves.toEqual({
        estado: 'PENDIENTE',
      });

      expect(service.tvSync({ codigo, userId: 'user-123' }).status).toBe(
        'SUCCESS',
      );
      prismaMock.usuario.findUnique.mockResolvedValue({
        id: 'user-123',
        nombre: 'Omar',
        correo: 'omar@test.com',
        rol: 'USUARIO',
      });

      const respuesta = await service.estadoTv(tvToken);
      expect(respuesta).toEqual({
        estado: 'APROBADO',
        accessToken: 'mock.jwt.token',
        token: 'mock.jwt.token',
        usuario: {
          id: 'user-123',
          nombre: 'Omar',
          correo: 'omar@test.com',
          rol: 'USUARIO',
        },
      });
      expect(jwtMock.sign).toHaveBeenCalledWith({
        sub: 'user-123',
        correo: 'omar@test.com',
        rol: 'USUARIO',
      });
      // Se entrega una sola vez.
      await expect(service.estadoTv(tvToken)).resolves.toEqual({
        estado: 'EXPIRADO',
      });
    });

    it('un código que no existe responde 404', () => {
      service.crearCodigoTv();
      expect(() =>
        service.tvSync({ codigo: 'ZZZZ-ZZZZ', userId: 'user-123' }),
      ).toThrow(NotFoundException);
    });

    it('si la cuenta se borró antes de que la TV preguntara, no entra', async () => {
      const { codigo, tvToken } = service.crearCodigoTv();
      service.tvSync({ codigo, userId: 'borrado' });
      prismaMock.usuario.findUnique.mockResolvedValue(null);
      await expect(service.estadoTv(tvToken)).resolves.toEqual({
        estado: 'EXPIRADO',
      });
      expect(jwtMock.sign).not.toHaveBeenCalled();
    });
  });
});
