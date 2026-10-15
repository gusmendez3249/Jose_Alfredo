import { Test, TestingModule } from '@nestjs/testing';
import { CancionesService } from './canciones.service';
import { PrismaService } from '../prisma/prisma.service';

describe('CancionesService', () => {
  let service: CancionesService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [CancionesService, { provide: PrismaService, useValue: {} }],
    }).compile();

    service = module.get<CancionesService>(CancionesService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
