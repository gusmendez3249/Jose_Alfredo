import { Test, TestingModule } from '@nestjs/testing';
import { CancionesController } from './canciones.controller';

describe('CancionesController', () => {
  let controller: CancionesController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [CancionesController],
    }).compile();

    controller = module.get<CancionesController>(CancionesController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
