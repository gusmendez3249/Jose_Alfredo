import { Test, TestingModule } from '@nestjs/testing';
import { BiografiasService } from './biografias.service';
import { PrismaService } from '../prisma/prisma.service';

describe('BiografiasService', () => {
  let service: BiografiasService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [BiografiasService, { provide: PrismaService, useValue: {} }],
    }).compile();

    service = module.get<BiografiasService>(BiografiasService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
