import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException, NotFoundException } from '@nestjs/common';
import { BoletosService } from './boletos.service';
import { PrismaService } from '../prisma/prisma.service';

/**
 * Pruebas unitarias de la compra de boletos: el monto lo decide el servidor
 * y una compra nunca crea eventos ni usuarios como efecto secundario.
 */
describe('BoletosService.comprar - Pruebas Unitarias', () => {
  let service: BoletosService;

  const tx = {
    boleto: {
      create: jest.fn((args) => Promise.resolve({ id: 'b1', ...args.data })),
      update: jest.fn((args) => Promise.resolve({ id: 'b1', ...args.data })),
    },
    pago: { create: jest.fn() },
  };
  const prismaMock = {
    evento: { findUnique: jest.fn(), findFirst: jest.fn(), create: jest.fn() },
    usuario: { create: jest.fn() },
    administrador: { create: jest.fn() },
    $transaction: jest.fn((fn) => fn(tx)),
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [BoletosService, { provide: PrismaService, useValue: prismaMock }],
    }).compile();
    service = module.get<BoletosService>(BoletosService);
    jest.clearAllMocks();
  });

  it('calcula el monto con el precio oficial e ignora precioTotal del cliente', async () => {
    prismaMock.evento.findUnique.mockResolvedValue({ id: 'EVT-001', estado: 'PUBLICADO' });
    const dto: any = { eventoId: 'EVT-001', categoria: 'VIP', cantidad: 2, precioTotal: 1 };
    await service.comprar('u1', dto);
    expect(tx.boleto.create).toHaveBeenCalledWith({
      data: { eventoId: 'EVT-001', usuarioId: 'u1', categoria: 'VIP', precio: 9000 },
    });
    expect(tx.pago.create.mock.calls[0][0].data.monto).toBe(9000);
  });

  it('usa GENERAL × 1 por defecto', async () => {
    prismaMock.evento.findUnique.mockResolvedValue({ id: 'EVT-001', estado: 'EN_CURSO' });
    await service.comprar('u1', { eventoId: 'EVT-001' });
    expect(tx.boleto.create.mock.calls[0][0].data.precio).toBe(1200);
  });

  it('responde 404 si el evento no existe y no crea nada', async () => {
    prismaMock.evento.findUnique.mockResolvedValue(null);
    await expect(service.comprar('u1', { eventoId: 'NOPE' })).rejects.toThrow(NotFoundException);
    expect(prismaMock.evento.findFirst).not.toHaveBeenCalled();
    expect(prismaMock.evento.create).not.toHaveBeenCalled();
    expect(prismaMock.usuario.create).not.toHaveBeenCalled();
    expect(prismaMock.$transaction).not.toHaveBeenCalled();
  });

  it('responde 400 si el evento está cancelado', async () => {
    prismaMock.evento.findUnique.mockResolvedValue({ id: 'EVT-002', estado: 'CANCELADO' });
    await expect(service.comprar('u1', { eventoId: 'EVT-002' })).rejects.toThrow(BadRequestException);
    expect(prismaMock.$transaction).not.toHaveBeenCalled();
  });
});
