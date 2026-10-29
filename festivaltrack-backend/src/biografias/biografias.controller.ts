import { Controller, Get, Post, Body, Param } from '@nestjs/common';
import { BiografiasService } from './biografias.service';
import { BiografiaAltaDto } from './factories/biografia.factory';

@Controller('biografias')
export class BiografiasController {
  constructor(private readonly biografiasService: BiografiasService) {}

  @Get()
  findAll() {
    return this.biografiasService.findAll();
  }

  @Post('alta')
  altaBiografia(@Body() body: BiografiaAltaDto) {
    return this.biografiasService.altaConFactory(body);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.biografiasService.findOne(id);
  }
}
