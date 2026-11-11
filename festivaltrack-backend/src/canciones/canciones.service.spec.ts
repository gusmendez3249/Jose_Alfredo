import { BadRequestException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { CancionesService } from './canciones.service';
import { PrismaService } from '../prisma/prisma.service';

describe('CancionesService', () => {
  let service: CancionesService;

  // Cliente de Prisma simulado: cada prueba define lo que devuelve cada consulta.
  const tx = {
    transmision: { upsert: jest.fn() },
    cancionTransmision: { deleteMany: jest.fn(), createMany: jest.fn() },
  };
  const prisma = {
    evento: { findUnique: jest.fn() },
    cancion: { count: jest.fn() },
    cancionTransmision: { findMany: jest.fn() },
    $transaction: jest.fn((cb: (t: typeof tx) => Promise<unknown>) => cb(tx)),
  };

  beforeEach(async () => {
    jest.clearAllMocks();
    const module: TestingModule = await Test.createTestingModule({
      providers: [CancionesService, { provide: PrismaService, useValue: prisma }],
    }).compile();
    service = module.get<CancionesService>(CancionesService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('findByEvento', () => {
    it('devuelve solo las canciones publicadas del evento, en el orden del repertorio', async () => {
      prisma.cancionTransmision.findMany.mockResolvedValue([
        { cancion: { id: 'c2', titulo: 'Segunda' } },
        { cancion: { id: 'c1', titulo: 'Primera' } },
      ]);

      const res = await service.findByEvento('EVT-1');

      expect(res.map((c: any) => c.id)).toEqual(['c2', 'c1']);
      expect(prisma.cancionTransmision.findMany).toHaveBeenCalledWith({
        where: { transmision: { eventoId: 'EVT-1' }, cancion: { estado: 'PUBLICADA' } },
        orderBy: { orden: 'asc' },
        include: { cancion: true },
      });
    });

    it('un concierto sin repertorio devuelve lista vacía (no el catálogo completo)', async () => {
      prisma.cancionTransmision.findMany.mockResolvedValue([]);
      await expect(service.findByEvento('EVT-1')).resolves.toEqual([]);
    });
  });

  describe('setRepertorio', () => {
    it('rechaza una lista que no es un arreglo', async () => {
      await expect(service.setRepertorio('EVT-1', 'c1' as any)).rejects.toBeInstanceOf(BadRequestException);
    });

    it('rechaza un evento que no existe', async () => {
      prisma.evento.findUnique.mockResolvedValue(null);
      await expect(service.setRepertorio('NO-EXISTE', ['c1'])).rejects.toBeInstanceOf(NotFoundException);
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });

    it('rechaza canciones que no existen en el catálogo', async () => {
      prisma.evento.findUnique.mockResolvedValue({ id: 'EVT-1', nombre: 'Gala' });
      prisma.cancion.count.mockResolvedValue(1); // se pidieron 2, existe 1
      await expect(service.setRepertorio('EVT-1', ['c1', 'fantasma'])).rejects.toBeInstanceOf(BadRequestException);
      expect(prisma.$transaction).not.toHaveBeenCalled();
    });

    it('reemplaza el repertorio guardando el orden y sin canciones repetidas', async () => {
      prisma.evento.findUnique.mockResolvedValue({ id: 'EVT-1', nombre: 'Gala' });
      prisma.cancion.count.mockResolvedValue(3);
      tx.transmision.upsert.mockResolvedValue({ id: 'T-1' });
      prisma.cancionTransmision.findMany.mockResolvedValue([]);

      await service.setRepertorio('EVT-1', ['c3', 'c1', 'c3', 'c2']);

      // el id repetido se ignora y el orden de llegada se conserva
      expect(prisma.cancion.count).toHaveBeenCalledWith({ where: { id: { in: ['c3', 'c1', 'c2'] } } });
      expect(tx.transmision.upsert).toHaveBeenCalledWith({
        where: { eventoId: 'EVT-1' },
        create: { eventoId: 'EVT-1', titulo: 'Gala' },
        update: {},
      });
      expect(tx.cancionTransmision.deleteMany).toHaveBeenCalledWith({ where: { transmisionId: 'T-1' } });
      expect(tx.cancionTransmision.createMany).toHaveBeenCalledWith({
        data: [
          { cancionId: 'c3', transmisionId: 'T-1', orden: 0 },
          { cancionId: 'c1', transmisionId: 'T-1', orden: 1 },
          { cancionId: 'c2', transmisionId: 'T-1', orden: 2 },
        ],
      });
    });

    it('con una lista vacía deja el concierto sin canciones', async () => {
      prisma.evento.findUnique.mockResolvedValue({ id: 'EVT-1', nombre: 'Gala' });
      prisma.cancion.count.mockResolvedValue(0);
      tx.transmision.upsert.mockResolvedValue({ id: 'T-1' });
      prisma.cancionTransmision.findMany.mockResolvedValue([]);

      await expect(service.setRepertorio('EVT-1', [])).resolves.toEqual([]);
      expect(tx.cancionTransmision.deleteMany).toHaveBeenCalled();
      expect(tx.cancionTransmision.createMany).not.toHaveBeenCalled();
    });
  });
});
