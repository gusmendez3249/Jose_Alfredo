import { Controller, Get, Param } from '@nestjs/common';
import { BiografiasService } from './biografias.service';

@Controller('biografias')
export class BiografiasController {
  constructor(private readonly biografiasService: BiografiasService) {}

  @Get()
  findAll() {
    return this.biografiasService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.biografiasService.findOne(id);
  }
}
