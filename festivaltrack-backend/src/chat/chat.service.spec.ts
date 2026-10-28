import { Test, TestingModule } from '@nestjs/testing';
import { NotFoundException, UnauthorizedException } from '@nestjs/common';
import { ChatService } from './chat.service';
import { PrismaService } from '../prisma/prisma.service';

/**
 * Pruebas unitarias de ChatService: el remitente y la marca de admin
 * deben salir del usuario autenticado, no del cliente.
 */
describe('ChatService - Pruebas Unitarias', () => {
  let service: ChatService;

  const prismaMock = {
    usuario: { findUnique: jest.fn() },
    evento: { findUnique: jest.fn() },
    chatStream: { create: jest.fn((args) => Promise.resolve(args.data)), findMany: jest.fn() },
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ChatService, { provide: PrismaService, useValue: prismaMock }],
    }).compile();
    service = module.get<ChatService>(ChatService);
    jest.clearAllMocks();
    prismaMock.evento.findUnique.mockResolvedValue({ id: 'EVT-001' });
  });

  it('usa el nombre del usuario autenticado y esAdmin=false para un usuario normal', async () => {
    prismaMock.usuario.findUnique.mockResolvedValue({ id: 'u1', nombre: 'Ana', rol: 'USUARIO', activo: true });
    const msg = await service.enviarMensaje('u1', { eventoId: 'EVT-001', mensaje: '  hola  ' });
    expect(msg).toEqual({ eventoId: 'EVT-001', usuarioNombre: 'Ana', mensaje: 'hola', esAdmin: false });
  });

  it('marca esAdmin=true solo cuando el rol es ADMINISTRADOR', async () => {
    prismaMock.usuario.findUnique.mockResolvedValue({ id: 'a1', nombre: 'Admin', rol: 'ADMINISTRADOR', activo: true });
    const msg = await service.enviarMensaje('a1', { eventoId: 'EVT-001', mensaje: 'Bienvenidos' });
    expect(msg.esAdmin).toBe(true);
  });

  it('rechaza usuarios inactivos', async () => {
    prismaMock.usuario.findUnique.mockResolvedValue({ id: 'u1', nombre: 'Ana', rol: 'USUARIO', activo: false });
    await expect(service.enviarMensaje('u1', { eventoId: 'EVT-001', mensaje: 'x' })).rejects.toThrow(UnauthorizedException);
  });

  it('responde 404 si el evento no existe', async () => {
    prismaMock.usuario.findUnique.mockResolvedValue({ id: 'u1', nombre: 'Ana', rol: 'USUARIO', activo: true });
    prismaMock.evento.findUnique.mockResolvedValue(null);
    await expect(service.enviarMensaje('u1', { eventoId: 'NOPE', mensaje: 'x' })).rejects.toThrow(NotFoundException);
    expect(prismaMock.chatStream.create).not.toHaveBeenCalled();
  });
});
