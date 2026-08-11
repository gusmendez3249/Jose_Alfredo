import { Test, TestingModule } from '@nestjs/testing';
import { BiografiasService } from './biografias.service';

describe('BiografiasService', () => {
  let service: BiografiasService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [BiografiasService],
    }).compile();

    service = module.get<BiografiasService>(BiografiasService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
