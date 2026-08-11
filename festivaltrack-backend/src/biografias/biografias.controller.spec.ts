import { Test, TestingModule } from '@nestjs/testing';
import { BiografiasController } from './biografias.controller';

describe('BiografiasController', () => {
  let controller: BiografiasController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [BiografiasController],
    }).compile();

    controller = module.get<BiografiasController>(BiografiasController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
