import { Test, TestingModule } from '@nestjs/testing';
import { BiografiasController } from './biografias.controller';
import { BiografiasService } from './biografias.service';

describe('BiografiasController', () => {
  let controller: BiografiasController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [BiografiasController],
      providers: [{ provide: BiografiasService, useValue: {} }],
    }).compile();

    controller = module.get<BiografiasController>(BiografiasController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
