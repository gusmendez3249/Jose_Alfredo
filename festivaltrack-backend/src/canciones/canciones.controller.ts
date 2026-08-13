import { Controller, Get, Post, Body } from '@nestjs/common';
import { CancionesService } from './canciones.service';

@Controller('canciones')
export class CancionesController {
  constructor(private readonly cancionesService: CancionesService) {}

  @Get()
  findAll() {
    return this.cancionesService.findAll();
  }

  @Post()
  create(@Body() body: any) {
    return this.cancionesService.create(body);
  }
}
